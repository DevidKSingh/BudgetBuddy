package com.expensetracker.test;

import com.expensetracker.model.Currency;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.storage.StorageManager;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Automated functional tests verifying all core requirements.
 */
public class ExpenseTrackerTest {

    public static void main(String[] args) {
        System.out.println("Starting Automated Tests for Daily Expense Tracker System...\n");

        File tempTestDir = new File(System.getProperty("java.io.tmpdir"), "test_expense_tracker_" + System.currentTimeMillis());
        StorageManager storageManager = new StorageManager(tempTestDir);
        ExpenseService service = new ExpenseService(storageManager);

        try {
            // Test 1: Add Expenses
            System.out.print("[TEST 1] Adding expenses... ");
            Expense e1 = new Expense(45.50, ExpenseCategory.FOOD.getDisplayName(), LocalDate.of(2026, 8, 1), "Lunch with team");
            Expense e2 = new Expense(120.00, ExpenseCategory.UTILITIES.getDisplayName(), LocalDate.of(2026, 8, 2), "Electricity bill");
            Expense e3 = new Expense(25.00, ExpenseCategory.TRANSPORT.getDisplayName(), LocalDate.of(2026, 8, 3), "Cab fare");
            Expense e4 = new Expense(300.00, ExpenseCategory.SHOPPING.getDisplayName(), LocalDate.of(2026, 7, 25), "New clothes");

            service.addExpense(e1);
            service.addExpense(e2);
            service.addExpense(e3);
            service.addExpense(e4);

            assertCondition(service.getAllExpenses().size() == 4, "Expected 4 expenses recorded");
            System.out.println("PASSED.");

            // Test 2: Calculate Spending & Aggregations
            System.out.print("[TEST 2] Total & Monthly Aggregations... ");
            double total = service.getTotalSpending();
            assertCondition(Math.abs(total - 490.50) < 0.01, "Expected total spend 490.50, got " + total);

            double augSpend = service.getMonthlySpending(YearMonth.of(2026, 8));
            assertCondition(Math.abs(augSpend - 190.50) < 0.01, "Expected August spend 190.50, got " + augSpend);
            System.out.println("PASSED.");

            // Test 3: Filtering by Date & Category & Search Keyword
            System.out.print("[TEST 3] Filtering expenses... ");
            List<Expense> foodOnly = service.filterExpenses(null, null, ExpenseCategory.FOOD.getDisplayName(), null);
            assertCondition(foodOnly.size() == 1 && foodOnly.get(0).getId().equals(e1.getId()), "Category filter failed");

            List<Expense> augOnly = service.filterExpenses(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);
            assertCondition(augOnly.size() == 3, "Date range filter failed");

            List<Expense> searchResult = service.filterExpenses(null, null, null, "Electricity");
            assertCondition(searchResult.size() == 1 && searchResult.get(0).getId().equals(e2.getId()), "Keyword filter failed");
            System.out.println("PASSED.");

            // Test 4: Update & Delete
            System.out.print("[TEST 4] Updating and deleting expenses... ");
            boolean updated = service.updateExpense(e1.getId(), 50.00, e1.getCategory(), e1.getDate(), "Updated team lunch");
            assertCondition(updated && Math.abs(service.getExpenseById(e1.getId()).getAmount() - 50.00) < 0.01, "Update failed");

            boolean deleted = service.deleteExpense(e3.getId());
            assertCondition(deleted && service.getExpenseById(e3.getId()) == null, "Delete failed");
            assertCondition(service.getAllExpenses().size() == 3, "Expected 3 remaining expenses");
            System.out.println("PASSED.");

            // Test 5: Budget Limits & Alert Thresholds
            System.out.print("[TEST 5] Monthly Budget Limits & Alerts... ");
            YearMonth augMonth = YearMonth.of(2026, 8);
            
            // Set budget to 300 (Spent is 50 + 120 = 170 => 56% OK)
            service.setMonthlyBudget(augMonth, 300.00);
            assertCondition(service.getBudgetStatus(augMonth) == ExpenseService.BudgetStatus.OK, "Expected Budget OK status");

            // Set budget to 200 (Spent is 170 => 85% WARNING)
            service.setMonthlyBudget(augMonth, 200.00);
            assertCondition(service.getBudgetStatus(augMonth) == ExpenseService.BudgetStatus.WARNING, "Expected Budget WARNING status");

            // Set budget to 150 (Spent is 170 => 113% EXCEEDED)
            service.setMonthlyBudget(augMonth, 150.00);
            assertCondition(service.getBudgetStatus(augMonth) == ExpenseService.BudgetStatus.EXCEEDED, "Expected Budget EXCEEDED status");
            System.out.println("PASSED.");

            // Test 6: Persistence Reload
            System.out.print("[TEST 6] Data Persistence (JSON save/load)... ");
            ExpenseService reloadedService = new ExpenseService(storageManager);
            assertCondition(reloadedService.getAllExpenses().size() == 3, "Reloaded expenses count mismatch");
            assertCondition(reloadedService.getMonthlyBudget(augMonth).getMonthlyLimit() == 150.00, "Reloaded budget limit mismatch");
            System.out.println("PASSED.");

            // Test 7: CSV Export & Import
            System.out.print("[TEST 7] CSV Export and Import... ");
            File csvFile = new File(tempTestDir, "export_test.csv");
            service.exportToCsv(csvFile, null);
            assertCondition(csvFile.exists() && csvFile.length() > 0, "CSV export file empty or missing");

            int importedCount = service.importFromCsv(csvFile);
            assertCondition(importedCount == 0, "Re-importing identical IDs should skip duplicates");
            System.out.println("PASSED.");

            // Test 8: Currency selection & Persistence
            System.out.print("[TEST 8] Currency Selection & Formatting... ");
            service.setCurrency(Currency.EUR);
            assertCondition(service.formatAmount(50.00).equals("€50.00"), "Expected EUR format €50.00");
            ExpenseService reloadedCurrencyService = new ExpenseService(storageManager);
            assertCondition(reloadedCurrencyService.getCurrency() == Currency.EUR, "Reloaded currency should be EUR");
            System.out.println("PASSED.");

            // Test 9: Income & Net Balance Calculations
            System.out.print("[TEST 9] Income Logging & Net Balance... ");
            Expense inc1 = new Expense(1000.00, "Salary", LocalDate.of(2026, 8, 1), "Monthly salary");
            service.addIncome(inc1);
            assertCondition(service.getAllIncomes().size() == 1, "Expected 1 income record");
            assertCondition(Math.abs(service.getTotalIncome() - 1000.00) < 0.01, "Expected total income 1000.00");
            
            reloadedCurrencyService.reload();
            assertCondition(reloadedCurrencyService.getAllIncomes().size() == 1, "Expected 1 income after reload()");
            assertCondition(Math.abs(reloadedCurrencyService.getCurrentBalance() - (1000.00 - reloadedCurrencyService.getTotalSpending())) < 0.01, "Net balance mismatch");
            System.out.println("PASSED.");

            System.out.println("\nALL 9 AUTOMATED TESTS PASSED SUCCESSFULLY! ✅");

        } catch (Throwable t) {
            System.err.println("\nTEST FAILED!");
            t.printStackTrace();
            System.exit(1);
        } finally {
            deleteDirectory(tempTestDir);
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteDirectory(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }
}
