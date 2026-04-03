package bugradar.ui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Reusable, styled Swing components for BugRadar */
public final class Components {
    private Components() {}

    // ── Button ──────────────────────────────────────────────────
    public static JButton button(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isPressed()  ? bg.darker()   :
                             getModel().isRollover() ? bg.brighter() : bg;
                g2.setColor(base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(Theme.FONT_BODY);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        return btn;
    }

    public static JButton primaryBtn(String text)  { return button(text, Theme.ACCENT,   Color.BLACK); }
    public static JButton dangerBtn(String text)   { return button(text, Theme.RED,     Color.WHITE); }
    public static JButton successBtn(String text)  { return button(text, Theme.GREEN,   Color.BLACK); }
    public static JButton ghostBtn(String text)    { return button(text, Theme.SURFACE2, Theme.TEXT); }
    public static JButton warningBtn(String text)  { return button(text, Theme.YELLOW,  Color.BLACK); }

    // ── Label ───────────────────────────────────────────────────
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    // ── TextField ───────────────────────────────────────────────
    public static JTextField textField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(Theme.MUTED);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(placeholder, getInsets().left + 4, y);
                    g2.dispose();
                }
            }
        };
        tf.setFont(Theme.FONT_BODY);
        tf.setForeground(Theme.TEXT);
        tf.setBackground(Theme.SURFACE2);
        tf.setCaretColor(Theme.ACCENT);
        tf.setSelectionColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 80));
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    // ── TextArea ────────────────────────────────────────────────
    public static JTextArea textArea() {
        JTextArea ta = new JTextArea();
        ta.setFont(Theme.FONT_BODY);
        ta.setForeground(Theme.TEXT);
        ta.setBackground(Theme.SURFACE2);
        ta.setCaretColor(Theme.ACCENT);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        ta.setSelectionColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 80));
        return ta;
    }

    // ── ComboBox ────────────────────────────────────────────────
    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setFont(Theme.FONT_BODY);
        cb.setForeground(Theme.TEXT);
        cb.setBackground(Theme.SURFACE2);
        cb.setBorder(new LineBorder(Theme.BORDER, 1, true));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object val, int idx, boolean sel, boolean foc) {
                super.getListCellRendererComponent(list, val, idx, sel, foc);
                setBackground(sel ? Theme.BORDER : Theme.SURFACE2);
                setForeground(Theme.TEXT);
                setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                return this;
            }
        });
        return cb;
    }

    // ── Card Panel ──────────────────────────────────────────────
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.SURFACE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 10, 10));
                g2.setColor(Theme.BORDER);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-2, getHeight()-2, 10, 10));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        return p;
    }

    // ── Progress Bar ────────────────────────────────────────────
    public static JProgressBar progressBar(Color color) {
        JProgressBar pb = new JProgressBar(0, 100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.SURFACE2);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                int w = (int)(getWidth() * (getValue() / 100.0));
                if (w > 0) {
                    GradientPaint gp = new GradientPaint(0, 0, color.darker(), w, 0, color);
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, getHeight(), getHeight(), getHeight()));
                }
                g2.dispose();
            }
        };
        pb.setBorderPainted(false);
        pb.setOpaque(false);
        pb.setPreferredSize(new Dimension(100, 10));
        return pb;
    }

    // ── ScrollPane ──────────────────────────────────────────────
    public static JScrollPane scrollPane(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.setBackground(Theme.SURFACE);
        sp.getViewport().setBackground(Theme.SURFACE2);
        sp.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        return sp;
    }

    // ── Separator ───────────────────────────────────────────────
    public static JSeparator separator() {
        JSeparator s = new JSeparator();
        s.setForeground(Theme.BORDER);
        s.setBackground(Theme.BORDER);
        return s;
    }

    // ── Dark Scrollbar UI (public for external use) ──────────────
    public static class DarkScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor         = Theme.BORDER;
            trackColor         = Theme.SURFACE;
            thumbHighlightColor = Theme.MUTED;
        }
        @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
        @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
        private JButton zeroBtn() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fill(new RoundRectangle2D.Float(r.x+2, r.y+2, r.width-4, r.height-4, 6, 6));
            g2.dispose();
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}