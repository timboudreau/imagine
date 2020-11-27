package org.netbeans.paint.api.cursor;

import java.awt.Color;
import static java.awt.Color.BLACK;
import static java.awt.Color.DARK_GRAY;
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
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import static java.lang.Math.max;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import com.mastfrog.geometry.Quadrant;
import static com.mastfrog.geometry.Quadrant.NORTHEAST;
import static com.mastfrog.geometry.Quadrant.NORTHWEST;
import static com.mastfrog.geometry.Quadrant.SOUTHEAST;
import static com.mastfrog.geometry.Quadrant.SOUTHWEST;
import static org.netbeans.paint.api.cursor.CursorUtils.configFor;
import static org.netbeans.paint.api.cursor.CursorUtils.isDarkBackground;
import static org.netbeans.paint.api.cursor.CursorUtils.mac;
import static org.netbeans.paint.api.cursor.CursorUtils.rotated;

/**
 * Provides horizontal, vertical, diagonal and circle with line through it
 * cursors.
 *
 * @author Tim Boudreau
 */
final class CursorsImpl implements Cursors {

    public static final String SYSTEM_PROP_DISABLE_CUSTOM_CURSORS
            = "disable.custom.cursors";
    private static CursorsImpl DARK;
    private static CursorsImpl LIGHT;
    static final Color BRIGHT_COLOR = WHITE;
    private static final boolean DISABLED = Boolean.getBoolean(
            SYSTEM_PROP_DISABLE_CUSTOM_CURSORS);
    private final CursorProperties props;
    private static final String CLIENT_PROP_CURSORS = "cursors";
    private static final CursorShapes shapes = new CursorShapes();
    private static final Map<CursorProperties, CursorsImpl> FOR_PROPERTIES
            = new HashMap<>();
    private final Cursor[] cursors = new Cursor[26];

    private static int defaultCursorSize() {
        return mac ? 24 : 16;
    }

    /*
     * On linux and perhaps others, we are actually dealing with two-color
     * 8-bit color images with a small palette, and color conversion can
     * turn any subtleties in color into black or white.
     */
    private static final CursorProperties TWO_COLOR_DEFAULT_DARK
            = new CursorPropertiesImpl(BRIGHT_COLOR, BLACK, defaultCursorSize(),
                    defaultCursorSize(), true, defaultConfig());

    private static final CursorProperties TWO_COLOR_DEFAULT_LIGHT
            = new CursorPropertiesImpl(DARK_GRAY, BRIGHT_COLOR,
                    defaultCursorSize(), defaultCursorSize(),
                    false, defaultConfig());

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

