package bugradar.ui;

import bugradar.logic.BugManager;
import bugradar.model.Bug;
import java.awt.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

/** Overview dashboard: stat cards, efficiency meter, bug-type chart */
public class DashboardPanel extends JPanel {

    private final BugManager manager;

    private JLabel  totalLbl, openLbl, progressLbl, resolvedLbl, criticalLbl;
    private JLabel  effLabel;
    private JProgressBar effBar;
    private JPanel  typeChartPanel;

    public DashboardPanel(BugManager manager) {
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
        header.add(Components.label("📊  Dashboard", Theme.FONT_TITLE, Theme.TEXT), BorderLayout.WEST);
        header.add(Components.label("Project Overview", Theme.FONT_SMALL, Theme.MUTED), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Scrollable content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        content.add(buildStatRow());
        content.add(Box.createVerticalStrut(18));
        content.add(buildEffRow());
        content.add(Box.createVerticalStrut(18));
        content.add(buildTypeChart());
        content.add(Box.createVerticalStrut(18));
        content.add(buildTipCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUI(new Components.DarkScrollBarUI());
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        totalLbl    = bigLabel("0");
        openLbl     = bigLabel("0");
        progressLbl = bigLabel("0");
        resolvedLbl = bigLabel("0");
        criticalLbl = bigLabel("0");

        row.add(statCard(totalLbl,    "Total Bugs",   Theme.ACCENT));
        row.add(statCard(openLbl,     "Open",         Theme.RED));
        row.add(statCard(progressLbl, "In Progress",  Theme.YELLOW));
        row.add(statCard(resolvedLbl, "Resolved",     Theme.GREEN));
        row.add(statCard(criticalLbl, "Critical",     Theme.P_CRITICAL));
        return row;
    }

    private JPanel statCard(JLabel num, String title, Color color) {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 4));
        num.setForeground(color);
        JLabel lbl = Components.label(title, Theme.FONT_SMALL, Theme.MUTED);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        num.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(num, BorderLayout.CENTER);
        card.add(lbl, BorderLayout.SOUTH);
        return card;
    }

    private JLabel bigLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 32));
        l.setForeground(Theme.TEXT);
        return l;
    }

    private JPanel buildEffRow() {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(Components.label("🎯  Correction Efficiency", Theme.FONT_HEADER, Theme.TEXT), BorderLayout.WEST);
        effLabel = Components.label("0%", Theme.FONT_HEADER, Theme.GREEN);
        effLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        topRow.add(effLabel, BorderLayout.EAST);

        effBar = Components.progressBar(Theme.GREEN);
        effBar.setPreferredSize(new Dimension(100, 12));

        JLabel sub = Components.label(
            "Percentage of bugs resolved or closed out of total reported",
            Theme.FONT_SMALL, Theme.MUTED);

        card.add(topRow, BorderLayout.NORTH);
        card.add(effBar,  BorderLayout.CENTER);
        card.add(sub,     BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildTypeChart() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));

        wrapper.add(Components.label("📈  Bugs by Type", Theme.FONT_HEADER, Theme.TEXT), BorderLayout.NORTH);

        typeChartPanel = new JPanel();
        typeChartPanel.setLayout(new BoxLayout(typeChartPanel, BoxLayout.Y_AXIS));
        typeChartPanel.setBackground(Theme.SURFACE);
        typeChartPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Theme.BORDER, 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        wrapper.add(typeChartPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildTipCard() {
        JPanel card = Components.card();
        card.setLayout(new BorderLayout(0, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        card.add(Components.label("💡  Quick Tips", Theme.FONT_HEADER, Theme.ACCENT), BorderLayout.NORTH);
        JLabel tips = Components.label(
            "<html>• Use <b>Code Analyzer</b> tab to auto-detect bugs in your Java code<br>" +
            "• Double-click a row in <b>Bug Tracker</b> to edit a record<br>" +
            "• Data is saved automatically to SQLite: ~/.bugradar/bugradar.db</html>",
            Theme.FONT_SMALL, Theme.MUTED);
        card.add(tips, BorderLayout.CENTER);
        return card;
    }

    // ── Refresh ──────────────────────────────────────────────────
    public void refresh() {
        manager.refreshFromDB();
        totalLbl.setText(String.valueOf(manager.getTotalCount()));
        openLbl.setText(String.valueOf(manager.getOpenCount()));
        progressLbl.setText(String.valueOf(manager.getInProgressCount()));
        resolvedLbl.setText(String.valueOf(manager.getResolvedCount()));
        criticalLbl.setText(String.valueOf(manager.getCriticalCount()));

        int eff = manager.getEfficiencyScore();
        effBar.setValue(eff);
        effLabel.setText(eff + "%");
        effLabel.setForeground(eff >= 70 ? Theme.GREEN : eff >= 40 ? Theme.YELLOW : Theme.RED);

        typeChartPanel.removeAll();
        Map<Bug.Type, Long> byType = manager.getCountByType();
        int total = manager.getTotalCount();

        if (byType.isEmpty()) {
            typeChartPanel.add(Components.label(
                "No bugs reported yet — use Bug Tracker to add bugs.",
                Theme.FONT_BODY, Theme.MUTED));
        } else {
            byType.forEach((type, count) -> {
                int pct = total > 0 ? (int)(count * 100 / total) : 0;
                typeChartPanel.add(typeRow(type.toString(), count, pct));
                typeChartPanel.add(Box.createVerticalStrut(7));
            });
        }

        typeChartPanel.revalidate();
        typeChartPanel.repaint();
        revalidate(); repaint();
    }

    private JPanel typeRow(String name, long count, int pct) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 4, 0, 4);

        g.gridx = 0; g.weightx = 0;
        JLabel lbl = Components.label(name, Theme.FONT_SMALL, Theme.TEXT);
        lbl.setPreferredSize(new Dimension(140, 20));
        row.add(lbl, g);

        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        JProgressBar bar = Components.progressBar(Theme.ACCENT);
        bar.setValue(pct);
        bar.setPreferredSize(new Dimension(200, 8));
        row.add(bar, g);

        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        JLabel cnt = Components.label(count + "  (" + pct + "%)", Theme.FONT_SMALL, Theme.MUTED);
        cnt.setPreferredSize(new Dimension(90, 20));
        row.add(cnt, g);

        return row;
    }
}