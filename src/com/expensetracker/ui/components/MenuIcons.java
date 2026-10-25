package com.expensetracker.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Factory for small painted icons used in context-menu JMenuItems.
 * All icons are drawn with Java2D — no image files required.
 */
public final class MenuIcons {

    private MenuIcons() {}

    /** Pencil / edit icon (16 × 16). */
    public static Icon editIcon() {
        return new Icon() {
            @Override public int getIconWidth()  { return 16; }
            @Override public int getIconHeight() { return 16; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                // Pencil body — slanted rectangle
                Color pencilYellow = new Color(250, 200, 50);
                Color pencilDark   = new Color(60, 60, 60);
                Color pencilTip    = new Color(220, 190, 160);
                Color eraserPink   = new Color(240, 130, 130);

                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Full pencil diagonal body (bottom-left to top-right)
                int[] bx = { x+1,  x+5,  x+15, x+11 };
                int[] by = { y+15, y+15, y+2,  y+2  };

                // Rotate the whole pencil 45° — easier to just draw a rotated polygon directly
                // Pencil body polygon (angled at ~45°)
                int[] px = { x+3,  x+10, x+13, x+6  };
                int[] py = { y+13, y+3,  y+4,  y+14 };
                g2.setColor(pencilYellow);
                g2.fillPolygon(px, py, 4);
                g2.setColor(pencilDark);
                g2.drawPolygon(px, py, 4);

                // Tip triangle
                int[] tx = { x+6,  x+13, x+9  };
                int[] ty = { y+14, y+4,  y+15 };
                g2.setColor(pencilTip);
                g2.fillPolygon(tx, ty, 3);
                g2.setColor(pencilDark);
                g2.drawPolygon(tx, ty, 3);

                // Eraser end
                int[] ex = { x+3, x+6,  x+5,  x+2  };
                int[] ey = { y+13, y+14, y+15, y+14 };
                g2.setColor(eraserPink);
                g2.fillPolygon(ex, ey, 4);
                g2.setColor(pencilDark);
                g2.drawPolygon(ex, ey, 4);

                // Center line on body
                g2.setColor(new Color(200, 160, 30, 180));
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawLine(x+5, y+13, x+12, y+4);

                g2.dispose();
            }
        };
    }

    /**
     * Pencil icon pre-styled for disabled state (greyed out).
     */
    public static Icon editIconDisabled() {
        return new Icon() {
            @Override public int getIconWidth()  { return 16; }
            @Override public int getIconHeight() { return 16; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                editIcon().paintIcon(c, g2, x, y);
                g2.dispose();
            }
        };
    }

    /** Trash-can / delete icon (16 × 16) in red. */
    public static Icon deleteIcon() {
        return deleteIcon(new Color(220, 38, 38));
    }

    /** Trash-can icon with custom color. */
    public static Icon deleteIcon(Color color) {
        return new Icon() {
            @Override public int getIconWidth()  { return 16; }
            @Override public int getIconHeight() { return 16; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color body   = color;
                Color bodyFill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 30);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Lid handle — small rectangle at top center
                g2.setColor(body);
                g2.fillRoundRect(x + 5, y + 1, 6, 2, 2, 2);

                // Lid — horizontal bar
                g2.setColor(body);
                g2.fillRoundRect(x + 2, y + 3, 12, 2, 2, 2);

                // Bin body — trapezoid drawn as filled rounded rect
                g2.setColor(bodyFill);
                g2.fillRoundRect(x + 3, y + 5, 10, 10, 3, 3);
                g2.setColor(body);
                g2.drawRoundRect(x + 3, y + 5, 10, 10, 3, 3);

                // Vertical stripes inside bin
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 6,  y + 7, x + 6,  y + 13);
                g2.drawLine(x + 8,  y + 7, x + 8,  y + 13);
                g2.drawLine(x + 10, y + 7, x + 10, y + 13);

                g2.dispose();
            }
        };
    }
}
