/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.BasicStroke;
import java.awt.Color;
import static java.awt.Color.BLACK;
import static java.awt.Color.DARK_GRAY;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import org.imagine.geometry.Arrow;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.Quadrant;
import org.imagine.geometry.Rhombus;
import org.imagine.geometry.Triangle2D;
import org.imagine.geometry.util.PooledTransform;

/**
 * Provides horizontal, vertical, diagonal and circle with line through it
 * cursors.
 *
 * @author Tim Boudreau
 */
public final class Cursors {

    private static Cursors DARK;
    private static Cursors LIGHT;

    private static final boolean DISABLED = Boolean.getBoolean("disable.custom.cursors");
    private final CursorProperties props;
    private final Cursor[] cursors = new Cursor[21];

    private Cursors(CursorProperties props) {
        this.props = props;
    }

    public static Cursors forDarkBackgrounds() {
        if (DARK == null) {
            DARK = new Cursors(forToolkit(true));
        }
        return DARK;
    }

    public static Cursors forBrightBackgrounds() {
        if (LIGHT == null) {
            LIGHT = new Cursors(forToolkit(false));
        }
        return LIGHT;
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel pnl = new JPanel();
        pnl.add(new Comp());
        pnl.setPreferredSize(new Dimension(400, 400));
        jf.setContentPane(pnl);

//        pnl.setBackground(Color.DARK_GRAY);
//        pnl.setForeground(Color.WHITE);

//        jf.setCursor(Cursors.forBrightBackgrounds().southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).horizontal());
//        jf.setCursor(Cursors.forComponent(pnl).southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).vertical());
//        jf.setCursor(Cursors.forComponent(pnl).star());
//        jf.setCursor(Cursors.forComponent(pnl).no());
//        jf.setCursor(Cursors.forComponent(pnl).hin());
        jf.setCursor(Cursors.forComponent(pnl).rotateMany());
//        jf.pack();
        jf.setSize(new Dimension(1200, 800));
        jf.setVisible(true);
    }

    static class Comp extends JComponent {

        private final AffineTransform xf = AffineTransform.getScaleInstance(16, 16);

        public Dimension getPreferredSize() {
            Rectangle r = new Rectangle(0, 0, 32, 32);
            return xf.createTransformedShape(r).getBounds().getSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getParent().getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D gg = (Graphics2D) g;
            gg.transform(xf);
//            drawStarImage(false, gg, 16, 16);
//            drawX(gg, false);
//            drawAngle0_90(gg, false);
//            drawAngle45(gg, true);
//            drawBarbellImage(true, gg, 16, 16);
//            drawNoImage(gg, TWO_COLOR_DEFAULT_DARK.scaled(2));
//            drawBarbellImage(gg, TWO_COLOR_DEFAULT_DARK.scaled(2));
//            drawRhombus(gg, TWO_COLOR_DEFAULT_LIGHT.scaled(2), false);
            drawRotateMany(gg, TWO_COLOR_DEFAULT_LIGHT.scaled(2));
        }
    }

    private static final Map<CursorProperties, Cursors> FOR_PROPERTIES
            = new HashMap<>();

    /**
     * Get the cursors instance for this component based on its background
     * colors.
     *
     * @param comp
     * @return
     */
    public static Cursors forComponent(JComponent comp) {
        Cursors result = (Cursors) comp.getClientProperty(
                CLIENT_PROP_CURSORS);
        if (result != null) {
            return result;
        }
        CursorProperties props = propertiesForComponent(comp);
        result = FOR_PROPERTIES.get(props);
        if (result == null && props != null) {
            result = new Cursors(props);
            FOR_PROPERTIES.put(props, result);
        }
        if (result == null) {
            boolean useLight = isDarkBackground(comp);
            if (useLight) {
                if (LIGHT == null) {
                    LIGHT = new Cursors(TWO_COLOR_DEFAULT_LIGHT);
                }
                result = LIGHT;
            } else {
                if (DARK == null) {
                    DARK = new Cursors(TWO_COLOR_DEFAULT_DARK);
                }
                result = DARK;
            }
        }
        comp.putClientProperty(CLIENT_PROP_CURSORS, result);
        return result;
    }

    private static boolean isDarkBackground(JComponent comp) {
        Color bg = comp.getBackground();
        Color fg = comp.getForeground();
        float bri1 = brightnessOf(fg);
        float bri2 = brightnessOf(bg);
        boolean result;
        if (Math.abs(bri1 - bri2) > 0.1) {
            result = bri1 > bri2;
        } else {
            result = Math.min(bri1, bri2) < 0.45F;
        }
        return result;
    }

    private static final String CLIENT_PROP_CURSORS = "angleCursors";

