package org.netbeans.paint.api.cursor;

import java.awt.BasicStroke;
import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.BasicStroke.JOIN_ROUND;
import java.awt.Color;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import java.awt.Cursor;
import static java.awt.Cursor.CROSSHAIR_CURSOR;
import static java.awt.Cursor.DEFAULT_CURSOR;
import static java.awt.Cursor.E_RESIZE_CURSOR;
import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.Cursor.MOVE_CURSOR;
import static java.awt.Cursor.S_RESIZE_CURSOR;
import static java.awt.Cursor.TEXT_CURSOR;
import static java.awt.Cursor.getPredefinedCursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import org.imagine.geometry.Arrow;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.Quadrant;
import static org.imagine.geometry.Quadrant.NORTHEAST;
import static org.imagine.geometry.Quadrant.NORTHWEST;
import static org.imagine.geometry.Quadrant.SOUTHEAST;
import static org.imagine.geometry.Quadrant.SOUTHWEST;
import org.imagine.geometry.Rhombus;
import org.imagine.geometry.Triangle2D;
import org.imagine.geometry.util.GeometryStrings;
import static org.netbeans.paint.api.cursor.CursorUtils.brightnessOf;
import static org.netbeans.paint.api.cursor.CursorUtils.configFor;
import static org.netbeans.paint.api.cursor.CursorUtils.rotated;
import static org.netbeans.paint.api.cursor.CursorUtils.twoColorRenderingHints;
import org.openide.util.Exceptions;

/**
 * Provides horizontal, vertical, diagonal and circle with line through it
 * cursors.
 *
 * @author Tim Boudreau
 */
final class CursorsImpl implements Cursors {

    private static CursorsImpl DARK;
    private static CursorsImpl LIGHT;
    static final Color BRIGHT_COLOR = WHITE;
    private static final boolean DISABLED = Boolean.getBoolean("disable.custom.cursors");
    private final CursorProperties props;
    private final Cursor[] cursors = new Cursor[26];
    /*
     * On linux and perhaps others, we are actually dealing with two-color
     * 8-bit color images with a small palette, and color conversion can
     * turn any subtleties in color into black or white.
     */
    private static final CursorProperties TWO_COLOR_DEFAULT_DARK
            = new CursorPropertiesImpl(BRIGHT_COLOR, BLACK, 16, 16, true);

    private static final CursorProperties TWO_COLOR_DEFAULT_LIGHT
            = new CursorPropertiesImpl(DARK_GRAY, BRIGHT_COLOR,
                    16, 16, false);

    private CursorsImpl(CursorProperties props) {
        this.props = props;
    }

    public static CursorsImpl darkBackgroundCursors() {
        if (DARK == null) {
            DARK = new CursorsImpl(forToolkit(true));
        }
        return DARK;
    }

    public static CursorsImpl brightBackgroundCursors() {
        if (LIGHT == null) {
            LIGHT = new CursorsImpl(forToolkit(false));
        }
        return LIGHT;
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel pnl = new JPanel();
        pnl.add(new Comp());
        pnl.setPreferredSize(new Dimension(400, 400));
        jf.setContentPane(pnl);

        int colors = getDefaultToolkit().getMaximumCursorColors();
        System.out.println("max cur colors " + colors);
        System.out.println("TOOLKIT CLASS " + getDefaultToolkit().getClass().getName()
        );

        //        pnl.setBackground(Color.DARK_GRAY);
//        pnl.setForeground(Color.WHITE);
//        jf.setCursor(Cursors.forBrightBackgrounds().southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).horizontal());
//        jf.setCursor(Cursors.forComponent(pnl).southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).vertical());
//        jf.setCursor(Cursors.forComponent(pnl).star());
        jf.setCursor(Cursors.forComponent(pnl).southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).hin());
//        jf.setCursor(Cursors.forComponent(pnl).triangleRight());
//        Cursor cur = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        try ( InputStream in = Cursor.class.getResourceAsStream("/sun/awt/resources/cursors/cursors.properties")) {
            System.out.println("IN " + in);
            if (in != null) {
                byte[] bytes = new byte[128];
                int count;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((count = in.read(bytes)) > 0) {
                    out.write(bytes, 0, count);
                }
                String s = new String(out.toByteArray(), US_ASCII);
                System.out.println("CURWSORS: \n" + s);
            } else {
                System.out.println("no cursor resource");
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

//        jf.setCursor(cur);
//        jf.pack();
        jf.setSize(new Dimension(1_200, 800));
        jf.setVisible(true);
    }

