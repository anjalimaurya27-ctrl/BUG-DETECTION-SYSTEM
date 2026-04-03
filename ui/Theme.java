package bugradar.ui;

import bugradar.model.Bug;
import java.awt.*;

/** Centralized dark-theme color and font palette for BugRadar */
public final class Theme {
    private Theme() {}

    // Base
    public static final Color BG       = new Color(0x0D, 0x11, 0x17);
    public static final Color SURFACE  = new Color(0x16, 0x1B, 0x22);
    public static final Color SURFACE2 = new Color(0x1C, 0x23, 0x33);
    public static final Color BORDER   = new Color(0x30, 0x36, 0x3D);
    public static final Color TEXT     = new Color(0xE6, 0xED, 0xF3);
    public static final Color MUTED    = new Color(0x8B, 0x94, 0x9E);

    // Accents
    public static final Color ACCENT   = new Color(0x58, 0xA6, 0xFF);
    public static final Color GREEN    = new Color(0x3F, 0xB9, 0x50);
    public static final Color YELLOW   = new Color(0xD2, 0x99, 0x22);
    public static final Color RED      = new Color(0xF8, 0x51, 0x49);
    public static final Color PURPLE   = new Color(0xBC, 0x8C, 0xFF);
    public static final Color ORANGE   = new Color(0xFF, 0xA6, 0x57);

    // Priority
    public static final Color P_CRITICAL = new Color(0xFF, 0x4C, 0x4C);
    public static final Color P_HIGH     = new Color(0xFF, 0x7B, 0x2F);
    public static final Color P_MEDIUM   = new Color(0xFF, 0xD6, 0x00);
    public static final Color P_LOW      = new Color(0x3F, 0xB9, 0x50);

    // Fonts
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO   = new Font("Consolas",  Font.PLAIN, 13);
    public static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BADGE  = new Font("Segoe UI", Font.BOLD,  11);

    public static Color priorityColor(Bug.Priority p) {
        return switch (p) {
            case CRITICAL -> P_CRITICAL;
            case HIGH     -> P_HIGH;
            case MEDIUM   -> P_MEDIUM;
            case LOW      -> P_LOW;
        };
    }

    public static Color statusColor(Bug.Status s) {
        return switch (s) {
            case OPEN        -> RED;
            case IN_PROGRESS -> YELLOW;
            case RESOLVED    -> GREEN;
            case CLOSED      -> MUTED;
        };
    }
}