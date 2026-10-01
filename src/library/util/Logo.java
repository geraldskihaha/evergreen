package library.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public final class Logo {

    private static final String RESOURCE = "/images/logo-mark.png";
    private static final Map<Integer, Image> CACHE = new HashMap<>();
    private static BufferedImage source;

    private Logo() { }

    public static Image mark(int height) {
        return CACHE.computeIfAbsent(height, h -> {
            BufferedImage src = source();
            int width = Math.max(1, (int) Math.round(src.getWidth() * (h / (double) src.getHeight())));
            return scale(src, width, h);
        });
    }

    public static ImageIcon icon(int height) {
        return new ImageIcon(mark(height));
    }

    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64, 128}) icons.add(mark(size));
        return icons;
    }

    private static BufferedImage source() {
        if (source == null) {
            BufferedImage loaded = null;
            try (InputStream in = Logo.class.getResourceAsStream(RESOURCE)) {
                if (in != null) loaded = ImageIO.read(in);
            } catch (IOException ex) {
                loaded = null;
            }

            source = loaded != null ? loaded : new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
        return source;
    }

    private static BufferedImage scale(BufferedImage src, int width, int height) {
        BufferedImage current = src;
        int w = src.getWidth();
        int h = src.getHeight();
        while (w / 2 >= width && h / 2 >= height) {
            w /= 2;
            h /= 2;
            current = draw(current, w, h);
        }
        return w == width && h == height ? current : draw(current, width, height);
    }

    private static BufferedImage draw(BufferedImage src, int width, int height) {
        BufferedImage out = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }
}
