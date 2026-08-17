package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Currency;
import com.expensetracker.model.Expense;
import com.expensetracker.storage.CsvExporter;
import com.expensetracker.storage.StorageManager;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Service providing core business logic, filtering, aggregations, currency management, and budget tracking.
 * Now also handles income entries (Expense objects with isIncome=true) alongside expense entries.
 */
public class ExpenseService {

    public enum BudgetStatus {
        OK,
        WARNING, // Spent >= 80% of budget
        EXCEEDED // Spent > 100% of budget
    }

    /** Filter type for transaction queries. */
    public enum TransactionFilter {
        ALL,
        EXPENSE,
        INCOME
    }

    private final StorageManager storageManager;
    /** Unified list — holds both expenses (isIncome=false) and income entries (isIncome=true). */
    private final List<Expense> expenses;
    private final Map<YearMonth, Budget> budgets;
    private Currency currency;

    public ExpenseService() {
        this.storageManager = new StorageManager();
        this.expenses = new ArrayList<>(storageManager.loadExpenses());
        this.budgets = new HashMap<>(storageManager.loadBudgets());
        this.currency = storageManager.loadCurrency();
    }

    public ExpenseService(StorageManager storageManager) {
        this.storageManager = storageManager;
        this.expenses = new ArrayList<>(storageManager.loadExpenses());
        this.budgets = new HashMap<>(storageManager.loadBudgets());
        this.currency = storageManager.loadCurrency();
    }

    /**
     * Reloads expenses, budgets, and settings from disk storage.
     */
    public synchronized void reload() {
        this.expenses.clear();
        this.expenses.addAll(storageManager.loadExpenses());
        this.budgets.clear();
        this.budgets.putAll(storageManager.loadBudgets());
        this.currency = storageManager.loadCurrency();
    }


    // --- Currency Management ---

    public synchronized Currency getCurrency() {
        return currency;
    }

    public synchronized void setCurrency(Currency currency) {
        if (currency != null) {
            this.currency = currency;
            storageManager.saveCurrency(currency);
        }
    }

    public synchronized String formatAmount(double amount) {
        return currency.format(amount);
    }

    // --- Expense CRUD Operations ---

    public synchronized void addExpense(Expense expense) {
        if (expense != null) {
            expense.setIncome(false); // ensure flag is correct
            expenses.add(expense);
            sortByDate();
            saveData();
        }
    }

    public synchronized boolean updateTransaction(String id, double newAmount, String newCategory, LocalDate newDate, String newNotes, boolean isIncome) {
        for (Expense e : expenses) {
            if (e.getId().equals(id)) {
                e.setAmount(newAmount);
                e.setCategory(newCategory);
                e.setDate(newDate);
                e.setNotes(newNotes);
                e.setIncome(isIncome);
                sortByDate();
                saveData();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean updateExpense(String id, double newAmount, String newCategory, LocalDate newDate, String newNotes) {
        Expense existing = getExpenseById(id);
        boolean isIncome = existing != null && existing.isIncome();
        return updateTransaction(id, newAmount, newCategory, newDate, newNotes, isIncome);
    }

    public synchronized boolean updateIncome(String id, double newAmount, String newCategory, LocalDate newDate, String newNotes) {
        return updateTransaction(id, newAmount, newCategory, newDate, newNotes, true);
    }

    public synchronized boolean deleteExpense(String id) {
        return deleteTransaction(id);
    }

    public synchronized boolean deleteIncome(String id) {
        return deleteTransaction(id);
    }

    public synchronized boolean deleteTransaction(String id) {
        boolean removed = expenses.removeIf(e -> e.getId().equals(id));
        if (removed) {
            saveData();
        }
        return removed;
    }

    public synchronized List<Expense> getAllExpenses() {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (!e.isIncome()) result.add(e);
        }
        return result;
    }

    public synchronized Expense getExpenseById(String id) {
        for (Expense e : expenses) {
            if (e.getId().equals(id)) return e;
        }
        return null;
    }

    // --- Income CRUD Operations ---

    public synchronized void addIncome(Expense income) {
        if (income != null) {
            income.setIncome(true); // ensure flag is correct
            expenses.add(income);
            sortByDate();
            saveData();
        }
    }

    public synchronized List<Expense> getAllIncomes() {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.isIncome()) result.add(e);
        }
        return result;
    }

