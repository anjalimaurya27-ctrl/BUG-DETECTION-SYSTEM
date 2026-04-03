package bugradar.ui;

import bugradar.db.DatabaseManager;
import bugradar.logic.BugManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Main JFrame — BugRadar Application.
 * Sidebar: Dashboard | Bug Tracker | Code Analyzer | Database
 * Storage: SQLite ONLY
 */
public class MainWindow extends JFrame {

    private final BugManager manager = new BugManager();

    private DashboardPanel dashPanel;
    private BugListPanel   bugPanel;
    private AnalyzerPanel  analyzerPanel;
    private DBStatusPanel  dbPanel;

    private JPanel     contentArea;
    private CardLayout cards;

    private NavButton btnDash, btnBugs, btnAnalyze, btnDb;
    private NavButton activeNav;

    public MainWindow() {
        super("BugRadar — Bug Detection & Tracking  |  SQLite  |  The Eternals Avengers");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setSize(1360, 820);
        setLocationRelativeTo(null);
        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        applyUIDefaults();
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG);

        add(buildSidebar(), BorderLayout.WEST);

        dashPanel     = new DashboardPanel(manager);
        bugPanel      = new BugListPanel(manager);
        analyzerPanel = new AnalyzerPanel(manager, this::onDataChanged);
        dbPanel       = new DBStatusPanel(manager);

        bugPanel.setOnDataChanged(this::onDataChanged);

        cards       = new CardLayout();
        contentArea = new JPanel(cards);
        contentArea.setBackground(Theme.BG);
        contentArea.add(dashPanel,     "dashboard");
        contentArea.add(bugPanel,      "bugs");
        contentArea.add(analyzerPanel, "analyzer");
        contentArea.add(dbPanel,       "database");
        add(contentArea, BorderLayout.CENTER);

