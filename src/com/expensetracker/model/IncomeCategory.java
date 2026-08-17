package com.expensetracker.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents source categories for income entries with associated UI colors (green/teal palette).
 */
public enum IncomeCategory {
    SALARY("Salary",                new Color(16, 185, 129)),   // Emerald green
    FREELANCE("Freelance",          new Color(5, 150, 105)),    // Dark emerald
    SCHOLARSHIP("Scholarship",      new Color(14, 165, 233)),   // Sky blue
    GIFT("Gift",                    new Color(168, 85, 247)),   // Purple
    INVESTMENT("Investment",        new Color(245, 158, 11)),   // Amber
    RENTAL("Rental Income",         new Color(59, 130, 246)),   // Blue
    OTHER("Other Income",           new Color(100, 116, 139));  // Slate

    private final String displayName;
    private final Color color;

    IncomeCategory(String displayName, Color color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getColor() {
        return color;
    }

    public static IncomeCategory fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return OTHER;
        }
        for (IncomeCategory cat : IncomeCategory.values()) {
            if (cat.name().equalsIgnoreCase(text) || cat.displayName.equalsIgnoreCase(text)) {
                return cat;
            }
        }
        return OTHER;
    }

    public static String[] getAllDisplayNames() {
        List<String> names = new ArrayList<>();
        for (IncomeCategory cat : IncomeCategory.values()) {
            names.add(cat.getDisplayName());
        }
        return names.toArray(new String[0]);
    }
}
