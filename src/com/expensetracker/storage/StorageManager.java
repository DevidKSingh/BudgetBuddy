package com.expensetracker.storage;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Currency;
import com.expensetracker.model.Expense;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence layer for saving and loading expenses (including income entries), budgets, and settings using light JSON format.
 * The {@code isIncome} field is written for every record and defaults to {@code false} for backward compatibility.
 */
public class StorageManager {
    private static final String DATA_DIR_NAME = "expense_tracker_data";
    private static final String EXPENSES_FILE = "expenses.json";
    private static final String BUDGETS_FILE = "budgets.json";
    private static final String SETTINGS_FILE = "settings.json";

    private final File dataDir;
    private final File expensesFile;
    private final File budgetsFile;
    private final File settingsFile;

    public StorageManager() {
        String userHome = System.getProperty("user.home");
        dataDir = new File(userHome, DATA_DIR_NAME);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        expensesFile = new File(dataDir, EXPENSES_FILE);
        budgetsFile = new File(dataDir, BUDGETS_FILE);
        settingsFile = new File(dataDir, SETTINGS_FILE);
    }

    public StorageManager(File customDirectory) {
        this.dataDir = customDirectory;
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        expensesFile = new File(dataDir, EXPENSES_FILE);
        budgetsFile = new File(dataDir, BUDGETS_FILE);
        settingsFile = new File(dataDir, SETTINGS_FILE);
    }

