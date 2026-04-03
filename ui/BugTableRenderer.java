package bugradar.ui;

import bugradar.model.Bug;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Colours priority and status cells; alternates row backgrounds */
public class BugTableRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean selected,
            boolean focused, int row, int col) {

        super.getTableCellRendererComponent(table, value, selected, focused, row, col);

        setFont(Theme.FONT_BODY);
        setForeground(Theme.TEXT);
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        setBackground(selected ? new Color(0x1F, 0x6F, 0xEB, 80)
                               : (row % 2 == 0 ? Theme.SURFACE : Theme.SURFACE2));
        setHorizontalAlignment(LEFT);

        if (value == null) return this;
        String v = value.toString();

        switch (col) {
            case 0 -> { setForeground(Theme.MUTED); setFont(Theme.FONT_SMALL); }
            case 3 -> {  // Priority
                try { setForeground(Theme.priorityColor(Bug.Priority.valueOf(v))); setFont(Theme.FONT_BADGE); }
                catch (Exception ignored) {}
            }
            case 4 -> {  // Status
                try { setForeground(Theme.statusColor(Bug.Status.valueOf(v.replace(' ','_')))); setFont(Theme.FONT_BADGE); }
                catch (Exception ignored) {}
            }
        }
        return this;
    }
}