        navigate("dashboard", btnDash);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SURFACE);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDER));

        // ── Logo ──
        JPanel logo = new JPanel();
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));
        logo.setBackground(Theme.SURFACE);
        logo.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(20, 16, 18, 16)));

        JLabel icon = new JLabel("🐛", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel("BugRadar", SwingConstants.CENTER);
        name.setFont(new Font("Segoe UI", Font.BOLD, 18));
        name.setForeground(Theme.ACCENT);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Bug Detection Studio", SwingConstants.CENTER);
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // DB status indicator
        JLabel dbDot = new JLabel("● SQLite Connected", SwingConstants.CENTER);
        dbDot.setFont(new Font("Segoe UI", Font.BOLD, 10));
        dbDot.setForeground(Theme.GREEN);
        dbDot.setAlignmentX(Component.CENTER_ALIGNMENT);

        logo.add(icon);
        logo.add(Box.createVerticalStrut(6));
        logo.add(name);
        logo.add(Box.createVerticalStrut(2));
        logo.add(sub);
        logo.add(Box.createVerticalStrut(6));
        logo.add(dbDot);
        sidebar.add(logo, BorderLayout.NORTH);

        // ── Nav buttons ──
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(Theme.SURFACE);
        nav.setBorder(BorderFactory.createEmptyBorder(14, 8, 8, 8));

        btnDash    = new NavButton("📊", "Dashboard");
        btnBugs    = new NavButton("🐛", "Bug Tracker");
        btnAnalyze = new NavButton("⚡", "Code Analyzer");
        btnDb      = new NavButton("🗄️", "Database");

        btnDash.addActionListener(e    -> navigate("dashboard", btnDash));
        btnBugs.addActionListener(e    -> navigate("bugs",      btnBugs));
        btnAnalyze.addActionListener(e -> navigate("analyzer",  btnAnalyze));
        btnDb.addActionListener(e      -> navigate("database",  btnDb));

        nav.add(btnDash);
        nav.add(Box.createVerticalStrut(4));
        nav.add(btnBugs);
        nav.add(Box.createVerticalStrut(4));
        nav.add(btnAnalyze);
        nav.add(Box.createVerticalStrut(4));
        nav.add(btnDb);
        nav.add(Box.createVerticalGlue());
        sidebar.add(nav, BorderLayout.CENTER);

        // ── Footer ──
        JPanel footer = new JPanel(new GridLayout(4, 1, 0, 2));
        footer.setBackground(Theme.SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(10, 14, 12, 14)));

        footer.add(Components.label("The Eternals Avengers", Theme.FONT_SMALL, Theme.MUTED));
        footer.add(Components.label("Java 21  •  Swing  •  SQLite", Theme.FONT_SMALL, Theme.MUTED));
        footer.add(Components.label("v2.0  —  2025", Theme.FONT_SMALL, new Color(0x3C, 0x42, 0x4B)));

        String dbPath = DatabaseManager.getInstance().getDbPath();
        String shortPath = dbPath.length() > 28 ? "..." + dbPath.substring(dbPath.length() - 25) : dbPath;
        footer.add(Components.label(shortPath,
            new Font("Consolas", Font.PLAIN, 9), new Color(0x3C, 0x42, 0x4B)));

        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private void navigate(String panel, NavButton btn) {
        cards.show(contentArea, panel);
        if (activeNav != null) activeNav.setActive(false);
        btn.setActive(true);
        activeNav = btn;
        if ("dashboard".equals(panel)) dashPanel.refresh();
        if ("bugs".equals(panel))      bugPanel.refresh();
        if ("database".equals(panel))  dbPanel.refresh();
    }

    private void onDataChanged() {
        dashPanel.refresh();
        bugPanel.refresh();
    }

    private void applyUIDefaults() {
        UIManager.put("OptionPane.background",        Theme.SURFACE);
        UIManager.put("Panel.background",             Theme.SURFACE);
        UIManager.put("OptionPane.messageForeground", Theme.TEXT);
        UIManager.put("Button.background",            Theme.SURFACE2);
        UIManager.put("Button.foreground",            Theme.TEXT);
        UIManager.put("TextField.background",         Theme.SURFACE2);
        UIManager.put("TextField.foreground",         Theme.TEXT);
        UIManager.put("TextArea.background",          Theme.SURFACE2);
        UIManager.put("TextArea.foreground",          Theme.TEXT);
        UIManager.put("ComboBox.background",          Theme.SURFACE2);
        UIManager.put("ComboBox.foreground",          Theme.TEXT);
        UIManager.put("ScrollPane.background",        Theme.SURFACE);
        UIManager.put("Viewport.background",          Theme.SURFACE2);
        UIManager.put("Table.background",             Theme.SURFACE);
        UIManager.put("Table.foreground",             Theme.TEXT);
        UIManager.put("TableHeader.background",       Theme.SURFACE2);
        UIManager.put("TableHeader.foreground",       Theme.MUTED);
        UIManager.put("SplitPane.background",         Theme.BG);
        UIManager.put("SplitPaneDivider.background",  Theme.BORDER);
    }

    // ── NavButton ────────────────────────────────────────────────
    static class NavButton extends JButton {
        private boolean active;

        NavButton(String icon, String label) {
            setText("  " + icon + "  " + label);
            setFont(Theme.FONT_BODY);
            setForeground(Theme.MUTED);
            setHorizontalAlignment(SwingConstants.LEFT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setMinimumSize(new Dimension(120, 42));
            setPreferredSize(new Dimension(195, 42));
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if (!active) setForeground(Theme.TEXT); }
                @Override public void mouseExited(MouseEvent e)  { if (!active) setForeground(Theme.MUTED); }
            });
        }

        void setActive(boolean a) {
            active = a;
            setForeground(a ? Theme.ACCENT : Theme.MUTED);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            if (active) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(
                    Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Theme.ACCENT);
                g2.fill(new RoundRectangle2D.Float(0, 8, 3, getHeight()-16, 3, 3));
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}