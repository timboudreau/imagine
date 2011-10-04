package org.netbeans.paint.api.util;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Wrapper implementation of Graphics2D which can be placed in the Lookup
 * of something without actually creating the underlying graphics until
 * something calls a method on it.
 *
 * @author Tim Boudreau
 */
public class LazyGraphics extends TrackingGraphics {
    private final GraphicsProvider provider;

    private LazyGraphics(GraphicsProvider provider) {
        this.provider = provider;
    }

    public String toString() {
        return graphics().toString();
    }

    public void setXORMode(Color c1) {
        graphics().setXORMode(c1);
    }

    public void setPaintMode() {
        graphics().setPaintMode();
    }

    public void setFont(Font font) {
        graphics().setFont(font);
    }

    public void setColor(Color c) {
        graphics().setColor(c);
    }

    public void setClip(Shape clip) {
        graphics().setClip(clip);
    }

    public void setClip(int x, int y, int width, int height) {
        graphics().setClip(x, y, width, height);
    }

    public boolean hitClip(int x, int y, int width, int height) {
        return graphics().hitClip(x, y, width, height);
    }

    public FontMetrics getFontMetrics(Font f) {
        return graphics().getFontMetrics(f);
    }

    public FontMetrics getFontMetrics() {
        return graphics().getFontMetrics();
    }

    public Font getFont() {
        return graphics().getFont();
    }

    public Color getColor() {
        return graphics().getColor();
    }

    public Rectangle getClipRect() {
        return graphics().getClipRect();
    }

    public Rectangle getClipBounds(Rectangle r) {
        return graphics().getClipBounds(r);
    }

    public Rectangle getClipBounds() {
        return graphics().getClipBounds();
    }

    public Shape getClip() {
        return graphics().getClip();
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        graphics().fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fillRect(int x, int y, int width, int height) {
        graphics().fillRect(x, y, width, height);
    }

    public void fillPolygon(Polygon p) {
        graphics().fillPolygon(p);
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        graphics().fillPolygon(xPoints, yPoints, nPoints);
    }

    public void fillOval(int x, int y, int width, int height) {
        graphics().fillOval(x, y, width, height);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        graphics().fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        graphics().drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void drawRect(int x, int y, int width, int height) {
        graphics().drawRect(x, y, width, height);
    }

    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        graphics().drawPolyline(xPoints, yPoints, nPoints);
    }

    public void drawPolygon(Polygon p) {
        graphics().drawPolygon(p);
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        graphics().drawPolygon(xPoints, yPoints, nPoints);
    }

    public void drawOval(int x, int y, int width, int height) {
        graphics().drawOval(x, y, width, height);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        graphics().drawLine(x1, y1, x2, y2);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return graphics().drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return graphics().drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return graphics().drawImage(img, x, y, width, height, bgcolor, observer);
    }

    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return graphics().drawImage(img, x, y, bgcolor, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return graphics().drawImage(img, x, y, width, height, observer);
    }

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return graphics().drawImage(img, x, y, observer);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y) {
        graphics().drawChars(data, offset, length, x, y);
    }

    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        graphics().drawBytes(data, offset, length, x, y);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        graphics().drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void dispose() {
        Graphics2D g = graphics();
        provider.onDispose(g);
        g.dispose();
    }

    public Graphics create(int x, int y, int width, int height) {
        Graphics2D g = (Graphics2D) graphics().create(x, y, width, height);
        return new LazyGraphics(new FixedGraphicsProvider(g));
    }