    // --- All Transactions (income + expenses combined) ---

    public synchronized List<Expense> getAllTransactions() {
        return new ArrayList<>(expenses);
    }

    // --- Filtering ---

    public synchronized List<Expense> filterExpenses(LocalDate startDate, LocalDate endDate, String category, String searchKeyword) {
        return filterTransactions(startDate, endDate, category, searchKeyword, TransactionFilter.EXPENSE);
    }

    public synchronized List<Expense> filterTransactions(LocalDate startDate, LocalDate endDate, String category,
                                                          String searchKeyword, TransactionFilter filter) {
        List<Expense> filtered = new ArrayList<>();
        for (Expense e : expenses) {
            // Type filter
            if (filter == TransactionFilter.EXPENSE && e.isIncome()) continue;
            if (filter == TransactionFilter.INCOME && !e.isIncome()) continue;

            if (startDate != null && e.getDate().isBefore(startDate)) continue;
            if (endDate != null && e.getDate().isAfter(endDate)) continue;
            if (category != null && !category.equalsIgnoreCase("All Categories") && !e.getCategory().equalsIgnoreCase(category)) {
                continue;
            }
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String kw = searchKeyword.toLowerCase().trim();
                boolean matchNotes = e.getNotes() != null && e.getNotes().toLowerCase().contains(kw);
                boolean matchCategory = e.getCategory() != null && e.getCategory().toLowerCase().contains(kw);
                boolean matchId = e.getId() != null && e.getId().toLowerCase().contains(kw);
                boolean matchAmount = String.valueOf(e.getAmount()).contains(kw);
                if (!matchNotes && !matchCategory && !matchId && !matchAmount) {
                    continue;
                }
            }
            filtered.add(e);
        }
        return filtered;
    }

    // --- Budget Management ---

    public synchronized void setMonthlyBudget(YearMonth yearMonth, double limit) {
        budgets.put(yearMonth, new Budget(yearMonth, limit));
        saveData();
    }

    public synchronized Budget getMonthlyBudget(YearMonth yearMonth) {
        return budgets.getOrDefault(yearMonth, new Budget(yearMonth, 0.0));
    }

    public synchronized double getMonthlySpending(YearMonth yearMonth) {
        double total = 0.0;
        for (Expense e : expenses) {
            if (!e.isIncome() && YearMonth.from(e.getDate()).equals(yearMonth)) {
                total += e.getAmount();
            }
        }
        return total;
    }

    public synchronized BudgetStatus getBudgetStatus(YearMonth yearMonth) {
        Budget budget = getMonthlyBudget(yearMonth);
        if (budget.getMonthlyLimit() <= 0) {
            return BudgetStatus.OK;
        }
        double spent = getMonthlySpending(yearMonth);
        double ratio = spent / budget.getMonthlyLimit();
        if (ratio >= 1.0) {
            return BudgetStatus.EXCEEDED;
        } else if (ratio >= 0.8) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.OK;
    }

    // --- Aggregations & Analytics (Expenses) ---

    public synchronized double getTotalSpending() {
        double total = 0.0;
        for (Expense e : expenses) {
            if (!e.isIncome()) total += e.getAmount();
        }
        return total;
    }

    public synchronized Map<String, Double> getSpendingByCategory(List<Expense> expenseList) {
        Map<String, Double> categoryMap = new HashMap<>();
        List<Expense> targetList = expenseList != null ? expenseList : getAllExpenses();
        for (Expense e : targetList) {
            if (!e.isIncome()) {
                categoryMap.put(e.getCategory(), categoryMap.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
            }
        }
        return categoryMap;
    }

    public synchronized Map<LocalDate, Double> getDailySpending(YearMonth yearMonth) {
        Map<LocalDate, Double> dailyMap = new TreeMap<>();
        for (Expense e : expenses) {
            if (!e.isIncome() && (yearMonth == null || YearMonth.from(e.getDate()).equals(yearMonth))) {
                dailyMap.put(e.getDate(), dailyMap.getOrDefault(e.getDate(), 0.0) + e.getAmount());
            }
        }
        return dailyMap;
    }

    public synchronized Map<YearMonth, Double> getMonthlySpendingHistory() {
        Map<YearMonth, Double> monthlyMap = new TreeMap<>();
        for (Expense e : expenses) {
            if (!e.isIncome()) {
                YearMonth ym = YearMonth.from(e.getDate());
                monthlyMap.put(ym, monthlyMap.getOrDefault(ym, 0.0) + e.getAmount());
            }
        }
        return monthlyMap;
    }

    public synchronized Expense getHighestExpense() {
        Expense max = null;
        for (Expense e : expenses) {
            if (!e.isIncome() && (max == null || e.getAmount() > max.getAmount())) {
                max = e;
            }
        }
        return max;
    }

    public synchronized String getTopCategory() {
        Map<String, Double> catMap = getSpendingByCategory(null);
        String topCat = "None";
        double maxAmt = 0.0;
        for (Map.Entry<String, Double> entry : catMap.entrySet()) {
            if (entry.getValue() > maxAmt) {
                maxAmt = entry.getValue();
                topCat = entry.getKey();
            }
        }
        return topCat;
    }

    // --- Aggregations & Analytics (Income) ---

    public synchronized double getTotalIncome() {
        double total = 0.0;
        for (Expense e : expenses) {
            if (e.isIncome()) total += e.getAmount();
        }
        return total;
    }

    public synchronized double getCurrentBalance() {
        return getTotalIncome() - getTotalSpending();
    }

    public synchronized double getMonthlyIncome(YearMonth yearMonth) {
        double total = 0.0;
        for (Expense e : expenses) {
            if (e.isIncome() && YearMonth.from(e.getDate()).equals(yearMonth)) {
                total += e.getAmount();
            }
        }
        return total;
    }

    public synchronized Map<String, Double> getIncomeBySource(List<Expense> incomeList) {
        Map<String, Double> sourceMap = new HashMap<>();
        List<Expense> targetList = incomeList != null ? incomeList : getAllIncomes();
        for (Expense e : targetList) {
            if (e.isIncome()) {
                sourceMap.put(e.getCategory(), sourceMap.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
            }
        }
        return sourceMap;
    }

    public synchronized Map<YearMonth, Double> getMonthlyIncomeHistory() {
        Map<YearMonth, Double> monthlyMap = new TreeMap<>();
        for (Expense e : expenses) {
            if (e.isIncome()) {
                YearMonth ym = YearMonth.from(e.getDate());
                monthlyMap.put(ym, monthlyMap.getOrDefault(ym, 0.0) + e.getAmount());
            }
        }
        return monthlyMap;
    }

    // --- Import / Export ---

    public void exportToCsv(File file, List<Expense> targetList) throws IOException {
        CsvExporter.exportToCsv(file, targetList != null ? targetList : getAllExpenses());
    }

    public synchronized int importFromCsv(File file) throws IOException {
        List<Expense> imported = CsvExporter.importFromCsv(file);
        int addedCount = 0;
        for (Expense e : imported) {
            if (getExpenseById(e.getId()) == null) {
                expenses.add(e);
                addedCount++;
            }
        }
        sortByDate();
        saveData();
        return addedCount;
    }

    // --- Helpers ---

    private void sortByDate() {
        expenses.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate())); // Descending (newest first)
    }

    private void saveData() {
        storageManager.saveExpenses(expenses);
        storageManager.saveBudgets(budgets);
        storageManager.saveCurrency(currency);
    }
}
