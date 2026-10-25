package com.expensetracker.storage;

import com.expensetracker.model.Expense;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles importing from and exporting to CSV files.
 */
public class CsvExporter {

    public static void exportToCsv(File outputFile, List<Expense> expenses) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            // Write Header with Type column
            writer.println("ID,Date,Type,Category,Amount,Description/Notes");
            for (Expense e : expenses) {
                String typeStr = e.isIncome() ? "Income" : "Expense";
                writer.printf(java.util.Locale.US, "%s,%s,%s,\"%s\",%.2f,\"%s\"%n",
                        escapeCsv(e.getId()),
                        e.getDate().toString(),
                        typeStr,
                        escapeCsv(e.getCategory()),
                        e.getAmount(),
                        escapeCsv(e.getNotes()));
            }
        }
    }

    public static List<Expense> importFromCsv(File inputFile) throws IOException {
        List<Expense> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine(); // Skip or inspect header
            if (headerLine == null) return imported;

            boolean hasTypeColumn = headerLine.toLowerCase().contains("type");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 4) {
                    try {
                        String id = tokens.get(0).trim();
                        LocalDate date = LocalDate.parse(tokens.get(1).trim());
                        boolean isIncome = false;
                        String category;
                        double amount;
                        String notes = "";

                        if (tokens.size() >= 6 || (hasTypeColumn && tokens.size() >= 5)) {
                            // 6-column format: ID, Date, Type, Category, Amount, Description/Notes
                            String typeVal = tokens.get(2).trim();
                            isIncome = "income".equalsIgnoreCase(typeVal);
                            category = tokens.get(3).trim();
                            amount = Double.parseDouble(tokens.get(4).trim());
                            notes = tokens.size() > 5 ? tokens.get(5).trim() : "";
                        } else {
                            // Legacy 5-column or 4-column format: ID, Date, Category, Amount, [Notes]
                            category = tokens.get(2).trim();
                            amount = Double.parseDouble(tokens.get(3).trim());
                            notes = tokens.size() > 4 ? tokens.get(4).trim() : "";
                        }

                        Expense expense = new Expense(id, amount, category, date, notes, null, isIncome);
                        imported.add(expense);
                    } catch (NumberFormatException | DateTimeParseException e) {
                        System.err.println("Skipping invalid CSV line: " + line);
                    }
                }
            }
        }
        return imported;
    }

    private static String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result;
    }
}