    public Graphics create() {
        Graphics2D g = (Graphics2D) graphics().create();
        return new LazyGraphics(new FixedGraphicsProvider(g));
    }

    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        graphics().copyArea(x, y, width, height, dx, dy);
    }

    public void clipRect(int x, int y, int width, int height) {
        graphics().clipRect(x, y, width, height);
    }

    public void clearRect(int x, int y, int width, int height) {
        graphics().clearRect(x, y, width, height);
    }

    public void translate(double tx, double ty) {
        graphics().translate(tx, ty);
    }

    public void translate(int x, int y) {
        graphics().translate(x, y);
    }

    public void transform(AffineTransform Tx) {
        graphics().transform(Tx);
    }

    public void shear(double shx, double shy) {
        graphics().shear(shx, shy);
    }

    public void setTransform(AffineTransform Tx) {
        graphics().setTransform(Tx);
    }

    public void setStroke(Stroke s) {
        graphics().setStroke(s);
    }

    public void setRenderingHints(Map<?, ?> hints) {
        graphics().setRenderingHints(hints);
    }

    public void setRenderingHint(Key hintKey, Object hintValue) {
        graphics().setRenderingHint(hintKey, hintValue);
    }

    public void setPaint(Paint paint) {
        graphics().setPaint(paint);
    }

    public void setComposite(Composite comp) {
        graphics().setComposite(comp);
    }

    public void setBackground(Color color) {
        graphics().setBackground(color);
    }

    public void scale(double sx, double sy) {
        graphics().scale(sx, sy);
    }

    public void rotate(double theta, double x, double y) {
        graphics().rotate(theta, x, y);
    }

    public void rotate(double theta) {
        graphics().rotate(theta);
    }

    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return graphics().hit(rect, s, onStroke);
    }

    public AffineTransform getTransform() {
        return graphics().getTransform();
    }

    public Stroke getStroke() {
        return graphics().getStroke();
    }

    public RenderingHints getRenderingHints() {
        return graphics().getRenderingHints();
    }

    public Object getRenderingHint(Key hintKey) {
        return graphics().getRenderingHint(hintKey);
    }

    public Paint getPaint() {
        return graphics().getPaint();
    }

    public FontRenderContext getFontRenderContext() {
        return graphics().getFontRenderContext();
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        return graphics().getDeviceConfiguration();
    }

    public Composite getComposite() {
        return graphics().getComposite();
    }

    public Color getBackground() {
        return graphics().getBackground();
    }

    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        graphics().fill3DRect(x, y, width, height, raised);
    }

    public void fill(Shape s) {
        graphics().fill(s);
    }

    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        graphics().drawString(iterator, x, y);
    }

    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        graphics().drawString(iterator, x, y);
    }

    public void drawString(String str, float x, float y) {
        graphics().drawString(str, x, y);
    }

    public void drawString(String str, int x, int y) {
        graphics().drawString(str, x, y);
    }

    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        graphics().drawRenderedImage(img, xform);
    }

    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        graphics().drawRenderableImage(img, xform);
    }

    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        graphics().drawImage(img, op, x, y);
    }

    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return graphics().drawImage(img, xform, obs);
    }

    public void drawGlyphVector(GlyphVector g, float x, float y) {
        graphics().drawGlyphVector(g, x, y);
    }

    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        graphics().draw3DRect(x, y, width, height, raised);
    }

    public void draw(Shape s) {
        graphics().draw(s);
    }

    public void clip(Shape s) {
        graphics().clip(s);
    }

    public void addRenderingHints(Map<?, ?> hints) {
        graphics().addRenderingHints(hints);
    }

    private Graphics2D graphics() {
        return provider.getGraphics();
    }
    
    @Override
    public void areaModified(int x, int y, int w, int h) {
        Graphics2D g = graphics();
        if (g instanceof TrackingGraphics) {
            ((TrackingGraphics) g).areaModified(x, y, w, h);
        }
    }
    public interface GraphicsProvider {
        /**
         * Get a Graphics2D implementation which should be wrapped
         * @return 
         */
        public Graphics2D getGraphics();
        /**
         * Called before dispose() is called
         * @param graphics 
         */
        public void onDispose(Graphics2D graphics);
    }
    
    public static Graphics2D create(GraphicsProvider provider, boolean cacheGraphicsUntilDispose) {
        if (cacheGraphicsUntilDispose) {
            provider = new CachingGraphicsProvider(provider);
        }
        return new LazyGraphics(provider);
    }
    
    private static final class CachingGraphicsProvider implements GraphicsProvider {
        private final GraphicsProvider delegate;
        private Graphics2D graphics;

        public CachingGraphicsProvider(GraphicsProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public Graphics2D getGraphics() {
            if (graphics == null) {
                graphics = delegate.getGraphics();
            }
            return graphics;
        }
        
        public void onDispose(Graphics2D graphics) {
            delegate.onDispose(graphics);
            this.graphics = null;
        }
    }
    
    private static final class FixedGraphicsProvider implements GraphicsProvider {
        private final Graphics2D graphics;

        public FixedGraphicsProvider(Graphics2D graphics) {
            this.graphics = graphics;
        }

        @Override
        public Graphics2D getGraphics() {
            return graphics;
        }

        @Override
        public void onDispose(Graphics2D graphics) {
            //do nothing
        }
    }
}