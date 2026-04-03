package bugradar.ui;

import bugradar.logic.BugManager;
import bugradar.model.Bug;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * Code Analyzer panel — paste Java code, run static analysis,
 * view detected issues with fixes, and optionally push to Bug Tracker.
 */
public class AnalyzerPanel extends JPanel {

    private final BugManager manager;
    private final Runnable   onBugsAdded;

    private JTextArea    codeInput;
    private JTextArea    lineNumArea;
    private JPanel       issueListPanel;
    private JLabel       scoreLabel;
    private JProgressBar scoreBar;
    private JLabel       summaryLabel;
    private JLabel       correctedCodeLabel;

    // Corrected-code view
    private JTextArea    correctedArea;
    private JPanel       correctedWrapper;

    public AnalyzerPanel(BugManager manager, Runnable onBugsAdded) {
        this.manager     = manager;
        this.onBugsAdded = onBugsAdded;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        buildUI();
    }

    // ── Build UI ─────────────────────────────────────────────────
    private void buildUI() {

        // ── Top toolbar ──
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(Theme.SURFACE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        titleBlock.add(Components.label("⚡  Code Analyzer", Theme.FONT_HEADER, Theme.TEXT));
        titleBlock.add(Components.label("Paste Java code to detect bugs automatically", Theme.FONT_SMALL, Theme.MUTED));
        toolbar.add(titleBlock, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        JButton clearBtn   = Components.ghostBtn("🗑  Clear");
        JButton analyzeBtn = Components.primaryBtn("  ⚡  Analyze Code  ");
        clearBtn.addActionListener(e   -> clearAll());
        analyzeBtn.addActionListener(e -> runAnalysis());
        btnRow.add(clearBtn);
        btnRow.add(analyzeBtn);
        toolbar.add(btnRow, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        // ── Horizontal split: editor | results ──
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBorder(null);
        split.setDividerSize(5);
        split.setDividerLocation(480);
        split.setBackground(Theme.BG);

        split.setLeftComponent(buildEditorSide());
        split.setRightComponent(buildResultsSide());

        add(split, BorderLayout.CENTER);
    }

    // ── Editor side ──────────────────────────────────────────────
    private JPanel buildEditorSide() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.SURFACE2);

        // Editor header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Theme.SURFACE);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        hdr.add(Components.label("📝  Java Source Code", Theme.FONT_SMALL, Theme.MUTED), BorderLayout.WEST);
        hdr.add(Components.label("Java", Theme.FONT_SMALL, Theme.ACCENT), BorderLayout.EAST);

        // Line numbers
        lineNumArea = new JTextArea("1");
        lineNumArea.setBackground(Theme.SURFACE);
        lineNumArea.setForeground(Theme.MUTED);
        lineNumArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        lineNumArea.setEditable(false);
        lineNumArea.setFocusable(false);
        lineNumArea.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        // Code input
        codeInput = new JTextArea();
        codeInput.setFont(new Font("Consolas", Font.PLAIN, 13));
        codeInput.setBackground(new Color(0x0D, 0x11, 0x17));
        codeInput.setForeground(new Color(0xC9, 0xD1, 0xD9));
        codeInput.setCaretColor(Theme.ACCENT);
        codeInput.setLineWrap(false);
        codeInput.setTabSize(4);
        codeInput.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        codeInput.setSelectionColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 60));
        codeInput.setText(getDefaultCode());

        // Sync line numbers
        codeInput.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { syncLineNums(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { syncLineNums(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { syncLineNums(); }
        });

        JScrollPane codeScroll = new JScrollPane(codeInput);
        codeScroll.setBorder(null);
        codeScroll.setRowHeaderView(lineNumArea);
        codeScroll.getVerticalScrollBar().setUI(new Components.DarkScrollBarUI());
        codeScroll.getHorizontalScrollBar().setUI(new Components.DarkScrollBarUI());

        panel.add(hdr, BorderLayout.NORTH);
        panel.add(codeScroll, BorderLayout.CENTER);
        return panel;
    }

    private void syncLineNums() {
        int lines = codeInput.getLineCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append(i);
            if (i < lines) sb.append('\n');
        }
        lineNumArea.setText(sb.toString());
    }

