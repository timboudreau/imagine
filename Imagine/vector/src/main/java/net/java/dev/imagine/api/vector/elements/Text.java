package net.java.dev.imagine.api.vector.elements;

import org.imagine.geometry.MinimalAggregateShapeFloat;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Textual;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.geometry.util.DoubleList;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.utils.java2d.GraphicsUtils;
import org.openide.util.Exceptions;
import net.java.dev.imagine.api.vector.Vectors;

/**
 * Wraps a string and the font used to render it so it can be turned into a
 * shape.
 *
 * @author Tim Boudreau
 */
public class Text implements Primitive, Vectors, Adjustable, Textual, Versioned {

    private transient static BufferedImage scratch;
    private StringWrapper text;
    private FontWrapper font;
    private AffineTransform xform;
    private transient double[] baselines;
    private float leadingMultiplier = 1;
    private int rev;

    public Text(StringWrapper string, FontWrapper font, AffineTransform xform) {
        this.text = string;
        this.font = font;
        this.xform = xform;
    }

    public Text(StringWrapper string, FontWrapper font) {
        this.text = string;
        this.font = font;
    }

    public Text(String text, Font font, double x, double y) {
        this.text = new StringWrapper(text, x, y);
        this.font = FontWrapper.create(font, true);
    }

    public int rev() {
        return rev + text.rev() + font.rev();
    }

    private void change() {
        rev++;
    }

    public AffineTransform transform() {
        return xform;
    }

