package net.java.dev.imagine.api.vector.elements;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Textual;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import static net.java.dev.imagine.api.vector.elements.Text.scratchImage;
import static net.java.dev.imagine.api.vector.elements.Text.trimTail;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import net.java.dev.imagine.api.vector.util.Pt;
import net.java.dev.imagine.api.vector.util.plot.ShapePlotter;
import org.imagine.geometry.Triangle2D;
import org.imagine.geometry.util.DoubleList;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.utils.java2d.GraphicsUtils;
import static org.imagine.utils.java2d.GraphicsUtils.transformHashCode;
import static org.imagine.utils.java2d.GraphicsUtils.transformToString;
import static org.imagine.utils.java2d.GraphicsUtils.transformsEqual;
import org.openide.util.Exceptions;
import net.java.dev.imagine.api.vector.Vectors;

/**
 * Wraps a string and the font used to render it so it can be turned into a
 * shape.
 *
 * @author Tim Boudreau
 */
public class PathText implements Primitive, Vectors, Adjustable, Textual, Versioned {

    public static final int MAX_RENDERING_VERSION = 1;

    private transient static BufferedImage scratch;
    private StringWrapper text;
    private FontWrapper font;
    private AffineTransform xform;
    private transient double[] baselines;
    private float leadingMultiplier = 1;
    private Shape cachedShape;
    private int hashAtShapeCache = -1;
    double[] charPositions = null;
    private boolean useArea = false;
    Area area;
    private int renderingVersion = MAX_RENDERING_VERSION;
    private Shaped shape;
    private int rev;

    public PathText(PathText other, AffineTransform xform) {
        this.shape = other.shape.copy();
        this.text = other.text.copy();
        this.font = other.font.copy();
        this.xform = xform;
        this.rev = xform == null ? other.rev : other.rev + 1;
        this.leadingMultiplier = other.leadingMultiplier;
        this.cachedShape = xform != null ? null : other.cachedShape;
        this.hashAtShapeCache = xform != null ? -1 : other.hashAtShapeCache;
        this.useArea = other.useArea;
        this.baselines = xform != null ? new double[0] : other.baselines == null ? new double[0] : Arrays.copyOf(other.baselines, other.baselines.length);
        this.charPositions = xform != null ? new double[0] : other.charPositions == null ? new double[0] : Arrays.copyOf(other.charPositions, other.charPositions.length);
        this.renderingVersion = other.renderingVersion;
    }

    public PathText(PathText other) {
        this.shape = other.shape.copy();
        this.text = other.text.copy();
        this.font = other.font.copy();
        this.xform = other.xform == null ? null : new AffineTransform(other.xform);
        this.rev = other.rev;
        this.leadingMultiplier = other.leadingMultiplier;
        this.cachedShape = other.cachedShape;
        this.hashAtShapeCache = other.hashAtShapeCache;
        this.useArea = other.useArea;
        this.baselines = other.baselines == null ? null : Arrays.copyOf(other.baselines, other.baselines.length);
        this.charPositions = other.charPositions == null ? null : Arrays.copyOf(other.charPositions, other.charPositions.length);
        this.renderingVersion = other.renderingVersion;
    }

    public PathText(Shaped shape, StringWrapper string, FontWrapper font, AffineTransform xform) {
        this.text = string;
        this.font = font;
        this.xform = xform;
        this.shape = shape;
    }

    public PathText(Shaped shape, StringWrapper string, FontWrapper font) {
        this.text = string;
        this.font = font;
        this.shape = shape;
    }

    public PathText(Shaped shape, String text, Font font, double x, double y) {
        this.text = new StringWrapper(text, x, y);
        this.font = FontWrapper.create(font, true);
        this.shape = shape;
    }

    public int rev() {
        int result = rev + 1000 * text.rev() + 10000 * font.rev();
        if (shape instanceof Versioned) {
            result += 100000 * ((Versioned) shape).rev();
        }
        return result;
    }

    private void change() {
        rev++;
    }

    public Shaped shape() {
        return shape;
    }

    public void setShape(Shaped shape) {
        this.shape = shape;
        invalidateCachedShape();
    }

    public AffineTransform transform() {
        return xform;
    }

    @Override
    public void translate(double x, double y) {
        if (xform == null) {
            xform = AffineTransform.getTranslateInstance(x, y);
        } else {
            xform.preConcatenate(AffineTransform.getTranslateInstance(x, y));
        }
        invalidateCachedShape();
    }