    // ── Results side ─────────────────────────────────────────────
    private JPanel buildResultsSide() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG);

        // Score bar header
        JPanel scoreHdr = new JPanel(new GridLayout(2, 1, 0, 4));
        scoreHdr.setBackground(Theme.SURFACE);
        scoreHdr.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        scoreLabel = Components.label("—", new Font("Segoe UI", Font.BOLD, 22), Theme.GREEN);
        topRow.add(Components.label("Code Quality Score", Theme.FONT_SMALL, Theme.MUTED), BorderLayout.WEST);
        topRow.add(scoreLabel, BorderLayout.EAST);

        scoreBar = Components.progressBar(Theme.GREEN);
        scoreBar.setPreferredSize(new Dimension(100, 10));

        scoreHdr.add(topRow);
        scoreHdr.add(scoreBar);
        panel.add(scoreHdr, BorderLayout.NORTH);

        // Vertical split: issues | corrected code
        JPanel resultsBody = new JPanel(new BorderLayout());
        resultsBody.setBackground(Theme.BG);

        summaryLabel = Components.label(
            "Paste code and click ⚡ Analyze to detect issues.",
            Theme.FONT_SMALL, Theme.MUTED);
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        resultsBody.add(summaryLabel, BorderLayout.NORTH);

        issueListPanel = new JPanel();
        issueListPanel.setLayout(new BoxLayout(issueListPanel, BoxLayout.Y_AXIS));
        issueListPanel.setBackground(Theme.BG);
        issueListPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));

        JScrollPane issueScroll = new JScrollPane(issueListPanel);
        issueScroll.setBorder(null);
        issueScroll.getViewport().setBackground(Theme.BG);
        issueScroll.getVerticalScrollBar().setUI(new Components.DarkScrollBarUI());
        resultsBody.add(issueScroll, BorderLayout.CENTER);

        // Corrected code section (hidden until analysis)
        correctedWrapper = new JPanel(new BorderLayout(0, 6));
        correctedWrapper.setBackground(Theme.BG);
        correctedWrapper.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));
        correctedWrapper.setVisible(false);

        JPanel corrHdr = new JPanel(new BorderLayout());
        corrHdr.setOpaque(false);
        corrHdr.add(Components.label("✅  Suggested Corrected Code", Theme.FONT_HEADER, Theme.GREEN), BorderLayout.WEST);

        correctedArea = new JTextArea();
        correctedArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        correctedArea.setBackground(new Color(0x0D, 0x17, 0x0D));
        correctedArea.setForeground(new Color(0x56, 0xD3, 0x64));
        correctedArea.setEditable(false);
        correctedArea.setLineWrap(false);
        correctedArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane corrScroll = new JScrollPane(correctedArea);
        corrScroll.setBorder(new LineBorder(new Color(0x3F, 0xB9, 0x50, 60), 1, true));
        corrScroll.setPreferredSize(new Dimension(100, 200));
        corrScroll.getVerticalScrollBar().setUI(new Components.DarkScrollBarUI());

        correctedWrapper.add(corrHdr,    BorderLayout.NORTH);
        correctedWrapper.add(corrScroll, BorderLayout.CENTER);

        JSplitPane resultsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultsBody, correctedWrapper);
        resultsSplit.setBorder(null);
        resultsSplit.setDividerSize(5);
        resultsSplit.setDividerLocation(320);
        resultsSplit.setBackground(Theme.BG);
        panel.add(resultsSplit, BorderLayout.CENTER);

        return panel;
    }

    // ── Analysis ─────────────────────────────────────────────────
    private void runAnalysis() {
        String code = codeInput.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please paste Java code to analyze.", "No Code", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<BugManager.AnalysisResult> issues = manager.analyzeCode(code);
        int score = manager.computeCodeScore(issues);
        manager.saveAnalysisSession(code, issues, score);

        // Update score widgets
        scoreBar.setValue(score);
        scoreLabel.setText(score + "%");
        scoreLabel.setForeground(score >= 80 ? Theme.GREEN : score >= 50 ? Theme.YELLOW : Theme.RED);

        long errors   = issues.stream().filter(r -> r.priority == Bug.Priority.CRITICAL || r.priority == Bug.Priority.HIGH).count();
        long warnings = issues.size() - errors;

        summaryLabel.setText(issues.isEmpty()
            ? "✅  No issues detected — code looks clean!"
            : "⚠  Found: " + errors + " error(s)   " + warnings + " warning(s)   " + issues.size() + " total issues");

        // Rebuild issue cards
        issueListPanel.removeAll();

        if (issues.isEmpty()) {
            JPanel ok = new JPanel(new FlowLayout(FlowLayout.LEFT));
            ok.setOpaque(false);
            ok.add(Components.label(
                "<html><br><b style='color:#3FB950'>✅  All Clear!</b>  No bugs detected in this code.</html>",
                Theme.FONT_BODY, Theme.GREEN));
            issueListPanel.add(ok);
            correctedWrapper.setVisible(false);
        } else {
            for (BugManager.AnalysisResult r : issues) {
                issueListPanel.add(buildIssueCard(r));
                issueListPanel.add(Box.createVerticalStrut(7));
            }

            // "Add All to Tracker" button
            issueListPanel.add(Box.createVerticalStrut(4));
            JButton addAllBtn = Components.successBtn("➕  Add All Issues to Bug Tracker  (" + issues.size() + ")");
            addAllBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            final List<BugManager.AnalysisResult> finalIssues = issues;
            final String finalCode = code;
            addAllBtn.addActionListener(e -> addAllToTracker(finalIssues, finalCode));
            issueListPanel.add(addAllBtn);

            // Show corrected code
            correctedWrapper.setVisible(true);
            correctedArea.setText(buildCorrectedCode(code, issues));
        }

        issueListPanel.revalidate();
        issueListPanel.repaint();
    }

    private JPanel buildIssueCard(BugManager.AnalysisResult r) {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 7));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Color accent bar on left edge
        Color accent = Theme.priorityColor(r.priority);
        card.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 3, 0, 0, accent),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        // Top badges row
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topRow.setOpaque(false);

        topRow.add(makeBadge(r.priority.name(), accent));
        topRow.add(makeBadge(r.type.toString(), Theme.ACCENT));
        topRow.add(Components.label("Line " + r.lineNo, Theme.FONT_SMALL, Theme.MUTED));
        card.add(topRow, BorderLayout.NORTH);

        // Message
        JLabel msg = new JLabel("<html>" + r.message + "</html>");
        msg.setFont(Theme.FONT_BODY);
        msg.setForeground(Theme.TEXT);
        card.add(msg, BorderLayout.CENTER);

        // Fix suggestion
        if (r.fix != null && !r.fix.isBlank()) {
            JLabel fix = new JLabel("<html><i>💡 Fix: </i>" + r.fix + "</html>");
            fix.setFont(Theme.FONT_SMALL);
            fix.setForeground(Theme.GREEN);
            fix.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(5, 0, 0, 0)));
            card.add(fix, BorderLayout.SOUTH);
        }
        return card;
    }

    private JLabel makeBadge(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_BADGE);
        l.setForeground(color);
        l.setOpaque(true);
        l.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
        l.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80), 1, true),
            BorderFactory.createEmptyBorder(1, 7, 1, 7)));
        return l;
    }

    private void addAllToTracker(List<BugManager.AnalysisResult> issues, String code) {
        String[] lines = code.split("\n");
        for (BugManager.AnalysisResult r : issues) {
            Bug b = new Bug(
                r.type + " at Line " + r.lineNo,
                r.message,
                r.type,
                r.priority
            );
            b.setLineNumber(r.lineNo);
            b.setSuggestedFix(r.fix);
            if (r.lineNo > 0 && r.lineNo <= lines.length)
                b.setCodeSnippet(lines[r.lineNo - 1].trim());
            manager.addBug(b);
        }
        if (onBugsAdded != null) onBugsAdded.run();
        JOptionPane.showMessageDialog(this,
            issues.size() + " issue(s) added to Bug Tracker!",
            "Bugs Added", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Build a simple corrected-code string with inline comments for each fix */
    private String buildCorrectedCode(String code, List<BugManager.AnalysisResult> issues) {
        String[] lines = code.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        sb.append("// ======================================================\n");
        sb.append("// BugRadar — Suggested Corrections\n");
        sb.append("// Issues found: ").append(issues.size()).append("\n");
        sb.append("// ======================================================\n\n");

        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            // Attach any fix hints for this line
            for (BugManager.AnalysisResult r : issues) {
                if (r.lineNo == lineNo) {
                    sb.append("// ⚠ [Line ").append(lineNo).append("] ")
                      .append(r.type).append(" — ").append(r.message).append("\n");
                    sb.append("// 💡 Fix: ").append(r.fix).append("\n");
                }
            }
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    private void clearAll() {
        codeInput.setText("");
        issueListPanel.removeAll();
        issueListPanel.revalidate();
        issueListPanel.repaint();
        scoreBar.setValue(0);
        scoreLabel.setText("—");
        scoreLabel.setForeground(Theme.GREEN);
        summaryLabel.setText("Paste code and click ⚡ Analyze to detect issues.");
        correctedWrapper.setVisible(false);
        syncLineNums();
    }

    private String getDefaultCode() {
        return """
// ── BugRadar Sample Code (contains intentional bugs) ──────────────
// Click ⚡ Analyze Code to detect all issues automatically.

public class BuggyExample {

    // Bug 1: Division by zero (b could be 0)
    public static int divide(int a, int b) {
        return a / b
    }

    // Bug 2: NullPointerException — s not null-checked
    public static int strLen(String s) {
        return s.length()
    }

    // Bug 3: Off-by-one error (should be i < arr.length)
    public static int sumArray(int[] arr) {
        int total = 0;
        for (int i = 0; i <= arr.length; i++) {
            total += arr[i]
        }
        return total;
    }

    public static void main(String[] args) {
        // Bug 4: String compared with == instead of .equals()
        String name = "Alice";
        if (name == "Alice") {
            System.out.println("Hello, Alice")
        }

        // Bug 5: Raw ArrayList (no generic type)
        ArrayList list = new ArrayList();
        list.add("item1");

        System.out.println(divide(10, 0));
    }
}
""";
    }
}