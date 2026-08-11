package com.expensetracker.cli;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Currency;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.service.ExpenseService;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Console-based interactive CLI mode for terminal users with currency selection support.
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
        System.out.println("     Welcome to Daily Expense Tracker (CLI)     ");
        System.out.println("=================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Select an option (1-10): ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    addExpense();
                    break;
                case "2":
                    viewAllExpenses();
                    break;
                case "3":
                    filterExpenses();
                    break;
                case "4":
                    updateExpense();
                    break;
                case "5":
                    deleteExpense();
                    break;
                case "6":
                    generateReports();
                    break;
                case "7":
                    manageBudget();
                    break;
                case "8":
                    exportOrImportCsv();
                    break;
                case "9":
                    changeCurrency();
                    break;
                case "10":
                    running = false;
                    System.out.println("\nThank you for using Daily Expense Tracker! Goodbye.");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 10.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n---------------- MAIN MENU ----------------");
        System.out.println("1. Add New Expense");
        System.out.println("2. View All Expenses");
        System.out.println("3. Filter Expenses (Date Range / Category / Keyword)");
        System.out.println("4. Edit / Update Expense Record");
        System.out.println("5. Delete Expense Record");
        System.out.println("6. Generate Summary Reports & Analytics");
        System.out.println("7. Set & View Monthly Spending Budget");
        System.out.println("8. CSV File Import / Export");
        System.out.println("9. Select System Currency [Active: " + expenseService.getCurrency().getDisplayName() + "]");
        System.out.println("10. Exit Application");
        System.out.println("-------------------------------------------");
    }

    private void addExpense() {
        System.out.println("\n--- Add New Expense ---");
        double amount = readDouble("Enter amount (" + expenseService.getCurrency().getSymbol() + "): ");
        
        System.out.println("Categories:");
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
        System.out.println("SUCCESS: Expense added with ID " + expense.getId());

        checkBudgetAlert(YearMonth.from(date));
    }

    private void viewAllExpenses() {
        System.out.println("\n--- All Recorded Expenses ---");
        List<Expense> list = expenseService.getAllExpenses();
        printExpenseTable(list);
    }

    private void filterExpenses() {
        System.out.println("\n--- Filter Expense History ---");
        System.out.println("Leave fields blank to skip filter.");
        
        LocalDate fromDate = readOptionalDate("From date (YYYY-MM-DD): ");
        LocalDate toDate = readOptionalDate("To date (YYYY-MM-DD): ");
        
        System.out.print("Category (or ENTER for all): ");
        String cat = scanner.nextLine().trim();
        if (cat.isEmpty()) cat = "All Categories";

        System.out.print("Search Keyword (or ENTER for none): ");
        String kw = scanner.nextLine().trim();

        List<Expense> filtered = expenseService.filterExpenses(fromDate, toDate, cat, kw);
        printExpenseTable(filtered);
    }

    private void updateExpense() {
        System.out.println("\n--- Edit / Update Expense Record ---");
        System.out.print("Enter Expense ID to edit: ");
        String id = scanner.nextLine().trim();

        Expense existing = expenseService.getExpenseById(id);
        if (existing == null) {
            System.out.println("ERROR: No expense found with ID: " + id);
            return;
        }

        System.out.println("Current details: " + existing);
        double amount = readDouble("Enter new amount (" + expenseService.getCurrency().getSymbol() + ") [" + existing.getAmount() + "]: ");
        
        System.out.print("Enter new category [" + existing.getCategory() + "]: ");
        String category = scanner.nextLine().trim();
        if (category.isEmpty()) category = existing.getCategory();

        LocalDate date = readDate("Enter new date (YYYY-MM-DD) [" + existing.getDate() + "]: ");
        
        System.out.print("Enter new notes [" + existing.getNotes() + "]: ");
        String notes = scanner.nextLine().trim();
        if (notes.isEmpty()) notes = existing.getNotes();

        boolean updated = expenseService.updateExpense(id, amount, category, date, notes);
        if (updated) {
            System.out.println("SUCCESS: Expense ID " + id + " updated.");
        } else {
            System.out.println("ERROR: Failed to update expense.");
        }
    }

    private void deleteExpense() {
        System.out.println("\n--- Delete Expense Record ---");
        System.out.print("Enter Expense ID to delete: ");
        String id = scanner.nextLine().trim();

        Expense existing = expenseService.getExpenseById(id);
        if (existing == null) {
            System.out.println("ERROR: No expense found with ID: " + id);
            return;
        }

        System.out.print("Are you sure you want to delete expense of " + expenseService.formatAmount(existing.getAmount()) + " (" + existing.getCategory() + ")? (y/n): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("y")) {
            expenseService.deleteExpense(id);
            System.out.println("SUCCESS: Expense deleted.");
        } else {
            System.out.println("Operation canceled.");
        }
    }

    private void generateReports() {
        System.out.println("\n=================================================");
        System.out.println("             EXPENSE SUMMARY REPORTS             ");
        System.out.println("=================================================");
        
        double totalSpend = expenseService.getTotalSpending();
        System.out.printf("Total All-Time Expenditure: %s%n", expenseService.formatAmount(totalSpend));

        YearMonth currentMonth = YearMonth.now();
        double monthSpend = expenseService.getMonthlySpending(currentMonth);
        System.out.printf("Current Month (%s) Expenditure: %s%n", currentMonth, expenseService.formatAmount(monthSpend));

        Expense maxExpense = expenseService.getHighestExpense();
        if (maxExpense != null) {
            System.out.printf("Highest Single Expense: %s (%s on %s)%n", expenseService.formatAmount(maxExpense.getAmount()), maxExpense.getCategory(), maxExpense.getDate());
        }

        System.out.println("\n--- Spending Breakdown by Category ---");
        Map<String, Double> catMap = expenseService.getSpendingByCategory(null);
        if (catMap.isEmpty()) {
            System.out.println("No records found.");
        } else {
            for (Map.Entry<String, Double> entry : catMap.entrySet()) {
                double pct = totalSpend > 0 ? (entry.getValue() / totalSpend) * 100 : 0;
                System.out.printf(" - %-25s: %12s (%5.1f%%)%n", entry.getKey(), expenseService.formatAmount(entry.getValue()), pct);
            }
        }

        System.out.println("\n--- Monthly Spending History ---");
        Map<YearMonth, Double> monthlyMap = expenseService.getMonthlySpendingHistory();
        for (Map.Entry<YearMonth, Double> entry : monthlyMap.entrySet()) {
            Budget budget = expenseService.getMonthlyBudget(entry.getKey());
            String budgetInfo = budget.getMonthlyLimit() > 0 ? String.format(" [Budget: %s]", expenseService.formatAmount(budget.getMonthlyLimit())) : "";
            System.out.printf(" - %-10s: %s%s%n", entry.getKey(), expenseService.formatAmount(entry.getValue()), budgetInfo);
        }
    }

    private void manageBudget() {
        System.out.println("\n--- Monthly Budget Management ---");
        YearMonth currentMonth = YearMonth.now();
        Budget currentBudget = expenseService.getMonthlyBudget(currentMonth);
        double spent = expenseService.getMonthlySpending(currentMonth);

        System.out.printf("Current Month: %s%n", currentMonth);
        System.out.printf("Current Spending: %s%n", expenseService.formatAmount(spent));
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
            System.out.printf("%n[ALERT ⚠️] BUDGET OVERRUN! You spent %s of %s monthly budget for %s (Over by %s)!%n",
                    expenseService.formatAmount(spent), expenseService.formatAmount(limit), ym, expenseService.formatAmount(spent - limit));
        } else if (status == ExpenseService.BudgetStatus.WARNING) {
            System.out.printf("%n[WARNING ⚠️] Approaching budget limit! Spent %s of %s (%.1f%% used).%n",
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
        System.out.println("1. Export all expenses to CSV");
        System.out.println("2. Import expenses from CSV file");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            System.out.print("Enter output filename [expenses.csv]: ");
            String filename = scanner.nextLine().trim();
            if (filename.isEmpty()) filename = "expenses.csv";
            File file = new File(filename);
            try {
                expenseService.exportToCsv(file, null);
                System.out.println("SUCCESS: Exported " + expenseService.getAllExpenses().size() + " expenses to " + file.getAbsolutePath());
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
                System.out.println("SUCCESS: Imported " + count + " new expense entries.");
            } catch (Exception e) {
                System.out.println("ERROR importing CSV: " + e.getMessage());
            }
        }
    }

    private void printExpenseTable(List<Expense> list) {
        if (list == null || list.isEmpty()) {
            System.out.println("No expense records found.");
            return;
        }

        String symbol = expenseService.getCurrency().getSymbol();
        String amtColHeader = String.format("Amount (%s)", symbol);

        System.out.println("+----------+------------+-----------------------+--------------+-----------------------------------+");
        System.out.printf("| ID       | Date       | Category              | %-12s | Description                       |%n", amtColHeader);
        System.out.println("+----------+------------+-----------------------+--------------+-----------------------------------+");

        double total = 0.0;
        for (Expense e : list) {
            System.out.printf("| %-8s | %-10s | %-21s | %12s | %-33s |%n",
                    e.getId(),
                    e.getDate(),
                    truncate(e.getCategory(), 21),
                    expenseService.formatAmount(e.getAmount()),
                    truncate(e.getNotes(), 33));
            total += e.getAmount();
        }
        System.out.println("+----------+------------+-----------------------+--------------+-----------------------------------+");
        System.out.printf("| TOTAL (%d records)                            | %12s |                                   |%n", list.size(), expenseService.formatAmount(total));
        System.out.println("+----------+------------+-----------------------+--------------+-----------------------------------+");
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
