package com.expensetracker.ui;

import com.expensetracker.model.Budget;
import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.service.ExpenseService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Visual analytics panel with Donut Chart (category breakdown) and
 * a smooth daily spending area chart for the current month.
 */
public class AnalyticsPanel extends JPanel {

    private final ExpenseService expenseService;
    private final DonutChartPanel donutChartPanel;
    private final DailyAreaChartPanel dailyAreaChartPanel;

    public AnalyticsPanel(ExpenseService expenseService) {
        this.expenseService = expenseService;
        setLayout(new GridLayout(1, 2, 20, 20));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 247, 250));

        donutChartPanel = new DonutChartPanel();
        dailyAreaChartPanel = new DailyAreaChartPanel();

        add(donutChartPanel);
        add(dailyAreaChartPanel);
    }

    public void refreshAnalytics() {
        donutChartPanel.repaint();
        dailyAreaChartPanel.repaint();
    }

    // -------------------------------------------------------------------------
    //  Donut Chart – Spending by Category (unchanged logic)
    // -------------------------------------------------------------------------

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
            String label = "Total";
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

    // -------------------------------------------------------------------------
    //  Daily Spending Area Chart – current month, one point per day
    // -------------------------------------------------------------------------

    private class DailyAreaChartPanel extends JPanel {

        private static final int PAD_LEFT   = 62;
        private static final int PAD_RIGHT  = 20;
        private static final int PAD_TOP    = 45;
        private static final int PAD_BOTTOM = 48;

        // Accent colors
        private static final Color LINE_COLOR = new Color(59, 130, 246);       // blue-500
        private static final Color FILL_TOP   = new Color(59, 130, 246, 90);   // translucent
        private static final Color FILL_BOT   = new Color(59, 130, 246, 0);    // transparent
        private static final Color GRID_COLOR = new Color(226, 232, 240);
        private static final Color AXIS_COLOR = new Color(148, 163, 184);
        private static final Color DOT_FILL   = Color.WHITE;
        private static final Color BUDGET_LINE= new Color(239, 68, 68, 180);   // red, semi-transparent

        public DailyAreaChartPanel() {
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);

            int W = getWidth();
            int H = getHeight();

            // --- Title + subtitle ---
            YearMonth currentMonth = YearMonth.now();
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2.setColor(new Color(30, 41, 59));
            g2.drawString("Daily Spending — " + currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), 15, 25);

            // --- Fetch data ---
            Map<LocalDate, Double> rawMap = expenseService.getDailySpending(currentMonth);

            if (rawMap.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                g2.setColor(new Color(148, 163, 184));
                String msg = "No expenses recorded for " + currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + " yet.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
                return;
            }

            // Build sorted day list from day-1 through today (or end of month if past)
            LocalDate firstDay = currentMonth.atDay(1);
            LocalDate lastDay  = LocalDate.now().isAfter(currentMonth.atEndOfMonth())
                    ? currentMonth.atEndOfMonth()
                    : LocalDate.now();

            List<LocalDate> days = new ArrayList<>();
            for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
                days.add(d);
            }
            if (days.isEmpty()) {
                days.add(LocalDate.now());
            }

            double[] values = new double[days.size()];
            double maxVal = 0;
            for (int i = 0; i < days.size(); i++) {
                values[i] = rawMap.getOrDefault(days.get(i), 0.0);
                if (values[i] > maxVal) maxVal = values[i];
            }

            // Include budget in scale if set
            Budget budget = expenseService.getMonthlyBudget(currentMonth);
            double budgetLimit = budget != null ? budget.getMonthlyLimit() : 0;
            double scaleMax = Math.max(maxVal * 1.15, budgetLimit > 0 ? budgetLimit * 1.1 : 1);

            // --- Chart area bounds ---
            int chartX = PAD_LEFT;
            int chartY = PAD_TOP;
            int chartW = W - PAD_LEFT - PAD_RIGHT;
            int chartH = H - PAD_TOP - PAD_BOTTOM;

            // --- Horizontal grid lines + Y labels ---
            int ySteps = 4;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setStroke(new BasicStroke(1f));
            for (int s = 0; s <= ySteps; s++) {
                double ratio = (double) s / ySteps;
                int ly = chartY + chartH - (int) (ratio * chartH);
                g2.setColor(GRID_COLOR);
                g2.drawLine(chartX, ly, chartX + chartW, ly);

                double yVal = ratio * scaleMax;
                String yLabel = expenseService.formatAmount(yVal);
                g2.setColor(AXIS_COLOR);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(yLabel, chartX - fm.stringWidth(yLabel) - 5, ly + 4);
            }

            // --- Compute pixel coordinates for each data point ---
            int n = days.size();
            int[] px = new int[n];
            int[] py = new int[n];
            for (int i = 0; i < n; i++) {
                px[i] = chartX + (n == 1 ? chartW / 2 : (int) ((double) i / (n - 1) * chartW));
                py[i] = chartY + chartH - (int) ((values[i] / scaleMax) * chartH);
            }

            // --- Smooth area fill using cubic Bezier ---
            Path2D area = buildSmoothPath(px, py, true, chartX, chartY, chartH);
            GradientPaint gp = new GradientPaint(0, chartY, FILL_TOP, 0, chartY + chartH, FILL_BOT);
            g2.setPaint(gp);
            g2.fill(area);

            // --- Line on top ---
            Path2D line = buildSmoothPath(px, py, false, chartX, chartY, chartH);
            g2.setColor(LINE_COLOR);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(line);

            // --- Budget limit dashed line ---
            if (budgetLimit > 0) {
                int budgetY = chartY + chartH - (int) ((budgetLimit / scaleMax) * chartH);
                g2.setColor(BUDGET_LINE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                        new float[]{6f, 4f}, 0f));
                g2.drawLine(chartX, budgetY, chartX + chartW, budgetY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString("Budget", chartX + chartW - 40, budgetY - 4);
                g2.setStroke(new BasicStroke(1f));
            }

            // --- Data point dots ---
            for (int i = 0; i < n; i++) {
                if (values[i] > 0) {
                    g2.setColor(LINE_COLOR);
                    g2.fillOval(px[i] - 5, py[i] - 5, 10, 10);
                    g2.setColor(DOT_FILL);
                    g2.fillOval(px[i] - 3, py[i] - 3, 6, 6);
                }
            }

            // --- X-axis day labels (show every N days to avoid overlap) ---
            g2.setStroke(new BasicStroke(1f));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(AXIS_COLOR);
            int labelEvery = Math.max(1, n / 8);
            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("d MMM");
            for (int i = 0; i < n; i += labelEvery) {
                String lbl = days.get(i).format(dayFmt);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lbl, px[i] - fm.stringWidth(lbl) / 2, chartY + chartH + 18);
                // Tick
                g2.drawLine(px[i], chartY + chartH, px[i], chartY + chartH + 5);
            }

            // --- Baseline axis ---
            g2.setColor(new Color(203, 213, 225));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(chartX, chartY + chartH, chartX + chartW, chartY + chartH);

            // --- Value callout for the highest spending day ---
            int maxIdx = 0;
            for (int i = 1; i < n; i++) {
                if (values[i] > values[maxIdx]) maxIdx = i;
            }
            if (values[maxIdx] > 0) {
                String callout = expenseService.formatAmount(values[maxIdx]);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int cw = fm.stringWidth(callout) + 10;
                int ch = 20;
                int cx = px[maxIdx] - cw / 2;
                int cy = py[maxIdx] - ch - 8;
                // Clamp to chart bounds
                cx = Math.max(chartX, Math.min(cx, chartX + chartW - cw));
                cy = Math.max(chartY + 2, cy);

                // Pill background
                g2.setColor(LINE_COLOR);
                g2.fillRoundRect(cx, cy, cw, ch, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawString(callout, cx + 5, cy + ch - 5);
            }
        }

        /**
         * Builds a smooth cubic Bezier path through the data points.
         * If {@code close} is true, closes the path along the baseline (for area fill).
         */
        private Path2D buildSmoothPath(int[] px, int[] py, boolean close,
                                       int chartX, int chartY, int chartH) {
            Path2D path = new Path2D.Double();
            int n = px.length;
            if (n == 1) {
                path.moveTo(px[0], py[0]);
                if (close) {
                    path.lineTo(px[0], chartY + chartH);
                    path.lineTo(chartX, chartY + chartH);
                    path.closePath();
                }
                return path;
            }

            path.moveTo(px[0], py[0]);
            for (int i = 0; i < n - 1; i++) {
                double cpx1 = px[i] + (px[i + 1] - px[i]) * 0.4;
                double cpy1 = py[i];
                double cpx2 = px[i + 1] - (px[i + 1] - px[i]) * 0.4;
                double cpy2 = py[i + 1];
                path.curveTo(cpx1, cpy1, cpx2, cpy2, px[i + 1], py[i + 1]);
            }

            if (close) {
                path.lineTo(px[n - 1], chartY + chartH);
                path.lineTo(px[0], chartY + chartH);
                path.closePath();
            }
            return path;
        }
    }
}