    private static float brightnessOf(Color c) {
        float[] hsb = new float[4];
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
        return hsb[2];
    }

    private static void cursors(Cursor[] cursors, CursorProperties props) {
        BufferedImage base_45 = props.createCursorImage(g -> {
            drawAngle45(g, props);
        });
        cursors[0] = Toolkit.getDefaultToolkit().
                createCustomCursor(base_45, new Point(props.centerX(), props.centerY()), "northwest");

        // diagonal top-left to bottom-right
        cursors[1] = Toolkit.getDefaultToolkit().
                createCustomCursor(rotated(base_45, 1), new Point(props.centerX(), props.centerY()), "northeast");

        BufferedImage base0_90 = props.createCursorImage(gg -> {
            drawAngle0_90(gg, props);
        });

        // horizontal
        cursors[2] = Toolkit.getDefaultToolkit().
                createCustomCursor(base0_90, new Point(7 * (props.width() / 16), 7 * (props.width() / 16)), "southeast");
        // vertical
        cursors[3] = Toolkit.getDefaultToolkit().
                createCustomCursor(rotated(base0_90, 1), new Point(7 * (props.width() / 16), 7 * (props.width() / 16)), "southwest");
//        BufferedImage no = createCursorImage(48, 48, g -> drawNoImage(g, light, 48, 48));
//        // no cursor
//        cursors[4] = Toolkit.getDefaultToolkit().
//                createCustomCursor(no, new Point(5, 5), "no");
        cursors[4] = props.createCursor("no", props.centerX(), props.centerY(), g -> {
            drawNoImage(g, props);
        });

//        BufferedImage starImage = createCursorImage(gg -> drawStarImage(light, gg, 16, 16));
//        // diagonal bottom-left to top-right
//        cursors[5] = Toolkit.getDefaultToolkit().
//                createCustomCursor(starImage, new Point(8, 8), "star");
        cursors[5] = props.createCursor("star", props.centerX(), props.centerY(), g -> {
            drawStarImage(g, props);
        });

        cursors[6] = props.createCursor("x", props.centerX(), props.centerY(), g -> {
            drawX(g, props);
        });

        cursors[7] = props.createCursor("arrowsX", props.centerX(), props.centerY(), g -> {
            arrowsX(g, props);
        });

        cursors[8] = props.createCursor("barbell", 0, props.centerY(), g -> {
            drawBarbellImage(g, props);
        });

        cursors[9] = props.createCursor("rhombus", props.centerX(), props.centerY(), g -> {
            drawRhombus(g, props, false);
        });

        cursors[10] = props.createCursor("rhombusFilled", props.centerX(), props.centerY(), g -> {
            drawRhombus(g, props, true);
        });
        cursors[11] = props.createCursor("triangleDown", props.centerX(), props.centerY(), g -> {
            drawTriangle(g, props, false);
        });
        cursors[12] = props.createCursor("triangleDownFilled", props.centerX(), props.height() - 1, g -> {
            drawTriangle(g, props, true);
        });
        cursors[13] = props.createCursor("triangleRight", props.centerX(), props.centerY(), g -> {
            drawTriangleRight(g, props, false);
        });
        cursors[14] = props.createCursor("triangleRightFilled", props.width() - 1, props.centerY(), g -> {
            drawTriangleRight(g, props, true);
        });
        cursors[15] = props.createCursor("triangleLeft", 0, props.centerY(), g -> {
            drawTriangleLeft(g, props, false);
        });
        cursors[16] = props.createCursor("triangleLeftFilled", 0, props.centerY(), g -> {
            drawTriangleLeft(g, props, true);
        });

        cursors[17] = props.createCursor("arrowsCrossed", props.centerX(), props.centerY(), g -> {
            drawAnglesCrossed(g, props);
        });

        cursors[18] = props.createCursor("multiMove", props.width() - 1, props.height() - 1, g -> {
            drawMultiMove(g, props);
        });

        cursors[19] = props.createCursor("rotate", 0, 0, g -> {
            drawRotate(g, props);
        });

        cursors[20] = props.createCursor("rotateMany", 0, 0, g -> {
            drawRotateMany(g, props);
        });

    }

    static final Color LG = new Color(214, 214, 214);

    private static void drawRotateMany(Graphics2D g, CursorProperties props) {
        drawRotate(g, props);
        double off = (props.width() / 9);
        double rad = props.width() / 10;
        Circle circ = new Circle(props.centerX()-off, props.centerY()-off, rad);
        fillLine(g, props, circ);
        paintLine(g, props, circ);
        circ.setCenter(props.centerX() + off, props.centerY() + off);
        fillLine(g, props, circ);
        paintLine(g, props, circ);
        circ.setCenter(props.centerX() - off, props.centerY() - off);
        paintLine(g, props, circ);
    }

