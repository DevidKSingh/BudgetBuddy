package com.expensetracker.model;

/**
 * Currency enumeration containing common world currencies and their symbols.
 */
public enum Currency {
    USD("USD - US Dollar", "$"),
    EUR("EUR - Euro", "€"),
    GBP("GBP - British Pound", "£"),
    INR("INR - Indian Rupee", "₹"),
    CAD("CAD - Canadian Dollar", "CA$"),
    AUD("AUD - Australian Dollar", "A$"),
    JPY("JPY - Japanese Yen", "¥"),
    CNY("CNY - Chinese Yuan", "CN¥"),
    CHF("CHF - Swiss Franc", "CHF"),
    SGD("SGD - Singapore Dollar", "S$");

    private final String displayName;
    private final String symbol;

    Currency(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String format(double amount) {
        return String.format("%s%.2f", symbol, amount);
    }

    public static Currency fromName(String name) {
        if (name == null || name.trim().isEmpty()) return USD;
        for (Currency c : values()) {
            if (c.name().equalsIgnoreCase(name) || c.displayName.equalsIgnoreCase(name)) {
                return c;
            }
        }
        return USD;
    }

    public static String[] getAllDisplayNames() {
        String[] names = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            names[i] = values()[i].displayName;
        }
        return names;
    }
}
