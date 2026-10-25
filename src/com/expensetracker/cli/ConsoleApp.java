package com.expensetracker.cli;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Currency;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.model.IncomeCategory;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.ExpenseService.TransactionFilter;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Console-based interactive CLI mode for terminal users with income & expense management,
 * currency selection, budgeting, and comprehensive analytics matching GUI reports.
 */
public class ConsoleApp {

    private final ExpenseService expenseService;
    private final Scanner scanner;

    public ConsoleApp(ExpenseService expenseService) {
        this.expenseService = expenseService;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("=================================================");
        System.out.println("   Welcome to Daily Expense & Income Tracker CLI ");
        System.out.println("=================================================");

        boolean running = true;
        while (running) {
            expenseService.reload();
            printMenu();
            System.out.print("Select an option (1-11): ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    addExpense();
                    break;
                case "2":
                    addIncome();
                    break;
                case "3":
                    viewAllTransactions();
                    break;
                case "4":
                    filterTransactions();
                    break;
                case "5":
                    updateTransaction();
                    break;
                case "6":
                    deleteTransaction();
                    break;
                case "7":
                    generateReports();
                    break;
                case "8":
                    manageBudget();
                    break;
                case "9":
                    exportOrImportCsv();
                    break;
                case "10":
                    changeCurrency();
                    break;
                case "11":
                    running = false;
                    System.out.println("\nThank you for using Daily Expense Tracker! Goodbye.");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 11.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n---------------- MAIN MENU ----------------");
        System.out.println("1. Log New Expense");
        System.out.println("2. Log New Income");
        System.out.println("3. View All Transactions (Income & Expenses)");
        System.out.println("4. Filter Transactions (Date Range / Category / Keyword / Type)");
        System.out.println("5. Edit / Update Transaction Record");
        System.out.println("6. Delete Transaction Record");
        System.out.println("7. Generate Summary Reports & Analytics");
        System.out.println("8. Set & View Monthly Spending Budget");
        System.out.println("9. CSV File Import / Export");
        System.out.println("10. Select System Currency [Active: " + expenseService.getCurrency().getDisplayName() + "]");
        System.out.println("11. Exit Application");
        System.out.println("-------------------------------------------");
    }

    private void addExpense() {
        System.out.println("\n--- Log New Expense ---");
        double amount = readDouble("Enter amount (" + expenseService.getCurrency().getSymbol() + "): ");
        
        System.out.println("Expense Categories:");
        ExpenseCategory[] categories = ExpenseCategory.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("%d. %s%n", i + 1, categories[i].getDisplayName());
        }
        int catChoice = readInt("Select category (1-" + categories.length + "): ", 1, categories.length);
        String category = categories[catChoice - 1].getDisplayName();

        LocalDate date = readDate("Enter date (YYYY-MM-DD) or press ENTER for today (" + LocalDate.now() + "): ");
        System.out.print("Enter notes/description (optional): ");
        String notes = scanner.nextLine().trim();

        Expense expense = new Expense(amount, category, date, notes);
        expenseService.addExpense(expense);
        System.out.println("SUCCESS: Expense recorded with ID " + expense.getId());

        checkBudgetAlert(YearMonth.from(date));
    }

    private void addIncome() {
        System.out.println("\n--- Log New Income ---");
        double amount = readDouble("Enter income amount (" + expenseService.getCurrency().getSymbol() + "): ");
        
        System.out.println("Income Sources/Categories:");
        IncomeCategory[] categories = IncomeCategory.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("%d. %s%n", i + 1, categories[i].getDisplayName());
        }
        int catChoice = readInt("Select category (1-" + categories.length + "): ", 1, categories.length);
        String category = categories[catChoice - 1].getDisplayName();

        LocalDate date = readDate("Enter date (YYYY-MM-DD) or press ENTER for today (" + LocalDate.now() + "): ");
        System.out.print("Enter notes/description (optional): ");
        String notes = scanner.nextLine().trim();

        Expense income = new Expense(amount, category, date, notes);
        expenseService.addIncome(income);
        System.out.println("SUCCESS: Income recorded with ID " + income.getId());
    }

    private void viewAllTransactions() {
        System.out.println("\n--- All Recorded Transactions ---");
        System.out.println("1. View All (Income & Expenses)");
        System.out.println("2. View Expenses Only");
        System.out.println("3. View Income Only");
        System.out.print("Select view option (1-3) [Default: 1]: ");
        String choice = scanner.nextLine().trim();

        List<Expense> list;
        if ("2".equals(choice)) {
            list = expenseService.getAllExpenses();
            System.out.println("\n[Expenses Only]");
        } else if ("3".equals(choice)) {
            list = expenseService.getAllIncomes();
            System.out.println("\n[Income Only]");
        } else {
            list = expenseService.getAllTransactions();
            System.out.println("\n[All Transactions]");
        }
        printExpenseTable(list);
    }