    @Override
    public Runnable restorableSnapshot() {
        Runnable tr = text.restorableSnapshot();
        Runnable fr = font.restorableSnapshot();
        Runnable sh = shape.restorableSnapshot();
        int oldRev = rev;
        int rv = renderingVersion;
        Shaped oldShape = shape;
        FontWrapper oldFont = font;
        StringWrapper oldText = text;
        AffineTransform xf = xform == null ? null : new AffineTransform(xform);
        return () -> {
            font = oldFont;
            text = oldText;
            rev = oldRev;
            xform = xf;
            shape = oldShape;
            renderingVersion = rv;
            tr.run();
            fr.run();
            sh.run();
            invalidateCachedShape();
        };
    }

    public FontWrapper font() {
        return font;
    }

    public StringWrapper text() {
        return text;
    }

    public String fontName() {
        return font.getName();
    }

    public float fontSize() {
        return font.getSize();
    }

    public int getFontStyle() {
        return font.getStyle();
    }

    public void setFontSize(float size) {
        invalidateCachedShape();
        font.setSize(size);
    }

    public void setFontName(String fontName) {
        invalidateCachedShape();
        font.setName(fontName);
    }

    public void setFontStyle(int style) {
        invalidateCachedShape();
        font.setStyle(style);
    }

    public void setTransform(AffineTransform xform) {
        this.xform = xform;
        invalidateCachedShape();
    }

    public void setText(StringWrapper text) {
        invalidateCachedShape();
        this.text = text;
    }

    public void setFont(FontWrapper font) {
        invalidateCachedShape();
        this.font = font;
    }

    public void setText(String txt) {
        invalidateCachedShape();
        text.setText(txt);
    }

    public double x() {
        return text.x();
    }

    public double y() {
        return text.y();
    }

    public void setX(double x) {
        text.setX(x);
        invalidateCachedShape();
    }

    public void setY(double y) {
        text.setY(y);
        invalidateCachedShape();
    }

    public String getText() {
        return text.getText();
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    @Override
    public void setLocation(double x, double y) {
        Pt loc = getLocation();
        xform.preConcatenate(AffineTransform.getTranslateInstance(loc.x - x, loc.y - y));
        invalidateCachedShape();
    }

    @Override
    public void clearLocation() {
        text.clearLocation();
        invalidateCachedShape();
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform != null && !xform.isIdentity()) {
            if (this.xform == null) {
                this.xform = xform;
            } else {
                this.xform.preConcatenate(xform);
            }
            invalidateCachedShape();
        }
    }

    public Font getFont() {
        return font.toFont();
    }

    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    @Override
    public void paint(Graphics2D g) {
        if (area != null) {
            Color old = g.getColor();
            g.setColor(Color.BLACK);
            g.fill(area);
            g.draw(area);
            g.setColor(old);
        }
        g.fill(toShape());
//        g.setColor(Color.BLACK);
//        g.draw(this.shape.toShape());
    }

    @Override
    public boolean is(Class<?> type) {
        if (font.is(type)) {
            return true;
        }
        if (text.is(type)) {
            return true;
        }
        if (shape.is(type)) {
            return true;
        }
        return Vectors.super.is(type);
    }

    @Override
    public <T> T as(Class<T> type) {
        T res = Vectors.super.as(type);
        if (res != null) {
            return res;
        }
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        if (font.is(type)) {
            return font.as(type);
        }
        if (text.is(type)) {
            return text.as(type);
        }
        if (shape.is(type)) {
            return shape.as(type);
        }
        T result = font.as(type);
        if (result == null) {
            result = text.as(type);
        }
        return result;
    }

    @Override
    public Pt getLocation() {
        return new Pt(x(), y());
    }

    @Override
    public PathText copy(AffineTransform xform) {
        PathText nue = new PathText(this);
        if (xform != null && !xform.isIdentity()) {
            nue.applyTransform(xform);
        }
        return nue;
    }

    @Override
    public PathText copy() {
        return new PathText(this);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        if (bds.isEmpty()) {
            getBounds(bds);
        } else {
            bds.add(toShape().getBounds2D());
        }
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        Shape shape = toShape();
        if (shape == null) {
            new Exception("Got null shape").printStackTrace();
            return;
        }
        dest.setFrame(shape.getBounds2D());
    }

    @Override
    public void collectSizings(SizingCollector c) {
        // do nothing
    }

    public double[] getBaselines() {
        toShape();
        return baselines == null ? new double[0] : baselines;
    }

