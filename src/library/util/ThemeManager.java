package library.util;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.UIManager;

public final class ThemeManager {

    public static final Color WINDOW = new Color(0x0D, 0x1F, 0x17);

    public static final Color SIDEBAR = new Color(0x10, 0x28, 0x1D);

    public static final Color SURFACE = new Color(0x17, 0x3A, 0x28);

    public static final Color RAISED = new Color(0x1B, 0x42, 0x30);

    public static final Color FIELD = new Color(0x0A, 0x1A, 0x12);
    public static final Color FOCUS = new Color(100, 174, 122);

    public static final Color TEXT = new Color(238, 246, 239);
    public static final Color SUBDUED = new Color(163, 190, 170);

    public static final Color GOLD = new Color(226, 181, 86);

    public static final Color HAIRLINE = new Color(255, 255, 255, 20);

    public static final Color PRIMARY = new Color(20, 105, 70);
    public static final Color MOSS = new Color(106, 183, 123);

    public static final Color DANGER = new Color(228, 132, 114);
    public static final Color DANGER_TEXT = new Color(0xF5, 0xB4, 0xA6);

    public static final Color DANGER_FILL = new Color(0x7A, 0x32, 0x26);
    public static final Color DANGER_EDGE = new Color(0xC9, 0x6B, 0x52);
    public static final Color DANGER_ON = new Color(0xFF, 0xE3, 0xDA);

    public static final Color TOOL_BG = new Color(0x1F, 0x4A, 0x35);
    public static final Color TOOL_TEXT = new Color(0xDD, 0xEC, 0xE1);

    public static final Color TOOL_EDGE = new Color(255, 255, 255, 34);
    public static final Color ACTIVE_BG = new Color(0x1E, 0x4B, 0x34);

    public static final Color EDGE = new Color(255, 255, 255, 26);

    public static final int CONTROL_HEIGHT = 38;

    private static final Color THUMB = new Color(0x24, 0x4E, 0x37);
    private static final Color THUMB_HOVER = new Color(0x33, 0x66, 0x48);

    private ThemeManager() { }

    public static void apply(Font defaultFont) {
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", defaultFont);
        UIManager.put("Panel.background", WINDOW);
        UIManager.put("Component.focusColor", FOCUS);

        UIManager.put("Component.accentColor", FOCUS);
        UIManager.put("Button.default.background", PRIMARY);
        UIManager.put("Button.default.foreground", TEXT);
        UIManager.put("Button.default.hoverBackground", new Color(0x18, 0x7A, 0x52));
        UIManager.put("Button.default.pressedBackground", new Color(0x10, 0x58, 0x3A));

        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.disabledBackground", new Color(0x22, 0x36, 0x2B));
        UIManager.put("Button.disabledText", new Color(0x76, 0x92, 0x7F));
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);

        UIManager.put("TextField.background", FIELD);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.placeholderForeground", SUBDUED);
        UIManager.put("PasswordField.background", FIELD);
        UIManager.put("PasswordField.foreground", TEXT);
        UIManager.put("ComboBox.background", FIELD);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.buttonBackground", FIELD);
        UIManager.put("ComboBox.buttonArrowColor", FOCUS);
        UIManager.put("ComboBox.selectionBackground", new Color(0x2A, 0x55, 0x3C));
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("PopupMenu.background", SURFACE);
        UIManager.put("PopupMenu.foreground", TEXT);

        UIManager.put("ScrollBar.track", FIELD);
        UIManager.put("ScrollBar.thumb", THUMB);
        UIManager.put("ScrollBar.hoverThumbColor", THUMB_HOVER);
        UIManager.put("ScrollBar.pressedThumbColor", FOCUS);
        UIManager.put("ScrollBar.thumbArc", 10);
        UIManager.put("ScrollBar.trackArc", 10);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.showButtons", false);

        UIManager.put("ToolTip.background", SURFACE);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.informationIcon", new RoundGlyph(FOCUS, "i"));
        UIManager.put("OptionPane.questionIcon", new RoundGlyph(FOCUS, "?"));
        UIManager.put("OptionPane.warningIcon", new RoundGlyph(GOLD, "!"));
        UIManager.put("OptionPane.errorIcon", new RoundGlyph(new Color(228, 132, 114), "x"));
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", HAIRLINE);
        UIManager.put("TableHeader.background", SIDEBAR);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("TableHeader.separatorColor", HAIRLINE);

        UIManager.put("TableHeader.hoverBackground", SIDEBAR);
        UIManager.put("TableHeader.hoverForeground", TEXT);
    }

    public static Color tint(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static void paintCard(Graphics2D g, int width, int height, int radius, Color base) {
        g.setColor(base);
        g.fill(new RoundRectangle2D.Float(0, 0, width, height, radius, radius));
    }

    private static final class RoundGlyph implements javax.swing.Icon {
        private final Color color;
        private final String glyph;
        private final int size = 34;

        RoundGlyph(Color color, String glyph) {
            this.color = color;
            this.glyph = glyph;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override public void paintIcon(java.awt.Component c, java.awt.Graphics raw, int x, int y) {
            Graphics2D g = (Graphics2D) raw.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(color);
            g.fill(new java.awt.geom.Ellipse2D.Double(x, y, size, size));
            g.setColor(new Color(0x0D, 0x1F, 0x17));
            FontManager.ensureLoaded();
            g.setFont(FontManager.ui(Font.BOLD, 15f));
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(glyph, x + (size - fm.stringWidth(glyph)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }
}