    public synchronized void saveExpenses(List<Expense> expenses) {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < expenses.size(); i++) {
            Expense e = expenses.get(i);
            json.append("  {\n");
            json.append("    \"id\": \"").append(escapeJson(e.getId())).append("\",\n");
            json.append("    \"amount\": ").append(e.getAmount()).append(",\n");
            json.append("    \"category\": \"").append(escapeJson(e.getCategory())).append("\",\n");
            json.append("    \"date\": \"").append(e.getDate().toString()).append("\",\n");
            json.append("    \"notes\": \"").append(escapeJson(e.getNotes())).append("\",\n");
            json.append("    \"createdAt\": \"").append(e.getCreatedAt().toString()).append("\",\n");
            json.append("    \"isIncome\": ").append(e.isIncome()).append("\n");
            json.append("  }").append(i < expenses.size() - 1 ? "," : "").append("\n");
        }
        json.append("]");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(expensesFile), StandardCharsets.UTF_8)) {
            writer.write(json.toString());
        } catch (IOException e) {
            System.err.println("Error saving expenses data: " + e.getMessage());
        }
    }

    public synchronized List<Expense> loadExpenses() {
        List<Expense> list = new ArrayList<>();
        if (!expensesFile.exists() || expensesFile.length() == 0) {
            return list;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(expensesFile), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            String content = sb.toString().trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1).trim();
                if (!content.isEmpty()) {
                    List<String> objectBlocks = splitJsonObjects(content);
                    for (String block : objectBlocks) {
                        Expense expense = parseExpenseObject(block);
                        if (expense != null) {
                            list.add(expense);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading expenses: " + e.getMessage());
        }
        return list;
    }

    public synchronized void saveBudgets(Map<YearMonth, Budget> budgets) {
        StringBuilder json = new StringBuilder("[\n");
        List<Map.Entry<YearMonth, Budget>> entries = new ArrayList<>(budgets.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Budget b = entries.get(i).getValue();
            json.append("  {\n");
            json.append("    \"yearMonth\": \"").append(b.getYearMonth().toString()).append("\",\n");
            json.append("    \"monthlyLimit\": ").append(b.getMonthlyLimit()).append("\n");
            json.append("  }").append(i < entries.size() - 1 ? "," : "").append("\n");
        }
        json.append("]");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(budgetsFile), StandardCharsets.UTF_8)) {
            writer.write(json.toString());
        } catch (IOException e) {
            System.err.println("Error saving budget data: " + e.getMessage());
        }
    }

    public synchronized Map<YearMonth, Budget> loadBudgets() {
        Map<YearMonth, Budget> map = new HashMap<>();
        if (!budgetsFile.exists() || budgetsFile.length() == 0) {
            return map;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(budgetsFile), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            String content = sb.toString().trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1).trim();
                if (!content.isEmpty()) {
                    List<String> objectBlocks = splitJsonObjects(content);
                    for (String block : objectBlocks) {
                        Budget budget = parseBudgetObject(block);
                        if (budget != null) {
                            map.put(budget.getYearMonth(), budget);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading budgets: " + e.getMessage());
        }
        return map;
    }

    public synchronized void saveCurrency(Currency currency) {
        String json = "{\n  \"currency\": \"" + currency.name() + "\"\n}";
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(settingsFile), StandardCharsets.UTF_8)) {
            writer.write(json);
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public synchronized Currency loadCurrency() {
        if (!settingsFile.exists() || settingsFile.length() == 0) {
            return Currency.USD;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(settingsFile), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            Map<String, String> values = parseSimpleJsonFields(sb.toString());
            if (values.containsKey("currency")) {
                return Currency.fromName(values.get("currency"));
            }
        } catch (Exception e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
        return Currency.USD;
    }

    public File getDataDirectory() {
        return dataDir;
    }

    private List<String> splitJsonObjects(String jsonArrayContent) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < jsonArrayContent.length(); i++) {
            char c = jsonArrayContent.charAt(i);
            if (c == '"' && (i == 0 || jsonArrayContent.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    current.append(c);
                    if (depth == 0) {
                        objects.add(current.toString());
                        current = new StringBuilder();
                        continue;
                    }
                }
            }
            if (depth > 0) {
                current.append(c);
            }
        }
        return objects;
    }

    private Expense parseExpenseObject(String objectJson) {
        try {
            Map<String, String> values = parseSimpleJsonFields(objectJson);
            String id = values.get("id");
            double amount = Double.parseDouble(values.getOrDefault("amount", "0.0"));
            String category = values.getOrDefault("category", "Miscellaneous");
            LocalDate date = LocalDate.parse(values.getOrDefault("date", LocalDate.now().toString()));
            String notes = values.getOrDefault("notes", "");
            LocalDateTime createdAt = LocalDateTime.now();
            if (values.containsKey("createdAt")) {
                try {
                    createdAt = LocalDateTime.parse(values.get("createdAt"));
                } catch (Exception ignored) {}
            }
            // Backward-compatible: missing isIncome field defaults to false (expense)
            boolean isIncome = false;
            if (values.containsKey("isIncome")) {
                isIncome = "true".equalsIgnoreCase(values.get("isIncome").trim());
            }
            return new Expense(id, amount, category, date, notes, createdAt, isIncome);
        } catch (Exception e) {
            return null;
        }
    }

    private Budget parseBudgetObject(String objectJson) {
        try {
            Map<String, String> values = parseSimpleJsonFields(objectJson);
            YearMonth ym = YearMonth.parse(values.get("yearMonth"));
            double limit = Double.parseDouble(values.getOrDefault("monthlyLimit", "0.0"));
            return new Budget(ym, limit);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> parseSimpleJsonFields(String json) {
        Map<String, String> map = new HashMap<>();
        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);

        List<String> pairs = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (c == ',' && !inQuotes) {
                if (current.length() > 0) {
                    pairs.add(current.toString().trim());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            pairs.add(current.toString().trim());
        }

        for (String pair : pairs) {
            int colonIdx = -1;
            boolean inQ = false;
            for (int i = 0; i < pair.length(); i++) {
                char c = pair.charAt(i);
                if (c == '"' && (i == 0 || pair.charAt(i - 1) != '\\')) {
                    inQ = !inQ;
                }
                if (c == ':' && !inQ) {
                    colonIdx = i;
                    break;
                }
            }
            if (colonIdx != -1) {
                String key = pair.substring(0, colonIdx).trim().replace("\"", "");
                String value = pair.substring(colonIdx + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                value = unescapeJson(value);
                map.put(key, value);
            }
        }
        return map;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
