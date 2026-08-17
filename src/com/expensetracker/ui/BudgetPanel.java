package com.expensetracker.ui;

import com.expensetracker.model.Budget;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.components.StyledButton;
import com.expensetracker.ui.components.StyledButton.ButtonType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Panel for configuring monthly budgets, monitoring spending thresholds, and viewing budget alerts.
 */
public class BudgetPanel extends JPanel {

    private final ExpenseService expenseService;
    private final Runnable onDataChanged;

    private JTextField monthField;
    private JTextField budgetLimitField;

    private JLabel statusTitleLabel;
    private JLabel statusDetailLabel;
    private JProgressBar progressBar;

    private JTable historyTable;
    private DefaultTableModel historyTableModel;

    public BudgetPanel(ExpenseService expenseService, Runnable onDataChanged) {
        this.expenseService = expenseService;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(new Color(245, 247, 250));

        initUI();
        refreshBudgetView();
    }

    private void initUI() {
        // --- Top: Budget Input & Alert Status ---
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        topPanel.setOpaque(false);

        // Left Form: Configure Budget
        JPanel configCard = new JPanel(new BorderLayout());
        configCard.setBackground(Color.WHITE);
        configCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel configTitle = new JLabel("Configure Monthly Budget Limit");
        configTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        configTitle.setForeground(new Color(30, 41, 59));
        configCard.add(configTitle, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formGrid.add(new JLabel("Month (YYYY-MM):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        monthField = new JTextField(YearMonth.now().toString());
        monthField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formGrid.add(monthField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formGrid.add(new JLabel("Budget Limit (" + expenseService.getCurrency().getSymbol() + "):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        budgetLimitField = new JTextField();
        budgetLimitField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formGrid.add(budgetLimitField, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0;
        StyledButton saveBudgetBtn = new StyledButton("Save Budget Limit", ButtonType.PRIMARY, "Save and apply the monthly spending budget cap");
        saveBudgetBtn.addActionListener(e -> onSaveBudget());
        formGrid.add(saveBudgetBtn, gbc);

        configCard.add(formGrid, BorderLayout.CENTER);
        topPanel.add(configCard);

        // Right Card: Current Alert Status
        JPanel statusCard = new JPanel(new BorderLayout());
        statusCard.setBackground(Color.WHITE);
        statusCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel statusHeader = new JLabel("Budget Alert Status");
        statusHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusHeader.setForeground(new Color(30, 41, 59));
        statusCard.add(statusHeader, BorderLayout.NORTH);

        JPanel statusContent = new JPanel();
        statusContent.setLayout(new BoxLayout(statusContent, BoxLayout.Y_AXIS));
        statusContent.setOpaque(false);

        statusTitleLabel = new JLabel("Normal Spending");
        statusTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusTitleLabel.setForeground(new Color(16, 185, 129));

        statusDetailLabel = new JLabel("Spending is within budget limits.");
        statusDetailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusDetailLabel.setForeground(new Color(100, 116, 139));

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(250, 12));
        progressBar.setForeground(new Color(16, 185, 129));

        statusContent.add(Box.createVerticalStrut(10));
        statusContent.add(statusTitleLabel);
        statusContent.add(Box.createVerticalStrut(5));
        statusContent.add(statusDetailLabel);
        statusContent.add(Box.createVerticalStrut(15));
        statusContent.add(progressBar);

        statusCard.add(statusContent, BorderLayout.CENTER);
        topPanel.add(statusCard);

        add(topPanel, BorderLayout.NORTH);

        // --- Center: Budget History Table ---
        JPanel historyCard = new JPanel(new BorderLayout());
        historyCard.setBackground(Color.WHITE);
        historyCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel historyTitle = new JLabel("Monthly Budget Breakdown");
        historyTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        historyTitle.setForeground(new Color(30, 41, 59));
        historyCard.add(historyTitle, BorderLayout.NORTH);

        String[] columns = {"Month", "Total Spent", "Budget Limit", "Remaining", "Status"};
        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(historyTableModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setRowHeight(34);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        historyTable.getTableHeader().setBackground(new Color(241, 245, 249));
        historyTable.getTableHeader().setForeground(new Color(30, 41, 59));
        historyTable.setShowVerticalLines(false);
        historyTable.setGridColor(new Color(241, 245, 249));

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        historyCard.add(scrollPane, BorderLayout.CENTER);

        add(historyCard, BorderLayout.CENTER);
    }

    private void onSaveBudget() {
        String monthStr = monthField.getText().trim();
        String limitStr = budgetLimitField.getText().trim();

        YearMonth ym;
        try {
            ym = YearMonth.parse(monthStr);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid Month format. Use YYYY-MM (e.g. 2026-08).", "Invalid Month", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double limit;
        try {
            limit = Double.parseDouble(limitStr);
            if (limit <= 0) {
                JOptionPane.showMessageDialog(this, "Budget limit must be a positive number.", "Invalid Limit", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric budget amount.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        expenseService.setMonthlyBudget(ym, limit);
        JOptionPane.showMessageDialog(this, "Budget limit for " + ym + " updated to " + expenseService.formatAmount(limit), "Budget Saved", JOptionPane.INFORMATION_MESSAGE);

        refreshBudgetView();
        if (onDataChanged != null) onDataChanged.run();
    }

    public void refreshBudgetView() {
        YearMonth currentMonth = YearMonth.now();
        try {
            currentMonth = YearMonth.parse(monthField.getText().trim());
        } catch (Exception ignored) {}

        Budget currentBudget = expenseService.getMonthlyBudget(currentMonth);
        double spent = expenseService.getMonthlySpending(currentMonth);
        double limit = currentBudget.getMonthlyLimit();

        if (limit > 0) {
            budgetLimitField.setText(String.format("%.2f", limit));
            double rem = limit - spent;
            int pct = (int) Math.min(100, (spent / limit) * 100);
            progressBar.setValue(pct);

            ExpenseService.BudgetStatus status = expenseService.getBudgetStatus(currentMonth);
            if (status == ExpenseService.BudgetStatus.EXCEEDED) {
                statusTitleLabel.setText("BUDGET EXCEEDED!");
                statusTitleLabel.setForeground(new Color(225, 29, 72));
                statusDetailLabel.setText(String.format("Over limit by %s (Spent %s of %s)", expenseService.formatAmount(Math.abs(rem)), expenseService.formatAmount(spent), expenseService.formatAmount(limit)));
                progressBar.setForeground(new Color(225, 29, 72));
            } else if (status == ExpenseService.BudgetStatus.WARNING) {
                statusTitleLabel.setText("BUDGET WARNING (80%+)");
                statusTitleLabel.setForeground(new Color(234, 88, 12));
                statusDetailLabel.setText(String.format("Approaching budget limit. Remaining: %s", expenseService.formatAmount(rem)));
                progressBar.setForeground(new Color(234, 88, 12));
            } else {
                statusTitleLabel.setText("BUDGET ON TRACK");
                statusTitleLabel.setForeground(new Color(16, 185, 129));
                statusDetailLabel.setText(String.format("Remaining budget: %s of %s limit", expenseService.formatAmount(rem), expenseService.formatAmount(limit)));
                progressBar.setForeground(new Color(16, 185, 129));
            }
        } else {
            statusTitleLabel.setText("NO BUDGET SET");
            statusTitleLabel.setForeground(new Color(148, 163, 184));
            statusDetailLabel.setText("Enter a budget limit above to start monitoring expenses.");
            progressBar.setValue(0);
        }

        // Table history breakdown
        historyTableModel.setRowCount(0);
        YearMonth startMonth = currentMonth.minusMonths(5);
        for (int i = 0; i <= 6; i++) {
            YearMonth ym = startMonth.plusMonths(i);
            double mSpent = expenseService.getMonthlySpending(ym);
            double mLimit = expenseService.getMonthlyBudget(ym).getMonthlyLimit();
            double mRem = mLimit > 0 ? mLimit - mSpent : 0.0;
            String st = "No Limit";
            if (mLimit > 0) {
                if (mSpent > mLimit) st = "EXCEEDED";
                else if (mSpent >= mLimit * 0.8) st = "WARNING";
                else st = "OK";
            }

            historyTableModel.addRow(new Object[]{
                    ym.toString(),
                    expenseService.formatAmount(mSpent),
                    mLimit > 0 ? expenseService.formatAmount(mLimit) : "N/A",
                    mLimit > 0 ? expenseService.formatAmount(mRem) : "N/A",
                    st
            });
        }
    }
}