    private static void drawRotate(Graphics2D g, CursorProperties props) {
        g.transform(AffineTransform.getQuadrantRotateInstance(2, props.centerX(), props.centerY()));
//        g.rotate(Math.toRadians(270), props.centerX(), props.centerY());
        double space = props.width() / 8;
        double rad = (props.width() - (props.width() / 3)) / 2;
        Circle circ = new Circle(props.centerX(), props.centerY(), rad);
        Area a = new Area(new Rectangle(0, 0, props.width(), props.height()));
        a.subtract(new Area(new Rectangle2D.Double(props.centerX(), props.centerY(), props.width() / 2, props.height() / 2)));
        double headAngle = 22.5;
        double dist = space;// + props.width() / 16;
        double arrOff = 0.5;
        double arrHeadDist = -2;
        circ.positionOf(90, (x, y) -> {
            double ht = props.height() / 6;
            Arrow arrow = new Arrow(ht, false, true, x, y + arrOff, x, y + ht + dist);
            arrow.headAngleB = headAngle;
            arrow.setHeadOffset(arrHeadDist);
            fillLine(g, props, arrow);
        });

        Shape old = g.getClip();
        g.setClip(a);
        fillLine(g, props, circ);
        paintLine(g, props, circ);

        g.setClip(old);
        circ.positionOf(90, (x, y) -> {
            double ht = props.height() / 6;
            Arrow arrow = new Arrow(ht, false, true, x, y + arrOff, x, y + ht + dist);
            arrow.headAngleB = headAngle;
            arrow.setHeadOffset(arrHeadDist);
            paintLine(g, props, arrow);
//            double w = (props.width() / 16);
//            Rectangle2D.Double rr = new Rectangle2D.Double(arrow.x2 - (w / 2), arrow.y2 + (w / 4), w, w);
//            g.fill(rr);
        });
        circ.positionOf(180, (x, y) -> {
            double h = props.shadowStroke().getLineWidth();
            EqLine ln = new EqLine(x, y - (h / 3.25), x, y + (h / 3.25));
            g.setColor(props.shadow());
            g.setStroke(props.mainStroke());
            g.draw(ln);
        });

    }

    private static void drawMultiMove(Graphics2D g, CursorProperties props) {
        double majorRadius = props.width() - (props.width() / 2);
        double radius = props.width() / 6;
        double qtr = props.width() / 4;
        Circle big = new Circle(props.centerX() + qtr, props.centerY() + qtr, majorRadius);

        Circle circ = new Circle(0, 0, radius);

        Circle cen = new Circle(0, 0, radius / 3);

        EqPointDouble bottomRight = new EqPointDouble(props.width() - radius / 2, props.height() - radius / 2);
        EqPointDouble bottomRightPlus = bottomRight.copy();
        bottomRightPlus.translate(props.width() / 32, props.width() / 32);

        EqLine ln = new EqLine(bottomRight, big.center());
        g.setColor(Color.WHITE);
        double r4 = big.radius() / 2;
        EqLine ln2 = new EqLine(bottomRightPlus, bottomRightPlus);
//        g.draw(big);
        big.positionOf(315, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            Point2D oldPoint2 = ln.getP2();
            big.positionOf(135, r4, (x1, y1) -> {
                ln.setPoint2(x1, y1);
            });
//            ln.setPoint1(circ.centerX(), circ.centerY());
            fillLine(g, props, ln);
            drawCircle(g, props, circ);
            ln.setPoint2(oldPoint2);
        });

