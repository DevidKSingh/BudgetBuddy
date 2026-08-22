package com.expensetracker.ui;

import com.expensetracker.model.Expense;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.components.StyledButton;
import com.expensetracker.ui.components.StyledButton.ButtonType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.util.List;

/**
 * Dashboard panel displaying 4 KPI summary cards (Total Income, Total Expenses, Net Balance, Budget Status),
 * and a recent unified transactions table showing both income and expense entries.
 */
public class DashboardPanel extends JPanel {

    private final ExpenseService expenseService;
    private final Frame parentFrame;
    private final Runnable onDataChanged;
    private final Runnable onAddExpenseRequested;

    // KPI card labels
    private JLabel totalIncomeValueLabel;
    private JLabel totalExpenseValueLabel;
    private JLabel netBalanceValueLabel;

    // Budget card widgets
    private JLabel monthlyTotalValueLabel;
    private JLabel budgetLimitValueLabel;
    private JLabel budgetAlertBadgeLabel;
    private JProgressBar budgetProgressBar;

    private JTable recentTable;
    private DefaultTableModel recentTableModel;
    private final java.util.List<Expense> currentRecentExpenses = new java.util.ArrayList<>();

    public DashboardPanel(ExpenseService expenseService, Frame parentFrame, Runnable onDataChanged, Runnable onAddExpenseRequested) {
        this.expenseService = expenseService;
        this.parentFrame = parentFrame;
        this.onDataChanged = onDataChanged;
        this.onAddExpenseRequested = onAddExpenseRequested;

        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(new Color(245, 247, 250));

        initUI();
        refreshDashboard();
    }

    public DashboardPanel(ExpenseService expenseService, Runnable onAddExpenseRequested) {
        this(expenseService, null, null, onAddExpenseRequested);
    }