    @Override
    public Rectangle getBounds() {
        return toShape().getBounds();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + text.hashCode();
        hash = 29 * hash + font.hashCode();
        hash = 29 * hash + shape.hashCode();
        hash = 29 * hash + transformHashCode(xform);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof PathText)) {
            return false;
        }
        final PathText other = (PathText) obj;
        return other.leadingMultiplier == leadingMultiplier
                && Objects.equals(this.text, other.text)
                && Objects.equals(this.font, other.font)
                && transformsEqual(this.xform, other.xform);
    }

    @Override
    public String toString() {
        return "Text{" + "text=" + text + ", font=" + font + ' '
                + transformToString(xform) + '}';
    }

    private void invalidateCachedShape() {
        cachedShape = null;
        hashAtShapeCache = -1;
        baselines = null;
        change();
    }

    public int renderingVersion() {
        return renderingVersion;
    }

    public void setRenderingVersion(int renderingVersion) {
        this.renderingVersion = renderingVersion;
        invalidateCachedShape();
    }

    @Override
    public Shape toShape() {
        Shape result = cachedShape;
        int hash;
        if (result != null) {
            hash = hashCode();
            if (hash == hashAtShapeCache) {
                return result;
            }
            result = cachedShape = makeShape();
            hashAtShapeCache = hash;
        } else {
            hashAtShapeCache = hashCode();
            result = cachedShape = makeShape();
        }
        return result;
    }

    private Shape makeShape() {
        // Okay, how this madness works:
        //  *  ShapePlotter replicates Java2D's shape plotting capacity -
        //     it will iterate the points that would be drawn in a shape, one
        //     by one, and call a callback with the point and two tangent points,
        //     which we can use to find the angle of the tangent and rotate the
        //     glyphs to that, and transform them to the right location
        //
        //  *  Do a first pass over the shape to get the total length in pixels
        //     available to wrap characters across - if it is < than the length
        //     of the GlyphVector (modulo rotation, which currently mangles the
        //     results a bit), we will compute a scaling transform so the text
        //     gets scaled down to fit the characters (needs work)
        //
        // *  We wrap a simpler interface with a Plotter instance, which returns
        //    the distance to the next point at which it can paint something, and
        //    the plotter merrily iterates away until it gets that distance
        //    (yes, we could and eventually should just compute the point based
        //    on the equation of the curve), then calls us with the coordinates
        //    and an EqLine representing the tangent, for ease of getting the angle
        //
        // *  Before we start iterating shape points, get the array of glyph offsets,
        //    transform it if needed, and create from it an array of glyph-to-glyph
        //    offsets, so we can easily return the number of pixels we must pass before
        //    the next glyph can be painted
        //
        // *  As we get called to paint one glyph, do a boatload of transforms on
        //    the current glyph:
        //    * Transform it to 0,0 - it arrives with its offset relative to the
        //      rest of the characters, which will be in the wrong coordinate space
        //      for following our path
        //    * Get the angle of the tangent and rotate it to that
        //    * Translate it to the real x/y coordinates we're at in our iteration of
        //      the points of the path
        //    * currently commented out: compare the distance between the
        //      last and current glyph bounding boxes, and if less then the distance
        //      we need (different rotation of adjacent characters can do that),
        //      translate the shape forward on the X axis so that it is out of the way
        //    * Add it to our bucket of shapes
        //
        // *  Return a shape instance which aggregates all of our glyph shapes as a
        //    single shape (simple aggregation, not using Area, which is massively
        //    computationally expensive and doesn't solve a problem here)
        Shape path = this.shape.toShape();

        plotArea(path);

        // Need a real FontRenderContext to get glyph vectors
        BufferedImage img = scratchImage();
        Graphics2D g = img.createGraphics();
        try {
            Font f = getFont();
            FontRenderContext frc = g.getFontRenderContext();
            // Leave off trailing spaces
            String txt = trimTail(getText());
            DoubleList cps = new DoubleList(getText().length() * 2);
            GlyphVector gv = f.createGlyphVector(frc, txt);

            // Get a shape the combines the entire line of text
            Shape origGlyphOutline = gv.getOutline(0, 0);
            Rectangle2D origOutlineBounds = origGlyphOutline.getBounds2D();

            double length = this.shape.cumulativeLength();

            double height = origOutlineBounds.getHeight();

            int glyphs = gv.getNumGlyphs();
            float[] glyphPositions = gv.getGlyphPositions(0, glyphs, null);
            AffineTransform scaleXform = null;
            if (length < origOutlineBounds.getWidth()) {
                double lastGlyphXPosition = glyphPositions[glyphPositions.length - 2];
                Shape last = gv.getGlyphOutline(glyphs - 1);
                lastGlyphXPosition += last.getBounds2D().getWidth();
//                double scale = Math.min(length / lastGlyphXPosition, length / (origOutlineBounds.getX() + origOutlineBounds.getWidth()));
                double scale;
                if (length < origOutlineBounds.getWidth()) {
                    scale = Math.max(length / lastGlyphXPosition, length / (origOutlineBounds.getX() + origOutlineBounds.getWidth()));
                } else {
                    scale = Math.min(length / lastGlyphXPosition, length / (origOutlineBounds.getX() + origOutlineBounds.getWidth()));
                }
                System.out.println("scale " + scale);
                scaleXform = AffineTransform.getScaleInstance(scale, scale);
                scaleXform.transform(glyphPositions, 0, glyphPositions, 0, glyphPositions.length / 2);
            }
            float[] glyphOffsets = new float[glyphPositions.length / 2];
            for (int i = 2; i < glyphPositions.length; i += 2) {
                glyphOffsets[i / 2] = glyphPositions[i] - glyphPositions[i - 2];
            }
            List<Shape> allShapes = new ArrayList<>(glyphs);
            AffineTransform scaler = scaleXform;

//            allShapes.add(tanLine);
            // Pending:  What we really need is to get the tangent
            // at both the start and end of the line, and use the
            // average angle at the center point
            PositionerImpl2 positioner = new PositionerImpl2(gv, scaler,
                    glyphOffsets, allShapes, glyphs, height);

            ShapePlotter.position(path, positioner, plotter -> {
                // Need extra points on our lines to avoid spacing
                // letters out too much
                plotter.setInterval(0.25);
                plotter.setTangentLength(height / 2);
            });
            // If the line wasn't long enough, just keep going in
            // whatever direction the line was headed in
            positioner.finish();

            double x = x();
            double y = y();
            cps.add(x);
            cps.add(y);
            for (int j = 0; j < glyphPositions.length; j += 2) {
                cps.add(glyphPositions[j] + x);
                cps.add(y);
            }
            baselines = new double[]{x, y};
            charPositions = cps.toDoubleArray();
            if (xform != null) {
                xform.transform(baselines, 0, baselines, 0, baselines.length / 2);
                xform.transform(charPositions, 0, charPositions, 0, charPositions.length / 2);
            }
            Path2D.Float p = new Path2D.Float();
            for (Shape s : allShapes) {
                p.append(s.getPathIterator(xform), false);
            }
            return p;
        } finally {
            g.dispose();
        }
    }

    private void plotArea(Shape path) {
        Rectangle2D.Double scratchRect = new Rectangle2D.Double();
        double[] lastCoords = new double[]{-1, -1};
        // Some debug code to brute-force paint the path that the plotter
        // thinks the shape follows
        if (useArea) {
            area = new Area();
            new ShapePlotter((double x, double y, double c, double tanX1, double tanY1, double tanX2, double tanY2) -> {
                if (Point2D.distance(lastCoords[0], lastCoords[1], x, y) < 1) {
                    return;
                }
                lastCoords[0] = x;
                lastCoords[1] = y;
                scratchRect.x = x;
                scratchRect.y = y;
                // need some width and height or it is clobbered
                scratchRect.width = 0.01;
                scratchRect.height = 0.01;
                scratchRect.add(tanX1, tanY1);
                scratchRect.add(tanX2, tanY2);
                area.add(new Area(scratchRect));
            }).plot(path);
        }
    }

    public static void main(String[] args) {
        Path2D.Double p2 = new Path2D.Double();
        p2.moveTo(10, 100);
//        p2.curveTo(200, 600, 200, 0, 400, 500);
        p2.quadTo(200, 600, 400, 500);
//        p2.curveTo(500, 800, 500, 600, 10, 700);
        p2.quadTo(500, 800, 10, 700);
        p2.quadTo(500, 600, 300, 800);
//        p2.curveTo(-100, 1200, 400, 800, 400, 1000);
//        p2.curveTo(200, 350, 200, 550, 425, 425);

        Triangle2D t2 = new Triangle2D(10, 10, 150, 100, 100, 100);

        FontWrapper fw = FontWrapper.create("monofur", 680F, Font.BOLD);
        Text tx = new Text("J", fw.toFont(), 100, 100);

        tx.translate(20, 400);

        Shape p3 = AffineTransform.getTranslateInstance(160, 20)
                .createTransformedShape(p2);

        PathText txt = new PathText(
                //                new CircleWrapper(300, 300, 200),
                //                new PathIteratorWrapper(p3),
                //                new PathIteratorWrapper(tx.toShape()),
                tx,
                //                                new PathIteratorWrapper(t2),
                new StringWrapper(
                        "Jack Jack Jack Jack Jack Jack Jack Jack Jack "
                        + "Jack Jack Jack Jack Jack Jack Jack Jack Jack "
                        + "Jack Jack Jack Jack Jack Jack",
                        //                        "This is the letter M.  This is the letter M."
                        //                                + "This is the letter M.  This is the letter M."
                        //                                + "This is the letter M.   This is the letter M. "
                        //                                + "This is the letter M.  This is the letter M."
                        //                                + "This is the letter M.",
                        //                        "Hello World, how about path text? "
                        //                        + "It looks like it could be kind of fun.  At least I "
                        //                        + "think it will be - let's scale it to be sure.  Well, "
                        //                        + "maybe it works.  "
                        //                        + "We'll see.",
                        0, 0), FontWrapper.create(
                        "monofur", 45F, Font.PLAIN));
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(new JScrollPane(new VComp(txt)));
        jf.pack();
        jf.setVisible(true);
    }

    static final class VComp extends JComponent {

        private final PathText txt;
        private AffineTransform scale = AffineTransform.getScaleInstance(0.5, 0.5);

        public VComp(PathText txt) {
            this.txt = txt;
            InputMap in = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = getActionMap();
            in.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "out");
            in.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "in");
            am.put("out", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double nue = scale.getScaleX() / 2D;
                    scale = AffineTransform.getScaleInstance(nue, nue);
                    invalidate();
                    revalidate();
                    repaint();
                }
            });
            am.put("in", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double nue = scale.getScaleX() * 2D;
                    scale = AffineTransform.getScaleInstance(nue, nue);
                    invalidate();
                    revalidate();
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Rectangle r = new Rectangle();
            r.setFrame(txt.toShape().getBounds2D());
            return scale.createTransformedShape(r).getBounds().getSize();
        }

        @Override
        public void paintComponent(Graphics g) {
            paintComponent((Graphics2D) g);
        }

        private void paintComponent(Graphics2D g) {
            GraphicsUtils.setHighQualityRenderingHints(g);
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.transform(scale);
            g.setColor(Color.WHITE);
            txt.paint(g);
            g.setColor(Color.BLACK);
            g.draw(txt.shape.toShape());
        }
    }

    @Override
    public double cumulativeLength() {
        return GeometryUtils.shapeLength(toShape());
    }

    /**
     * For multi-line text, set the multiplier applied to the font's leading -
     * usually a value between 0 and 2, to increase line spacing when &gt; 1 and
     * reduce it when &lt; 1.
     *
     * @param val The new multiplier
     */
    public void setLeadingMultiplier(float val) {
        this.leadingMultiplier = val;
        invalidateCachedShape();
    }

    /**
     * For multi-line text, get the fraction the leading of the font is
     * multiplied by to determine the interline spacing.
     *
     * @return
     */
    public float getLeadingMultiplier() {
        return leadingMultiplier;
    }

    public String getFontName() {
        return font.getName();
    }

    @Override
    public int getControlPointCount() {
        if (shape.is(Adjustable.class)) {
            return shape.as(Adjustable.class).getControlPointCount();
        }
        return 0;
    }

    @Override
    public void getControlPoints(double[] xy) {
        if (shape.is(Adjustable.class)) {
            shape.as(Adjustable.class).getControlPoints(xy);
            if (xform != null) {
                xform.transform(xy, 0, xy, 0, getControlPointCount());
            }
        }
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        if (shape.is(Adjustable.class)) {
            return shape.as(Adjustable.class).getVirtualControlPointIndices();
        }
        return new int[0];
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        if (shape.is(Adjustable.class)) {
            if (xform != null) {
                Point2D loc = location.toPoint2D();
                try {
                    xform.createInverse().transform(loc, loc);
                } catch (NoninvertibleTransformException ex) {
                    Exceptions.printStackTrace(ex);
                }
                location = new Pt(loc);
            }
            shape.as(Adjustable.class).setControlPointLocation(pointIndex, location);
            invalidateCachedShape();
        }
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        if (shape.is(Adjustable.class)) {
            return shape.as(Adjustable.class).getControlPointKinds();
        }
        return new ControlPointKind[0];
    }

    @Override
    public boolean hasReadOnlyControlPoints() {
        if (shape.is(Adjustable.class)) {
            return shape.as(Adjustable.class).hasReadOnlyControlPoints();
        }
        return true;
    }

    @Override
    public boolean isControlPointReadOnly(int index) {
        if (shape.is(Adjustable.class)) {
            return shape.as(Adjustable.class).isControlPointReadOnly(index);
        }
        return true;
    }
}