        big.positionOf(0, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            fillLine(g, props, ln);
            drawCircle(g, props, circ);
        });

        big.positionOf(270, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            fillLine(g, props, ln);
            drawCircle(g, props, circ);
        });

        ln2.setPoint1(bottomRightPlus.x, bottomRightPlus.y - radius);
        fillLine(g, props, ln2);
        ln2.setPoint1(bottomRightPlus.x - radius, bottomRightPlus.y);
        fillLine(g, props, ln2);

        big.positionOf(315, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            Point2D oldPoint2 = ln.getP2();
            big.positionOf(135, r4, (x1, y1) -> {
                ln.setPoint2(x1, y1);
            });
            paintLine(g, props, ln);
            ln.setPoint2(oldPoint2);

            cen.setCenter(x, y);
            g.setColor(props.shadow());
            g.fill(cen);
//            g.setBackground(new Color(255,255,255,0));
//            g.clearRect(cen.addToBounds().x, cen.addToBounds().y, cen.addToBounds().width, cen.addToBounds().height);
        });

        big.positionOf(0, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            paintLine(g, props, ln);

            cen.setCenter(x, y);
            g.setColor(props.shadow());
            g.fill(cen);
//            g.setBackground(new Color(255,255,255,0));
//            g.clearRect(cen.addToBounds().x, cen.addToBounds().y, cen.addToBounds().width, cen.addToBounds().height);
        });

        big.positionOf(270, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            paintLine(g, props, ln);

            cen.setCenter(x, y);
            g.setColor(props.shadow());
            g.fill(cen);
//            g.setBackground(new Color(255,255,255,0));
//            g.clearRect(cen.addToBounds().x, cen.addToBounds().y, cen.addToBounds().width, cen.addToBounds().height);
        });

        ln2.setPoint1(bottomRightPlus.x, bottomRightPlus.y - radius);
        paintLine(g, props, ln2);
        ln2.setPoint1(bottomRightPlus.x - radius, bottomRightPlus.y);
        paintLine(g, props, ln2);
    }

    private static void drawCircle(Graphics2D g, CursorProperties props, Circle circ) {
        g.setStroke(props.mainStroke());
        g.setColor(props.shadow());
        g.draw(circ);
        g.setColor(props.primary());
        g.fill(circ);
    }

    private static void drawLine(Graphics2D g, CursorProperties props, Shape line) {
        fillLine(g, props, line);
        paintLine(g, props, line);
    }

    private static void paintLine(Graphics2D g, CursorProperties props1, Shape line) {
        g.setStroke(props1.mainStroke());
        g.setColor(props1.primary());
        g.draw(line);
    }

    private static void fillLine(Graphics2D g, CursorProperties props1, Shape line) {
        g.setStroke(props1.shadowStroke());
//        g.setStroke(new BasicStroke(props1.mainStroke().getLineWidth() * 1.5F));
        g.setColor(props1.shadow());
        g.draw(line);
    }

    private static void drawAnglesCrossed(Graphics2D g, CursorProperties props) {
        AffineTransform old = g.getTransform();
        drawAngle45(g, props);
        g.transform(AffineTransform.getQuadrantRotateInstance(1, props.width() / 2, props.height() / 2));
        drawAngle45(g, props);
        g.transform(AffineTransform.getQuadrantRotateInstance(1, props.width() / 2, props.height() / 2));
        g.setStroke(props.mainStroke());
        g.setColor(props.primary());
        g.drawLine(props.cornerOffset(), props.cornerOffset(), props.width() - props.cornerOffset(), props.height() - props.cornerOffset());
        g.setTransform(old);
    }

    private static void drawAngle45(Graphics2D g, CursorProperties props) {
        g.setStroke(props.shadowStroke());
        g.setColor(props.shadow());
        int cornerOffset = props.cornerOffset();
        int w = props.width();
        int h = props.height();

        int leftTop = w / 8;
        int cxA = props.centerX() - (w / 8);
        int cxB = props.centerX() + (w / 8);
        int rightBottom = w - (w / 8);

        g.drawLine(cornerOffset, cornerOffset, w - cornerOffset, h - cornerOffset);
        g.drawLine(leftTop, leftTop, leftTop, cxA);
        g.drawLine(leftTop, leftTop, cxA, leftTop);
        g.drawLine(rightBottom, rightBottom, rightBottom, cxB);
        g.drawLine(rightBottom, rightBottom, cxB, rightBottom);
        g.setStroke(props.mainStroke());
        g.setColor(props.primary());
        g.drawLine(cornerOffset, cornerOffset, w - cornerOffset, h - cornerOffset);
        g.drawLine(leftTop, leftTop, leftTop, cxA);
        g.drawLine(leftTop, leftTop, cxA, leftTop);
        g.drawLine(rightBottom, rightBottom, rightBottom, cxB);
        g.drawLine(rightBottom, rightBottom, cxB, rightBottom);
    }

    private static void drawAngle0_90(Graphics2D g, CursorProperties props) {
        int w = props.width();
        int h = props.height();
        int start = w / 8;
        int end = w - start;
        g.setColor(props.shadow());
        g.setStroke(props.wideShadowStroke());

        int centerX = props.centerX();
        int centerY = props.centerY();

        g.drawLine(start + (w / 16), centerY, end - (w / 16), centerY);
        g.drawLine(start, centerY, (w / 4), (h / 2) - (h / 8));
        g.drawLine(start, centerY, (w / 4), (h / 2) + (h / 8));
        g.drawLine(end, centerY, centerX + (w / 4), centerY - (w / 8));
        g.drawLine(end, centerY, centerX + (w / 4), centerY + (w / 8));
        g.setStroke(props.wideMainStroke());
        g.setColor(props.primary());
        g.drawLine(start + (w / 16), centerY, end - (w / 16), centerY);
        g.drawLine(start, centerY, (w / 4), (h / 2) - (h / 8));
        g.drawLine(start, centerY, (w / 4), (h / 2) + (h / 8));
        g.drawLine(end, centerY, centerX + (w / 4), centerY - (w / 8));
        g.drawLine(end, centerY, centerX + (w / 4), centerY + (w / 8));
    }

    private static void drawX(Graphics2D g, CursorProperties props) {
        g.setStroke(props.wideShadowStroke());
        g.setColor(props.shadow());

        int w = props.width();
        int h = props.height();
        int x1 = w / 4;
        int y1 = h / 4;
        int x2 = w - x1;
        int y2 = h - y1;

        g.drawLine(x1, y2, x2, y1);
        g.drawLine(x1, y1, x2, y2);

        g.setStroke(props.wideMainStroke());
        g.setColor(props.primary());
        g.drawLine(x1, y2, x2, y1);
        g.drawLine(x1, y1, x2, y2);
    }

    private static void drawRhombus(Graphics2D g, CursorProperties props, boolean fill) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;

        Rhombus rhom = new Rhombus(new Rectangle(o1 * 2, o1 * 2, w - (o1 * 4), h - (o1 * 4)), 0);
        if (fill) {
            g.setColor(props.primary());
            g.fill(rhom);
        }
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
    }

    private static void drawTriangle(Graphics2D g, CursorProperties props, boolean fill) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;

        Triangle2D rhom = new Triangle2D(o1 * 2, o1 * 2, w - (o1 * 2), o1 * 2, props.centerX(), h - (o1 * 2));
        if (fill) {
            g.setColor(props.primary());
            g.fill(rhom);
        }
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
    }

    private static void drawTriangleRight(Graphics2D g, CursorProperties props, boolean fill) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;

        Triangle2D rhom = new Triangle2D(o1 * 2, o1 * 2, o1 * 2, h - (o1 * 2), w - (o1 * 2), props.centerY());
        if (fill) {
            g.setColor(props.primary());
            g.fill(rhom);
        }
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
    }

    private static void drawTriangleLeft(Graphics2D g, CursorProperties props, boolean fill) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;

        Triangle2D rhom = new Triangle2D(w - (o1 * 2), o1 * 2, w - (o1 * 2), h - (o1 * 2), 0, props.centerY());
        if (fill) {
            g.setColor(props.primary());
            g.fill(rhom);
        }
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
    }

    private static void arrowsX(Graphics2D g, CursorProperties props) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;
        int e = w / 8;
        Triangle2D tri = new Triangle2D(o1, o1 + e, props.centerX() - e, props.centerY(), o1, w - (o1 + e));
        Triangle2D tri2 = new Triangle2D(w - o1, h - (o1 + e), props.centerX() + e, props.centerY(), w - o1, o1 + e);

        g.setStroke(props.wideShadowStroke());
        g.setColor(props.shadow());
        g.fill(tri);
        g.fill(tri2);
        g.setStroke(new BasicStroke(props.shadowStroke().getLineWidth() * 1.5F));
        g.drawLine(props.centerX(), o1 * 2, props.centerX(), props.height() - (o1 * 2));

        g.setStroke(props.wideMainStroke());
        g.setColor(props.primary());
        g.draw(tri);
        g.draw(tri2);
        g.setStroke(new BasicStroke(props.mainStroke().getLineWidth() * 1.5F));
        g.drawLine(props.centerX(), o1 * 2, props.centerX(), props.height() - (o1 * 2));
    }

    private static BufferedImage createCursorImage(int w, int h, Consumer<Graphics2D> c) {
        BufferedImage result = createCursorImage(w, h);
        Graphics2D g = result.createGraphics();
        try {
            c.accept(g);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static BufferedImage createCursorImage(int w, int h) {
        BufferedImage result = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        return result;
    }

    private static void twoColorRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
    }

    private static void drawBarbellImage(Graphics2D g, CursorProperties props) {
        props = props.attentionVariant();
        int w = props.width();
        int h = props.height();
        Rectangle rect1 = new Rectangle((w / 8) + 1, (h / 8) + 1);
        rect1.x = (w / 8);
        rect1.y = (h / 2) - 1;
//        int e = w/16;
//        int triHeight = (w/4)-(e*3);
//        int triWidth = triHeight + (e*3);
//        Triangle2D rect1 = new Triangle2D(e, props.centerY(), props.centerX() - triWidth, e + triHeight, props.centerX() - triWidth, h-(e+triHeight));

        Rectangle rect2 = new Rectangle((w / 8) + 1, (h / 8) + 1);
        rect2.x = w - (rect2.width + 1);
        rect2.y = (h / 2) - 1;
        rect1.width -= w / 16;
        rect2.width -= w / 16;
        g.setColor(props.shadow());
//        g.setStroke(new BasicStroke(props.mainStroke().getLineWidth()*1.75F));
        g.setStroke(props.wideShadowStroke());
        g.draw(rect1);
        g.fill(rect1);
        g.draw(rect2);
        g.fill(rect2);

        Line2D ln = new Line2D.Double(rect1.getCenterX(), rect1.getCenterY(),
                rect2.getCenterX(), rect2.getCenterY());
        g.draw(ln);

        g.setColor(props.primary());
        g.setStroke(props.mainStroke());

        g.draw(rect1);
        g.draw(rect2);
        g.fill(rect1);
        g.fill(rect2);
        g.draw(ln);

//        g.setBackground(new Color(255, 255, 255, 0));
//        Rectangle r = new Rectangle((int) rect1.getCenterX()-1, (int) rect1.getCenterY()-1, 3, 3);
//        g.clearRect(r.x, r.y, r.width, r.height);
    }

    private static void drawNoImage(Graphics2D g, CursorProperties props) {
        props = props.warningVariant();
        int w = props.width();
        int h = props.height();
        if (w % 16 != 0) {
            w = (w / 16) * 16;
        }
        if (h % 16 != 0) {
            h = (h / 16) * 16;
        }
        twoColorRenderingHints(g);
        Ellipse2D.Double ell = new Ellipse2D.Double(w / 8, w / 8, w - (w / 4), h - (h / 4));
        g.setStroke(props.wideShadowStroke());
        g.setColor(props.shadow());
        int cornerOffsetX = (w / 8) + 1;
        int cornerOffsetY = (h / 8) + 1;
        g.drawLine(cornerOffsetX, cornerOffsetY, w - cornerOffsetX, h - cornerOffsetY);
        g.draw(ell);
        g.setStroke(new BasicStroke(w / 8F));
        g.setColor(props.primary());
        g.draw(ell);
        g.drawLine(cornerOffsetX, cornerOffsetY, w - cornerOffsetX, h - cornerOffsetY);
    }

    private static void drawStarImage(Graphics2D g, CursorProperties props) {
        props = props.creationVariant();
        int w = props.width();
        int h = props.height();
        int centerFree = Math.min(w, h) / 8;
        int edgeFree = centerFree / 2;
        int cornerFree = centerFree * 2;

        int cx = (w / 2);
        int cy = (h / 2);
        g.setColor(props.shadow());
        g.setStroke(props.wideShadowStroke());
        drawStarPattern(g, cornerFree, cx, centerFree, cy, h, w, edgeFree);
        g.setColor(props.primary());
        g.setStroke(props.wideMainStroke());
        drawStarPattern(g, cornerFree, cx, centerFree, cy, h, w, edgeFree);
    }

    private static void drawStarPattern(Graphics2D g,
            int cornerFree, int cx, int centerFree, int cy, int h, int w, int edgeFree) {
        g.drawLine(cornerFree, cornerFree, cx - centerFree, cy - centerFree);
        g.drawLine(cornerFree, h - cornerFree, cx - centerFree, cy + centerFree);
        g.drawLine(w - cornerFree, h - cornerFree, cx + centerFree, cy + centerFree);
        g.drawLine(w - cornerFree, cornerFree, cx + centerFree, cy - centerFree);
        // vertical
        g.drawLine(centerFree, cy, cx - (centerFree + edgeFree), cy);
        g.drawLine(w - centerFree, cy, cx + centerFree + edgeFree, cy);
        // horizontal
        g.drawLine(cx, centerFree, cx, cy - (centerFree + edgeFree));
        g.drawLine(cx, cy + centerFree + edgeFree, cx, h - centerFree);
        // and a dot in the center
        g.fillRect(cx - edgeFree, cy - edgeFree, centerFree, centerFree);
    }

    public Cursor star() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[5];
    }

    public Cursor barbell() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        }
        checkInit();
        return cursors[8];
    }

    public Cursor x() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        }
        checkInit();
        return cursors[6];
    }

    public Cursor hin() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        }
        checkInit();
        return cursors[7];
    }

    public Cursor no() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[4];
    }

    public Cursor horizontal() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        }
        checkInit();
        return cursors[2];
    }

    public Cursor vertical() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        }
        checkInit();
        return cursors[3];
    }

    public Cursor southWestNorthEast() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[1];
    }

    public Cursor southEastNorthWest() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[0];
    }

    public Cursor rhombus() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[9];
    }

    public Cursor rhombusFilled() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[10];
    }

    public Cursor triangleDown() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[11];
    }

    public Cursor triangleDownFilled() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[12];
    }

    public Cursor triangleRight() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[13];
    }

    public Cursor triangleRightFilled() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[14];
    }

    public Cursor triangleLeft() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[15];
    }

    public Cursor triangleLeftFilled() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[16];
    }

    public Cursor arrowsCrossed() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        }
        checkInit();
        return cursors[17];
    }

    public Cursor multiMove() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        }
        checkInit();
        return cursors[18];
    }

    public Cursor rotate() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        }
        checkInit();
        return cursors[19];
    }

    public Cursor rotateMany() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        }
        checkInit();
        return cursors[20];
    }

    private void checkInit() {
        if (DISABLED) {
            return;
        }
        if (cursors[0] == null) {
            cursors(this.cursors, props);
        }
    }

    public Cursor cursorPerpendicularTo(double angle) {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        Quadrant quad = Quadrant.forAngle(angle);
        int quarter = quad.quarter(angle);
        checkInit();
        switch (quad) {
            case NORTHEAST:
            case SOUTHWEST:
                switch (quarter) {
                    case 0:
                        return cursors[2];
                    case 3:
                        return cursors[3];
                }
                break;
            default:
                switch (quarter) {
                    case 0:
                        return cursors[3];
                    case 3:
                        return cursors[2];
                }
        }
        return cursorPerpendicularToQuadrant(quad);
    }

    public Cursor cursorPerpendicularToQuadrant(Quadrant quad) {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        switch (quad) {
            case SOUTHEAST:
            case NORTHWEST:
                return cursors[1];
            case SOUTHWEST:
            case NORTHEAST:
                return cursors[0];
            default:
                throw new AssertionError(quad);
        }
    }

    private static BufferedImage rotated(BufferedImage img, int quadrants) {
        BufferedImage nue = new BufferedImage(img.getWidth(), img.getHeight(),
                img.getType());
        PooledTransform.withQuadrantRotateInstance(quadrants, img.getWidth() / 2D, img.getHeight() / 2D, xform -> {
            Graphics2D g = (Graphics2D) nue.getGraphics();
            try {
                g.drawImage(img, xform, null);
            } finally {
                g.dispose();
            }
        });
        return nue;
    }

    private static GraphicsConfiguration configFor(Component comp) {
        GraphicsConfiguration config = comp.getGraphicsConfiguration();
        if (config == null) {
            Frame[] fr = Frame.getFrames();
            if (fr != null && fr.length > 0) {
                config = fr[0].getGraphicsConfiguration();
            }
        }
        if (config == null) {
            config = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }
        return config;
    }

    private static CursorProperties propertiesForComponent(JComponent component) {
        boolean dark = Cursors.isDarkBackground(component);
        return propertiesForComponent(component, dark);
    }

    private static CursorProperties propertiesForComponent(JComponent component, boolean dark) {
        GraphicsConfiguration config = configFor(component);
        Rectangle screenBounds = config.getBounds();
        CursorProperties result = forScreenSize(screenBounds.getSize(), dark ? TWO_COLOR_DEFAULT_LIGHT : TWO_COLOR_DEFAULT_DARK);
        return result;
    }

    private static CursorProperties forToolkit(boolean dark) {
        Rectangle screenBounds = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration().getBounds();

        CursorProperties result = forScreenSize(screenBounds.getSize(), dark ? TWO_COLOR_DEFAULT_LIGHT : TWO_COLOR_DEFAULT_DARK);
        return result;
    }

    private static CursorProperties forScreenSize(Dimension screenBounds, CursorProperties base) {
        if (Math.max(screenBounds.width, screenBounds.height) > 1900) {
            base = base.scaled(2);
        }
        return base;
    }

    private static boolean isDarker(Color a, Color b) {
        return brightnessOf(a) < brightnessOf(b);
    }

    interface CursorProperties {

        Color shadow();

        Color primary();

        Color attention();

        Color warning();

        default Color darkerMainColor() {
            return isDarker(shadow(), primary()) ? shadow() : primary();
        }

        default Color lighterMainColor() {
            return !isDarker(shadow(), primary()) ? shadow() : primary();
        }

        int width();

        int height();

        default BasicStroke shadowStroke() {
            return new BasicStroke((width() / 8F) + 1);
        }

        default BasicStroke mainStroke() {
            return new BasicStroke(width() / 16F);
        }

        default BasicStroke wideShadowStroke() {
            return new BasicStroke((width() / 8F) + 2);
        }

        default BasicStroke wideMainStroke() {
            return new BasicStroke(height() / 16F);
        }

        default int centerX() {
            return width() / 2;
        }

        default int centerY() {
            return height() / 2;
        }

        int edgeOffset();

        int cornerOffset();

        int minimumHollow();

        CursorProperties scaled(int by);

        boolean isDarkBackground();

        Graphics2D hint(Graphics2D g);

        default Cursor createCursor(String name, int hitX, int hitY, Consumer<Graphics2D> c) {
            BufferedImage img = createCursorImage(c);
            return Toolkit.getDefaultToolkit().
                    createCustomCursor(img, new Point(hitX, hitY), name);
        }

        default CursorProperties warningVariant() {
            Color newColor = Color.RED;
            return isDarker(primary(), shadow()) ? withColors(newColor, shadow())
                    : withColors(primary(), newColor);
        }

        default CursorProperties attentionVariant() {
            Color newColor = Color.BLUE;
            return isDarker(primary(), shadow()) ? withColors(newColor, shadow())
                    : withColors(primary(), newColor);
        }

        default CursorProperties creationVariant() {
            Color newColor = new Color(0, 128, 0);
            return isDarker(primary(), shadow()) ? withColors(newColor, shadow())
                    : withColors(primary(), newColor);
        }

        default CursorProperties withColors(Color primary, Color shadow) {
            return this;
        }

        CursorProperties withSize(int w, int h);

        default BufferedImage createCursorImage(Consumer<Graphics2D> c) {
            return Cursors.createCursorImage(width(), height(), g -> {
                hint(g);
                c.accept(g);
            });
        }
    }

    /*
     * On linux and perhaps others, we are actually dealing with two-color
     * 8-bit color images with a small palette, and color conversion can
     * turn any subtleties in color into black or white.
     */
    private static final CursorProperties TWO_COLOR_DEFAULT_DARK
            = new CursorPropertiesImpl(LG, BLACK, 16, 16, true);

    private static final CursorProperties TWO_COLOR_DEFAULT_LIGHT
            = new CursorPropertiesImpl(DARK_GRAY, LG, 16, 16, false);

    static class CursorPropertiesImpl implements CursorProperties {

        private final Color shadow;
        private final Color main;
        private final int width;
        private final int height;
        private final boolean darkBackground;

        public CursorPropertiesImpl(Color shadow, Color main, int width, int height, boolean darkBackground) {
            this.shadow = shadow;
            this.main = main;
            if (width % 16 != 0) {
                width = Math.max(16, (width / 16) * 16);
            }
            if (height % 16 != 0) {
                height = Math.max(16, (height / 16) * 16);
            }
            Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(width, height);
            this.width = Math.max(4, d.width);
            this.height = Math.max(4, d.height);
            this.darkBackground = darkBackground;
        }

        @Override
        public CursorProperties withSize(int w, int h) {
            return new CursorPropertiesImpl(shadow, main, w, h, darkBackground);
        }

        @Override
        public CursorProperties withColors(Color primary, Color shad) {
            return new CursorPropertiesImpl(primary, shad, width, height, darkBackground);
        }

        public Graphics2D hint(Graphics2D g) {
            twoColorRenderingHints(g);
            return g;
        }

        public boolean isDarkBackground() {
            return darkBackground;
        }

        public CursorProperties scaled(int by) {
            return new CursorPropertiesImpl(shadow, main, width * by, height * by, darkBackground);
        }

        public int minimumHollow() {
            return (width / 8) + 1;
        }

        public int edgeOffset() {
            return width / 16;
        }

        public int cornerOffset() {
            return (width / 8) + 1;
        }

        @Override
        public Color shadow() {
            return shadow;
        }

        @Override
        public Color primary() {
            return main;
        }

        @Override
        public Color attention() {
            return main;
        }

        @Override
        public Color warning() {
            return main;
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public String toString() {
            return "CursorPropertiesImpl{" + "shadow=" + shadow + ", main="
                    + main + ", width=" + width + ", height=" + height
                    + ", darkBackground=" + darkBackground + '}';
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.shadow);
            hash = 29 * hash + Objects.hashCode(this.main);
            hash = 29 * hash + this.width;
            hash = 29 * hash + this.height;
            hash = 29 * hash + (this.darkBackground ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CursorPropertiesImpl other = (CursorPropertiesImpl) obj;
            if (this.width != other.width) {
                return false;
            }
            if (this.height != other.height) {
                return false;
            }
            if (this.darkBackground != other.darkBackground) {
                return false;
            }
            if (!Objects.equals(this.shadow, other.shadow)) {
                return false;
            }
            if (!Objects.equals(this.main, other.main)) {
                return false;
            }
            return true;
        }
    }
}
