package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.components.StyledButton;
import com.expensetracker.ui.components.StyledButton.ButtonType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Dialog for adding or editing an expense record with form validation, currency formatting, and styled buttons.
 */
public class ExpenseDialog extends JDialog {

    private JTextField amountField;
    private JComboBox<String> categoryComboBox;
    private JTextField dateField;
    private JTextArea notesArea;

    private boolean confirmed = false;
    private Expense expense;
    private final String currencySymbol;

    public ExpenseDialog(Frame owner, String title, Expense existingExpense, ExpenseService service) {
        super(owner, title, true);
        this.expense = existingExpense;
        this.currencySymbol = service != null ? service.getCurrency().getSymbol() : "$";
        initUI();
    }

    private void initUI() {
        setSize(450, 520);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        mainPanel.setBackground(new Color(250, 251, 253));

        // Title Header
        JLabel headerLabel = new JLabel(expense == null ? "Log New Expense Entry" : "Edit Expense Entry");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerLabel.setForeground(new Color(33, 37, 41));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(headerLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Amount Field
        mainPanel.add(createLabel("Amount (" + currencySymbol + "):"));
        amountField = new JTextField();
        styleTextField(amountField);
        if (expense != null) {
            amountField.setText(String.format("%.2f", expense.getAmount()));
        }
        mainPanel.add(amountField);
        mainPanel.add(Box.createVerticalStrut(12));

        // Category ComboBox
        mainPanel.add(createLabel("Category:"));
        DefaultComboBoxModel<String> catModel = new DefaultComboBoxModel<>(ExpenseCategory.getAllDisplayNames());
        if (expense != null && expense.getCategory() != null) {
            String existingCat = expense.getCategory();
            boolean found = false;
            for (int i = 0; i < catModel.getSize(); i++) {
                if (catModel.getElementAt(i).equalsIgnoreCase(existingCat)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                catModel.addElement(existingCat);
            }
        }
        categoryComboBox = new JComboBox<>(catModel);
        categoryComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        categoryComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        categoryComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (expense != null) {
            categoryComboBox.setSelectedItem(expense.getCategory());
        }
        mainPanel.add(categoryComboBox);
        mainPanel.add(Box.createVerticalStrut(12));

        // Date Field (YYYY-MM-DD)
        mainPanel.add(createLabel("Date (YYYY-MM-DD):"));
        dateField = new JTextField();
        styleTextField(dateField);
        dateField.setText(expense != null ? expense.getDate().toString() : LocalDate.now().toString());
        mainPanel.add(dateField);
        mainPanel.add(Box.createVerticalStrut(12));

        // Notes Field
        mainPanel.add(createLabel("Notes / Description (Optional):"));
        notesArea = new JTextArea(3, 20);
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        if (expense != null && expense.getNotes() != null) {
            notesArea.setText(expense.getNotes());
        }
        mainPanel.add(notesScroll);
        mainPanel.add(Box.createVerticalStrut(20));

        add(mainPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 15));
        buttonPanel.setBackground(new Color(245, 247, 250));

        StyledButton cancelButton = new StyledButton("Cancel", ButtonType.SECONDARY, "Discard changes and close form");
        cancelButton.addActionListener(e -> dispose());

        StyledButton saveButton = new StyledButton(expense == null ? "Save Expense Entry" : "Update Expense Entry", ButtonType.PRIMARY, "Save this expense record to system persistence");
        saveButton.addActionListener(e -> onSave());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(70, 80, 95));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void onSave() {
        String amountText = amountField.getText().trim();
        String dateText = dateField.getText().trim();
        String category = (String) categoryComboBox.getSelectedItem();
        String notes = notesArea.getText().trim();

        // Validation
        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be a positive number.", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric amount.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD (e.g. 2026-08-01).", "Invalid Date", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (expense == null) {
            expense = new Expense(amount, category, date, notes);
        } else {
            expense.setAmount(amount);
            expense.setCategory(category);
            expense.setDate(date);
            expense.setNotes(notes);
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Expense getExpense() {
        return expense;
    }
}
