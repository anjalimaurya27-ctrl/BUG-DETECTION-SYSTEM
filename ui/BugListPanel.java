package bugradar.ui;

import bugradar.logic.BugManager;
import bugradar.model.Bug;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.JTableHeader;

/**
 * Bug Tracker panel — full bug list with search, filter,
 * add / edit / delete / resolve toolbar actions.
 */
public class BugListPanel extends JPanel {

    private final BugManager    manager;
    private final BugTableModel tableModel = new BugTableModel();
    private       JTable        table;
    private       JTextField    searchField;
    private       JComboBox<String> statusFilter, priorityFilter;
    private       JLabel        countLbl;
    private       Runnable      onDataChanged;

    public BugListPanel(BugManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        buildUI();
        refresh();
    }

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    // ── Build UI ─────────────────────────────────────────────────
    private void buildUI() {
        // ── Toolbar ──
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(Theme.SURFACE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(Components.label("🐛  Bug Tracker", Theme.FONT_HEADER, Theme.TEXT));
        countLbl = Components.label("0 bugs", Theme.FONT_SMALL, Theme.MUTED);
        left.add(countLbl);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton resolveBtn = Components.successBtn("✅  Resolve");
        JButton editBtn    = Components.ghostBtn("✏️  Edit");
        JButton deleteBtn  = Components.dangerBtn("🗑️  Delete");
        JButton addBtn     = Components.primaryBtn("➕  Report Bug");

        resolveBtn.addActionListener(e -> onResolve());
        editBtn.addActionListener(e    -> onEdit());
        deleteBtn.addActionListener(e  -> onDelete());
        addBtn.addActionListener(e     -> onAdd());

        right.add(resolveBtn);
        right.add(editBtn);
        right.add(deleteBtn);
        right.add(addBtn);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        // ── Filter Bar ──
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBackground(Theme.SURFACE2);
        filterBar.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER));

        filterBar.add(Components.label("🔍", Theme.FONT_BODY, Theme.MUTED));
        searchField = Components.textField("Search bugs...");
        searchField.setPreferredSize(new Dimension(200, 28));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { refresh(); }
            public void removeUpdate(DocumentEvent e)  { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        filterBar.add(searchField);

        filterBar.add(Components.label("Status:", Theme.FONT_SMALL, Theme.MUTED));
        statusFilter = smallCombo(new String[]{"All","OPEN","IN_PROGRESS","RESOLVED","CLOSED"});
        statusFilter.addActionListener(e -> refresh());
        filterBar.add(statusFilter);

        filterBar.add(Components.label("Priority:", Theme.FONT_SMALL, Theme.MUTED));
        priorityFilter = smallCombo(new String[]{"All","CRITICAL","HIGH","MEDIUM","LOW"});
        priorityFilter.addActionListener(e -> refresh());
        filterBar.add(priorityFilter);

        JButton clearBtn = Components.ghostBtn("✕ Clear");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            priorityFilter.setSelectedIndex(0);
        });
        filterBar.add(clearBtn);

        // Combine toolbar + filter into north wrapper
        JPanel north = new JPanel(new BorderLayout());
        north.add(toolbar,   BorderLayout.NORTH);
        north.add(filterBar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        // ── Table ──
        table = new JTable(tableModel);
        styleTable();

        JScrollPane scroll = Components.scrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // ── Status Bar ──
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5));
        statusBar.setBackground(Theme.SURFACE);
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER));
        statusBar.add(Components.label(
            "Double-click a row to edit  •  Select row then use toolbar to delete / resolve",
            Theme.FONT_SMALL, Theme.MUTED));
        add(statusBar, BorderLayout.SOUTH);
    }

    private void styleTable() {
        table.setFont(Theme.FONT_BODY);
        table.setBackground(Theme.SURFACE);
        table.setForeground(Theme.TEXT);
        table.setGridColor(Theme.BORDER);
        table.setRowHeight(34);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(0x1F, 0x6F, 0xEB, 80));
        table.setSelectionForeground(Theme.TEXT);
        table.setFocusable(true);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(Theme.SURFACE2);
        header.setForeground(Theme.MUTED);
        header.setFont(Theme.FONT_BADGE);
        header.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER));
        header.setReorderingAllowed(false);

        BugTableRenderer renderer = new BugTableRenderer();
        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);

        int[] widths = {90, 210, 110, 80, 100, 55, 120, 120};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEdit();
            }
        });
    }

    // ── CRUD Actions ─────────────────────────────────────────────

    private void onAdd() {
        BugFormDialog dlg = new BugFormDialog(parentFrame(), null);
        dlg.setVisible(true);
        Bug b = dlg.getResult();
        if (b != null) { manager.addBug(b); refresh(); notifyChanged(); }
    }

    private void onEdit() {
        Bug sel = selectedBug();
        if (sel == null) { noSelectionMsg("edit"); return; }
        BugFormDialog dlg = new BugFormDialog(parentFrame(), sel);
        dlg.setVisible(true);
        if (dlg.getResult() != null) { manager.updateBug(sel); refresh(); notifyChanged(); }
    }

    private void onDelete() {
        Bug sel = selectedBug();
        if (sel == null) { noSelectionMsg("delete"); return; }
        int ok = JOptionPane.showConfirmDialog(this,
            "Delete bug: \"" + sel.getTitle() + "\"?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            manager.deleteBug(sel.getId());
            refresh(); notifyChanged();
        }
    }

    private void onResolve() {
        Bug sel = selectedBug();
        if (sel == null) { noSelectionMsg("resolve"); return; }
        sel.setStatus(Bug.Status.RESOLVED);
        manager.updateBug(sel);
        refresh(); notifyChanged();
    }

    // ── Refresh / Filter ─────────────────────────────────────────

    public void refresh() {
        if (manager != null) manager.refreshFromDB();
        String q   = searchField   != null ? searchField.getText().trim() : "";
        String st  = statusFilter  != null ? (String) statusFilter.getSelectedItem()   : "All";
        String pri = priorityFilter != null ? (String) priorityFilter.getSelectedItem() : "All";

        List<Bug> list = q.isEmpty() ? manager.getAllBugs() : manager.search(q);

        if (!"All".equals(st)) {
            final String s = st;
            list = list.stream().filter(b -> b.getStatus().name().equals(s)).toList();
        }
        if (!"All".equals(pri)) {
            final String p = pri;
            list = list.stream().filter(b -> b.getPriority().name().equals(p)).toList();
        }

        tableModel.setBugs(list);
        if (countLbl != null) countLbl.setText(list.size() + " bug(s)");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private Bug selectedBug() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getBugAt(table.convertRowIndexToModel(row));
    }

    private Frame parentFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void notifyChanged() {
        if (onDataChanged != null) onDataChanged.run();
    }

    private void noSelectionMsg(String action) {
        JOptionPane.showMessageDialog(this,
            "Please select a bug to " + action + ".",
            "No Selection", JOptionPane.INFORMATION_MESSAGE);
    }

    private JComboBox<String> smallCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(Theme.FONT_SMALL);
        cb.setForeground(Theme.TEXT);
        cb.setBackground(Theme.SURFACE2);
        cb.setPreferredSize(new Dimension(120, 28));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object val, int idx, boolean sel, boolean foc) {
                super.getListCellRendererComponent(list, val, idx, sel, foc);
                setBackground(sel ? Theme.BORDER : Theme.SURFACE2);
                setForeground(Theme.TEXT);
                setFont(Theme.FONT_SMALL);
                setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                return this;
            }
        });
        return cb;
    }
}