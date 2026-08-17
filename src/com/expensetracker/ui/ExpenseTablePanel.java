package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.model.IncomeCategory;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.ExpenseService.TransactionFilter;
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
 * Unified Transactions panel showing both expense and income entries.
 * Includes a type-filter tab strip (All / Income / Expenses), color-coded rows,
 * and separate Add Expense / Add Income buttons.
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

    // Type filter state
    private TransactionFilter activeFilter = TransactionFilter.ALL;
    private JToggleButton btnAll;
    private JToggleButton btnIncome;
    private JToggleButton btnExpense;

    // Income row highlight color
    private static final Color INCOME_ROW_BG   = new Color(236, 253, 245); // mint green
    private static final Color EXPENSE_ROW_BG  = Color.WHITE;
    private static final Color INCOME_ROW_ALT  = new Color(220, 252, 231); // slightly darker mint
    private static final Color EXPENSE_ROW_ALT = new Color(248, 250, 252); // faint slate

    public ExpenseTablePanel(ExpenseService expenseService, Frame parentFrame, Runnable onDataChanged) {
        this.expenseService = expenseService;
        this.parentFrame = parentFrame;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(new Color(245, 247, 250));

        initTypeTabStrip();
        initFilterBar();
        initTable();
        initFooter();

        refreshTable();
    }

    // -------------------------------------------------------------------------
    //  Type Tab Strip — All | Income | Expenses
    // -------------------------------------------------------------------------

    private void initTypeTabStrip() {
        JPanel tabStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabStrip.setOpaque(false);
        tabStrip.setBorder(new EmptyBorder(0, 0, 8, 0));

        ButtonGroup group = new ButtonGroup();
        btnAll     = createTabButton("All Transactions", TransactionFilter.ALL);
        btnIncome  = createTabButton("💚  Income",       TransactionFilter.INCOME);
        btnExpense = createTabButton("🔴  Expenses",     TransactionFilter.EXPENSE);

        group.add(btnAll);
        group.add(btnIncome);
        group.add(btnExpense);

        btnAll.setSelected(true);

        tabStrip.add(btnAll);
        tabStrip.add(Box.createHorizontalStrut(6));
        tabStrip.add(btnIncome);
        tabStrip.add(Box.createHorizontalStrut(6));
        tabStrip.add(btnExpense);

        add(tabStrip, BorderLayout.NORTH);
    }

    private JToggleButton createTabButton(String label, TransactionFilter filter) {
        JToggleButton btn = new JToggleButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected()) {
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    setForeground(Color.WHITE);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(new Color(203, 213, 225));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    setForeground(new Color(71, 85, 105));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 18, 7, 18));

        btn.addActionListener(e -> {
            activeFilter = filter;
            updateCategoryFilterOptions();
            refreshTable();
        });
        return btn;
    }

    // -------------------------------------------------------------------------
    //  Filter Bar
    // -------------------------------------------------------------------------

    private JPanel filterBarPanel;

    private void initFilterBar() {
        filterBarPanel = new JPanel(new GridBagLayout());
        filterBarPanel.setBackground(Color.WHITE);
        filterBarPanel.setBorder(BorderFactory.createCompoundBorder(
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
        filterBarPanel.add(searchLbl, gbc);

        gbc.gridx = col++; gbc.weightx = 0.22;
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setToolTipText("Enter text or amount to search");
        filterBarPanel.add(searchField, gbc);

        // --- Category ---
        gbc.gridx = col++; gbc.weightx = 0;
        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        catLbl.setForeground(new Color(71, 85, 105));
        filterBarPanel.add(catLbl, gbc);

        gbc.gridx = col++; gbc.weightx = 0.28;
        categoryFilterBox = new JComboBox<>(buildCategoryOptions(TransactionFilter.ALL));
        categoryFilterBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterBarPanel.add(categoryFilterBox, gbc);

        // --- From ---
        gbc.gridx = col++; gbc.weightx = 0;
        JLabel fromLbl = new JLabel("From:");
        fromLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        fromLbl.setForeground(new Color(71, 85, 105));
        filterBarPanel.add(fromLbl, gbc);

        gbc.gridx = col++; gbc.weightx = 0.15;
        fromDateField = new JTextField();
        fromDateField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fromDateField.setToolTipText("Start date (YYYY-MM-DD)");
        filterBarPanel.add(fromDateField, gbc);

        // --- To ---
        gbc.gridx = col++; gbc.weightx = 0;
        JLabel toLbl = new JLabel("To:");
        toLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        toLbl.setForeground(new Color(71, 85, 105));
        filterBarPanel.add(toLbl, gbc);

        gbc.gridx = col++; gbc.weightx = 0.15;
        toDateField = new JTextField();
        toDateField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        toDateField.setToolTipText("End date (YYYY-MM-DD)");
        filterBarPanel.add(toDateField, gbc);

        // --- Apply button ---
        gbc.gridx = col++; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        StyledButton applyBtn = new StyledButton("Apply Filters", ButtonType.PRIMARY, "Filter records by entered criteria");
        applyBtn.addActionListener(e -> refreshTable());
        filterBarPanel.add(applyBtn, gbc);

        // --- Reset button ---
        gbc.gridx = col; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        StyledButton resetBtn = new StyledButton("Reset", ButtonType.SECONDARY, "Clear all filter fields and show all entries");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilterBox.setSelectedIndex(0);
            fromDateField.setText("");
            toDateField.setText("");
            refreshTable();
        });
        filterBarPanel.add(resetBtn, gbc);

        // We add this inside a wrapper so the tab strip stays NORTH
        JPanel filterWrapper = new JPanel(new BorderLayout());
        filterWrapper.setOpaque(false);
        filterWrapper.add(filterBarPanel, BorderLayout.CENTER);

        // The CENTER will hold both filter bar and table; use a sub-layout
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(filterWrapper, BorderLayout.NORTH);

        // Store ref so we can add the table scroll pane to centerPanel later
        this.centerPanel = centerPanel;
        add(centerPanel, BorderLayout.CENTER);
    }

    // stored to allow initTable() to add scroll to it
    private JPanel centerPanel;

    // -------------------------------------------------------------------------
    //  Table
    // -------------------------------------------------------------------------

    private void initTable() {
        String[] columns = {"ID", "Date", "Type", "Category", "Amount (" + expenseService.getCurrency().getSymbol() + ")", "Description / Notes"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    boolean income = false;
                    if (row < currentDisplayedExpenses.size()) {
                        income = currentDisplayedExpenses.get(row).isIncome();
                    }
                    c.setBackground(income
                            ? (row % 2 == 0 ? INCOME_ROW_BG : INCOME_ROW_ALT)
                            : (row % 2 == 0 ? EXPENSE_ROW_BG : EXPENSE_ROW_ALT));
                } else {
                    c.setBackground(new Color(191, 219, 254)); // blue selection
                }
                return c;
            }
        };
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(30, 41, 59));
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 245, 249));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(95);
        table.getColumnModel().getColumn(2).setPreferredWidth(85);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(115);
        table.getColumnModel().getColumn(5).setPreferredWidth(280);

        // Right align amount column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        // Center-align Type column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));

        centerPanel.add(scrollPane, BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    //  Footer
    // -------------------------------------------------------------------------

    private void initFooter() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);

        // Left Action Buttons
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        StyledButton addExpenseBtn = new StyledButton("+ Add Expense", ButtonType.DANGER, "Open form to input a new expense record");
        addExpenseBtn.addActionListener(e -> onAddExpense());

        StyledButton addIncomeBtn = new StyledButton("+ Add Income", ButtonType.SUCCESS, "Open form to record a new income entry");
        addIncomeBtn.addActionListener(e -> onAddIncome());

        StyledButton editBtn = new StyledButton("Edit Selected", ButtonType.SECONDARY, "Edit the currently selected table row");
        editBtn.addActionListener(e -> onEditTransaction());

        StyledButton deleteBtn = new StyledButton("Delete Selected", ButtonType.SECONDARY, "Delete the currently selected row");
        deleteBtn.addActionListener(e -> onDeleteTransaction());

        leftActions.add(addExpenseBtn);
        leftActions.add(addIncomeBtn);
        leftActions.add(editBtn);
        leftActions.add(deleteBtn);

        // Right Action Buttons
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        totalSumLabel = new JLabel("Filtered Total: " + expenseService.formatAmount(0));
        totalSumLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalSumLabel.setForeground(new Color(30, 41, 59));

        StyledButton exportBtn = new StyledButton("Export CSV", ButtonType.SECONDARY, "Save current filtered list to a CSV file");
        exportBtn.addActionListener(e -> onExportCsv());

        StyledButton importBtn = new StyledButton("Import CSV", ButtonType.SECONDARY, "Import expense entries from a CSV file");
        importBtn.addActionListener(e -> onImportCsv());

        rightActions.add(totalSumLabel);
        rightActions.add(Box.createHorizontalStrut(15));
        rightActions.add(exportBtn);
        rightActions.add(importBtn);

        footerPanel.add(leftActions, BorderLayout.WEST);
        footerPanel.add(rightActions, BorderLayout.EAST);

        add(footerPanel, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    //  Refresh Table
    // -------------------------------------------------------------------------

    public void refreshTable() {
        // Update header currency symbol
        table.getColumnModel().getColumn(4).setHeaderValue("Amount (" + expenseService.getCurrency().getSymbol() + ")");
        table.getTableHeader().repaint();

        String searchKw = searchField.getText().trim();
        String selectedCat = (String) categoryFilterBox.getSelectedItem();

        LocalDate fromDate = null;
        if (!fromDateField.getText().trim().isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromDateField.getText().trim());
            } catch (DateTimeParseException e) { /* ignore */ }
        }

        LocalDate toDate = null;
        if (!toDateField.getText().trim().isEmpty()) {
            try {
                toDate = LocalDate.parse(toDateField.getText().trim());
            } catch (DateTimeParseException e) { /* ignore */ }
        }

        currentDisplayedExpenses = expenseService.filterTransactions(fromDate, toDate, selectedCat, searchKw, activeFilter);
        tableModel.setRowCount(0);

        double totalSum = 0.0;
        for (Expense e : currentDisplayedExpenses) {
            String typeLabel = e.isIncome() ? "💚 Income" : "🔴 Expense";
            String amountStr = expenseService.formatAmount(e.getAmount());
            // When showing all, prefix sign
            if (activeFilter == TransactionFilter.ALL) {
                amountStr = (e.isIncome() ? "+" : "-") + amountStr;
            }
            tableModel.addRow(new Object[]{
                    e.getId(),
                    e.getDate().toString(),
                    typeLabel,
                    e.getCategory(),
                    amountStr,
                    e.getNotes()
            });
            totalSum += e.isIncome() ? e.getAmount() : -e.getAmount();
        }

        // Label context-sensitive
        if (activeFilter == TransactionFilter.INCOME) {
            totalSumLabel.setText("Total Income: " + expenseService.formatAmount(
                    currentDisplayedExpenses.stream().mapToDouble(Expense::getAmount).sum()));
            totalSumLabel.setForeground(new Color(5, 150, 105));
        } else if (activeFilter == TransactionFilter.EXPENSE) {
            totalSumLabel.setText("Total Expenses: " + expenseService.formatAmount(
                    currentDisplayedExpenses.stream().mapToDouble(Expense::getAmount).sum()));
            totalSumLabel.setForeground(new Color(220, 38, 38));
        } else {
            Color balColor = totalSum >= 0 ? new Color(5, 150, 105) : new Color(220, 38, 38);
            totalSumLabel.setText("Net Balance: " + (totalSum >= 0 ? "+" : "") + expenseService.formatAmount(totalSum));
            totalSumLabel.setForeground(balColor);
        }

        table.repaint();
    }

    // -------------------------------------------------------------------------
    //  Category filter options (changes based on active tab)
    // -------------------------------------------------------------------------

    private String[] buildCategoryOptions(TransactionFilter filter) {
        List<String> cats = new ArrayList<>();
        cats.add("All Categories");
        if (filter == TransactionFilter.ALL || filter == TransactionFilter.EXPENSE) {
            for (String c : ExpenseCategory.getAllDisplayNames()) cats.add(c);
        }
        if (filter == TransactionFilter.ALL || filter == TransactionFilter.INCOME) {
            for (String c : IncomeCategory.getAllDisplayNames()) cats.add(c);
        }
        return cats.toArray(new String[0]);
    }

    private void updateCategoryFilterOptions() {
        String current = (String) categoryFilterBox.getSelectedItem();
        categoryFilterBox.setModel(new DefaultComboBoxModel<>(buildCategoryOptions(activeFilter)));
        // Preserve selection if still valid
        if (current != null) categoryFilterBox.setSelectedItem(current);
        if (categoryFilterBox.getSelectedIndex() == -1) categoryFilterBox.setSelectedIndex(0);
    }

    // -------------------------------------------------------------------------
    //  CRUD actions
    // -------------------------------------------------------------------------

    public void onAddExpense() {
        ExpenseDialog dialog = new ExpenseDialog(parentFrame, "Add New Expense", null, expenseService);
        dialog.setVisible(true);
        if (dialog.isConfirmed() && dialog.getExpense() != null) {
            expenseService.addExpense(dialog.getExpense());
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    public void onAddIncome() {
        IncomeDialog dialog = new IncomeDialog(parentFrame, "Add New Income", null, expenseService);
        dialog.setVisible(true);
        if (dialog.isConfirmed() && dialog.getIncome() != null) {
            expenseService.addIncome(dialog.getIncome());
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    private void onEditTransaction() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tableModel.getValueAt(selectedRow, 0);
        Expense selected = expenseService.getExpenseById(id);
        if (selected == null) return;

        if (selected.isIncome()) {
            IncomeDialog dialog = new IncomeDialog(parentFrame, "Edit Income Entry", selected, expenseService);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                Expense updated = dialog.getIncome();
                expenseService.updateExpense(id, updated.getAmount(), updated.getCategory(), updated.getDate(), updated.getNotes());
            }
        } else {
            ExpenseDialog dialog = new ExpenseDialog(parentFrame, "Edit Expense Record", selected, expenseService);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                Expense updated = dialog.getExpense();
                expenseService.updateExpense(id, updated.getAmount(), updated.getCategory(), updated.getDate(), updated.getNotes());
            }
        }

        refreshTable();
        if (onDataChanged != null) onDataChanged.run();
    }

    private void onDeleteTransaction() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tableModel.getValueAt(selectedRow, 0);
        Expense selected = expenseService.getExpenseById(id);
        String type = (selected != null && selected.isIncome()) ? "income" : "expense";
        String details = selected != null
                ? String.format("%s (%s - %s)", id, selected.getCategory(), expenseService.formatAmount(selected.getAmount()))
                : id;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently delete this " + type + " record ID: " + details + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            expenseService.deleteExpense(id);
            refreshTable();
            if (onDataChanged != null) onDataChanged.run();
        }
    }

    private void onExportCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Transactions to CSV File");
        fileChooser.setSelectedFile(new File("transactions.csv"));
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
