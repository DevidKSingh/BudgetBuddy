package com.expensetracker.ui;

import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.service.ExpenseService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Visual analytics panel with:
 *   Left:  Donut Chart (expense spending by category)
 *   Right: Monthly Income vs Expenses grouped bar chart (last 6 months)
 */
public class AnalyticsPanel extends JPanel {

    private final ExpenseService expenseService;
    private final DonutChartPanel donutChartPanel;
    private final IncomeVsExpenseBarChart incomeVsExpenseChart;

    public AnalyticsPanel(ExpenseService expenseService) {
        this.expenseService = expenseService;
        setLayout(new GridLayout(1, 2, 20, 20));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 247, 250));

        donutChartPanel = new DonutChartPanel();
        incomeVsExpenseChart = new IncomeVsExpenseBarChart();

        add(donutChartPanel);
        add(incomeVsExpenseChart);
    }

    public void refreshAnalytics() {
        donutChartPanel.repaint();
        incomeVsExpenseChart.repaint();
    }

    // =========================================================================
    //  Donut Chart – Spending by Category (unchanged)
    // =========================================================================

    private class DonutChartPanel extends JPanel {
        public DonutChartPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                    new EmptyBorder(15, 15, 15, 15)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2.setColor(new Color(30, 41, 59));
            g2.drawString("Spending by Category", 15, 25);

            Map<String, Double> catMap = expenseService.getSpendingByCategory(null);
            double total = catMap.values().stream().mapToDouble(Double::doubleValue).sum();

            if (total <= 0) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("No expense data recorded yet.", width / 4, height / 2);
                return;
            }

            int size = Math.min(width - 180, height - 70);
            int x = 20, y = 50;

            double startAngle = 90;
            for (Map.Entry<String, Double> entry : catMap.entrySet()) {
                if (entry.getValue() <= 0) continue;
                double angle = (entry.getValue() / total) * 360.0;
                ExpenseCategory categoryEnum = ExpenseCategory.fromString(entry.getKey());
                g2.setColor(categoryEnum.getColor());
                g2.fill(new Arc2D.Double(x, y, size, size, startAngle, -angle, Arc2D.PIE));
                startAngle -= angle;
            }

            // Donut hole
            int holeSize = (int) (size * 0.55);
            int holeX = x + (size - holeSize) / 2;
            int holeY = y + (size - holeSize) / 2;
            g2.setColor(Color.WHITE);
            g2.fillOval(holeX, holeY, holeSize, holeSize);

            // Center label
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(new Color(100, 116, 139));
            FontMetrics fm = g2.getFontMetrics();
            String label = "Expenses";
            g2.drawString(label, holeX + (holeSize - fm.stringWidth(label)) / 2, holeY + holeSize / 2 - 10);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g2.setColor(new Color(15, 23, 42));
            String totalStr = expenseService.formatAmount(total);
            fm = g2.getFontMetrics();
            g2.drawString(totalStr, holeX + (holeSize - fm.stringWidth(totalStr)) / 2, holeY + holeSize / 2 + 12);

            // Legend
            int legendX = x + size + 25;
            int legendY = 60;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (Map.Entry<String, Double> entry : catMap.entrySet()) {
                if (entry.getValue() <= 0) continue;
                ExpenseCategory cat = ExpenseCategory.fromString(entry.getKey());
                g2.setColor(cat.getColor());
                g2.fillRoundRect(legendX, legendY, 12, 12, 4, 4);
                g2.setColor(new Color(51, 65, 85));
                double pct = (entry.getValue() / total) * 100;
                g2.drawString(String.format("%s (%.1f%%)", cat.getDisplayName(), pct), legendX + 18, legendY + 10);
                legendY += 22;
                if (legendY > height - 20) break;
            }
        }
    }

    // =========================================================================
    //  Income vs Expenses — Monthly Grouped Bar Chart (last 6 months)
    // =========================================================================

    private class IncomeVsExpenseBarChart extends JPanel {

        private static final int PAD_LEFT   = 70;
        private static final int PAD_RIGHT  = 20;
        private static final int PAD_TOP    = 55;
        private static final int PAD_BOTTOM = 52;

        private static final Color INCOME_COLOR  = new Color(16, 185, 129);   // emerald
        private static final Color EXPENSE_COLOR = new Color(239, 68, 68);    // red-500
        private static final Color GRID_COLOR    = new Color(226, 232, 240);
        private static final Color AXIS_COLOR    = new Color(148, 163, 184);

        public IncomeVsExpenseBarChart() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                    new EmptyBorder(15, 15, 15, 15)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int W = getWidth();
            int H = getHeight();

            // Title
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2.setColor(new Color(30, 41, 59));
            g2.drawString("Income vs Expenses — Last 6 Months", 15, 25);

            // Legend
            int lx = W - 200;
            int ly = 14;
            g2.setColor(INCOME_COLOR);
            g2.fillRoundRect(lx, ly, 12, 12, 4, 4);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(51, 65, 85));
            g2.drawString("Income", lx + 16, ly + 10);

            g2.setColor(EXPENSE_COLOR);
            g2.fillRoundRect(lx + 70, ly, 12, 12, 4, 4);
            g2.setColor(new Color(51, 65, 85));
            g2.drawString("Expenses", lx + 86, ly + 10);

            // Collect last 6 months
            YearMonth now = YearMonth.now();
            List<YearMonth> months = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                months.add(now.minusMonths(i));
            }

            double[] incomes  = new double[6];
            double[] expenses = new double[6];
            double maxVal = 0.0;
            for (int i = 0; i < 6; i++) {
                incomes[i]  = expenseService.getMonthlyIncome(months.get(i));
                expenses[i] = expenseService.getMonthlySpending(months.get(i));
                maxVal = Math.max(maxVal, Math.max(incomes[i], expenses[i]));
            }

            if (maxVal <= 0) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                g2.setColor(new Color(148, 163, 184));
                String msg = "No income or expense data yet.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
                return;
            }

            double scaleMax = maxVal * 1.15;

            int chartX = PAD_LEFT;
            int chartY = PAD_TOP;
            int chartW = W - PAD_LEFT - PAD_RIGHT;
            int chartH = H - PAD_TOP - PAD_BOTTOM;

            // Horizontal grid + Y labels
            int ySteps = 4;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setStroke(new BasicStroke(1f));
            for (int s = 0; s <= ySteps; s++) {
                double ratio = (double) s / ySteps;
                int ly2 = chartY + chartH - (int) (ratio * chartH);
                g2.setColor(GRID_COLOR);
                g2.drawLine(chartX, ly2, chartX + chartW, ly2);
                double yVal = ratio * scaleMax;
                String yLabel = expenseService.formatAmount(yVal);
                g2.setColor(AXIS_COLOR);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(yLabel, chartX - fm.stringWidth(yLabel) - 5, ly2 + 4);
            }

            // Bars
            int n = months.size();
            int groupW = chartW / n;
            int barPad = (int) (groupW * 0.12);
            int barTotalW = groupW - 2 * barPad;
            int barW = Math.max(6, barTotalW / 2 - 2);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");

            for (int i = 0; i < n; i++) {
                int groupX = chartX + i * groupW + barPad;

                // Income bar
                int incH = (int) ((incomes[i] / scaleMax) * chartH);
                int incY = chartY + chartH - incH;
                g2.setColor(INCOME_COLOR);
                if (incH > 0) {
                    g2.fillRoundRect(groupX, incY, barW, incH, 4, 4);
                    // Value label on top
                    if (incH > 15) {
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                        String valStr = expenseService.formatAmount(incomes[i]);
                        FontMetrics fm = g2.getFontMetrics();
                        if (fm.stringWidth(valStr) + 4 < barW) {
                            g2.drawString(valStr, groupX + (barW - fm.stringWidth(valStr)) / 2, incY + 12);
                        }
                    }
                }

                // Expense bar
                int expH = (int) ((expenses[i] / scaleMax) * chartH);
                int expY = chartY + chartH - expH;
                g2.setColor(EXPENSE_COLOR);
                if (expH > 0) {
                    g2.fillRoundRect(groupX + barW + 2, expY, barW, expH, 4, 4);
                    if (expH > 15) {
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                        String valStr = expenseService.formatAmount(expenses[i]);
                        FontMetrics fm = g2.getFontMetrics();
                        if (fm.stringWidth(valStr) + 4 < barW) {
                            g2.drawString(valStr, groupX + barW + 2 + (barW - fm.stringWidth(valStr)) / 2, expY + 12);
                        }
                    }
                }

                // Net balance indicator (small dot/line above bars)
                double net = incomes[i] - expenses[i];
                int maxBarH = Math.max(incH, expH);
                if (net != 0) {
                    int dotY = chartY + chartH - maxBarH - 14;
                    Color netColor = net > 0 ? new Color(5, 150, 105) : new Color(185, 28, 28);
                    g2.setColor(netColor);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    String netStr = (net > 0 ? "+" : "-") + expenseService.formatAmount(Math.abs(net));
                    FontMetrics fm = g2.getFontMetrics();
                    int nx = groupX + (barTotalW - fm.stringWidth(netStr)) / 2;
                    if (dotY > chartY + 2) {
                        g2.drawString(netStr, nx, Math.max(chartY + 12, dotY));
                    }
                }

                // X label (month)
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(new Color(71, 85, 105));
                String lbl = months.get(i).format(fmt);
                FontMetrics fm = g2.getFontMetrics();
                int lblX = groupX + (barTotalW - fm.stringWidth(lbl)) / 2;
                g2.drawString(lbl, lblX, chartY + chartH + 18);
            }

            // Baseline
            g2.setColor(new Color(203, 213, 225));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(chartX, chartY + chartH, chartX + chartW, chartY + chartH);

            // Summary row at bottom
            double totalInc = expenseService.getTotalIncome();
            double totalExp = expenseService.getTotalSpending();
            double totalBal = totalInc - totalExp;
            String summary = String.format("All-time: Income %s  |  Expenses %s  |  Balance %s%s",
                    expenseService.formatAmount(totalInc),
                    expenseService.formatAmount(totalExp),
                    totalBal >= 0 ? "+" : "",
                    expenseService.formatAmount(totalBal));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(100, 116, 139));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(summary, (W - fm.stringWidth(summary)) / 2, chartY + chartH + 38);
        }
    }
}
