package bugradar.ui;

import bugradar.db.AnalysisDAO;
import bugradar.db.DatabaseManager;
import bugradar.logic.BugManager;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Database Status Panel — shows SQLite connection info,
 * table record counts, schema, analysis history, SQL log.
 */
public class DBStatusPanel extends JPanel {

    private final BugManager manager;

    private JLabel    bugsCountLabel;
    private JLabel    sessionsCountLabel;
    private JLabel    issuesCountLabel;
    private JTextArea sqlLogArea;
    private JPanel    sessionListPanel;

    public DBStatusPanel(BugManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        buildUI();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        header.add(Components.label("🗄️  Database — SQLite", Theme.FONT_TITLE, Theme.TEXT),
            BorderLayout.WEST);
        JButton refreshBtn = Components.primaryBtn("↻  Refresh");
        refreshBtn.addActionListener(e -> refresh());
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        body.add(buildConnectionCard());
        body.add(Box.createVerticalStrut(14));
        body.add(buildTablesCard());
        body.add(Box.createVerticalStrut(14));
        body.add(buildSchemaCard());
        body.add(Box.createVerticalStrut(14));
        body.add(buildHistoryCard());
        body.add(Box.createVerticalStrut(14));
        body.add(buildSqlLogCard());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUI(new Components.DarkScrollBarUI());
        add(scroll, BorderLayout.CENTER);
    }

