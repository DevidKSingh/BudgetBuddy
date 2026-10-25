package com.expensetracker.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * A modern date-picker field composed of a read-only styled text field and a
 * calendar-icon button. Clicking the button (or the field) opens a floating
 * popup calendar with month/year navigation, a day grid, a "Today" shortcut,
 * and a "Clear" option.
 */
public class DatePickerField extends JPanel {

    // ── Design tokens ────────────────────────────────────────────────────────
    private static final Color BG_FIELD      = Color.WHITE;
    private static final Color BORDER_NORMAL = new Color(203, 213, 225);
    private static final Color BORDER_FOCUS  = new Color(37, 99, 235);
    private static final Color TEXT_MAIN     = new Color(30, 41, 59);
    private static final Color TEXT_MUTED    = new Color(148, 163, 184);
    private static final Color ACCENT        = new Color(37, 99, 235);
    private static final Color ACCENT_LIGHT  = new Color(219, 234, 254);
    private static final Color TODAY_BG      = new Color(239, 246, 255);
    private static final Color POPUP_BG      = Color.WHITE;
    private static final Color HEADER_BG     = new Color(37, 99, 235);
    private static final Color WEEKEND_FG    = new Color(220, 38, 38);
    private static final Color DAY_HOVER     = new Color(241, 245, 249);
    private static final Color OUTSIDE_MONTH = new Color(203, 213, 225);

    private static final DateTimeFormatter DISPLAY_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter PARSE_FMT    = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── State ────────────────────────────────────────────────────────────────
    private LocalDate selectedDate;
    private YearMonth  viewMonth;

    // ── Child widgets ────────────────────────────────────────────────────────
    private final JTextField displayField;
    private final JButton    calBtn;
    private JWindow          popup;
    private boolean          popupOpen = false;
    private final String     placeholder;

    // ── Listeners ────────────────────────────────────────────────────────────
    private Runnable onDateChanged;

    // ─────────────────────────────────────────────────────────────────────────

