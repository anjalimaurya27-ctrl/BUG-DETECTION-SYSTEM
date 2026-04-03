package bugradar.ui;

import bugradar.model.Bug;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/** Modal dialog for adding or editing a bug */
public class BugFormDialog extends JDialog {

    private Bug result;

    private JTextField   titleField, reporterField, assigneeField, lineField;
    private JTextArea    descArea, codeArea, fixArea;
    private JComboBox<Bug.Type>     typeCombo;
    private JComboBox<Bug.Priority> priorityCombo;
    private JComboBox<Bug.Status>   statusCombo;

    public BugFormDialog(Frame parent, Bug existing) {
        super(parent, existing == null ? "  🐛  Report New Bug" : "  ✏️  Edit Bug — " + existing.getId(), true);
        setSize(660, 680);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI(existing);
    }

    private void buildUI(Bug existing) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        header.add(Components.label(
            existing == null ? "Report a New Bug" : "Edit Bug Report",
            Theme.FONT_TITLE, Theme.TEXT), BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BG);
        form.setBorder(BorderFactory.createEmptyBorder(16, 22, 8, 22));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill   = GridBagConstraints.HORIZONTAL;

        int r = 0;
        // Title
        row(form, g, r++, "Bug Title *",
            titleField = Components.textField("Short, clear description of the bug"));

        // Type / Priority / Status (2-col grid)
        JPanel row1 = new JPanel(new GridLayout(1, 3, 10, 0));
        row1.setOpaque(false);
        typeCombo     = comboOf(Bug.Type.values());
        priorityCombo = comboOf(Bug.Priority.values());
        statusCombo   = comboOf(Bug.Status.values());
        row1.add(labeled("Type", typeCombo));
        row1.add(labeled("Priority", priorityCombo));
        row1.add(labeled("Status", statusCombo));
        fullRow(form, g, r++, row1);

        // Reporter / Assignee
        JPanel row2 = new JPanel(new GridLayout(1, 2, 10, 0));
        row2.setOpaque(false);
        reporterField = Components.textField("Your name");
        assigneeField = Components.textField("Assigned to");
        row2.add(labeled("Reported By", reporterField));
        row2.add(labeled("Assigned To", assigneeField));
        fullRow(form, g, r++, row2);

        // Line number
        row(form, g, r++, "Line Number",
            lineField = Components.textField("0 = unknown"));

        // Description
        descArea = Components.textArea();
        descArea.setRows(3);
        row(form, g, r++, "Description",
            wrap(descArea, 400, 72));

        // Code Snippet
        codeArea = Components.textArea();
        codeArea.setFont(Theme.FONT_MONO);
        codeArea.setRows(3);
        row(form, g, r++, "Code Snippet",
            wrap(codeArea, 400, 72));

        // Suggested Fix
        fixArea = Components.textArea();
        fixArea.setRows(2);
        row(form, g, r++, "Suggested Fix",
            wrap(fixArea, 400, 56));

        // Pre-fill
        if (existing != null) {
            titleField.setText(existing.getTitle());
            descArea.setText(existing.getDescription());
            codeArea.setText(existing.getCodeSnippet());
            fixArea.setText(existing.getSuggestedFix());
            typeCombo.setSelectedItem(existing.getType());
            priorityCombo.setSelectedItem(existing.getPriority());
            statusCombo.setSelectedItem(existing.getStatus());
            reporterField.setText(existing.getReportedBy());
            assigneeField.setText(existing.getAssignedTo());
            lineField.setText(String.valueOf(existing.getLineNumber()));
        }

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(Theme.BG);
        formScroll.getVerticalScrollBar().setUI(new Components.DarkScrollBarUI());
        root.add(formScroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Theme.SURFACE);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER));
        JButton cancel = Components.ghostBtn("Cancel");
        JButton save   = Components.primaryBtn(existing == null ? "  ➕  Report Bug  " : "  💾  Save Changes  ");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave(existing));
        footer.add(cancel);
        footer.add(save);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void onSave(Bug existing) {
        String t = titleField.getText().trim();
        if (t.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bug title is required.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Bug b = existing != null ? existing
              : new Bug(t, "", (Bug.Type) typeCombo.getSelectedItem(),
                              (Bug.Priority) priorityCombo.getSelectedItem());
        b.setTitle(t);
        b.setDescription(descArea.getText().trim());
        b.setType((Bug.Type) typeCombo.getSelectedItem());
        b.setPriority((Bug.Priority) priorityCombo.getSelectedItem());
        b.setStatus((Bug.Status) statusCombo.getSelectedItem());
        b.setReportedBy(reporterField.getText().trim());
        b.setAssignedTo(assigneeField.getText().trim());
        b.setCodeSnippet(codeArea.getText().trim());
        b.setSuggestedFix(fixArea.getText().trim());
        try { b.setLineNumber(Integer.parseInt(lineField.getText().trim())); }
        catch (NumberFormatException ignored) {}
        result = b;
        dispose();
    }

    // ── Helpers ─────────────────────────────────────────────────

    private <T> JComboBox<T> comboOf(T[] items) {
        return Components.comboBox(items);
    }

    private JPanel labeled(String text, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        p.add(Components.label(text, Theme.FONT_SMALL, Theme.MUTED), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JScrollPane wrap(JTextArea ta, int w, int h) {
        JScrollPane sp = Components.scrollPane(ta);
        sp.setPreferredSize(new Dimension(w, h));
        return sp;
    }

    private void row(JPanel p, GridBagConstraints g, int r, String lbl, JComponent c) {
        g.gridx = 0; g.gridy = r; g.weightx = 0; g.gridwidth = 1;
        JLabel l = Components.label(lbl, Theme.FONT_SMALL, Theme.MUTED);
        l.setPreferredSize(new Dimension(110, 22));
        p.add(l, g);
        g.gridx = 1; g.weightx = 1;
        p.add(c, g);
    }

    private void fullRow(JPanel p, GridBagConstraints g, int r, JComponent c) {
        g.gridx = 0; g.gridy = r; g.weightx = 1; g.gridwidth = 2;
        p.add(c, g);
        g.gridwidth = 1;
    }

    public Bug getResult() { return result; }
}