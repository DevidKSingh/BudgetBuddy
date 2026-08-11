package com.expensetracker.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents categories for expenses with associated UI colors.
 */
public enum ExpenseCategory {
    FOOD("Food & Dining", new Color(239, 83, 80)),       // Red / Coral
    TRANSPORT("Transportation", new Color(66, 165, 245)), // Blue
    UTILITIES("Utilities & Bills", new Color(255, 167, 38)), // Orange
    ENTERTAINMENT("Entertainment", new Color(171, 71, 188)), // Purple
    HEALTH("Healthcare & Fitness", new Color(102, 187, 106)), // Green
    SHOPPING("Shopping & Retail", new Color(240, 98, 146)),  // Pink
    EDUCATION("Education & Learning", new Color(38, 198, 218)), // Teal
    HOUSING("Housing & Rent", new Color(141, 110, 99)),     // Brown
    MISCELLANEOUS("Miscellaneous", new Color(120, 144, 156)); // Blue-Grey

    private final String displayName;
    private final Color color;

    ExpenseCategory(String displayName, Color color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getColor() {
        return color;
    }

    public static ExpenseCategory fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return MISCELLANEOUS;
        }
        for (ExpenseCategory cat : ExpenseCategory.values()) {
            if (cat.name().equalsIgnoreCase(text) || cat.displayName.equalsIgnoreCase(text)) {
                return cat;
            }
        }
        return MISCELLANEOUS;
    }

    public static String[] getAllDisplayNames() {
        List<String> names = new ArrayList<>();
        for (ExpenseCategory cat : ExpenseCategory.values()) {
            names.add(cat.getDisplayName());
        }
        return names.toArray(new String[0]);
    }
}