        //        pnl.setBackground(Color.DARK_GRAY);
//        pnl.setForeground(Color.WHITE);
//        jf.setCursor(Cursors.forBrightBackgrounds().southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).horizontal());
//        jf.setCursor(Cursors.forComponent(pnl).southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).vertical());
//        jf.setCursor(Cursors.forComponent(pnl).star());
        jf.pack();
        jf.setCursor(Cursors.forComponent(pnl).arrowTilde());

//        jf.setCursor(Cursors.forComponent(pnl).hin());
//        jf.setCursor(Cursors.forComponent(pnl).triangleRight());
//        Cursor cur = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
//        jf.setCursor(cur);
//        jf.pack();
        jf.setSize(new Dimension(1_200, 800));
        jf.setVisible(true);
    }

    static class Comp extends JComponent {

        private final AffineTransform xf = AffineTransform.getScaleInstance(10, 10);

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
            CursorUtils.applyRenderingHints(gg);
//            drawStarImage(false, gg, 16, 16);
//            drawX(gg, false);
//            drawAngle0_90(gg, false);
//            drawAngle45(gg, true);
//            drawBarbellImage(true, gg, 16, 16);
//            drawNoImage(gg, TWO_COLOR_DEFAULT_DARK.scaled(2));
//            drawBarbellImage(gg, TWO_COLOR_DEFAULT_DARK.scaled(2));
//            drawRhombus(gg, TWO_COLOR_DEFAULT_LIGHT.scaled(2), false);
            shapes.drawArrowTilde(gg, TWO_COLOR_DEFAULT_DARK.scaled(2));
        }
    }

    private static void cursors(Cursor[] cursors, CursorProperties props) {
        BufferedImage base_45 = props.createCursorImage(g -> {
            shapes.drawAngle45(g, props);
        });
        cursors[0] = getDefaultToolkit().
                createCustomCursor(base_45, new Point(props.centerX(), props.centerY()), "northwest");

        // diagonal top-left to bottom-right
        cursors[1] = getDefaultToolkit().
                createCustomCursor(rotated(base_45, 1), new Point(props.centerX(), props.centerY()), "northeast");

        BufferedImage base0_90 = props.createCursorImage(gg -> {
            shapes.drawAngle0_90(gg, props);
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
        cursors[4] = props.createCursor("no", props.centerX(), props.centerY(), shapes::drawNoImage);

//        BufferedImage starImage = createCursorImage(gg -> drawStarImage(light, gg, 16, 16));
//        // diagonal bottom-left to top-right
//        cursors[5] = Toolkit.getDefaultToolkit().
//                createCustomCursor(starImage, new Point(8, 8), "star");
        cursors[5] = props.createCursor("star", props.centerX(), props.centerY(), shapes::drawStarImage);

        cursors[6] = props.createCursor("x", 0, 0, shapes::drawX);

        cursors[7] = props.createCursor("arrowsX", props.centerX(), props.centerY(), shapes::drawArrowsCrossed);

        cursors[8] = props.createCursor("barbell", 0, props.centerY(), shapes::drawBarbellImage);

        cursors[9] = props.createCursor("rhombus", props.centerX(), props.centerY(), g -> {
            shapes.drawRhombus(g, props, false);
        });

        cursors[10] = props.createCursor("rhombusFilled", props.centerX(), props.centerY(), g -> {
            shapes.drawRhombus(g, props, true);
        });
        cursors[11] = props.createCursor("triangleDown", props.centerX(), props.centerY(), g -> {
            shapes.drawTriangle(g, props, false);
        });
        cursors[12] = props.createCursor("triangleDownFilled", props.centerX(), props.height() - 1, g -> {
            shapes.drawTriangle(g, props, true);
        });
        cursors[13] = props.createCursor("triangleRight", props.centerX(), props.centerY(), g -> {
            shapes.drawTriangleRight(g, props, false);
        });
        cursors[14] = props.createCursor("triangleRightFilled", props.width() - 1, props.centerY(), g -> {
            shapes.drawTriangleRight(g, props, true);
        });
        cursors[15] = props.createCursor("triangleLeft", 0, props.centerY(), g -> {
            shapes.drawTriangleLeft(g, props, false);
        });
        cursors[16] = props.createCursor("triangleLeftFilled", 0, props.centerY(), g -> {
            shapes.drawTriangleLeft(g, props, true);
        });

        cursors[17] = props.createCursor("arrowsCrossed", 0, 0, shapes::drawAnglesCrossed);

        cursors[18] = props.createCursor("multiMove", props.width() - 1, props.height() - 1, shapes::drawMultiMove);

        cursors[19] = props.createCursor("rotate", 0, 0, shapes::drawRotate);

        cursors[20] = props.createCursor("rotateMany", 0, 0, shapes::drawRotateMany);

        cursors[21] = props.createCursor("dottedRect", 0, 0, shapes::drawDottedRect);

        cursors[22] = props.createCursor("arrow+", 0, 0, shapes::drawArrowPlus);
        cursors[23] = props.createCursor("shortArrow", 0, 0, shapes::drawArrowPlus);
        cursors[24] = props.createCursor("closeShape", 0, 0, shapes::drawCloseShape);
        cursors[25] = props.createCursor("arrowTilde", 0, 0, shapes::drawArrowTilde);
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

    /**
     * Get the cursors instance for this component based on its background
     * colors.
     *
     * @param comp
     * @return
     */
    static Cursors cursorsForComponent(JComponent comp) {
        Cursors result = (Cursors) comp.getClientProperty(
                CLIENT_PROP_CURSORS);
        if (result != null) {
            return result;
        }
        result = new CursorsProxy(comp);
        comp.putClientProperty(CLIENT_PROP_CURSORS, result);
        return result;
    }

    static CursorsImpl rawCursorsForComponent(JComponent comp) {
        boolean useLight = isDarkBackground(comp);
        CursorsImpl result;
        CursorProperties props = propertiesForComponent(comp);
        result = FOR_PROPERTIES.get(props);
        if (result == null && props != null) {
            result = new CursorsImpl(props);
            FOR_PROPERTIES.put(props, result);
        }
        if (useLight) {
            if (LIGHT == null) {
                LIGHT = new CursorsImpl(TWO_COLOR_DEFAULT_LIGHT);
            }
            result = new CursorsImpl(props);
        } else {
            if (DARK == null) {
                DARK = new CursorsImpl(TWO_COLOR_DEFAULT_DARK);
            }
            result = new CursorsImpl(props);
        }
        return result;
    }

    private static CursorProperties propertiesForComponent(JComponent component) {
        boolean dark = isDarkBackground(component);
        return propertiesForComponent(component, dark);
    }

    private static CursorProperties propertiesForComponent(JComponent component, boolean dark) {
        GraphicsConfiguration config = configFor(component);
        Rectangle screenBounds = config.getBounds();
//        int w = config.getDevice().getDisplayMode().getWidth();
//        int h = config.getDevice().getDisplayMode().getHeight();
//        Rectangle2D r2d = new Rectangle2D.Double(0, 0, 1, 1);
//        r2d = config.getNormalizingTransform().createTransformedShape(r2d).getBounds2D();
//        r2d.setFrame(0, 0, 1, 1);
//        r2d = config.getDefaultTransform().createTransformedShape(r2d).getBounds2D();
        Dimension size = screenBounds.getSize();
        CursorProperties result = forScreenSize(config, size, dark ? TWO_COLOR_DEFAULT_LIGHT : TWO_COLOR_DEFAULT_DARK);
        return result;
    }

    private static CursorProperties forToolkit(boolean dark) {
        Frame[] f = Frame.getFrames();
        for (int i = 0; i < f.length; i++) {
            if (f[i] instanceof JFrame) {
                JRootPane root = ((JFrame) f[i]).getRootPane();
                return propertiesForComponent(root, dark);
            }
        }
        Rectangle screenBounds = getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration().getBounds();

        CursorProperties result = forScreenSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration(), screenBounds.getSize(),
                dark ? TWO_COLOR_DEFAULT_LIGHT : TWO_COLOR_DEFAULT_DARK);
        return result;
    }

    private static GraphicsConfiguration defaultConfig() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    private static CursorProperties forScreenSize(GraphicsConfiguration config, Dimension screenBounds, CursorProperties base) {
        if (max(screenBounds.width, screenBounds.height) > 1_900) {
            base = base.scaled(2, config);
        }
        return base;
    }
}
