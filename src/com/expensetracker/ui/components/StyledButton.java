package com.expensetracker.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom modern styled button with smooth hover effects, rounded corners, clean padding, and color variants.
 */
public class StyledButton extends JButton {

    public enum ButtonType {
        PRIMARY,    // Blue
        SUCCESS,    // Emerald Green
        DANGER,     // Red
        SECONDARY,  // Slate outline / light fill
        SIDEBAR     // Dark sidebar style
    }

    private final ButtonType type;
    private Color normalBg;
    private Color hoverBg;
    private Color pressedBg;
    private Color textColor;
    private Color borderColor;
    private boolean hovering = false;
    private boolean pressing = false;

    public StyledButton(String text, ButtonType type) {
        super(text);
        this.type = type;
        initStyle();
    }

    public StyledButton(String text, ButtonType type, String tooltip) {
        super(text);
        this.type = type;
        setToolTipText(tooltip);
        initStyle();
    }

    private void initStyle() {
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false); // we paint ourselves
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 18, 8, 18));

        switch (type) {
            case PRIMARY:
                normalBg   = new Color(37, 99, 235);
                hoverBg    = new Color(29, 78, 216);
                pressedBg  = new Color(30, 64, 175);
                textColor  = Color.WHITE;
                borderColor = null;
                break;
            case SUCCESS:
                normalBg   = new Color(16, 185, 129);
                hoverBg    = new Color(5, 150, 105);
                pressedBg  = new Color(4, 120, 87);
                textColor  = Color.WHITE;
                borderColor = null;
                break;
            case DANGER:
                normalBg   = new Color(220, 38, 38);
                hoverBg    = new Color(185, 28, 28);
                pressedBg  = new Color(153, 27, 27);
                textColor  = Color.WHITE;
                borderColor = null;
                break;
            case SECONDARY:
                normalBg   = new Color(255, 255, 255);
                hoverBg    = new Color(241, 245, 249);
                pressedBg  = new Color(226, 232, 240);
                textColor  = new Color(51, 65, 85);
                borderColor = new Color(203, 213, 225);
                break;
            case SIDEBAR:
                normalBg   = new Color(30, 41, 59);
                hoverBg    = new Color(51, 65, 85);
                pressedBg  = new Color(71, 85, 105);
                textColor  = new Color(203, 213, 225);
                borderColor = null;
                break;
        }

        setForeground(textColor);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e)  { hovering = true;  pressing = false; repaint(); }
            @Override public void mouseExited(MouseEvent e)   { hovering = false; pressing = false; repaint(); }
            @Override public void mousePressed(MouseEvent e)  { pressing = true;  repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressing = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // Pick background
        Color bg;
        if (!isEnabled()) {
            bg = new Color(203, 213, 225);
        } else if (pressing || getModel().isPressed()) {
            bg = pressedBg;
        } else if (hovering || getModel().isRollover()) {
            bg = hoverBg;
        } else {
            bg = normalBg;
        }

        // Filled rounded background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, 10, 10);

        // Border for SECONDARY buttons only
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, 10, 10);
        }

        g2.dispose();

        // Let the text/icon paint (but NOT the L&F background)
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Suppress default border painting — we do it above
    }
}