    private void filterTransactions() {
        System.out.println("\n--- Filter Transaction History ---");
        System.out.println("Leave fields blank to skip filter.");

        System.out.println("Type Filter:");
        System.out.println("1. All Transactions");
        System.out.println("2. Expenses Only");
        System.out.println("3. Income Only");
        int typeChoice = readInt("Select type (1-3) [Default: 1]: ", 1, 3);
        TransactionFilter typeFilter = TransactionFilter.ALL;
        if (typeChoice == 2) typeFilter = TransactionFilter.EXPENSE;
        if (typeChoice == 3) typeFilter = TransactionFilter.INCOME;

        LocalDate fromDate = readOptionalDate("From date (YYYY-MM-DD): ");
        LocalDate toDate = readOptionalDate("To date (YYYY-MM-DD): ");

        System.out.print("Category (or ENTER for all): ");
        String cat = scanner.nextLine().trim();
        if (cat.isEmpty()) cat = "All Categories";

        System.out.print("Search Keyword (or ENTER for none): ");
        String kw = scanner.nextLine().trim();

        List<Expense> filtered = expenseService.filterTransactions(fromDate, toDate, cat, kw, typeFilter);
        printExpenseTable(filtered);
    }

    private void updateTransaction() {
        System.out.println("\n--- Edit / Update Transaction Record ---");
        List<Expense> allTx = expenseService.getAllTransactions();
        if (allTx.isEmpty()) {
            System.out.println("No recorded transactions available to edit.");
            return;
        }

        System.out.print("Enter Transaction ID to edit (or press ENTER to list transactions): ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty() || "?".equals(id) || "list".equalsIgnoreCase(id)) {
            printExpenseTable(allTx);
            System.out.print("Enter Transaction ID to edit: ");
            id = scanner.nextLine().trim();
        }

        Expense existing = expenseService.getExpenseById(id);
        if (existing == null) {
            System.out.println("ERROR: No transaction found with ID: " + id);
            return;
        }

        String typeStr = existing.isIncome() ? "INCOME" : "EXPENSE";
        System.out.printf("%nCurrent Record [%s]: ID=%s | Category=%s | Date=%s | Amount=%s | Notes=%s%n",
                typeStr, existing.getId(), existing.getCategory(), existing.getDate(),
                expenseService.formatAmount(existing.getAmount()), existing.getNotes());

        System.out.print("Transaction Type (1=Expense, 2=Income) [Current: " + (existing.isIncome() ? "Income" : "Expense") + "]: ");
        String typeChoice = scanner.nextLine().trim();
        boolean isIncome = existing.isIncome();
        if ("1".equals(typeChoice)) isIncome = false;
        else if ("2".equals(typeChoice)) isIncome = true;

        double amount = readDoubleWithDefault("Enter new amount (" + expenseService.getCurrency().getSymbol() + ") [" + existing.getAmount() + "]: ", existing.getAmount());

        String category = existing.getCategory();
        System.out.println("Select Category Option:");
        if (isIncome) {
            IncomeCategory[] categories = IncomeCategory.values();
            for (int i = 0; i < categories.length; i++) {
                System.out.printf("%d. %s%n", i + 1, categories[i].getDisplayName());
            }
            System.out.printf("%d. Keep current [%s]%n", categories.length + 1, existing.getCategory());
            System.out.printf("%d. Custom Category Name%n", categories.length + 2);
            int catChoice = readInt("Choice (1-" + (categories.length + 2) + ") [Default: " + (categories.length + 1) + "]: ", 1, categories.length + 2);
            if (catChoice <= categories.length) {
                category = categories[catChoice - 1].getDisplayName();
            } else if (catChoice == categories.length + 2) {
                System.out.print("Enter custom category name: ");
                String customCat = scanner.nextLine().trim();
                if (!customCat.isEmpty()) category = customCat;
            }
        } else {
            ExpenseCategory[] categories = ExpenseCategory.values();
            for (int i = 0; i < categories.length; i++) {
                System.out.printf("%d. %s%n", i + 1, categories[i].getDisplayName());
            }
            System.out.printf("%d. Keep current [%s]%n", categories.length + 1, existing.getCategory());
            System.out.printf("%d. Custom Category Name%n", categories.length + 2);
            int catChoice = readInt("Choice (1-" + (categories.length + 2) + ") [Default: " + (categories.length + 1) + "]: ", 1, categories.length + 2);
            if (catChoice <= categories.length) {
                category = categories[catChoice - 1].getDisplayName();
            } else if (catChoice == categories.length + 2) {
                System.out.print("Enter custom category name: ");
                String customCat = scanner.nextLine().trim();
                if (!customCat.isEmpty()) category = customCat;
            }
        }

        LocalDate date = readDateWithDefault("Enter new date (YYYY-MM-DD) [" + existing.getDate() + "]: ", existing.getDate());

        System.out.print("Enter new notes [" + existing.getNotes() + "]: ");
        String notes = scanner.nextLine().trim();
        if (notes.isEmpty()) notes = existing.getNotes();

        boolean updated = expenseService.updateTransaction(id, amount, category, date, notes, isIncome);
        if (updated) {
            System.out.println("SUCCESS: Transaction ID " + id + " updated successfully.");
        } else {
            System.out.println("ERROR: Failed to update transaction.");
        }
    }