    @Override
    public void translate(double x, double y) {
        if (xform != null) {
            double[] offsets = new double[]{x, y};
            try {
                AffineTransform xf = xform.createInverse();
                xf.deltaTransform(offsets, 0, offsets, 0, 1);
                x = offsets[0];
                y = offsets[1];
            } catch (NoninvertibleTransformException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        text.translate(x, y);
        invalidateCachedShape();
    }

    @Override
    public Runnable restorableSnapshot() {
        Runnable tr = text.restorableSnapshot();
        Runnable fr = font.restorableSnapshot();
        AffineTransform xf = xform == null ? null : new AffineTransform(xform);
        return () -> {
            xform = xf;
            tr.run();
            fr.run();
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
        change();
    }

    public void setText(StringWrapper text) {
        invalidateCachedShape();
        if (!this.text.equals(text)) {
            this.text = text;
            change();
        }
    }

    public void setFont(FontWrapper font) {
        invalidateCachedShape();
        if (!this.font.equals(font)) {
            this.font = font;
            change();
        }
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

    @Override
    public String getText() {
        return text.getText();
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    @Override
    public void setLocation(double x, double y) {
        text.setLocation(x, y);
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
            if (xform.getType() == AffineTransform.TYPE_TRANSLATION) {
                double[] xy = new double[]{0, 0};
                xform.transform(xy, 0, xy, 0, 1);
                text.translate(xy[0], xy[1]);
            } else {
                if (this.xform == null) {
                    this.xform = xform;
                    change();
                } else {
                    this.xform.concatenate(xform);
                    if (this.xform.isIdentity()) {
                        this.xform = null;
                    }
                    change();
                }
            }
            invalidateCachedShape();
        }
    }

    public Font getFont() {
        return font.toFont();
    }

    @Override
    public void paint(Graphics2D g) {
        font.paint(g);
        text.paint(g);
    }

    @Override
    public <T> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        if (type.isInstance(font)) {
            return type.cast(font);
        }
        if (type.isInstance(text)) {
            return type.cast(text);
        }
        T result = font.as(type);
        if (result == null) {
            result = text.as(type);
        }
        if (result == null) {
            result = Vectors.super.as(type);
        }
        return result;
    }

    @Override
    public Pt getLocation() {
        return new Pt(x(), y());
    }

    @Override
    public Text copy(AffineTransform transform) {
        StringWrapper txt = text.copy();
        FontWrapper fnt = font.copy();
        if (xform != null) {
            AffineTransform xf = new AffineTransform(xform);
            xf.preConcatenate(transform);
            return new Text(txt, fnt, xf);
        }
        Text result = new Text(txt, fnt, transform);
        result.rev = rev;
        return result;
    }

    @Override
    public Text copy() {
        StringWrapper txt = text.copy();
        FontWrapper fnt = font.copy();
        Text result = new Text(txt, fnt, xform == null || xform.isIdentity()
                ? null
                : new AffineTransform(xform));
        result.rev = rev;
        return result;
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
        // XXX in toShape, capture the baselines
        Rectangle2D r = toShape().getBounds2D();
        c.dimension(r.getHeight(), true, -1, -1);
        c.dimension(r.getWidth(), false, -1, -1);
    }

    public double[] getBaselines() {
        toShape();
        return baselines;
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
        hash = 29 * hash + Float.floatToIntBits(leadingMultiplier);
        hash = 29 * hash + GraphicsUtils.transformHashCode(xform);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Text)) {
            return false;
        }
        final Text other = (Text) obj;
        return other.leadingMultiplier == leadingMultiplier
                && Objects.equals(this.text, other.text)
                && Objects.equals(this.font, other.font)
                && GraphicsUtils.transformsEqual(this.xform, other.xform);
    }

    @Override
    public String toString() {
        return "Text{" + "text=" + text + ", font=" + font + ' '
                + GraphicsUtils.transformToString(xform) + '}';
    }

    private void invalidateCachedShape() {
        cachedShape = null;
        hashAtShapeCache = -1;
        baselines = null;
    }

    private Shape cachedShape;
    private int hashAtShapeCache = -1;

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

    double[] charPositions = null;

    private Shape makeShape() {
        BufferedImage img = scratchImage();
        Graphics2D g = img.createGraphics();
        try {
            Font f = getFont();
            FontRenderContext frc = g.getFontRenderContext();
            String txt = trimTail(getText());
            FontMetrics fm = g.getFontMetrics(f);
            DoubleList cps = new DoubleList(getText().length() * 2);
            if (txt.indexOf('\n') > 0) {
                String[] lines = txt.split("\\s*?\\n");
                DoubleList bs = new DoubleList(lines.length * 2);
                List<Shape> vectors = new ArrayList<>(lines.length);
                double x = x();
                double y = y();
                for (int i = 0; i < lines.length; i++) {
                    LineMetrics lm = fm.getLineMetrics(lines[i], g);
                    double interlineGap = lm.getLeading() * leadingMultiplier;

                    bs.add(x);
                    bs.add(y);

                    GlyphVector gv = f.createGlyphVector(frc, lines[i]);

                    int glyphs = gv.getNumGlyphs();
                    float[] glyphPositions = gv.getGlyphPositions(0, glyphs, null);
                    for (int j = 0; j < glyphPositions.length; j += 2) {
                        cps.add(glyphPositions[j] + x);
                        cps.add(y);
                    }

                    Shape shape = gv.getOutline((float) x, (float) y);
                    y += interlineGap + lm.getHeight();
                    vectors.add(shape);
                }
                baselines = bs.toDoubleArray();
                charPositions = cps.toDoubleArray();
                if (xform != null && baselines.length > 0) {
                    xform.transform(baselines, 0, baselines, 0, baselines.length / 2);
                    xform.transform(charPositions, 0, charPositions, 0, charPositions.length / 2);
                }
                return new MinimalAggregateShapeFloat(xform,
                        vectors.toArray(new Shape[vectors.size()]));
            }
            GlyphVector gv = f.createGlyphVector(frc, txt);
            double x = x();
            double y = y();
            cps.add(x);
            cps.add(y);
            int glyphs = gv.getNumGlyphs();
            float[] glyphPositions = gv.getGlyphPositions(0, glyphs, null);
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
            return new MinimalAggregateShapeFloat(xform, new Shape[]{gv.getOutline((float) x(), (float) y())});
        } finally {
            g.dispose();
        }
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

    /**
     * Trims trailing but not leading whitespace from a shape, and converts tabs
     * to spaces (4).
     *
     * @param s Some text
     * @return The revised text
     */
    static String trimTail(String s) {
        int ix = s.lastIndexOf('\n');
        if (ix < 0) {
            return s;
        }
        int last = s.length() - 1;
        for (int i = last; i >= ix - 1; i--) {
            if (Character.isWhitespace(s.charAt(i))) {
                last--;
            } else {
                break;
            }
        }
        if (last == s.length() - 1) {
            return s;
        }
        if (s.indexOf('\t') >= 0) {
            s = s.replace("\\t", "    ");
        }
        return s.substring(0, last + 1);
    }

    @Override
    public double cumulativeLength() {
        return GeometryUtils.shapeLength(toShape());
    }

    /**
     * A cached image we use the font render context from for generating glyph
     * vectors. Hmm, may be sensitive to the graphics device having a transform.
     *
     * @return
     */
    static BufferedImage scratchImage() {
        if (scratch == null) {
            scratch = GraphicsUtils.newBufferedImage(1, 1);
        }
        return scratch;
    }

    public String getFontName() {
        return font.getName();
    }

    private void ensureShape() {
        if (cachedShape == null) {
            toShape();
        }
    }

    @Override
    public int getControlPointCount() {
        ensureShape();
        return baselines == null ? 0 : (baselines.length + charPositions.length) / 2;
    }

    @Override
    public void getControlPoints(double[] xy) {
        ensureShape();
        if (baselines == null) {
            Arrays.fill(xy, 0);
            return;
        }
        System.arraycopy(baselines, 0, xy, 0, Math.min(xy.length, baselines.length));
        System.arraycopy(charPositions, 0, xy, baselines.length,
                Math.min(xy.length, charPositions.length));

    }

    @Override
    public int[] getVirtualControlPointIndices() {
        ensureShape();
        if (baselines == null) {
            return new int[0];
        }
        int[] result = new int[(baselines.length + charPositions.length) / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        // do nothing
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ensureShape();
        if (baselines == null) {
            return new ControlPointKind[0];
        }
        int count = baselines == null ? 0 : (baselines.length + charPositions.length) / 2;
        ControlPointKind[] result = new ControlPointKind[count];
        Arrays.fill(result, ControlPointKind.CHARACTER_POSITION);
        Arrays.fill(result, 0, baselines.length / 2, ControlPointKind.TEXT_BASELINE);
//        Arrays.fill(result, baselines.length / 2, charPositions.length / 2, ControlPointKind.CHARACTER_POSITION);
        return result;
    }

    @Override
    public boolean hasReadOnlyControlPoints() {
        return true;
    }

    @Override
    public boolean isControlPointReadOnly(int index) {
        return true;
    }
}
