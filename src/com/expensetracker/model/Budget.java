package com.expensetracker.model;

import java.time.YearMonth;
import java.util.Objects;

/**
 * Model representing a monthly budget limit.
 */
public class Budget {
    private YearMonth yearMonth;
    private double monthlyLimit;

    public Budget() {
        this.yearMonth = YearMonth.now();
        this.monthlyLimit = 0.0;
    }

    public Budget(YearMonth yearMonth, double monthlyLimit) {
        this.yearMonth = yearMonth != null ? yearMonth : YearMonth.now();
        this.monthlyLimit = Math.max(0.0, monthlyLimit);
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = Math.max(0.0, monthlyLimit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Budget budget = (Budget) o;
        return Objects.equals(yearMonth, budget.yearMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(yearMonth);
    }

    @Override
    public String toString() {
        return String.format("Budget[Month=%s, Limit=%.2f]", yearMonth, monthlyLimit);
    }
}