    private void deleteTransaction() {
        System.out.println("\n--- Delete Transaction Record(s) ---");
        List<Expense> allTx = expenseService.getAllTransactions();
        if (allTx.isEmpty()) {
            System.out.println("No recorded transactions available to delete.");
            return;
        }

        System.out.print("Enter Transaction ID(s) to delete (comma-separated, or press ENTER to list): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty() || "?".equals(input) || "list".equalsIgnoreCase(input)) {
            printExpenseTable(allTx);
            System.out.print("Enter Transaction ID(s) to delete (comma-separated): ");
            input = scanner.nextLine().trim();
        }

        if (input.isEmpty()) {
            System.out.println("No ID entered. Operation canceled.");
            return;
        }

        String[] rawIds = input.split("[,\\s]+");
        List<String> validIds = new java.util.ArrayList<>();
        for (String rawId : rawIds) {
            String trimmed = rawId.trim();
            if (!trimmed.isEmpty()) {
                Expense existing = expenseService.getExpenseById(trimmed);
                if (existing != null) {
                    validIds.add(trimmed);
                } else {
                    System.out.println("WARNING: No transaction found with ID: " + trimmed);
                }
            }
        }

        if (validIds.isEmpty()) {
            System.out.println("ERROR: No valid transaction IDs found to delete.");
            return;
        }

        if (validIds.size() == 1) {
            String id = validIds.get(0);
            Expense existing = expenseService.getExpenseById(id);
            String typeStr = existing.isIncome() ? "INCOME" : "EXPENSE";
            System.out.printf("Are you sure you want to delete [%s] ID: %s of %s (%s - %s)? (y/n): ",
                    typeStr, existing.getId(), expenseService.formatAmount(existing.getAmount()), existing.getCategory(), existing.getDate());
        } else {
            System.out.printf("Are you sure you want to delete %d selected transactions? (y/n): ", validIds.size());
        }

        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("y")) {
            for (String id : validIds) {
                expenseService.deleteTransaction(id);
            }
            System.out.printf("SUCCESS: %d transaction(s) permanently deleted.%n", validIds.size());
        } else {
            System.out.println("Operation canceled.");
        }
    }

    private void generateReports() {
        System.out.println("\n=================================================");
        System.out.println("       SUMMARY REPORTS & FINANCIAL ANALYTICS     ");
        System.out.println("=================================================");
        
        double totalIncome = expenseService.getTotalIncome();
        double totalSpend = expenseService.getTotalSpending();
        double netBalance = expenseService.getCurrentBalance();

        System.out.printf("Total All-Time Income:      %16s%n", expenseService.formatAmount(totalIncome));
        System.out.printf("Total All-Time Expenditure: %16s%n", expenseService.formatAmount(totalSpend));
        System.out.printf("Net Overall Balance:        %16s%n", expenseService.formatAmount(netBalance));

        YearMonth currentMonth = YearMonth.now();
        double monthSpend = expenseService.getMonthlySpending(currentMonth);
        double monthIncome = expenseService.getMonthlyIncome(currentMonth);
        System.out.printf("%nCurrent Month (%s) Income:   %s%n", currentMonth, expenseService.formatAmount(monthIncome));
        System.out.printf("Current Month (%s) Expenses: %s%n", currentMonth, expenseService.formatAmount(monthSpend));

        Expense maxExpense = expenseService.getHighestExpense();
        if (maxExpense != null) {
            System.out.printf("Highest Single Expense:     %s (%s on %s)%n", expenseService.formatAmount(maxExpense.getAmount()), maxExpense.getCategory(), maxExpense.getDate());
        }

        System.out.println("\n--- Spending Breakdown by Category (Expenses) ---");
        Map<String, Double> expCatMap = expenseService.getSpendingByCategory(null);
        if (expCatMap.isEmpty()) {
            System.out.println("No expense records found.");
        } else {
            for (Map.Entry<String, Double> entry : expCatMap.entrySet()) {
                double pct = totalSpend > 0 ? (entry.getValue() / totalSpend) * 100 : 0;
                System.out.printf(" - %-25s: %12s (%5.1f%%)%n", entry.getKey(), expenseService.formatAmount(entry.getValue()), pct);
            }
        }

        System.out.println("\n--- Income Breakdown by Source ---");
        Map<String, Double> incCatMap = expenseService.getIncomeBySource(null);
        if (incCatMap.isEmpty()) {
            System.out.println("No income records found.");
        } else {
            for (Map.Entry<String, Double> entry : incCatMap.entrySet()) {
                double pct = totalIncome > 0 ? (entry.getValue() / totalIncome) * 100 : 0;
                System.out.printf(" - %-25s: %12s (%5.1f%%)%n", entry.getKey(), expenseService.formatAmount(entry.getValue()), pct);
            }
        }

        System.out.println("\n--- Monthly History Overview ---");
        Map<YearMonth, Double> expMonthly = expenseService.getMonthlySpendingHistory();
        Map<YearMonth, Double> incMonthly = expenseService.getMonthlyIncomeHistory();
        
        java.util.Set<YearMonth> allMonths = new java.util.TreeSet<>(expMonthly.keySet());
        allMonths.addAll(incMonthly.keySet());

        if (allMonths.isEmpty()) {
            System.out.println("No transaction history available.");
        } else {
            System.out.println("+------------+--------------+--------------+--------------+-----------------------+");
            System.out.println("| Month      | Income       | Expenditure  | Net Change   | Budget Limit          |");
            System.out.println("+------------+--------------+--------------+--------------+-----------------------+");
            for (YearMonth ym : allMonths) {
                double inc = incMonthly.getOrDefault(ym, 0.0);
                double exp = expMonthly.getOrDefault(ym, 0.0);
                double net = inc - exp;
                Budget budget = expenseService.getMonthlyBudget(ym);
                String budgetStr = budget.getMonthlyLimit() > 0 ? expenseService.formatAmount(budget.getMonthlyLimit()) : "Not set";
                System.out.printf("| %-10s | %12s | %12s | %12s | %-21s |%n",
                        ym.toString(), expenseService.formatAmount(inc), expenseService.formatAmount(exp),
                        expenseService.formatAmount(net), budgetStr);
            }
            System.out.println("+------------+--------------+--------------+--------------+-----------------------+");
        }
    }

    private void manageBudget() {
        System.out.println("\n--- Monthly Budget Management ---");
        YearMonth currentMonth = YearMonth.now();
        Budget currentBudget = expenseService.getMonthlyBudget(currentMonth);
        double spent = expenseService.getMonthlySpending(currentMonth);

        System.out.printf("Current Month:        %s%n", currentMonth);
        System.out.printf("Current Spending:     %s%n", expenseService.formatAmount(spent));
        System.out.printf("Current Budget Limit: %s%n", expenseService.formatAmount(currentBudget.getMonthlyLimit()));

        checkBudgetAlert(currentMonth);

        System.out.print("\nDo you want to set/update a budget limit? (y/n): ");
        String choice = scanner.nextLine().trim();
        if (choice.equalsIgnoreCase("y")) {
            double newLimit = readDouble("Enter monthly budget limit (" + expenseService.getCurrency().getSymbol() + "): ");
            expenseService.setMonthlyBudget(currentMonth, newLimit);
            System.out.printf("SUCCESS: Budget limit for %s set to %s%n", currentMonth, expenseService.formatAmount(newLimit));
            checkBudgetAlert(currentMonth);
        }
    }

    private void checkBudgetAlert(YearMonth ym) {
        ExpenseService.BudgetStatus status = expenseService.getBudgetStatus(ym);
        double spent = expenseService.getMonthlySpending(ym);
        double limit = expenseService.getMonthlyBudget(ym).getMonthlyLimit();

        if (status == ExpenseService.BudgetStatus.EXCEEDED) {
            System.out.printf("%n[ALERT] BUDGET OVERRUN! You spent %s of %s monthly budget for %s (Over by %s)!%n",
                    expenseService.formatAmount(spent), expenseService.formatAmount(limit), ym, expenseService.formatAmount(spent - limit));
        } else if (status == ExpenseService.BudgetStatus.WARNING) {
            System.out.printf("%n[WARNING] Approaching budget limit! Spent %s of %s (%.1f%% used).%n",
                    expenseService.formatAmount(spent), expenseService.formatAmount(limit), (spent / limit) * 100);
        }
    }

    private void changeCurrency() {
        System.out.println("\n--- Select System Currency ---");
        Currency[] currencies = Currency.values();
        for (int i = 0; i < currencies.length; i++) {
            System.out.printf("%d. %s (%s)%n", i + 1, currencies[i].getDisplayName(), currencies[i].getSymbol());
        }
        int choice = readInt("Select currency (1-" + currencies.length + "): ", 1, currencies.length);
        Currency selected = currencies[choice - 1];
        expenseService.setCurrency(selected);
        System.out.println("SUCCESS: Active currency updated to " + selected.getDisplayName() + " (" + selected.getSymbol() + ").");
    }

    private void exportOrImportCsv() {
        System.out.println("\n--- CSV Export / Import ---");
        System.out.println("1. Export all transactions to CSV");
        System.out.println("2. Import transactions from CSV file");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            System.out.print("Enter output filename [expenses.csv]: ");
            String filename = scanner.nextLine().trim();
            if (filename.isEmpty()) filename = "expenses.csv";
            File file = new File(filename);
            try {
                expenseService.exportToCsv(file, null);
                System.out.println("SUCCESS: Exported " + expenseService.getAllTransactions().size() + " transactions to " + file.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("ERROR exporting CSV: " + e.getMessage());
            }
        } else if (choice.equals("2")) {
            System.out.print("Enter input CSV file path: ");
            String path = scanner.nextLine().trim();
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("ERROR: File does not exist: " + path);
                return;
            }
            try {
                int count = expenseService.importFromCsv(file);
                System.out.println("SUCCESS: Imported " + count + " new transaction entries.");
            } catch (Exception e) {
                System.out.println("ERROR importing CSV: " + e.getMessage());
            }
        }
    }

    private void printExpenseTable(List<Expense> list) {
        if (list == null || list.isEmpty()) {
            System.out.println("No transaction records found.");
            return;
        }

        String symbol = expenseService.getCurrency().getSymbol();
        String amtColHeader = String.format("Amount (%s)", symbol);

        System.out.println("+----------+-----------+------------+-----------------------+--------------+-----------------------------------+");
        System.out.printf("| ID       | Type      | Date       | Category              | %-12s | Description                       |%n", amtColHeader);
        System.out.println("+----------+-----------+------------+-----------------------+--------------+-----------------------------------+");

        double totalExp = 0.0;
        double totalInc = 0.0;
        for (Expense e : list) {
            String typeTag = e.isIncome() ? "INCOME " : "EXPENSE";
            System.out.printf("| %-8s | %-9s | %-10s | %-21s | %12s | %-33s |%n",
                    e.getId(),
                    typeTag,
                    e.getDate(),
                    truncate(e.getCategory(), 21),
                    expenseService.formatAmount(e.getAmount()),
                    truncate(e.getNotes(), 33));
            if (e.isIncome()) {
                totalInc += e.getAmount();
            } else {
                totalExp += e.getAmount();
            }
        }
        System.out.println("+----------+-----------+------------+-----------------------+--------------+-----------------------------------+");
        System.out.printf("| SUMMARY (%d records): Total Income: %s | Total Expenses: %s | Net: %s%n",
                list.size(), expenseService.formatAmount(totalInc), expenseService.formatAmount(totalExp),
                expenseService.formatAmount(totalInc - totalExp));
        System.out.println("+------------------------------------------------------------------------------------------------------+");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val > 0) return val;
                System.out.println("Amount must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric input. Try again.");
            }
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) return val;
            } catch (NumberFormatException ignored) {}
            System.out.printf("Please enter an integer between %d and %d.%n", min, max);
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return LocalDate.now();
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD (e.g. 2026-08-01).");
            }
        }
    }

    private double readDoubleWithDefault(String prompt, double defaultVal) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return defaultVal;
            try {
                double val = Double.parseDouble(input);
                if (val > 0) return val;
                System.out.println("Amount must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric input. Try again.");
            }
        }
    }

    private LocalDate readDateWithDefault(String prompt, LocalDate defaultDate) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return defaultDate;
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD.");
            }
        }
    }

    private LocalDate readOptionalDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return null;
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD or leave blank.");
            }
        }
    }
}
