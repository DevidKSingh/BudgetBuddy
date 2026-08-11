package com.expensetracker.ui;

import com.expensetracker.service.ExpenseService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.YearMonth;

/**
 * Main Window Frame containing navigation sidebar, CardLayout view switching, and top alert banner.
 */
public class MainFrame extends JFrame {

    private final ExpenseService expenseService;

    private JPanel cardPanel;
    private CardLayout cardLayout;

    private DashboardPanel dashboardPanel;
    private ExpenseTablePanel expenseTablePanel;
    private AnalyticsPanel analyticsPanel;
    private BudgetPanel budgetPanel;
    private SettingsPanel settingsPanel;

    private JPanel alertBanner;
    private JLabel alertBannerLabel;

    private JButton activeNavButton;

    public MainFrame() {
        this.expenseService = new ExpenseService();
        initUI();
    }

    public MainFrame(ExpenseService expenseService) {
        this.expenseService = expenseService;
        initUI();
    }

    private void initUI() {
        setTitle("Daily Expense Tracker System");
        setSize(1150, 750);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Application Icon / Styling
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setLayout(new BorderLayout());

        // --- Alert Banner (Top) ---
        alertBanner = new JPanel(new BorderLayout());
        alertBanner.setBackground(new Color(225, 29, 72));
        alertBanner.setBorder(new EmptyBorder(8, 20, 8, 20));
        alertBannerLabel = new JLabel("");
        alertBannerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        alertBannerLabel.setForeground(Color.WHITE);
        alertBanner.add(alertBannerLabel, BorderLayout.CENTER);
        alertBanner.setVisible(false);
        add(alertBanner, BorderLayout.NORTH);

        // --- Sidebar (Left) ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(new Color(30, 41, 59)); // Dark slate navy
        sidebar.setBorder(new EmptyBorder(25, 15, 25, 15));

        // Logo Title
        JLabel appTitle = new JLabel("ExpenseTracker");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        appTitle.setForeground(Color.WHITE);
        appTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel appSubtitle = new JLabel("Personal Finance Assistant");
        appSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        appSubtitle.setForeground(new Color(148, 163, 184));
        appSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(appTitle);
        sidebar.add(appSubtitle);
        sidebar.add(Box.createVerticalStrut(25));

        // Navigation Buttons with clear titles and tooltips
        JButton navDashboard = createNavButton("   Dashboard", "DASHBOARD", "View summary overview, KPIs, and recent transactions");
        JButton navExpenses = createNavButton("   Expense Records", "EXPENSES", "View, search, filter, edit, or delete expense records");
        JButton navAnalytics = createNavButton("   Analytics & Charts", "ANALYTICS", "View category pie charts and monthly spending bar trends");
        JButton navBudget = createNavButton("   Budget & Alerts", "BUDGET", "Configure monthly budget limits and monitor spending warnings");
        JButton navSettings = createNavButton("   Currency & Settings", "SETTINGS", "Select active currency unit (USD, EUR, GBP, INR, JPY, CAD, etc.)");

        sidebar.add(navDashboard);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(navExpenses);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(navAnalytics);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(navBudget);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(navSettings);

        sidebar.add(Box.createVerticalGlue());

        // Quick Add Button at bottom of sidebar
        JButton quickAddBtn = new JButton("+ Log New Expense");
        quickAddBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        quickAddBtn.setBackground(new Color(37, 99, 235));
        quickAddBtn.setForeground(Color.WHITE);
        quickAddBtn.setFocusPainted(false);
        quickAddBtn.setToolTipText("Open modal form to record a new expense");
        quickAddBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        quickAddBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickAddBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        quickAddBtn.addActionListener(e -> expenseTablePanel.onAddExpense());
        sidebar.add(quickAddBtn);

        add(sidebar, BorderLayout.WEST);

        // --- Center CardLayout Views ---
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        dashboardPanel = new DashboardPanel(expenseService, () -> expenseTablePanel.onAddExpense());
        expenseTablePanel = new ExpenseTablePanel(expenseService, this, () -> refreshAllViews());
        analyticsPanel = new AnalyticsPanel(expenseService);
        budgetPanel = new BudgetPanel(expenseService, () -> refreshAllViews());
        settingsPanel = new SettingsPanel(expenseService, () -> refreshAllViews());

        cardPanel.add(dashboardPanel, "DASHBOARD");
        cardPanel.add(expenseTablePanel, "EXPENSES");
        cardPanel.add(analyticsPanel, "ANALYTICS");
        cardPanel.add(budgetPanel, "BUDGET");
        cardPanel.add(settingsPanel, "SETTINGS");

        add(cardPanel, BorderLayout.CENTER);

        // Set active view to Dashboard
        setActiveNav(navDashboard, "DASHBOARD");

        refreshAllViews();
    }

