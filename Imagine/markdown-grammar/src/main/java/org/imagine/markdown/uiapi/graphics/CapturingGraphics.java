package org.imagine.markdown.uiapi.graphics;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tim Boudreau
 */
public class CapturingGraphics extends Graphics2D {

    private final List<PaintItem> capture;
    private final Graphics2D delegate;
    private static Graphics2D scratchGraphics;
    private final boolean isChild;
    private StringDetailsSupplier stringDetails;

    public CapturingGraphics(Graphics2D delegate, List<PaintItem> sharedCapture, StringDetailsSupplier stringDetails) {
        capture = sharedCapture == null ? new ArrayList<>(256) : sharedCapture;
        isChild = sharedCapture != null;
        this.delegate = delegate == null ? scratchGraphics() : delegate;
        this.stringDetails = stringDetails;
    }

    public CapturingGraphics() {
        this(null, null);
    }

    public CapturingGraphics(StringDetailsSupplier stringDetails) {
        this(null, stringDetails);
    }

    public CapturingGraphics(Graphics2D delegate, StringDetailsSupplier stringDetails) {
        this.delegate = delegate == null ? scratchGraphics() : delegate;
        capture = new ArrayList<>();
        this.stringDetails = stringDetails;
        isChild = false;
    }

    public MarkdownDetailsModel toDetailsModel(Rectangle2D.Float renderedBounds) {
        if (stringDetails == null) {
            throw new IllegalStateException("Not created to produce string details");
        }
        return new MarkdownDetailsModel(capture, textItemPositions, renderedBounds);
    }

    public MarkdownRenderingModel toModel(Rectangle2D.Float renderedBounds) {
        return new MarkdownRenderingModel(capture, renderedBounds);
    }

    private static Graphics2D scratchGraphics() {
        if (scratchGraphics == null) {
            scratchGraphics = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(1, 1, Transparency.TRANSLUCENT).createGraphics();
        }
        return scratchGraphics;
    }

    @Override
    public void dispose() {
        if (isChild) {
            delegate.dispose();
        }
    }

    @Override
    public void setComposite(Composite comp) {
        delegate.setComposite(comp);
        push(g -> g.setComposite(comp));
    }

    @Override
    public void setPaint(Paint paint) {
        delegate.setPaint(paint);
        push(g -> g.setPaint(paint));
    }

    @Override
    public void setStroke(Stroke s) {
        delegate.setStroke(s);
        push(g -> g.setStroke(s));
    }

    @Override
    public void translate(int x, int y) {
        delegate.translate(x, y);
        push(g -> g.translate(x, y));
    }

    @Override
    public void translate(double tx, double ty) {
        delegate.translate(tx, ty);
        push(g -> g.translate(tx, ty));
    }

    @Override
    public void rotate(double theta) {
        delegate.rotate(theta);
        push(g -> g.rotate(theta));
    }

    @Override
    public void rotate(double theta, double x, double y) {
        delegate.rotate(theta, x, y);
        push(g -> g.rotate(theta, x, y));
    }

    @Override
    public void scale(double sx, double sy) {
        delegate.scale(sx, sy);
        push(g -> g.scale(sx, sy));
    }

    @Override
    public void shear(double shx, double shy) {
        delegate.shear(shx, shy);
        push(g -> g.shear(shx, shy));
    }

    @Override
    public void transform(AffineTransform Tx) {
        delegate.transform(Tx);
        AffineTransform copy = new AffineTransform(Tx);
        push(g -> g.transform(copy));
    }

    @Override
    public void setTransform(AffineTransform Tx) {
        delegate.setTransform(Tx);
        AffineTransform copy = new AffineTransform(Tx);
        push(g -> g.setTransform(copy));
    }

    @Override
    public void setBackground(Color color) {
        delegate.setBackground(color);
        push(g -> g.setBackground(color));
    }

    @Override
    public void clip(Shape s) {
        setClip(s);
    }

