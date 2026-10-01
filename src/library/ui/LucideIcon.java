package library.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

public final class LucideIcon implements Icon {

    private enum Shape {
        BOOK(2, "M12 5v16 M12 5a5 5 0 0 0-4-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4a5 5 0 0 1 4 2 M12 5a5 5 0 0 1 4-2h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4a5 5 0 0 0-4 2"),
        HOUSE(2, "M3 10.5 12 3l9 7.5 M5.5 8.8V20h13V8.8 M10 20v-6.5h4V20"),
        SEARCH(2, "m21 21-4.34-4.34", new double[][]{{11, 11, 8}}),
        PLUS(2, "M5 12h14 M12 5v14"),
        CHECK(2, "M20 6 9 17l-5-5"),
        ARROW_RIGHT(2, "M5 12h14 M12 5l7 7-7 7"),
        ARROW_LEFT(2, "m12 19-7-7 7-7 M19 12H5"),
        USERS(2, "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 3.128a4 4 0 0 1 0 7.744 M22 21v-2a4 4 0 0 0-3-3.87", new double[][]{{9, 7, 4}}),
        CHART(2, "M3 3v16a2 2 0 0 0 2 2h16 M18 17V9 M13 17V5 M8 17v-3"),
        TRASH(2, "M10 11v6 M14 11v6 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M3 6h18 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"),
        USER(2, "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", new double[][]{{12, 7, 4}}),
        LOGOUT(2, "m16 17 5-5-5-5 M21 12H9 M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"),
        POWER(2, "M12 2v10 M18.4 6.6a9 9 0 1 1-12.77.04"),
        DOWNLOAD(2, "M12 15V3 M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M7 10l5 5 5-5");

        final String pathData;
        final double strokeWidth;
        final double[][] circles;

        Shape(double strokeWidth, String pathData) { this(strokeWidth, pathData, new double[0][]); }

        Shape(double strokeWidth, String pathData, double[][] circles) {
            this.strokeWidth = strokeWidth;
            this.pathData = pathData;
            this.circles = circles;
        }
    }

    private final Shape shape;
    private final Color color;
    private final int size;

    private LucideIcon(Shape shape, Color color, int size) {
        this.shape = shape;
        this.color = color;
        this.size = size;
    }

    public static Icon book(Color c, int s)     { return new LucideIcon(Shape.BOOK, c, s); }
    public static Icon home(Color c, int s)     { return new LucideIcon(Shape.HOUSE, c, s); }
    public static Icon search(Color c, int s)   { return new LucideIcon(Shape.SEARCH, c, s); }
    public static Icon plus(Color c, int s)     { return new LucideIcon(Shape.PLUS, c, s); }
    public static Icon check(Color c, int s)    { return new LucideIcon(Shape.CHECK, c, s); }
    public static Icon borrow(Color c, int s)   { return new LucideIcon(Shape.ARROW_RIGHT, c, s); }
    public static Icon giveBack(Color c, int s) { return new LucideIcon(Shape.ARROW_LEFT, c, s); }
    public static Icon users(Color c, int s)    { return new LucideIcon(Shape.USERS, c, s); }
    public static Icon report(Color c, int s)   { return new LucideIcon(Shape.CHART, c, s); }
    public static Icon trash(Color c, int s)    { return new LucideIcon(Shape.TRASH, c, s); }
    public static Icon user(Color c, int s)     { return new LucideIcon(Shape.USER, c, s); }
    public static Icon logout(Color c, int s)   { return new LucideIcon(Shape.LOGOUT, c, s); }
    public static Icon power(Color c, int s)    { return new LucideIcon(Shape.POWER, c, s); }
    public static Icon export(Color c, int s)   { return new LucideIcon(Shape.DOWNLOAD, c, s); }

    public int getIconWidth() { return size; }
    public int getIconHeight() { return size; }

    public void paintIcon(Component c, Graphics raw, int x, int y) {
        Graphics2D g = (Graphics2D) raw.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);

