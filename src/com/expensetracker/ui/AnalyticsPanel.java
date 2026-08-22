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
    // =========================================================================
    //  Donut Chart – Spending by Category
    // =========================================================================

    private class DonutChartPanel extends JPanel {

        // ---- hover state ----
        private String hoveredText  = null;
        private Color  hoveredColor = null;
        private int    mouseX = -1, mouseY = -1;

        public DonutChartPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                    new EmptyBorder(15, 15, 15, 15)
            ));
            setToolTipText(null); // disable default Swing tooltip

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    mouseX = e.getX();
                    mouseY = e.getY();
                    updateHover(e.getX(), e.getY());
                }
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hoveredText  = null;
                    hoveredColor = null;
                    mouseX = -1;
                    mouseY = -1;
                    repaint();
                }
            });
        }

        private void updateHover(int mx, int my) {
            Map<String, Double> catMap = expenseService.getSpendingByCategory(null);
            double total = catMap.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total <= 0) { hoveredText = null; repaint(); return; }

            int width  = getWidth();
            int height = getHeight();
            boolean stackVertical = width < 420;
            int size;
            int x, y;
            if (stackVertical) {
                size = Math.min(width - 40, height - 60);
                if (size < 60) size = 60;
                x = (width - size) / 2;
                y = 40;
            } else {
                size = Math.min(width - 40, height - 70);
                if (size < 60) size = 60;
                x = (width - size) / 2;
                y = (height - size) / 2 + 10;
            }

            double cx = x + size / 2.0;
            double cy = y + size / 2.0;
            double dist = Math.sqrt((mx - cx) * (mx - cx) + (my - cy) * (my - cy));
            double outerR = size / 2.0;
            double innerR = (size * 0.55) / 2.0; // matches holeSize / 2

            String found = null;
            Color  foundColor = null;

            if (dist >= innerR && dist <= outerR) {
                double angle = Math.toDegrees(Math.atan2(-(my - cy), mx - cx));
                double normalized = (90 - angle + 360) % 360;

                double startAngle = 0;
                for (Map.Entry<String, Double> entry : catMap.entrySet()) {
                    if (entry.getValue() <= 0) continue;
                    double sweep = (entry.getValue() / total) * 360.0;
                    if (normalized >= startAngle && normalized < startAngle + sweep) {
                        ExpenseCategory cat = ExpenseCategory.fromString(entry.getKey());
                        double pct = (entry.getValue() / total) * 100;
                        found      = String.format("%s  %.1f%%", cat.getDisplayName(), pct);
                        foundColor = cat.getColor();
                        break;
                    }
                    startAngle += sweep;
                }
            }

            if (!java.util.Objects.equals(found, hoveredText)) {
                hoveredText  = found;
                hoveredColor = foundColor;
                repaint();
            }
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

            boolean stackVertical = width < 420;
            int size;
            int x, y;

            if (stackVertical) {
                size = Math.min(width - 40, height - 60);
                if (size < 60) size = 60;
                x = (width - size) / 2;
                y = 40;
            } else {
                size = Math.min(width - 40, height - 70);
                if (size < 60) size = 60;
                x = (width - size) / 2;
                y = (height - size) / 2 + 10;
            }

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
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(new Color(100, 116, 139));
            FontMetrics fm = g2.getFontMetrics();
            String label = hoveredText != null ? "" : "Expenses";
            g2.drawString(label, holeX + (holeSize - fm.stringWidth(label)) / 2, holeY + holeSize / 2 - 8);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(new Color(15, 23, 42));
            String totalStr = expenseService.formatAmount(total);
            fm = g2.getFontMetrics();
            g2.drawString(totalStr, holeX + (holeSize - fm.stringWidth(totalStr)) / 2, holeY + holeSize / 2 + 12);

            // --- Hover tooltip ---
            if (hoveredText != null && mouseX >= 0 && mouseY >= 0) {
                String tip = hoveredText;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                fm = g2.getFontMetrics();
                int tipW = fm.stringWidth(tip) + 28;
                int tipH = fm.getHeight() + 12;

                int tipX = mouseX + 14;
                int tipY = mouseY - tipH / 2;
                if (tipX + tipW > width - 4) tipX = mouseX - tipW - 10;
                if (tipY < 4)                tipY = 4;
                if (tipY + tipH > height - 4) tipY = height - tipH - 4;

                // drop shadow
                g2.setColor(new Color(0, 0, 0, 35));
                g2.fillRoundRect(tipX + 2, tipY + 2, tipW, tipH, 10, 10);
                // background
                g2.setColor(new Color(15, 23, 42, 225));
                g2.fillRoundRect(tipX, tipY, tipW, tipH, 10, 10);
                // colored dot matching the slice
                if (hoveredColor != null) {
                    g2.setColor(hoveredColor);
                    g2.fillOval(tipX + 8, tipY + tipH / 2 - 5, 10, 10);
                }
                // text
                g2.setColor(Color.WHITE);
                g2.drawString(tip, tipX + 24, tipY + fm.getAscent() + 6);
            }
        }
    }

    // =========================================================================
    //  Income vs Expenses — Monthly Grouped Bar Chart (last 6 months)
    // =========================================================================

    private class IncomeVsExpenseBarChart extends JPanel {

        private static final int PAD_RIGHT  = 20;
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
            g2.drawString("Income vs Expenses", 15, 25);

            // Legend layout (side by side if wide enough, otherwise second row)
            boolean wideLegend = W >= 460;
            int lx = wideLegend ? W - 180 : 15;
            int ly = wideLegend ? 14 : 34;
            int padTop = wideLegend ? 50 : 65;

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

            // Calculate dynamic left padding for Y-axis currency labels
            int ySteps = 4;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fmY = g2.getFontMetrics();
            int maxLabelWidth = 40;
            for (int s = 0; s <= ySteps; s++) {
                String yLabel = expenseService.formatAmount(((double) s / ySteps) * scaleMax);
                maxLabelWidth = Math.max(maxLabelWidth, fmY.stringWidth(yLabel));
            }
            int padLeft = maxLabelWidth + 14;

            int chartX = padLeft;
            int chartY = padTop;
            int chartW = W - padLeft - PAD_RIGHT;
            int chartH = H - padTop - PAD_BOTTOM;

            if (chartW < 50 || chartH < 50) return;

            // Horizontal grid + Y labels
            g2.setStroke(new BasicStroke(1f));
            for (int s = 0; s <= ySteps; s++) {
                double ratio = (double) s / ySteps;
                int ly2 = chartY + chartH - (int) (ratio * chartH);
                g2.setColor(GRID_COLOR);
                g2.drawLine(chartX, ly2, chartX + chartW, ly2);
                double yVal = ratio * scaleMax;
                String yLabel = expenseService.formatAmount(yVal);
                g2.setColor(AXIS_COLOR);
                g2.drawString(yLabel, chartX - fmY.stringWidth(yLabel) - 5, ly2 + 4);
            }

            // Bars
            int n = months.size();
            int groupW = chartW / n;
            int barPad = (int) (groupW * 0.12);
            int barTotalW = groupW - 2 * barPad;
            int barW = Math.max(4, barTotalW / 2 - 2);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");

            for (int i = 0; i < n; i++) {
                int groupX = chartX + i * groupW + barPad;

                // Income bar
                int incH = (int) ((incomes[i] / scaleMax) * chartH);
                int incY = chartY + chartH - incH;
                g2.setColor(INCOME_COLOR);
                if (incH > 0) {
                    g2.fillRoundRect(groupX, incY, barW, incH, 4, 4);
                }

                // Expense bar
                int expH = (int) ((expenses[i] / scaleMax) * chartH);
                int expY = chartY + chartH - expH;
                g2.setColor(EXPENSE_COLOR);
                if (expH > 0) {
                    g2.fillRoundRect(groupX + barW + 2, expY, barW, expH, 4, 4);
                }

                // Net balance indicator (small line/text above bars)
                double net = incomes[i] - expenses[i];
                int maxBarH = Math.max(incH, expH);
                if (net != 0) {
                    int dotY = chartY + chartH - maxBarH - 12;
                    Color netColor = net > 0 ? new Color(5, 150, 105) : new Color(185, 28, 28);
                    g2.setColor(netColor);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    String netStr = (net > 0 ? "+" : "-") + expenseService.formatAmount(Math.abs(net));
                    FontMetrics fm = g2.getFontMetrics();
                    int nx = groupX + (barTotalW - fm.stringWidth(netStr)) / 2;
                    if (dotY > chartY + 2) {
                        g2.drawString(netStr, Math.max(chartX, nx), Math.max(chartY + 12, dotY));
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
            String summary = String.format("All-time: Inc %s  |  Exp %s  |  Bal %s%s",
                    expenseService.formatAmount(totalInc),
                    expenseService.formatAmount(totalExp),
                    totalBal >= 0 ? "+" : "",
                    expenseService.formatAmount(totalBal));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(100, 116, 139));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(summary, Math.max(10, (W - fm.stringWidth(summary)) / 2), chartY + chartH + 38);
        }
    }
}
