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
 * Dashboard panel displaying KPI summary metrics, budget progress alert, and recent expenses.
 */
public class DashboardPanel extends JPanel {

    private final ExpenseService expenseService;
    private final Runnable onAddExpenseRequested;

    private JLabel monthlyTotalValueLabel;
    private JLabel budgetLimitValueLabel;
    private JLabel budgetAlertBadgeLabel;
    private JProgressBar budgetProgressBar;

    private JLabel topCategoryLabel;
    private JLabel totalEntriesLabel;

    private JTable recentTable;
    private DefaultTableModel recentTableModel;

    public DashboardPanel(ExpenseService expenseService, Runnable onAddExpenseRequested) {
        this.expenseService = expenseService;
        this.onAddExpenseRequested = onAddExpenseRequested;

        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(new Color(245, 247, 250));

        initUI();
        refreshDashboard();
    }

    private void initUI() {
        // --- Top Cards Panel ---
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setOpaque(false);

        // Card 1: Monthly Total & Budget Status
        cardsPanel.add(createBudgetCard());

        // Card 2: Top Category
        cardsPanel.add(createMetricCard("Top Spending Category", topCategoryLabel = new JLabel("-"), new Color(124, 58, 237), "Highest category by spend"));

        // Card 3: Total Expense Entries
        cardsPanel.add(createMetricCard("Total Entries Logged", totalEntriesLabel = new JLabel("0"), new Color(16, 185, 129), "Active recorded expense entries"));

        add(cardsPanel, BorderLayout.NORTH);

        // --- Center Panel: Recent Expenses ---
        JPanel recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBackground(Color.WHITE);
        recentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        // Header with title and "+ Log New Expense" button
        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setOpaque(false);
        JLabel recentTitle = new JLabel("Recent Transactions");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        recentTitle.setForeground(new Color(30, 41, 59));

        StyledButton quickAddBtn = new StyledButton("Log New Expense", ButtonType.PRIMARY, "Click to open form and record a new expense");
        quickAddBtn.addActionListener(e -> {
            if (onAddExpenseRequested != null) onAddExpenseRequested.run();
        });

        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(quickAddBtn, BorderLayout.EAST);
        recentPanel.add(recentHeader, BorderLayout.NORTH);

        // Table
        String[] columns = {"Date", "Category", "Amount (" + expenseService.getCurrency().getSymbol() + ")", "Description"};
        recentTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recentTable = new JTable(recentTableModel);
        recentTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        recentTable.setRowHeight(34);
        recentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        recentTable.getTableHeader().setBackground(new Color(241, 245, 249));
        recentTable.getTableHeader().setForeground(new Color(30, 41, 59));
        recentTable.setShowVerticalLines(false);
        recentTable.setGridColor(new Color(241, 245, 249));

        // Amount right align
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        recentTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = new JScrollPane(recentTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        recentPanel.add(scrollPane, BorderLayout.CENTER);

        add(recentPanel, BorderLayout.CENTER);
    }

    private JPanel createBudgetCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 18, 15, 18)
        ));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("This Month's Budget");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(100, 116, 139));

        budgetAlertBadgeLabel = new JLabel(" NORMAL ");
        budgetAlertBadgeLabel.setOpaque(true);
        budgetAlertBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        budgetAlertBadgeLabel.setForeground(Color.WHITE);
        budgetAlertBadgeLabel.setBackground(new Color(16, 185, 129));
        budgetAlertBadgeLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(budgetAlertBadgeLabel, BorderLayout.EAST);

        // Body
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);

        monthlyTotalValueLabel = new JLabel("0.00");
        monthlyTotalValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        monthlyTotalValueLabel.setForeground(new Color(15, 23, 42));

        budgetLimitValueLabel = new JLabel("of limit");
        budgetLimitValueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        budgetLimitValueLabel.setForeground(new Color(100, 116, 139));

        budgetProgressBar = new JProgressBar(0, 100);
        budgetProgressBar.setPreferredSize(new Dimension(200, 10));
        budgetProgressBar.setForeground(new Color(37, 99, 235));
        budgetProgressBar.setBackground(new Color(226, 232, 240));

        bodyPanel.add(Box.createVerticalStrut(8));
        bodyPanel.add(monthlyTotalValueLabel);
        bodyPanel.add(budgetLimitValueLabel);
        bodyPanel.add(Box.createVerticalStrut(10));
        bodyPanel.add(budgetProgressBar);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(bodyPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor, String subtitle) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 18, 15, 18)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(accentColor);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(148, 163, 184));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(Box.createVerticalStrut(10));
        body.add(valueLabel);
        body.add(Box.createVerticalStrut(5));
        body.add(subLbl);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        return card;
    }

    public void refreshDashboard() {
        // Update column header currency symbol
        recentTable.getColumnModel().getColumn(2).setHeaderValue("Amount (" + expenseService.getCurrency().getSymbol() + ")");
        recentTable.getTableHeader().repaint();

        YearMonth currentMonth = YearMonth.now();
        double spent = expenseService.getMonthlySpending(currentMonth);
        double limit = expenseService.getMonthlyBudget(currentMonth).getMonthlyLimit();

        monthlyTotalValueLabel.setText(expenseService.formatAmount(spent));
        budgetLimitValueLabel.setText(limit > 0 ? String.format("of %s limit", expenseService.formatAmount(limit)) : "No budget set");

        if (limit > 0) {
            int pct = (int) Math.min(100, (spent / limit) * 100);
            budgetProgressBar.setValue(pct);

            ExpenseService.BudgetStatus status = expenseService.getBudgetStatus(currentMonth);
            if (status == ExpenseService.BudgetStatus.EXCEEDED) {
                budgetAlertBadgeLabel.setText(" BUDGET OVERRUN! ");
                budgetAlertBadgeLabel.setBackground(new Color(225, 29, 72)); // Crimson red
                budgetProgressBar.setForeground(new Color(225, 29, 72));
            } else if (status == ExpenseService.BudgetStatus.WARNING) {
                budgetAlertBadgeLabel.setText(" NEAR LIMIT (80%+) ");
                budgetAlertBadgeLabel.setBackground(new Color(234, 88, 12)); // Orange
                budgetProgressBar.setForeground(new Color(234, 88, 12));
            } else {
                budgetAlertBadgeLabel.setText(" ON TRACK ");
                budgetAlertBadgeLabel.setBackground(new Color(16, 185, 129)); // Green
                budgetProgressBar.setForeground(new Color(16, 185, 129));
            }
        } else {
            budgetProgressBar.setValue(0);
            budgetAlertBadgeLabel.setText(" NO BUDGET ");
            budgetAlertBadgeLabel.setBackground(new Color(148, 163, 184));
        }

        String topCat = expenseService.getTopCategory();
        topCategoryLabel.setText(topCat);

        List<Expense> all = expenseService.getAllExpenses();
        totalEntriesLabel.setText(String.valueOf(all.size()));

        // Populate recent transactions table (top 6)
        recentTableModel.setRowCount(0);
        int count = 0;
        for (Expense e : all) {
            recentTableModel.addRow(new Object[]{
                    e.getDate().toString(),
                    e.getCategory(),
                    expenseService.formatAmount(e.getAmount()),
                    e.getNotes()
            });
            count++;
            if (count >= 6) break;
        }
    }
}