    // ── Connection Card ──────────────────────────────────────────
    private JPanel buildConnectionCard() {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.add(Components.label("🔌  Connection", Theme.FONT_HEADER, Theme.TEXT),
            BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridLayout(2, 1, 0, 6));
        rows.setOpaque(false);

        // Status row
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusRow.setOpaque(false);
        JLabel statusLbl = Components.label("● Connected  —  SQLite via JDBC",
            Theme.FONT_BADGE, Theme.GREEN);
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusRow.add(statusLbl);

        // Path row
        JPanel pathRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pathRow.setOpaque(false);
        pathRow.add(Components.label("File:", Theme.FONT_SMALL, Theme.MUTED));
        JLabel pathLbl = Components.label(
            DatabaseManager.getInstance().getDbPath(),
            new Font("Consolas", Font.PLAIN, 11), Theme.ACCENT);
        pathRow.add(pathLbl);

        rows.add(statusRow);
        rows.add(pathRow);
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    // ── Tables Card ──────────────────────────────────────────────
    private JPanel buildTablesCard() {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        card.add(Components.label("📋  Tables & Records", Theme.FONT_HEADER, Theme.TEXT),
            BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 3, 10, 0));
        grid.setOpaque(false);

        bugsCountLabel     = bigNum(String.valueOf(manager.getTotalCount()), Theme.ACCENT);
        sessionsCountLabel = bigNum(String.valueOf(manager.getTotalAnalysisSessions()), Theme.PURPLE);
        issuesCountLabel = bigNum(String.valueOf(manager.getTotalAnalysisIssues()), Theme.YELLOW);

        grid.add(tableCard(bugsCountLabel,     "bugs",             Theme.ACCENT));
        grid.add(tableCard(sessionsCountLabel, "analysis_sessions",Theme.PURPLE));
        grid.add(tableCard(issuesCountLabel,   "analysis_issues",  Theme.YELLOW));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JLabel bigNum(String text, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 28));
        l.setForeground(color);
        return l;
    }

    private JPanel tableCard(JLabel num, String tbl, Color color) {
        JPanel p = Components.card();
        p.setLayout(new BorderLayout(0, 4));
        num.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel name = Components.label(tbl, new Font("Consolas", Font.PLAIN, 10), Theme.MUTED);
        name.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(num, BorderLayout.CENTER);
        p.add(name, BorderLayout.SOUTH);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 3, 0, 0, color),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return p;
    }

    // ── Schema Card ──────────────────────────────────────────────
    private JPanel buildSchemaCard() {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        card.add(Components.label("🧱  SQL Schema", Theme.FONT_HEADER, Theme.TEXT),
            BorderLayout.NORTH);

        JTextArea ta = new JTextArea();
        ta.setFont(new Font("Consolas", Font.PLAIN, 11));
        ta.setBackground(new Color(0x0D, 0x11, 0x17));
        ta.setForeground(new Color(0x79, 0xC0, 0xFF));
        ta.setEditable(false);
        ta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        ta.setText("""
CREATE TABLE bugs (
    id              TEXT PRIMARY KEY,
    title           TEXT NOT NULL,
    description     TEXT,
    type            TEXT NOT NULL,
    priority        TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'OPEN',
    reported_by     TEXT,
    assigned_to     TEXT,
    line_number     INTEGER DEFAULT 0,
    code_snippet    TEXT,
    suggested_fix   TEXT,
    detection_score INTEGER DEFAULT 0,
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);

CREATE TABLE analysis_sessions (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    session_code  TEXT,
    language      TEXT DEFAULT 'Java',
    total_issues  INTEGER,
    error_count   INTEGER,
    warning_count INTEGER,
    quality_score INTEGER,
    analyzed_at   TEXT
);

CREATE TABLE analysis_issues (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id     INTEGER,
    line_number    INTEGER,
    issue_type     TEXT,
    priority       TEXT,
    message        TEXT,
    fix_suggestion TEXT,
    FOREIGN KEY (session_id) REFERENCES analysis_sessions(id)
);""");

        JScrollPane sp = Components.scrollPane(ta);
        sp.setPreferredSize(new Dimension(100, 180));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── History Card ─────────────────────────────────────────────
    private JPanel buildHistoryCard() {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        card.add(Components.label("📜  Recent Analysis Sessions", Theme.FONT_HEADER, Theme.TEXT),
            BorderLayout.NORTH);

        sessionListPanel = new JPanel();
        sessionListPanel.setLayout(new BoxLayout(sessionListPanel, BoxLayout.Y_AXIS));
        sessionListPanel.setBackground(Theme.SURFACE2);

        JScrollPane sp = Components.scrollPane(sessionListPanel);
        sp.setPreferredSize(new Dimension(100, 180));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── SQL Log ──────────────────────────────────────────────────
    private JPanel buildSqlLogCard() {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(Components.label("📟  SQL Activity Log", Theme.FONT_HEADER, Theme.TEXT),
            BorderLayout.WEST);
        JButton clr = Components.ghostBtn("Clear");
        clr.addActionListener(e -> sqlLogArea.setText(""));
        hdr.add(clr, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        sqlLogArea = new JTextArea();
        sqlLogArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        sqlLogArea.setBackground(new Color(0x06, 0x0D, 0x07));
        sqlLogArea.setForeground(new Color(0x3F, 0xB9, 0x50));
        sqlLogArea.setEditable(false);
        sqlLogArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        appendLog("-- BugRadar SQL Activity Log --");
        appendLog("-- DB: " + DatabaseManager.getInstance().getDbPath() + " --");
        appendLog("-- Status: Connected --");

        JScrollPane sp = Components.scrollPane(sqlLogArea);
        sp.setPreferredSize(new Dimension(100, 130));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── Refresh ──────────────────────────────────────────────────
    public void refresh() {
        manager.refreshFromDB();
        bugsCountLabel.setText(String.valueOf(manager.getTotalCount()));
        sessionsCountLabel.setText(String.valueOf(manager.getTotalAnalysisSessions()));
        issuesCountLabel.setText(String.valueOf(manager.getTotalAnalysisIssues()));

        // Rebuild session history
        sessionListPanel.removeAll();

        // Column headers
        JPanel hdrRow = new JPanel(new GridLayout(1, 4, 4, 0));
        hdrRow.setBackground(Theme.SURFACE);
        hdrRow.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        hdrRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        hdrRow.add(Components.label("Session",  Theme.FONT_BADGE, Theme.MUTED));
        hdrRow.add(Components.label("Issues",   Theme.FONT_BADGE, Theme.MUTED));
        hdrRow.add(Components.label("Score",    Theme.FONT_BADGE, Theme.MUTED));
        hdrRow.add(Components.label("Time",     Theme.FONT_BADGE, Theme.MUTED));
        sessionListPanel.add(hdrRow);

        List<AnalysisDAO.SessionSummary> sessions = manager.getRecentAnalysisSessions(20);

        if (sessions.isEmpty()) {
            JPanel empty = new JPanel(new FlowLayout(FlowLayout.LEFT));
            empty.setOpaque(false);
            empty.add(Components.label(
                "  No analysis sessions yet. Use ⚡ Code Analyzer to create one.",
                Theme.FONT_SMALL, Theme.MUTED));
            sessionListPanel.add(empty);
        } else {
            for (AnalysisDAO.SessionSummary s : sessions) {
                sessionListPanel.add(buildSessionRow(s));
            }
        }

        sessionListPanel.revalidate();
        sessionListPanel.repaint();

        appendLog("-- Refreshed: " +
            java.time.LocalDateTime.now().toString().substring(0, 19) + " --");
        appendLog("SELECT * FROM bugs;  -- " + manager.getTotalCount() + " rows");
        appendLog("SELECT * FROM analysis_sessions;  -- " +
            manager.getTotalAnalysisSessions() + " rows");

        revalidate(); repaint();
    }

    private JPanel buildSessionRow(AnalysisDAO.SessionSummary s) {
        JPanel row = new JPanel(new GridLayout(1, 4, 4, 0));
        row.setBackground(s.id % 2 == 0 ? Theme.SURFACE : Theme.SURFACE2);
        row.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        Color scoreColor = s.score >= 80 ? Theme.GREEN
                         : s.score >= 50 ? Theme.YELLOW
                         : Theme.RED;

        String time = (s.analyzedAt != null && s.analyzedAt.length() >= 19)
            ? s.analyzedAt.substring(11, 19) : "";

        row.add(Components.label("#" + s.id,                   Theme.FONT_SMALL, Theme.MUTED));
        row.add(Components.label(s.totalIssues + " issues",    Theme.FONT_SMALL, Theme.TEXT));
        row.add(Components.label(s.score + "%",                Theme.FONT_SMALL, scoreColor));
        row.add(Components.label(time,                         Theme.FONT_SMALL, Theme.MUTED));
        return row;
    }

    public void appendLog(String text) {
        if (sqlLogArea != null) {
            sqlLogArea.append(text + "\n");
            sqlLogArea.setCaretPosition(sqlLogArea.getDocument().getLength());
        }
    }
}