package library.util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class FontManager {

    private static Font uiMedium;
    private static Font uiSemiBold;
    private static Font uiBold;
    private static Font displayRegular;

    private FontManager() { }

    public static synchronized void ensureLoaded() {
        if (uiMedium != null) return;
        uiMedium = read("/fonts/Manrope-Medium.ttf", "SansSerif");
        uiSemiBold = read("/fonts/Manrope-SemiBold.ttf", "SansSerif");
        uiBold = read("/fonts/Manrope-Bold.ttf", "SansSerif");
        displayRegular = read("/fonts/DMSerifDisplay-Regular.ttf", "Serif");
    }

    public static Font ui(int style, float size) {
        ensureLoaded();
        return (style == Font.BOLD ? uiBold : uiMedium).deriveFont(size);
    }

    public static Font uiLabel(float size) {
        ensureLoaded();
        return uiSemiBold.deriveFont(size);
    }

    public static Font display(int style, float size) {
        ensureLoaded();
        return displayRegular.deriveFont(style, size);
    }

    public static Font tracked(Font base, float em) {
        Map<java.awt.font.TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(java.awt.font.TextAttribute.TRACKING, em);
        return base.deriveFont(attributes);
    }

    private static Font read(String resource, String fallbackName) {
        try (InputStream in = FontManager.class.getResourceAsStream(resource)) {
            if (in != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font;
            }
        } catch (Exception ex) {
            System.out.println("Using fallback font for " + resource + ": " + ex.getMessage());
        }
        return new Font(fallbackName, Font.PLAIN, 13);
    }
}
