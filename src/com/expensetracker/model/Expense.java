package com.expensetracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Model representing an individual daily expense record.
 */
public class Expense {
    private String id;
    private double amount;
    private String category;
    private LocalDate date;
    private String notes;
    private LocalDateTime createdAt;

    public Expense() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.date = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.notes = "";
    }

    public Expense(double amount, String category, LocalDate date, String notes) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.amount = amount;
        this.category = category;
        this.date = date != null ? date : LocalDate.now();
        this.notes = notes != null ? notes.trim() : "";
        this.createdAt = LocalDateTime.now();
    }

    public Expense(String id, double amount, String category, LocalDate date, String notes, LocalDateTime createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString().substring(0, 8);
        this.amount = amount;
        this.category = category;
        this.date = date != null ? date : LocalDate.now();
        this.notes = notes != null ? notes.trim() : "";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return Objects.equals(id, expense.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Expense[ID=%s, Date=%s, Category=%s, Amount=%.2f, Notes='%s']",
                id, date, category, amount, notes);
    }
}
