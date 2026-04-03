package bugradar.ui;

import bugradar.model.Bug;
import java.util.*;
import javax.swing.table.AbstractTableModel;

/** JTable model for the bug list */
public class BugTableModel extends AbstractTableModel {

    private static final String[] COLS = {
        "ID", "Title", "Type", "Priority", "Status", "Line #", "Reported", "Updated"
    };

    private final List<Bug> rows = new ArrayList<>();

    public void setBugs(List<Bug> list) {
        rows.clear();
        rows.addAll(list);
        fireTableDataChanged();
    }

    public Bug getBugAt(int row) {
        return (row >= 0 && row < rows.size()) ? rows.get(row) : null;
    }

    @Override public int getRowCount()    { return rows.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int c) { return COLS[c]; }
    @Override public boolean isCellEditable(int r, int c) { return false; }

    @Override public Object getValueAt(int row, int col) {
        Bug b = rows.get(row);
        return switch (col) {
            case 0 -> b.getId();
            case 1 -> b.getTitle();
            case 2 -> b.getType().toString();
            case 3 -> b.getPriority().name();
            case 4 -> b.getStatus().name().replace('_', ' ');
            case 5 -> b.getLineNumber() > 0 ? String.valueOf(b.getLineNumber()) : "—";
            case 6 -> b.getCreatedFormatted();
            case 7 -> b.getUpdatedFormatted();
            default -> "";
        };
    }
}