    @Override
    public void drawString(String str, int x, int y) {
        drawString(str, x, y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        if (str.length() == 0) {
            return;
        }
        delegate.drawString(str, x, y);
        Consumer<Graphics2D> c = g -> g.drawString(str, x, y);
        FontMetrics fm = delegate.getFontMetrics();
        int width = fm.stringWidth(str);
        Rectangle2D.Float bounds = new Rectangle2D.Float(x, y - fm.getAscent(), width, fm.getHeight() + fm.getDescent());
        if (stringDetails == null) {
            SimpleTextItem simp = new SimpleTextItem(str, bounds, c);
            push(simp);
        } else {
            int[] details = stringDetails.currentDetails(str);
            assert details.length == 3;
            FontRenderContext frc = delegate.getFontRenderContext();
            Font font = delegate.getFont();
            GlyphVector vect = font.createGlyphVector(frc, str);
            float[] xyPositions = vect.getGlyphPositions(0, vect.getNumGlyphs(), null);
            float[] xPositions = new float[xyPositions.length / 2];
            float cursor = x;
            for (int i = 0; i < xyPositions.length; i += 2) {
                float xAbs = xyPositions[i];
//                System.out.println("xabs " + i + " is " + xAbs + " for " + (i < str.length() ? str.charAt(i/2): "end"));
                xPositions[i / 2] = xAbs;
            }
            TextDetailsItem item = new TextDetailsItem(str, details, bounds, c, xPositions, fm.getHeight());
            push(item);
        }
    }

    static class TextDetailsItem implements PaintItem, TextItem, TextDetails {

        private final int documentOffset;
        private final int line;
        private final int charPositionInLine;
        final Rectangle2D.Float bounds;
        final Consumer<Graphics2D> painter;
        final String text;
        private final float[] charOffsets;
        private final float baseline;

        TextDetailsItem(String text, int[] details, Rectangle2D.Float bounds, Consumer<Graphics2D> painter, float[] charOffsets, float baseline) {
            assert text.length() - 1 == charOffsets.length;
            documentOffset = details[0];
            line = details[1];
            charPositionInLine = details[2];
            this.bounds = bounds;
            this.painter = painter;
            this.text = text;
            this.charOffsets = charOffsets;
            this.baseline = baseline;
        }

        public String toString() {
            return "'" + text + "'@ " + bounds.x + ", " + bounds.y + ", " + bounds.width + " x " + bounds.height;
        }

        @Override
        public int charOffsetAt(float x, float y) {
            return charOffset(x, y);
        }

        public char charAt(int charPosition) {
            return text.charAt(charPosition);
        }

        @Override
        public float charXStart(int charOffset) {
            return bounds.x + charOffsets[charOffset];
        }

        @Override
        public float charXEnd(int charOffset) {
            if (charOffset == charOffsets.length - 1) {
                return bounds.x + bounds.width;
            }
            return bounds.x + charOffsets[charOffset + 1];
        }

        public int charOffset(float x, float y) {
            if (bounds.contains(x, y)) {
                x -= bounds.x;
                for (int i = 0; i < charOffsets.length; i++) {
                    float next = i == charOffsets.length -1 ? bounds.width : charOffsets[i+1];
                    if (x >= charOffsets[i] && x < next) {
                        return i;
                    }
                }
            }
            return -1;
        }

        public void characterBounds(int character, Rectangle2D.Float into) {
            into.x = bounds.x + charOffsets[character];
            into.y = bounds.y;
            into.width = character < charOffsets.length - 2 ? charOffsets[character + 1] - charOffsets[character]
                    : bounds.x + bounds.width - into.x;
            into.height = bounds.height;
        }

        @Override
        public boolean contains(float x, float y) {
            return bounds.contains(x, y);
        }

        public int documentOffset(float x, float y) {
            int choff = charOffset(x, y);
            return choff < 0 ? -1 : documentOffset(choff);
        }

        public int charPositionInLine(float x, float y) {
            int choff = charOffset(x, y);
            return choff < 0 ? -1 : charPositionInLine(choff);
        }

        public int documentOffset(int charOffset) {
            return documentOffset + charOffset;
        }

        public int charPositionInLine(int charOffset) {
            return charPositionInLine + charOffset;
        }

        public int documentOffset() {
            return documentOffset;
        }

        public int line() {
            return line;
        }

        public int charPositionInLine() {
            return charPositionInLine;
        }

        public String text() {
            return text;
        }

        @Override
        public void fetchBounds(Rectangle2D into) {
            into.setFrame(into);
        }

        @Override
        public void paint(Graphics2D g) {
            painter.accept(g);
        }
    }

    static class SimpleTextItem implements PaintItem, TextItem {

        final String text;
        final Rectangle2D.Float bounds;
        final Consumer<Graphics2D> painter;

        public SimpleTextItem(String text, Rectangle2D.Float bounds, Consumer<Graphics2D> painter) {
            this.text = text;
            this.bounds = bounds;
            this.painter = painter;
        }

        public String text() {
            return text;
        }

        @Override
        public void fetchBounds(Rectangle2D into) {
            into.setFrame(bounds);
        }

        @Override
        public void paint(Graphics2D g) {
            painter.accept(g);
        }

        @Override
        public boolean contains(float x, float y) {
            return bounds.contains(x, y);
        }
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        delegate.drawString(iterator, x, y);
        AttributedCharacterIterator copy = (AttributedCharacterIterator) iterator.clone();
        push(g -> g.drawString(copy, x, y));
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        delegate.drawString(iterator, x, y);
        push(gg -> gg.drawString(iterator, x, y));
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        delegate.drawGlyphVector(g, x, y);
        push(gg -> gg.drawGlyphVector(g, x, y));
    }

    @Override
    public void fill(Shape s) {
        delegate.fill(s);
        push(g -> g.draw(s));
    }

    @Override
    public void draw(Shape s) {
        delegate.draw(s);
        push(g -> g.draw(s));
    }

    @Override
    public Shape getClip() {
        return delegate.getClip();
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        delegate.copyArea(x, y, width, height, dx, dy);
        push(g -> g.copyArea(x, y, width, height, dx, dy));
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        delegate.drawLine(x1, y1, x2, y2);
        Rectangle r = new Rectangle();
        r.setFrameFromDiagonal(x1, y1, x2, y2);
        push(r, g -> g.draw(new Line2D.Float(x1, y1, x2, y2)));
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        delegate.fillRect(x, y, width, height);
        Rectangle r = new Rectangle(x, y, width, height);
        push(r, g -> g.fill(r));
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        Rectangle r = new Rectangle(x, y, width, height);
        delegate.clearRect(x, y, width, height);
        push(r, g -> g.clearRect(x, y, width, height));
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        RoundRectangle2D.Float rect = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
        push(rect.getBounds2D(), g -> g.draw(rect));
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        RoundRectangle2D.Float rect = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
        push(rect.getBounds2D(), g -> g.fill(rect));
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        delegate.drawOval(x, y, width, height);
        push(x, y, width, height, g -> g.drawOval(x, y, width, height));
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        delegate.fillOval(x, y, width, height);
        push(x, y, width, height, g -> g.fillOval(x, y, width, height));
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        delegate.drawArc(x, y, width, height, startAngle, arcAngle);
        push(x, y, width, height, g -> g.drawArc(x, y, width, height, startAngle, arcAngle));
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        delegate.fillArc(x, y, width, height, startAngle, arcAngle);
        push(x, y, width, height, g -> g.fillArc(x, y, width, height, startAngle, arcAngle));
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        int[] xPointsC = (int[]) xPoints.clone();
        int[] yPointsC = (int[]) yPoints.clone();
        delegate.drawPolyline(xPointsC, yPointsC, nPoints);
        push(g -> g.drawPolyline(xPointsC, yPointsC, nPoints));
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        int[] xPointsC = (int[]) xPoints.clone();
        int[] yPointsC = (int[]) yPoints.clone();
        delegate.drawPolygon(xPointsC, yPointsC, nPoints);
        push(g -> g.drawPolygon(xPointsC, yPointsC, nPoints));
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        int[] xPointsC = (int[]) xPoints.clone();
        int[] yPointsC = (int[]) yPoints.clone();
        delegate.fillPolygon(xPointsC, yPointsC, nPoints);
        push(g -> g.fillPolygon(xPointsC, yPointsC, nPoints));
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        boolean result = delegate.drawImage(img, x, y, observer);
        push(g -> g.drawImage(img, x, y, null));
        return result;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        boolean result = delegate.drawImage(img, x, y, width, height, observer);
        push(g -> g.drawImage(img, x, y, width, height, null));
        return result;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        boolean result = delegate.drawImage(img, x, y, bgcolor, observer);
        push(g -> g.drawImage(img, x, y, bgcolor, null));
        return result;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        boolean result = delegate.drawImage(img, x, y, width, height, bgcolor, observer);
        push(g -> g.drawImage(img, x, y, width, height, bgcolor, null));
        return result;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        boolean result = delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
        push(g -> g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null));
        return result;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        boolean result = delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
        push(g -> g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, null));
        return result;
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        delegate.drawRenderedImage(img, xform);
        push(g -> g.drawRenderedImage(img, xform));
    }

    // Stuff unlikely to be used from here
    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        boolean result = delegate.drawImage(img, xform, obs);
        push(g -> g.drawImage(img, xform, obs));
        return result;
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        delegate.drawImage(img, op, x, y);
        push(g -> g.drawImage(img, op, x, y));
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        delegate.drawRenderableImage(img, xform);
        push(g -> g.drawRenderableImage(img, xform));
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return delegate.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return delegate.getDeviceConfiguration();
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        delegate.setRenderingHint(hintKey, hintValue);
        push(g -> g.setRenderingHint(hintKey, hintValue));
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return delegate.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        delegate.setRenderingHints(hints);
        push(g -> g.setRenderingHints(hints));
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        delegate.addRenderingHints(hints);
        push(g -> g.addRenderingHints(hints));
    }

    @Override
    public RenderingHints getRenderingHints() {
        return delegate.getRenderingHints();
    }

    @Override
    public AffineTransform getTransform() {
        return delegate.getTransform();
    }

    @Override
    public Paint getPaint() {
        return delegate.getPaint();
    }

    @Override
    public Composite getComposite() {
        return delegate.getComposite();
    }

    @Override
    public Color getBackground() {
        return delegate.getBackground();
    }

    @Override
    public Stroke getStroke() {
        return delegate.getStroke();
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return delegate.getFontRenderContext();
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        delegate.draw3DRect(x, y, width, height, raised);
        push(x, y, width, height, g -> g.draw3DRect(x, y, width, height, raised));
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        delegate.fill3DRect(x, y, width, height, raised);
        push(x, y, width, height, g -> g.fill3DRect(x, y, width, height, raised));
    }

    @Override
    public Graphics create() {
        Graphics2D result = (Graphics2D) delegate.create();
        return new CapturingGraphics(result, capture, stringDetails);
    }

    @Override
    public Color getColor() {
        return delegate.getColor();
    }

    @Override
    public void setColor(Color c) {
        delegate.setColor(c);
        push(g -> g.setColor(c));
    }

    @Override
    public void setPaintMode() {
        delegate.setPaintMode();
        push(g -> g.setPaintMode());
    }

    @Override
    public void setXORMode(Color c1) {
        delegate.setXORMode(c1);
        push(g -> g.setXORMode(c1));
    }

    @Override
    public Font getFont() {
        return delegate.getFont();
    }

    @Override
    public void setFont(Font font) {
        delegate.setFont(font);
        push(g -> g.setFont(font));
    }

    @Override
    public FontMetrics getFontMetrics() {
        return delegate.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return delegate.getFontMetrics(f);
    }

    @Override
    public Rectangle getClipBounds() {
        return delegate.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        setClip(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        setClip(new Rectangle(x, y, width, height));
    }

    @Override
    public void setClip(Shape clip) {
        delegate.setClip(clip);
        if (clip == null) {
            push(g -> g.setClip(null));
        } else {
            push(withCopy(clip, copy -> {
                return g -> g.setClip(copy);
            }));
        }
    }

    private <T> T withCopy(Shape shape, Function<Shape, T> c) {
        return c.apply(copy(shape));
    }

    private Shape copy(Shape orig) {
        if (orig instanceof RectangularShape) {
            return (Shape) ((RectangularShape) orig).clone();
        } else if (orig instanceof Path2D) {
            return (Shape) ((Path2D) orig).clone();
        } else if (orig instanceof Line2D) {
            return (Shape) ((Line2D) orig).clone();
        } else if (orig instanceof Polygon) {
            Polygon o = (Polygon) orig;
            return new Polygon((int[]) o.xpoints.clone(), (int[]) o.ypoints.clone(), o.npoints);
        } else if (orig instanceof Serializable) {
            Serializable ser = (Serializable) orig;
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                try (ObjectOutputStream oout = new ObjectOutputStream(out)) {
                    oout.writeObject(ser);
                }
                try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
                    try (ObjectInputStream oin = new ObjectInputStream(in)) {
                        return (Shape) oin.readObject();
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(CapturingGraphics.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        throw new IllegalArgumentException("Not serializable or cloneable: " + orig);
    }

    @Override
    public Rectangle getClipRect() {
        return delegate.getClipRect();
    }

    @Override
    public Rectangle getClipBounds(Rectangle r) {
        return delegate.getClipBounds(r);
    }

    private BitSet textItemPositions = new BitSet(128);

    private void push(PaintItem item) {
        if (item instanceof TextItem) {
            textItemPositions.set(capture.size());
        }
        capture.add(item);
    }

    private void push(double x, double y, double w, double h, Consumer<Graphics2D> painter) {
        push(new Rectangle2D.Double(x, y, w, h), painter);
    }

    private void push(Rectangle2D bds, Consumer<Graphics2D> painter) {
        push(new GenericPaintItem(painter, bds));
    }
}