    private JButton createNavButton(String text, String cardName, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(new Color(203, 213, 225));
        btn.setBackground(new Color(30, 41, 59));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> setActiveNav(btn, cardName));
        return btn;
    }

    private void setActiveNav(JButton btn, String cardName) {
        if (activeNavButton != null) {
            activeNavButton.setBackground(new Color(30, 41, 59));
            activeNavButton.setForeground(new Color(203, 213, 225));
            activeNavButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }

        activeNavButton = btn;
        activeNavButton.setBackground(new Color(51, 65, 85));
        activeNavButton.setForeground(Color.WHITE);
        activeNavButton.setFont(new Font("Segoe UI", Font.BOLD, 13));

        cardLayout.show(cardPanel, cardName);

        if ("ANALYTICS".equals(cardName)) {
            analyticsPanel.refreshAnalytics();
        } else if ("DASHBOARD".equals(cardName)) {
            dashboardPanel.refreshDashboard();
        } else if ("EXPENSES".equals(cardName)) {
            expenseTablePanel.refreshTable();
        } else if ("BUDGET".equals(cardName)) {
            budgetPanel.refreshBudgetView();
        } else if ("SETTINGS".equals(cardName)) {
            settingsPanel.refreshSettingsView();
        }
    }

    public void refreshAllViews() {
        dashboardPanel.refreshDashboard();
        expenseTablePanel.refreshTable();
        analyticsPanel.refreshAnalytics();
        budgetPanel.refreshBudgetView();
        settingsPanel.refreshSettingsView();

        // Check budget warning for alert banner
        YearMonth currentMonth = YearMonth.now();
        ExpenseService.BudgetStatus status = expenseService.getBudgetStatus(currentMonth);
        if (status == ExpenseService.BudgetStatus.EXCEEDED) {
            double spent = expenseService.getMonthlySpending(currentMonth);
            double limit = expenseService.getMonthlyBudget(currentMonth).getMonthlyLimit();
            alertBannerLabel.setText(String.format("⚠️ WARNING: You have EXCEEDED your budget limit for %s! Spent %s of %s limit.", currentMonth, expenseService.formatAmount(spent), expenseService.formatAmount(limit)));
            alertBanner.setBackground(new Color(225, 29, 72));
            alertBanner.setVisible(true);
        } else if (status == ExpenseService.BudgetStatus.WARNING) {
            double spent = expenseService.getMonthlySpending(currentMonth);
            double limit = expenseService.getMonthlyBudget(currentMonth).getMonthlyLimit();
            alertBannerLabel.setText(String.format("⚠️ WARNING: You have reached %.0f%% of your budget limit for %s! Spent %s of %s.", (spent / limit) * 100, currentMonth, expenseService.formatAmount(spent), expenseService.formatAmount(limit)));
            alertBanner.setBackground(new Color(234, 88, 12));
            alertBanner.setVisible(true);
        } else {
            alertBanner.setVisible(false);
        }

        revalidate();
        repaint();
    }
}