    private void initUI() {
        // --- Top KPI Cards (Responsive 1x4 or 2x2) ---
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 14, 14));
        cardsPanel.setOpaque(false);

        cardsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = cardsPanel.getWidth();
                if (w < 850 && cardsPanel.getLayout() instanceof GridLayout) {
                    GridLayout gl = (GridLayout) cardsPanel.getLayout();
                    if (gl.getColumns() != 2) {
                        cardsPanel.setLayout(new GridLayout(2, 2, 14, 14));
                        cardsPanel.revalidate();
                    }
                } else if (w >= 850 && cardsPanel.getLayout() instanceof GridLayout) {
                    GridLayout gl = (GridLayout) cardsPanel.getLayout();
                    if (gl.getColumns() != 4) {
                        cardsPanel.setLayout(new GridLayout(1, 4, 14, 14));
                        cardsPanel.revalidate();
                    }
                }
            }
        });

        // Card 1: Total Income (green)
        cardsPanel.add(createSimpleCard(
                "Total Income",
                totalIncomeValueLabel = new JLabel("0.00"),
                new Color(16, 185, 129),
                "",
                "All-time income recorded"
        ));

        // Card 2: Total Expenses (red)
        cardsPanel.add(createSimpleCard(
                "Total Expenses",
                totalExpenseValueLabel = new JLabel("0.00"),
                new Color(220, 38, 38),
                "",
                "All-time expenses recorded"
        ));

        // Card 3: Net Balance (dynamic color)
        cardsPanel.add(createSimpleCard(
                "Net Balance",
                netBalanceValueLabel = new JLabel("0.00"),
                new Color(37, 99, 235),
                "",
                "Income minus Expenses"
        ));

        // Card 4: Budget Status card
        cardsPanel.add(createBudgetCard());

        add(cardsPanel, BorderLayout.NORTH);

        // --- Center Panel: Recent Transactions ---
        JPanel recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBackground(Color.WHITE);
        recentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        // Header
        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setOpaque(false);
        JLabel recentTitle = new JLabel("Recent Transactions");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        recentTitle.setForeground(new Color(30, 41, 59));

        StyledButton quickAddBtn = new StyledButton("+ Log New Expense", ButtonType.PRIMARY, "Click to open form and record a new expense");
        quickAddBtn.addActionListener(e -> {
            if (onAddExpenseRequested != null) onAddExpenseRequested.run();
        });

        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(quickAddBtn, BorderLayout.EAST);
        recentPanel.add(recentHeader, BorderLayout.NORTH);

        // Table with Type column
        String[] columns = {"ID", "Date", "Type", "Category", "Amount (" + expenseService.getCurrency().getSymbol() + ")", "Description"};
        recentTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recentTable = new JTable(recentTableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row) && row < recentTableModel.getRowCount()) {
                    String type = (String) recentTableModel.getValueAt(row, 2);
                    if ("Income".equals(type)) {
                        c.setBackground(row % 2 == 0 ? new Color(236, 253, 245) : new Color(220, 252, 231));
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    }
                } else if (isRowSelected(row)) {
                    c.setBackground(new Color(191, 219, 254));
                }
                return c;
            }
        };
        recentTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        recentTable.setRowHeight(34);
        recentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        recentTable.getTableHeader().setBackground(new Color(241, 245, 249));
        recentTable.getTableHeader().setForeground(new Color(30, 41, 59));
        recentTable.setShowVerticalLines(false);
        recentTable.setGridColor(new Color(241, 245, 249));

        // Column widths
        recentTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        recentTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        recentTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        recentTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        recentTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        recentTable.getColumnModel().getColumn(5).setPreferredWidth(230);

        // Right align amount
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        recentTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        // Center align Type
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        recentTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        // Mouse double-click and right-click context menu
        recentTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    onEditRecentTransaction();
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) showPopupMenu(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) showPopupMenu(e);
            }

            private void showPopupMenu(java.awt.event.MouseEvent e) {
                int row = recentTable.rowAtPoint(e.getPoint());
                if (row != -1 && !recentTable.isRowSelected(row)) {
                    recentTable.setRowSelectionInterval(row, row);
                }
                if (recentTable.getSelectedRow() != -1) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem editItem = new JMenuItem("Edit Record");
                    editItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    editItem.addActionListener(ev -> onEditRecentTransaction());

                    JMenuItem deleteItem = new JMenuItem("Delete Record");
                    deleteItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    deleteItem.addActionListener(ev -> onDeleteRecentTransaction());

                    popup.add(editItem);
                    popup.add(deleteItem);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // Key listener for Delete key shortcut
        recentTable.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE || e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE) {
                    onDeleteRecentTransaction();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(recentTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        recentPanel.add(scrollPane, BorderLayout.CENTER);

        add(recentPanel, BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    //  Card Builders
    // -------------------------------------------------------------------------

    private JPanel createSimpleCard(String title, JLabel valueLabel, Color accentColor, String icon, String subtitle) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 18, 15, 18)
        ));

        // Header row: title
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        headerRow.setOpaque(false);
        if (icon != null && !icon.isEmpty()) {
            JLabel iconLbl = new JLabel(icon);
            iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            headerRow.add(iconLbl);
        }
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(new Color(100, 116, 139));
        headerRow.add(titleLbl);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(new Color(148, 163, 184));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(Box.createVerticalStrut(8));
        body.add(valueLabel);
        body.add(Box.createVerticalStrut(4));
        body.add(subLbl);

        card.add(headerRow, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBudgetCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 18, 15, 18)
        ));

        // Header using GridBagLayout to prevent title/badge overlap
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        titleBox.setOpaque(false);
        JLabel titleLabel = new JLabel("Monthly Budget");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleBox.add(titleLabel);

        budgetAlertBadgeLabel = new JLabel(" ON TRACK ");
        budgetAlertBadgeLabel.setOpaque(true);
        budgetAlertBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        budgetAlertBadgeLabel.setForeground(Color.WHITE);
        budgetAlertBadgeLabel.setBackground(new Color(16, 185, 129));
        budgetAlertBadgeLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        headerPanel.add(titleBox, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        headerPanel.add(budgetAlertBadgeLabel, gbc);

        // Body
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);

        monthlyTotalValueLabel = new JLabel("0.00");
        monthlyTotalValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        monthlyTotalValueLabel.setForeground(new Color(15, 23, 42));

        budgetLimitValueLabel = new JLabel("of limit");
        budgetLimitValueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        budgetLimitValueLabel.setForeground(new Color(100, 116, 139));

        budgetProgressBar = new JProgressBar(0, 100);
        budgetProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        budgetProgressBar.setPreferredSize(new Dimension(100, 8));
        budgetProgressBar.setForeground(new Color(37, 99, 235));
        budgetProgressBar.setBackground(new Color(226, 232, 240));

        bodyPanel.add(Box.createVerticalStrut(8));
        bodyPanel.add(monthlyTotalValueLabel);
        bodyPanel.add(budgetLimitValueLabel);
        bodyPanel.add(Box.createVerticalStrut(8));
        bodyPanel.add(budgetProgressBar);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(bodyPanel, BorderLayout.CENTER);
        return card;
    }

    // -------------------------------------------------------------------------
    //  Refresh
    // -------------------------------------------------------------------------

    public void refreshDashboard() {
        // Update header currency symbol on recent table (Column 4 is Amount)
        recentTable.getColumnModel().getColumn(4).setHeaderValue("Amount (" + expenseService.getCurrency().getSymbol() + ")");
        recentTable.getTableHeader().repaint();

        // KPI: Total Income
        double totalIncome = expenseService.getTotalIncome();
        totalIncomeValueLabel.setText(expenseService.formatAmount(totalIncome));
        totalIncomeValueLabel.setForeground(new Color(16, 185, 129));

        // KPI: Total Expenses
        double totalExpenses = expenseService.getTotalSpending();
        totalExpenseValueLabel.setText(expenseService.formatAmount(totalExpenses));
        totalExpenseValueLabel.setForeground(new Color(220, 38, 38));

        // KPI: Net Balance
        double balance = expenseService.getCurrentBalance();
        netBalanceValueLabel.setText((balance >= 0 ? "+" : "") + expenseService.formatAmount(balance));
        netBalanceValueLabel.setForeground(balance >= 0 ? new Color(16, 185, 129) : new Color(220, 38, 38));

        // Budget Card
        YearMonth currentMonth = YearMonth.now();
        double spent = expenseService.getMonthlySpending(currentMonth);
        double limit = expenseService.getMonthlyBudget(currentMonth).getMonthlyLimit();

        monthlyTotalValueLabel.setText(expenseService.formatAmount(spent));
        budgetLimitValueLabel.setText(limit > 0
                ? String.format("of %s limit", expenseService.formatAmount(limit))
                : "No budget set");

        if (limit > 0) {
            int pct = (int) Math.min(100, (spent / limit) * 100);
            budgetProgressBar.setValue(pct);

            ExpenseService.BudgetStatus status = expenseService.getBudgetStatus(currentMonth);
            if (status == ExpenseService.BudgetStatus.EXCEEDED) {
                budgetAlertBadgeLabel.setText(" OVERRUN! ");
                budgetAlertBadgeLabel.setBackground(new Color(225, 29, 72));
                budgetProgressBar.setForeground(new Color(225, 29, 72));
            } else if (status == ExpenseService.BudgetStatus.WARNING) {
                budgetAlertBadgeLabel.setText(" NEAR LIMIT ");
                budgetAlertBadgeLabel.setBackground(new Color(234, 88, 12));
                budgetProgressBar.setForeground(new Color(234, 88, 12));
            } else {
                budgetAlertBadgeLabel.setText(" ON TRACK ");
                budgetAlertBadgeLabel.setBackground(new Color(16, 185, 129));
                budgetProgressBar.setForeground(new Color(16, 185, 129));
            }
        } else {
            budgetProgressBar.setValue(0);
            budgetAlertBadgeLabel.setText(" NO BUDGET ");
            budgetAlertBadgeLabel.setBackground(new Color(148, 163, 184));
        }

        // Recent Transactions (top 8 — mixed income + expense)
        recentTableModel.setRowCount(0);
        currentRecentExpenses.clear();
        List<Expense> all = expenseService.getAllTransactions();
        int count = 0;
        for (Expense e : all) {
            String typeLabel = e.isIncome() ? "Income" : "Expense";
            recentTableModel.addRow(new Object[]{
                    e.getId(),
                    e.getDate().toString(),
                    typeLabel,
                    e.getCategory(),
                    (e.isIncome() ? "+" : "-") + expenseService.formatAmount(e.getAmount()),
                    e.getNotes()
            });
            currentRecentExpenses.add(e);
            count++;
            if (count >= 8) break;
        }

        revalidate();
        repaint();
    }

    private void onEditRecentTransaction() {
        int selectedRow = recentTable.getSelectedRow();
        if (selectedRow == -1 || selectedRow >= currentRecentExpenses.size()) {
            JOptionPane.showMessageDialog(this, "Please select a row from the recent transactions table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Expense selected = currentRecentExpenses.get(selectedRow);
        if (selected == null) return;

        if (selected.isIncome()) {
            IncomeDialog dialog = new IncomeDialog(parentFrame, "Edit Income Entry", selected, expenseService);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                Expense updated = dialog.getIncome();
                expenseService.updateTransaction(selected.getId(), updated.getAmount(), updated.getCategory(), updated.getDate(), updated.getNotes(), updated.isIncome());
                if (onDataChanged != null) onDataChanged.run();
                else refreshDashboard();
            }
        } else {
            ExpenseDialog dialog = new ExpenseDialog(parentFrame, "Edit Expense Record", selected, expenseService);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                Expense updated = dialog.getExpense();
                expenseService.updateTransaction(selected.getId(), updated.getAmount(), updated.getCategory(), updated.getDate(), updated.getNotes(), updated.isIncome());
                if (onDataChanged != null) onDataChanged.run();
                else refreshDashboard();
            }
        }
    }

    private void onDeleteRecentTransaction() {
        int selectedRow = recentTable.getSelectedRow();
        if (selectedRow == -1 || selectedRow >= currentRecentExpenses.size()) {
            JOptionPane.showMessageDialog(this, "Please select a row from the recent transactions table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Expense selected = currentRecentExpenses.get(selectedRow);
        if (selected == null) return;

        String type = selected.isIncome() ? "income" : "expense";
        String details = String.format("%s (%s - %s)", selected.getId(), selected.getCategory(), expenseService.formatAmount(selected.getAmount()));

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently delete this " + type + " record ID: " + details + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            expenseService.deleteTransaction(selected.getId());
            if (onDataChanged != null) onDataChanged.run();
            else refreshDashboard();
        }
    }
}