    static class Comp extends JComponent {

        private final AffineTransform xf = AffineTransform.getScaleInstance(1, 1);

        @Override
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
            drawAngle45(gg, TWO_COLOR_DEFAULT_DARK.scaled(2));
        }
    }

    private static final Map<CursorProperties, CursorsImpl> FOR_PROPERTIES
            = new HashMap<>();

    /**
     * Get the cursors instance for this component based on its background
     * colors.
     *
     * @param comp
     * @return
     */
    static CursorsImpl cursorsForComponent(JComponent comp) {
        CursorsImpl result = (CursorsImpl) comp.getClientProperty(
                CLIENT_PROP_CURSORS);
        if (result != null) {
            return result;
        }
        CursorProperties props = propertiesForComponent(comp);
        result = FOR_PROPERTIES.get(props);
        if (result == null && props != null) {
            result = new CursorsImpl(props);
            FOR_PROPERTIES.put(props, result);
        }
        if (result == null) {
            boolean useLight = isDarkBackground(comp);
            if (useLight) {
                if (LIGHT == null) {
                    LIGHT = new CursorsImpl(TWO_COLOR_DEFAULT_LIGHT);
                }
                result = LIGHT;
            } else {
                if (DARK == null) {
                    DARK = new CursorsImpl(TWO_COLOR_DEFAULT_DARK);
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
        if (abs(bri1 - bri2) > 0.1) {
            result = bri1 > bri2;
        } else {
            result = min(bri1, bri2) < 0.45F;
        }
        return result;
    }

    private static final String CLIENT_PROP_CURSORS = "angleCursors";

    private static void cursors(Cursor[] cursors, CursorProperties props) {
        BufferedImage base_45 = props.createCursorImage(g -> {
            drawAngle45(g, props);
        });
        cursors[0] = getDefaultToolkit().
                createCustomCursor(base_45, new Point(props.centerX(), props.centerY()), "northwest");

        // diagonal top-left to bottom-right
        cursors[1] = getDefaultToolkit().
                createCustomCursor(rotated(base_45, 1), new Point(props.centerX(), props.centerY()), "northeast");

        BufferedImage base0_90 = props.createCursorImage(gg -> {
            drawAngle0_90(gg, props);
        });

        // horizontal
        cursors[2] = getDefaultToolkit().
                createCustomCursor(base0_90, new Point(7 * (props.width() / 16), 7 * (props.width() / 16)), "southeast");
        // vertical
        cursors[3] = getDefaultToolkit().
                createCustomCursor(rotated(base0_90, 1), new Point(7 * (props.width() / 16), 7 * (props.width() / 16)), "southwest");
//        BufferedImage no = createCursorImage(48, 48, g -> drawNoImage(g, light, 48, 48));
//        // no cursor
//        cursors[4] = Toolkit.getDefaultToolkit().
//                createCustomCursor(no, new Point(5, 5), "no");
        cursors[4] = props.createCursor("no", props.centerX(), props.centerY(), CursorsImpl::drawNoImage);

//        BufferedImage starImage = createCursorImage(gg -> drawStarImage(light, gg, 16, 16));
//        // diagonal bottom-left to top-right
//        cursors[5] = Toolkit.getDefaultToolkit().
//                createCustomCursor(starImage, new Point(8, 8), "star");
        cursors[5] = props.createCursor("star", props.centerX(), props.centerY(), CursorsImpl::drawStarImage);

        cursors[6] = props.createCursor("x", 0, 0, CursorsImpl::drawX);

        cursors[7] = props.createCursor("arrowsX", props.centerX(), props.centerY(), CursorsImpl::drawArrowsCrossed);

        cursors[8] = props.createCursor("barbell", 0, props.centerY(), CursorsImpl::drawBarbellImage);

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

        cursors[17] = props.createCursor("arrowsCrossed", 0, 0, CursorsImpl::drawAnglesCrossed);

        cursors[18] = props.createCursor("multiMove", props.width() - 1, props.height() - 1, CursorsImpl::drawMultiMove);

        cursors[19] = props.createCursor("rotate", 0, 0, CursorsImpl::drawRotate);

        cursors[20] = props.createCursor("rotateMany", 0, 0, CursorsImpl::drawRotateMany);

        cursors[21] = props.createCursor("dottedRect", 0, 0, CursorsImpl::drawDottedRect);

        cursors[22] = props.createCursor("arrow+", 0, 0, CursorsImpl::drawArrowPlus);
        cursors[23] = props.createCursor("shortArrow", 0, 0, CursorsImpl::drawArrowPlus);
        cursors[24] = props.createCursor("closeShape", 0, 0, CursorsImpl::drawCloseShape);
        cursors[25] = props.createCursor("arrowTilde", 0, 0, CursorsImpl::drawArrowTilde);
    }

    private static void drawCloseShape(Graphics2D g, CursorProperties props) {
        Circle circ = new Circle(props.centerX(), props.centerY(), (props.centerX() - ((props.width() / 12) + (props.width() / 16))));
        Area a = new Area(new Rectangle(0, 0, props.centerX(), props.centerY()));
        Area a2 = new Area(new Rectangle(0, 0, props.width(), props.height()));
        a2.subtract(a);
        g.setClip(a2);
        g.setColor(props.shadow());
        g.setStroke(new BasicStroke(props.mainStroke().getLineWidth() * 1.5F));
        g.draw(circ);

        g.setColor(props.primary());
        g.setStroke(props.mainStroke());
        g.draw(circ);
//        g.drawRect(0, 0, props.width() - 1, props.height() - 1);
        g.setClip(null);
        BasicStroke mstroke = new BasicStroke(props.mainStroke().getLineWidth(), CAP_SQUARE, JOIN_MITER, 17F);
        BasicStroke lstroke = new BasicStroke(props.mainStroke().getLineWidth() * 1.5F, CAP_SQUARE, JOIN_MITER, 17F);
        Triangle2D[] tris = new Triangle2D[2];
        double off = -(props.width() / 16);
        double ooff = props.width() / 32;
        Runnable heads = () -> {
            double headLength = props.width() / 10;
            circ.positionOf(0, (x, y) -> {
                Circle.positionOf(135, x + off, y, headLength, (x1, y1) -> {
                    Circle.positionOf(45, x + off, y, headLength, (x2, y2) -> {
//                        Area aa = new Area(new Rectangle2D.Double(x2 + off, y - (lstroke.getLineWidth() / 2),
//                                x2 - x, lstroke.getLineWidth()));
//                        Area aa1 = new Area(new Rectangle(0, 0, props.width(), props.height()));
//                        aa1.subtract(aa);
//                        g.setClip(aa1);
                        g.draw(tris[0] = new Triangle2D(x + off, y + ooff, x1 - off, y1 + ooff, x2 - off, y2 + ooff));
//                        g.setClip(null);
                    });
                });
            });
            circ.positionOf(270, (x, y) -> {
                Circle.positionOf(135, x, y - off, headLength, (x1, y1) -> {
                    Circle.positionOf(-135, x, y - off, headLength, (x2, y2) -> {
                        g.draw(tris[1] = new Triangle2D(x + ooff, y + off, x1 + ooff, y1 + off, x2 + ooff, y2 + off));
                    });
                });
            });
        };
        g.setClip(0, 0, props.width(), props.height());
        g.setColor(props.shadow());
        g.setStroke(lstroke);
        heads.run();
        int lo = props.width() / 12;
        g.drawLine(lo, lo, props.centerX() / 2, props.centerY() / 2);
        g.setColor(props.primary());
        g.setStroke(mstroke);
        heads.run();
        g.drawLine(lo, lo, props.centerX() / 2, props.centerY() / 2);
        g.fill(tris[0]);
        g.fill(tris[1]);

//        g.setColor(Color.GREEN);
        Rectangle2D.Double r = new Rectangle2D.Double();
        r.x = tris[0].bx() + (off / 2) - (ooff / 2);
        r.y = tris[0].centerY() - (mstroke.getLineWidth() / 2);
        r.width = mstroke.getLineWidth();
        r.height = mstroke.getLineWidth() - (mstroke.getLineWidth() / 2);
        g.fill(r);
        g.draw(r);

    }

    public static void drawShortArrow(Graphics2D g, CursorProperties props) {
        Area bds = new Area(new Rectangle(0, 0, props.width(), props.height()));
        Area a = new Area(new Triangle2D(props.width(), props.height(),
                props.width(), 0,
                0, props.height()));
        bds.subtract(a);
        g.setClip(bds);
        drawAngle45(g, props);
        g.setClip(null);
        drawAngle45(g, props);
//        g.setBackground(new Color(255, 255, 255, 0));
//        g.clearRect(props.centerX(), props.centerY(), (props.width() - props.centerX()) + 2, (props.height() - props.centerY()) + 2);
    }

    public static void drawArrowTilde(Graphics2D g, CursorProperties props) {
        Area bds = new Area(new Rectangle(0, 0, props.width(), props.height()));
        Area a = new Area(new Triangle2D(props.width(), props.height(),
                props.width(), 0,
                0, props.height()));
        bds.subtract(a);
        g.setClip(bds);
        drawAngle45(g, props);
        g.setClip(null);

        g.translate(-((props.width() / 16) - props.width() / 16), -((props.height() / 4) + (props.height() / 8) + props.height() / 32));

        int in = props.width() / 16;
        int in2 = in * 2;
        int halfY = ((props.height() - props.centerY()) / 2) - in;
        int halfX = ((props.width() - props.centerX()) / 2) - in;

        Path2D.Double path = new Path2D.Double();
        double ax = props.centerX() + in;
        double ay = props.centerY() + halfY + in / 2;
        path.moveTo(ax, ay);

        double cx = props.width() - in;
        double cy = props.centerY() + halfY + in / 2;

        double qx = props.centerX() + halfX + in;
        double qy = props.centerY() + in;

        path.quadTo(qx, qy, cx, cy);
        double len = cx - ax;
        double qy2 = ay + (in * 2);
        path.quadTo(qx + len, qy2, cx + len, ay);

        g.translate(-props.centerX() + (props.width() / 32), props.centerY());

        g.setStroke(props.shadowStroke());
        g.setColor(props.shadow());
//        g.drawLine(props.centerX() + in, props.centerY() + halfY + in / 2, props.width() - in, props.centerY() + halfY + in / 2);
//        g.drawLine(props.centerX() + halfX + in, props.centerY() + in, props.centerX() + halfX + in, props.height() - in2);
        g.draw(path);
        g.setColor(props.primary());
        g.setStroke(props.mainStroke());
        g.draw(path);
//        g.drawLine(props.centerX() + in, props.centerY() + halfY + in / 2, props.width() - in, props.centerY() + halfY + in / 2);
//        g.drawLine(props.centerX() + halfX + in, props.centerY() + in, props.centerX() + halfX + in, props.height() - in2);
    }

    public static void drawArrowPlus(Graphics2D g, CursorProperties props) {
        drawAngle45(g, props);
        g.setBackground(new Color(255, 255, 255, 0));
        g.clearRect(props.centerX(), props.centerY(), (props.width() - props.centerX()) + 5, (props.height() - props.centerY()) + 5);
        g.translate(-((props.width() / 16) - props.width() / 16), -((props.height() / 4) + (props.height() / 8) + props.height() / 32));

        int in = props.width() / 12;
        int in2 = in * 2;
        int halfY = ((props.height() - props.centerY()) / 2) - in;
        int halfX = ((props.width() - props.centerX()) / 2) - in;

        g.setStroke(props.shadowStroke());
        g.setColor(props.shadow());
        g.drawLine(props.centerX() + in, props.centerY() + halfY + in / 2, props.width() - in, props.centerY() + halfY + in / 2);
        g.drawLine(props.centerX() + halfX + in, props.centerY() + in, props.centerX() + halfX + in, props.height() - in2);
        g.setColor(props.primary());
        g.setStroke(props.mainStroke());
        g.drawLine(props.centerX() + in, props.centerY() + halfY + in / 2, props.width() - in, props.centerY() + halfY + in / 2);
        g.drawLine(props.centerX() + halfX + in, props.centerY() + in, props.centerX() + halfX + in, props.height() - in2);
    }

    public static void drawDottedRect(Graphics2D g, CursorProperties props) {
        int off = props.width() / 8;
        Rectangle rect = new Rectangle(off, off, props.width() - (off * 2), props.height() - (off * 2));
        float ws = props.mainStroke().getLineWidth() * 1.5F;
        int sz = props.width() - (off * 2);
        System.out.println("sz " + sz + " ");
        float[] flts = new float[]{sz / 4F};
        float shift = (sz / 8);
//        shift = 0;
        BasicStroke shadowStroke = new BasicStroke(ws, CAP_ROUND, JOIN_ROUND, 1F, flts, shift);
        g.setStroke(shadowStroke);
        g.setColor(props.shadow());
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
//        g.draw(rect);
        float wl = props.mainStroke().getLineWidth();
        g.setColor(props.primary());
        BasicStroke lineStroke = new BasicStroke(wl, CAP_BUTT, JOIN_ROUND, 1F, flts, shift);
        g.setStroke(lineStroke);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    private static void drawRotateMany(Graphics2D g, CursorProperties props) {
        drawRotate(g, props);
        double off = (props.width() / 9);
        double rad = props.width() / 10;
        Circle circ = new Circle(props.centerX() - off, props.centerY() - off, rad);
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
        g.setColor(WHITE);
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
        g.setBackground(new Color(255, 255, 255, 0));
        EnhRectangle2D r = new EnhRectangle2D(0, 0, props.width() / 6, props.height() / 6);
        r.setCenter(props.centerX(), props.centerY());
        Rectangle r2 = r.getBounds();
        g.clearRect(r2.x, r2.y, r2.width, r2.height);
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
        g.setColor(BLUE);
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
        g.setColor(RED);
        g.draw(new Line2D.Double(rhom.ax(), rhom.ay(), rhom.centerX(),
                rhom.cy()));
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

    private static void drawArrowsCrossed(Graphics2D g, CursorProperties props) {
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
        int centerFree = min(w, h) / 8;
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

    @Override
    public Cursor star() {
        if (DISABLED) {
            return getPredefinedCursor(CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[5];
    }

    @Override
    public Cursor barbell() {
        if (DISABLED) {
            return getPredefinedCursor(TEXT_CURSOR);
        }
        checkInit();
        return cursors[8];
    }

    @Override
    public Cursor x() {
        if (DISABLED) {
            return getPredefinedCursor(MOVE_CURSOR);
        }
        checkInit();
        return cursors[6];
    }

    @Override
    public Cursor hin() {
        if (DISABLED) {
            return getPredefinedCursor(E_RESIZE_CURSOR);
        }
        checkInit();
        return cursors[7];
    }

    @Override
    public Cursor no() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[4];
    }

    @Override
    public Cursor horizontal() {
        if (DISABLED) {
            return getPredefinedCursor(E_RESIZE_CURSOR);
        }
        checkInit();
        return cursors[2];
    }

    @Override
    public Cursor vertical() {
        if (DISABLED) {
            return getPredefinedCursor(S_RESIZE_CURSOR);
        }
        checkInit();
        return cursors[3];
    }

    @Override
    public Cursor southWestNorthEast() {
        if (DISABLED) {
            return getPredefinedCursor(CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[1];
    }

    @Override
    public Cursor southEastNorthWest() {
        if (DISABLED) {
            return getPredefinedCursor(CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[0];
    }

    @Override
    public Cursor rhombus() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[9];
    }

    @Override
    public Cursor rhombusFilled() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[10];
    }

    @Override
    public Cursor triangleDown() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[11];
    }

    @Override
    public Cursor triangleDownFilled() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[12];
    }

    @Override
    public Cursor triangleRight() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[13];
    }

    @Override
    public Cursor triangleRightFilled() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[14];
    }

    @Override
    public Cursor triangleLeft() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[15];
    }

    @Override
    public Cursor triangleLeftFilled() {
        if (DISABLED) {
            return getPredefinedCursor(DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[16];
    }

    @Override
    public Cursor arrowsCrossed() {
        if (DISABLED) {
            return getPredefinedCursor(MOVE_CURSOR);
        }
        checkInit();
        return cursors[17];
    }

    @Override
    public Cursor multiMove() {
        if (DISABLED) {
            return getPredefinedCursor(MOVE_CURSOR);
        }
        checkInit();
        return cursors[18];
    }

    @Override
    public Cursor rotate() {
        if (DISABLED) {
            return getPredefinedCursor(TEXT_CURSOR);
        }
        checkInit();
        return cursors[19];
    }

    @Override
    public Cursor rotateMany() {
        if (DISABLED) {
            return getPredefinedCursor(TEXT_CURSOR);
        }
        checkInit();
        return cursors[20];
    }

    @Override
    public Cursor dottedRect() {
        if (DISABLED) {
            return getPredefinedCursor(HAND_CURSOR);
        }
        checkInit();
        return cursors[21];
    }

    @Override
    public Cursor arrowPlus() {
        if (DISABLED) {
            return getPredefinedCursor(HAND_CURSOR);
        }
        checkInit();
        return cursors[22];
    }

    @Override
    public Cursor shortArrow() {
        if (DISABLED) {
            return getPredefinedCursor(HAND_CURSOR);
        }
        checkInit();
        return cursors[23];
    }

    @Override
    public Cursor closeShape() {
        if (DISABLED) {
            return getPredefinedCursor(CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[24];
    }

    @Override
    public Cursor arrowTilde() {
        if (DISABLED) {
            return getPredefinedCursor(HAND_CURSOR);
        }
        checkInit();
        return cursors[25];
    }

    private void checkInit() {
        if (DISABLED) {
            return;
        }
        if (cursors[0] == null) {
            cursors(this.cursors, props);
        }
    }

    @Override
    public Cursor cursorPerpendicularTo(double angle) {
        if (DISABLED) {
            return getPredefinedCursor(CROSSHAIR_CURSOR);
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

    @Override
    public Cursor cursorPerpendicularToQuadrant(Quadrant quad) {
        if (DISABLED) {
            return getPredefinedCursor(CROSSHAIR_CURSOR);
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

    private static CursorProperties propertiesForComponent(JComponent component) {
        boolean dark = isDarkBackground(component);
        return propertiesForComponent(component, dark);
    }

    private static CursorProperties propertiesForComponent(JComponent component, boolean dark) {
        GraphicsConfiguration config = configFor(component);
        System.out.println("CONFIG " + config.hashCode() + " - " + config);
        Rectangle screenBounds = config.getBounds();
        System.out.println("Screeen bounds " + GeometryStrings.toString(screenBounds));
        int w = config.getDevice().getDisplayMode().getWidth();
        int h = config.getDevice().getDisplayMode().getHeight();
        System.out.println("screen size " + w + " * " + h);
        Rectangle2D r2d = new Rectangle2D.Double(0, 0, 1, 1);
        r2d = config.getNormalizingTransform().createTransformedShape(r2d).getBounds2D();
        System.out.println("WITH NORM XFFORM " + GeometryStrings.toString(r2d));
        r2d.setFrame(0, 0, 1, 1);
        r2d = config.getDefaultTransform().createTransformedShape(r2d).getBounds2D();
        System.out.println("WITH DEFL XFFORM " + GeometryStrings.toString(r2d));
        System.out.println("CurSize 16" + getDefaultToolkit().getBestCursorSize(16, 16));
//        screenBounds = config.getNormalizingTransform().createTransformedShape(screenBounds).getBounds();
        System.out.println("FINAL SCREEEN SIZE " + screenBounds.getSize());
        Dimension size = screenBounds.getSize();
        CursorProperties result = forScreenSize(size, dark ? TWO_COLOR_DEFAULT_LIGHT : TWO_COLOR_DEFAULT_DARK);
        return result;
    }

    private static CursorProperties forToolkit(boolean dark) {
        Rectangle screenBounds = getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration().getBounds();

        CursorProperties result = forScreenSize(screenBounds.getSize(), dark ? TWO_COLOR_DEFAULT_LIGHT : TWO_COLOR_DEFAULT_DARK);
        return result;
    }

    private static CursorProperties forScreenSize(Dimension screenBounds, CursorProperties base) {
        if (true) {
//            return base.scaled(3);
        }
        if (max(screenBounds.width, screenBounds.height) > 1_900) {
            base = base.scaled(2);
        }
        return base;
    }
}