        double k = size / 24.0;
        g.scale(k, k);
        g.setColor(color);
        g.setStroke(new BasicStroke((float) shape.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.draw(parse(shape.pathData));
        for (double[] circle : shape.circles) {
            g.draw(new Ellipse2D.Double(circle[0] - circle[2], circle[1] - circle[2], circle[2] * 2, circle[2] * 2));
        }
        g.dispose();
    }

    private static java.awt.Shape parse(String data) {
        Path2D.Double path = new Path2D.Double();
        java.util.List<String> tokens = tokenize(data);

        double px = 0, py = 0;
        double sx = 0, sy = 0;
        char command = 'M';
        int i = 0;

        while (i < tokens.size()) {
            String token = tokens.get(i);
            if (isCommand(token)) {
                command = token.charAt(0);
                i++;
                if (command == 'Z' || command == 'z') {
                    path.closePath();
                    px = sx;
                    py = sy;
                }
                continue;
            }
            if (!isNumber(token)) { i++; continue; }

            switch (command) {
                case 'M' -> { px = d(tokens.get(i++)); py = d(tokens.get(i++)); path.moveTo(px, py); sx = px; sy = py; command = 'L'; }
                case 'm' -> { px += d(tokens.get(i++)); py += d(tokens.get(i++)); path.moveTo(px, py); sx = px; sy = py; command = 'l'; }
                case 'L' -> { px = d(tokens.get(i++)); py = d(tokens.get(i++)); path.lineTo(px, py); }
                case 'l' -> { px += d(tokens.get(i++)); py += d(tokens.get(i++)); path.lineTo(px, py); }
                case 'H' -> { px = d(tokens.get(i++)); path.lineTo(px, py); }
                case 'h' -> { px += d(tokens.get(i++)); path.lineTo(px, py); }
                case 'V' -> { py = d(tokens.get(i++)); path.lineTo(px, py); }
                case 'v' -> { py += d(tokens.get(i++)); path.lineTo(px, py); }
                case 'A', 'a' -> {
                    if (i + 6 >= tokens.size()) { i = tokens.size(); break; }
                    double rx = d(tokens.get(i++)), ry = d(tokens.get(i++));
                    i++;
                    boolean largeArc = d(tokens.get(i++)) != 0;
                    boolean sweep = d(tokens.get(i++)) != 0;
                    double ex = d(tokens.get(i++)), ey = d(tokens.get(i++));
                    if (command == 'a') { ex += px; ey += py; }
                    arcTo(path, px, py, rx, ry, largeArc, sweep, ex, ey);
                    px = ex; py = ey;
                }
                default -> i++;
            }
        }
        return path;
    }

    private static java.util.List<String> tokenize(String data) {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("[MmLlHhVvAaZz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?")
                .matcher(data);
        while (m.find()) tokens.add(m.group());
        return tokens;
    }

    private static boolean isCommand(String token) {
        return token.length() == 1 && Character.isLetter(token.charAt(0));
    }

    private static boolean isNumber(String token) {
        return !isCommand(token);
    }

    private static double d(String token) {
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static void arcTo(Path2D.Double path, double x1, double y1, double rx, double ry,
                              boolean largeArc, boolean sweep, double x2, double y2) {
        if (rx == 0 || ry == 0 || (x1 == x2 && y1 == y2)) {
            path.lineTo(x2, y2);
            return;
        }
        double dx2 = (x1 - x2) / 2, dy2 = (y1 - y2) / 2;
        double x1p = dx2, y1p = dy2;
        double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
        if (lambda > 1) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
        }
        double sign = largeArc == sweep ? -1 : 1;
        double numerator = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
        double denominator = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
        double factor = sign * Math.sqrt(Math.max(0, numerator / denominator));
        double cxp = factor * (rx * y1p / ry);
        double cyp = factor * -(ry * x1p / rx);
        double cx = cxp + (x1 + x2) / 2;
        double cy = cyp + (y1 + y2) / 2;

        double theta1 = Math.atan2((y1p - cyp) / ry, (x1p - cxp) / rx);
        double theta2 = Math.atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx);
        double delta = theta2 - theta1;
        if (!sweep && delta > 0) delta -= 2 * Math.PI;
        if (sweep && delta < 0) delta += 2 * Math.PI;

        java.awt.geom.Arc2D.Double arc = new java.awt.geom.Arc2D.Double(
                cx - rx, cy - ry, rx * 2, ry * 2,
                Math.toDegrees(-theta1), Math.toDegrees(-delta), java.awt.geom.Arc2D.OPEN);
        path.append(arc, true);
    }
}