    public DatePickerField(String placeholder) {
        this.placeholder = placeholder;
        this.viewMonth   = YearMonth.now();

        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        // Outer rounded panel that paints the border
        JPanel wrapper = new JPanel(new BorderLayout(0, 0)) {
            private Color currentBorder = BORDER_NORMAL;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_FIELD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(displayField.isFocusOwner() ? BORDER_FOCUS : BORDER_NORMAL);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);

        // ── Text field ───────────────────────────────────────────────────────
        displayField = new JTextField(12) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(TEXT_MUTED);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(placeholder, getInsets().left, y);
                    g2.dispose();
                }
            }
        };
        displayField.setEditable(false);
        displayField.setOpaque(false);
        displayField.setBorder(new EmptyBorder(8, 12, 8, 4));
        displayField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        displayField.setForeground(TEXT_MAIN);
        displayField.setCursor(new Cursor(Cursor.HAND_CURSOR));
        displayField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { togglePopup(); }
        });

        // ── Calendar icon button ─────────────────────────────────────────────
        calBtn = new JButton(calendarIcon()) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 6, 6);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        calBtn.setFocusPainted(false);
        calBtn.setBorderPainted(false);
        calBtn.setContentAreaFilled(false);
        calBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        calBtn.setBorder(new EmptyBorder(6, 8, 6, 10));
        calBtn.setToolTipText("Pick a date");
        calBtn.addActionListener(e -> togglePopup());

        wrapper.add(displayField, BorderLayout.CENTER);
        wrapper.add(calBtn,       BorderLayout.EAST);
        add(wrapper, BorderLayout.CENTER);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns the currently selected date, or {@code null} if nothing is selected. */
    public LocalDate getDate() { return selectedDate; }

    /**
     * Returns the date formatted as {@code yyyy-MM-dd}, or an empty string if
     * no date is selected — compatible with the existing filter logic.
     */
    public String getText() {
        return selectedDate != null ? selectedDate.format(PARSE_FMT) : "";
    }

    /** Programmatically set a date (pass {@code null} to clear). */
    public void setDate(LocalDate date) {
        this.selectedDate = date;
        this.viewMonth = (date != null) ? YearMonth.from(date) : YearMonth.now();
        updateDisplay();
    }

    /** Clear the selected date. */
    public void clear() { setDate(null); }

    /** Register a callback that fires whenever the date changes. */
    public void setOnDateChanged(Runnable r) { this.onDateChanged = r; }

    // ── Popup ─────────────────────────────────────────────────────────────────

    private void togglePopup() {
        if (popupOpen) {
            closePopup();
        } else {
            openPopup();
        }
    }

    private void openPopup() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) return;

        popup = new JWindow(owner);
        popup.setFocusableWindowState(true);
        popup.setContentPane(buildCalendarPanel());
        popup.pack();

        // Position below the field (or above if it overflows screen)
        Point loc = getLocationOnScreen();
        int popupW = popup.getWidth();
        int popupH = popup.getHeight();
        int targetX = loc.x;
        int targetY = loc.y + getHeight() + 2;

        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle screenBounds = (gc != null) ? gc.getBounds() : new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        // Check vertical overflow: flip above if not enough room below
        if (targetY + popupH > screenBounds.y + screenBounds.height) {
            int aboveY = loc.y - popupH - 2;
            if (aboveY >= screenBounds.y) {
                targetY = aboveY;
            } else {
                targetY = Math.max(screenBounds.y, screenBounds.y + screenBounds.height - popupH - 10);
            }
        }

        // Check horizontal overflow
        if (targetX + popupW > screenBounds.x + screenBounds.width) {
            targetX = Math.max(screenBounds.x, screenBounds.x + screenBounds.width - popupW - 10);
        }
        if (targetX < screenBounds.x) {
            targetX = screenBounds.x;
        }

        popup.setLocation(targetX, targetY);

        popup.setVisible(true);
        popupOpen = true;

        // Close when focus leaves the popup
        popup.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent e) { closePopup(); }
        });
    }

    private void closePopup() {
        if (popup != null) {
            popup.setVisible(false);
            popup.dispose();
            popup = null;
        }
        popupOpen = false;
    }

    // ── Calendar Panel ────────────────────────────────────────────────────────

    private JPanel buildCalendarPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        root.setBackground(POPUP_BG);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildDayGrid(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        return root;
    }

    // ── Header (prev / MonthYear / next) ─────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel monthLabel = new JLabel(viewMonth.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + viewMonth.getYear());
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        monthLabel.setForeground(Color.WHITE);

        JButton prev = arrowButton("‹");
        JButton next = arrowButton("›");

        prev.addActionListener(e -> shiftMonth(-1));
        next.addActionListener(e -> shiftMonth(+1));

        header.add(prev,       BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(next,       BorderLayout.EAST);

        // store label reference for re-draw
        header.putClientProperty("monthLabel", monthLabel);
        return header;
    }

    private JButton arrowButton(String symbol) {
        JButton btn = new JButton(symbol) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(0, 4, 0, 4));
        return btn;
    }

    // ── Day grid ─────────────────────────────────────────────────────────────

    private JPanel buildDayGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
        grid.setBackground(POPUP_BG);
        grid.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] dayNames = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        for (int i = 0; i < 7; i++) {
            JLabel lbl = new JLabel(dayNames[i], JLabel.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(i == 0 || i == 6 ? WEEKEND_FG : TEXT_MUTED);
            grid.add(lbl);
        }

        LocalDate today     = LocalDate.now();
        LocalDate firstDay  = viewMonth.atDay(1);
        int startDow        = firstDay.getDayOfWeek().getValue() % 7; // Sun=0
        int daysInMonth     = viewMonth.lengthOfMonth();

        // Leading blanks from previous month
        YearMonth prevMonth = viewMonth.minusMonths(1);
        int prevLen = prevMonth.lengthOfMonth();
        for (int i = startDow - 1; i >= 0; i--) {
            grid.add(dayCell(prevLen - i, true, false, false, null));
        }

        // Days of current month
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date    = viewMonth.atDay(d);
            boolean isToday   = date.equals(today);
            boolean isSelected= date.equals(selectedDate);
            int dow = date.getDayOfWeek().getValue() % 7; // Sun=0, Sat=6
            boolean isWeekend = (dow == 0 || dow == 6);
            grid.add(dayCell(d, false, isToday, isSelected, date));
        }

        // Trailing blanks
        int totalCells = startDow + daysInMonth;
        int trailing   = (7 - (totalCells % 7)) % 7;
        for (int d = 1; d <= trailing; d++) {
            grid.add(dayCell(d, true, false, false, null));
        }

        return grid;
    }

    private JLabel dayCell(int day, boolean outside, boolean today, boolean selected, LocalDate date) {
        JLabel cell = new JLabel(String.valueOf(day), JLabel.CENTER) {
            private boolean hovered = false;

            {
                if (date != null) {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                        @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                        @Override public void mouseClicked(MouseEvent e) {
                            selectDate(date);
                        }
                    });
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth()  - size) / 2;
                int y = (getHeight() - size) / 2;

                if (selected) {
                    g2.setColor(ACCENT);
                    g2.fillOval(x, y, size, size);
                    setForeground(Color.WHITE);
                } else if (hovered && date != null) {
                    g2.setColor(DAY_HOVER);
                    g2.fillOval(x, y, size, size);
                    setForeground(today ? ACCENT : (outside ? OUTSIDE_MONTH : TEXT_MAIN));
                } else if (today && !selected) {
                    g2.setColor(TODAY_BG);
                    g2.fillOval(x, y, size, size);
                    g2.setColor(ACCENT);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(x, y, size, size);
                    setForeground(ACCENT);
                } else {
                    setForeground(outside ? OUTSIDE_MONTH : TEXT_MAIN);
                    // weekend tint (only for in-month non-selected non-today)
                    if (!outside && date != null) {
                        int dow = date.getDayOfWeek().getValue() % 7;
                        if (dow == 0 || dow == 6) setForeground(WEEKEND_FG);
                    }
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        cell.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 14));
        cell.setPreferredSize(new Dimension(38, 38));
        return cell;
    }

    // ── Footer (Today / Clear) ────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                new EmptyBorder(2, 0, 2, 0)));

        JButton todayBtn = footerButton("Today", ACCENT, Color.WHITE);
        todayBtn.addActionListener(e -> selectDate(LocalDate.now()));

        JButton clearBtn = footerButton("Clear", new Color(241, 245, 249), new Color(71, 85, 105));
        clearBtn.addActionListener(e -> { selectedDate = null; updateDisplay(); closePopup(); fireChanged(); });

        footer.add(todayBtn);
        footer.add(clearBtn);
        return footer;
    }

    private JButton footerButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 20, 7, 20));
        return btn;
    }

    // ── Month navigation ──────────────────────────────────────────────────────

    private void shiftMonth(int delta) {
        viewMonth = viewMonth.plusMonths(delta);
        rebuildPopup();
    }

    private void rebuildPopup() {
        if (popup == null) return;
        popup.setContentPane(buildCalendarPanel());
        popup.pack();
        popup.revalidate();
        popup.repaint();
    }

    // ── Selection helper ──────────────────────────────────────────────────────

    private void selectDate(LocalDate date) {
        selectedDate = date;
        viewMonth    = YearMonth.from(date);
        updateDisplay();
        closePopup();
        fireChanged();
    }

    private void updateDisplay() {
        displayField.setText(selectedDate != null ? selectedDate.format(DISPLAY_FMT) : "");
    }

    private void fireChanged() {
        if (onDateChanged != null) onDateChanged.run();
    }

    // ── Calendar SVG-like icon (painted) ─────────────────────────────────────

    private Icon calendarIcon() {
        return new Icon() {
            @Override public int getIconWidth()  { return 18; }
            @Override public int getIconHeight() { return 18; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color iconColor = new Color(100, 116, 139);
                g2.setColor(iconColor);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Outer rounded rect
                g2.drawRoundRect(x + 1, y + 2, 16, 14, 3, 3);
                // Top binding strip
                g2.setColor(new Color(37, 99, 235));
                g2.fillRoundRect(x + 1, y + 2, 16, 5, 3, 3);
                g2.setColor(iconColor);
                g2.fillRect(x + 1, y + 5, 16, 2);

                // Ring pegs
                g2.setColor(new Color(37, 99, 235));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 5, y,     x + 5, y + 4);
                g2.drawLine(x + 13, y,    x + 13, y + 4);

                // Grid dots
                g2.setColor(iconColor);
                int[][] dots = {{5,10},{9,10},{13,10},{5,14},{9,14},{13,14}};
                for (int[] d : dots) {
                    g2.fillOval(x + d[0] - 1, y + d[1] - 1, 2, 2);
                }
                g2.dispose();
            }
        };
    }
}
