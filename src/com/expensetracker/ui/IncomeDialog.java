package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.model.IncomeCategory;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.components.DatePickerField;
import com.expensetracker.ui.components.StyledButton;
import com.expensetracker.ui.components.StyledButton.ButtonType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/**
 * Dialog for adding or editing an income record.
 * Uses IncomeCategory for source selection and marks the resulting Expense with isIncome=true.
 */
public class IncomeDialog extends JDialog {

    private JTextField amountField;
    private JComboBox<String> categoryComboBox;
    private DatePickerField datePicker;
    private JTextArea notesArea;

    private boolean confirmed = false;
    private Expense income;
    private final String currencySymbol;

    public IncomeDialog(Frame owner, String title, Expense existingIncome, ExpenseService service) {
        super(owner, title, true);
        this.income = existingIncome;
        this.currencySymbol = service != null ? service.getCurrency().getSymbol() : "$";
        initUI();
    }

    private void initUI() {
        setSize(480, 560);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(22, 28, 22, 28));
        mainPanel.setBackground(new Color(240, 253, 248)); // Light mint background

        // Title Header with green accent
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headerLabel = new JLabel(income == null ? "Log New Income Entry" : "Edit Income Entry");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerLabel.setForeground(new Color(5, 150, 105));
        headerRow.add(headerLabel);
        mainPanel.add(headerRow);
        mainPanel.add(Box.createVerticalStrut(6));

        JLabel subLabel = new JLabel("Track your earnings and income sources");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(new Color(100, 116, 139));
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(subLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Amount Field
        mainPanel.add(createLabel("Amount (" + currencySymbol + "):"));
        amountField = new JTextField();
        styleTextField(amountField);
        if (income != null) {
            amountField.setText(String.format(java.util.Locale.US, "%.2f", income.getAmount()));
        }
        mainPanel.add(amountField);
        mainPanel.add(Box.createVerticalStrut(14));

        // Source / Category ComboBox
        mainPanel.add(createLabel("Income Source / Category:"));
        DefaultComboBoxModel<String> catModel = new DefaultComboBoxModel<>(IncomeCategory.getAllDisplayNames());
        if (income != null && income.getCategory() != null) {
            String existingCat = income.getCategory();
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
        categoryComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        categoryComboBox.setPreferredSize(new Dimension(0, 40));
        categoryComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (income != null && income.getCategory() != null) {
            categoryComboBox.setSelectedItem(income.getCategory());
        }
        mainPanel.add(categoryComboBox);
        mainPanel.add(Box.createVerticalStrut(14));

        // Date Picker Field
        mainPanel.add(createLabel("Date:"));
        datePicker = new DatePickerField("Select date");
        datePicker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        datePicker.setPreferredSize(new Dimension(0, 40));
        datePicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        datePicker.setDate(income != null ? income.getDate() : LocalDate.now());
        mainPanel.add(datePicker);
        mainPanel.add(Box.createVerticalStrut(14));

        // Notes Field
        mainPanel.add(createLabel("Notes / Description (Optional):"));
        notesArea = new JTextArea(3, 20);
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        notesScroll.setPreferredSize(new Dimension(0, 85));
        if (income != null && income.getNotes() != null) {
            notesArea.setText(income.getNotes());
        }
        mainPanel.add(notesScroll);
        mainPanel.add(Box.createVerticalStrut(20));

        add(mainPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 15));
        buttonPanel.setBackground(new Color(236, 253, 245)); // Light green tint

        StyledButton cancelButton = new StyledButton("Cancel", ButtonType.SECONDARY, "Discard changes and close form");
        cancelButton.addActionListener(e -> dispose());

        StyledButton saveButton = new StyledButton(
                income == null ? "Save Income Entry" : "Update Income Entry",
                ButtonType.SUCCESS,
                "Save this income record"
        );
        saveButton.addActionListener(e -> onSave());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(70, 80, 95));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tf.setPreferredSize(new Dimension(0, 40));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void onSave() {
        String amountText = amountField.getText().trim();
        LocalDate date = datePicker.getDate();
        String category = (String) categoryComboBox.getSelectedItem();
        String notes = notesArea.getText().trim();

        // Validation
        if (amountText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Amount is required.", "Missing Amount", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be a positive number greater than 0.", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric amount.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (date == null) {
            JOptionPane.showMessageDialog(this, "Please select a date.", "Missing Date", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (category == null || category.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an income source / category.", "Missing Category", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (income == null) {
            income = new Expense(amount, category, date, notes, true); // isIncome = true
        } else {
            income.setAmount(amount);
            income.setCategory(category);
            income.setDate(date);
            income.setNotes(notes);
            income.setIncome(true);
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Expense getIncome() {
        return income;
    }
}