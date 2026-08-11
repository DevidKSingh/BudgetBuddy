package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.components.StyledButton;
import com.expensetracker.ui.components.StyledButton.ButtonType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Full interactive tabular view with filtering by date range, category, search query, CSV export/import, and modern styled action buttons.
 */
public class ExpenseTablePanel extends JPanel {

    private final ExpenseService expenseService;
    private final Frame parentFrame;
    private final Runnable onDataChanged;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Expense> currentDisplayedExpenses = new ArrayList<>();

    private JTextField searchField;
    private JComboBox<String> categoryFilterBox;
    private JTextField fromDateField;
    private JTextField toDateField;

    private JLabel totalSumLabel;

    public ExpenseTablePanel(ExpenseService expenseService, Frame parentFrame, Runnable onDataChanged) {
        this.expenseService = expenseService;
        this.parentFrame = parentFrame;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(new Color(245, 247, 250));

        initFilterBar();
        initTable();
        initFooter();

        refreshTable();
    }

    private void initFilterBar() {
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 6, 0, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        int col = 0;

        // --- Search ---
        gbc.gridx = col++; gbc.weightx = 0;
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchLbl.setForeground(new Color(71, 85, 105));
        filterPanel.add(searchLbl, gbc);

        gbc.gridx = col++; gbc.weightx = 0.22;
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setToolTipText("Enter text or amount to search");
        filterPanel.add(searchField, gbc);

        // --- Category ---
        gbc.gridx = col++; gbc.weightx = 0;
        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        catLbl.setForeground(new Color(71, 85, 105));
        filterPanel.add(catLbl, gbc);

        gbc.gridx = col++; gbc.weightx = 0.28;
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        for (String cat : ExpenseCategory.getAllDisplayNames()) {
            categories.add(cat);
        }
        categoryFilterBox = new JComboBox<>(categories.toArray(new String[0]));
        categoryFilterBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterPanel.add(categoryFilterBox, gbc);

        // --- From ---
        gbc.gridx = col++; gbc.weightx = 0;
        JLabel fromLbl = new JLabel("From:");
        fromLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        fromLbl.setForeground(new Color(71, 85, 105));
        filterPanel.add(fromLbl, gbc);

        gbc.gridx = col++; gbc.weightx = 0.15;
        fromDateField = new JTextField();
        fromDateField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fromDateField.setToolTipText("Start date (YYYY-MM-DD)");
        filterPanel.add(fromDateField, gbc);

        // --- To ---
        gbc.gridx = col++; gbc.weightx = 0;
        JLabel toLbl = new JLabel("To:");
        toLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        toLbl.setForeground(new Color(71, 85, 105));
        filterPanel.add(toLbl, gbc);

        gbc.gridx = col++; gbc.weightx = 0.15;
        toDateField = new JTextField();
        toDateField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        toDateField.setToolTipText("End date (YYYY-MM-DD)");
        filterPanel.add(toDateField, gbc);

        // --- Apply button ---
        gbc.gridx = col++; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        StyledButton applyBtn = new StyledButton("Apply Filters", ButtonType.PRIMARY, "Filter expense records by entered criteria");
        applyBtn.addActionListener(e -> refreshTable());
        filterPanel.add(applyBtn, gbc);

        // --- Reset button ---
        gbc.gridx = col; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        StyledButton resetBtn = new StyledButton("Reset", ButtonType.SECONDARY, "Clear all filter fields and show all expense entries");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilterBox.setSelectedIndex(0);
            fromDateField.setText("");
            toDateField.setText("");
            refreshTable();
        });
        filterPanel.add(resetBtn, gbc);

        add(filterPanel, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] columns = {"ID", "Date", "Category", "Amount (" + expenseService.getCurrency().getSymbol() + ")", "Description / Notes"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(30, 41, 59));
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 245, 249));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);

        // Right align amount column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));

        add(scrollPane, BorderLayout.CENTER);
    }

    private void initFooter() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);

        // Left Action Buttons: Add, Edit, Delete
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        StyledButton addBtn = new StyledButton("+ Add Expense", ButtonType.SUCCESS, "Open form to input a new expense record");
        addBtn.addActionListener(e -> onAddExpense());

        StyledButton editBtn = new StyledButton("Edit Selected", ButtonType.SECONDARY, "Edit the currently selected table row");
        editBtn.addActionListener(e -> onEditExpense());

        StyledButton deleteBtn = new StyledButton("Delete Selected", ButtonType.DANGER, "Delete the currently selected row from record history");
        deleteBtn.addActionListener(e -> onDeleteExpense());

        leftActions.add(addBtn);
        leftActions.add(editBtn);
        leftActions.add(deleteBtn);

        // Right Action Buttons: CSV Export / Import
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        totalSumLabel = new JLabel("Filtered Total: " + expenseService.formatAmount(0));
        totalSumLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalSumLabel.setForeground(new Color(30, 41, 59));

        StyledButton exportBtn = new StyledButton("Export CSV", ButtonType.SECONDARY, "Save current list of expenses to a CSV spreadsheet file");
        exportBtn.addActionListener(e -> onExportCsv());

        StyledButton importBtn = new StyledButton("Import CSV", ButtonType.SECONDARY, "Import expense entries from an existing CSV file");
        importBtn.addActionListener(e -> onImportCsv());

        rightActions.add(totalSumLabel);
        rightActions.add(Box.createHorizontalStrut(15));
        rightActions.add(exportBtn);
        rightActions.add(importBtn);

        footerPanel.add(leftActions, BorderLayout.WEST);
        footerPanel.add(rightActions, BorderLayout.EAST);

        add(footerPanel, BorderLayout.SOUTH);
    }

    public void refreshTable() {
        // Update header currency symbol
        table.getColumnModel().getColumn(3).setHeaderValue("Amount (" + expenseService.getCurrency().getSymbol() + ")");
        table.getTableHeader().repaint();

        String searchKw = searchField.getText().trim();
        String selectedCat = (String) categoryFilterBox.getSelectedItem();

        LocalDate fromDate = null;
        if (!fromDateField.getText().trim().isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromDateField.getText().trim());
            } catch (DateTimeParseException e) {
                // Ignore invalid date
            }
        }

        LocalDate toDate = null;
        if (!toDateField.getText().trim().isEmpty()) {
            try {
                toDate = LocalDate.parse(toDateField.getText().trim());
            } catch (DateTimeParseException e) {
                // Ignore invalid date
            }
        }

        currentDisplayedExpenses = expenseService.filterExpenses(fromDate, toDate, selectedCat, searchKw);
        tableModel.setRowCount(0);

        double totalSum = 0.0;
        for (Expense e : currentDisplayedExpenses) {
            tableModel.addRow(new Object[]{
                    e.getId(),
                    e.getDate().toString(),
                    e.getCategory(),
                    expenseService.formatAmount(e.getAmount()),
                    e.getNotes()
            });
            totalSum += e.getAmount();
        }

        totalSumLabel.setText("Filtered Total: " + expenseService.formatAmount(totalSum));
    }

    public void onAddExpense() {
        ExpenseDialog dialog = new ExpenseDialog(parentFrame, "Add New Expense", null, expenseService);
        dialog.setVisible(true);
        if (dialog.isConfirmed() && dialog.getExpense() != null) {
            expenseService.addExpense(dialog.getExpense());
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    private void onEditExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an expense row from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tableModel.getValueAt(selectedRow, 0);
        Expense selectedExpense = expenseService.getExpenseById(id);
        if (selectedExpense == null) return;

        ExpenseDialog dialog = new ExpenseDialog(parentFrame, "Edit Expense Record", selectedExpense, expenseService);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Expense updated = dialog.getExpense();
            expenseService.updateExpense(id, updated.getAmount(), updated.getCategory(), updated.getDate(), updated.getNotes());
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    private void onDeleteExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an expense row from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tableModel.getValueAt(selectedRow, 0);
        Expense selectedExpense = expenseService.getExpenseById(id);
        String details = selectedExpense != null ? String.format("%s (%s - %s)", id, selectedExpense.getCategory(), expenseService.formatAmount(selectedExpense.getAmount())) : id;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently delete expense record ID: " + details + "?",
                "Confirm Expense Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            expenseService.deleteExpense(id);
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    private void onExportCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Expenses to CSV File");
        fileChooser.setSelectedFile(new File("expense_records.csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dest = fileChooser.getSelectedFile();
            if (!dest.getName().toLowerCase().endsWith(".csv")) {
                dest = new File(dest.getAbsolutePath() + ".csv");
            }
            try {
                expenseService.exportToCsv(dest, currentDisplayedExpenses);
                JOptionPane.showMessageDialog(this, "Successfully exported " + currentDisplayedExpenses.size() + " records to:\n" + dest.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error exporting CSV: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onImportCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Expenses from CSV File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File src = fileChooser.getSelectedFile();
            try {
                int importedCount = expenseService.importFromCsv(src);
                JOptionPane.showMessageDialog(this, "Successfully imported " + importedCount + " expense records.",
                        "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
                if (onDataChanged != null) onDataChanged.run();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error importing CSV: " + e.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
