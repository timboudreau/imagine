/*
 * DummyGraphics.java
 *
 * Created on October 25, 2006, 10:22 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.imagine.utils.java2d;

import java.awt.AlphaComposite;
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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import static java.awt.Transparency.TRANSLUCENT;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

/**
 * A graphics which paints to nothing, but retains properties
 * set on it.
 *
 * @author Tim Boudreau
 */
final class DummyGraphics extends Graphics2D {

    private static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

    @SuppressWarnings(value = "unchecked")
    private final RenderingHints hints = new RenderingHints(new HashMap());
    private Composite composite;
    AffineTransform xform = AffineTransform.getTranslateInstance(0d, 0d);
    Color background;
    private Shape clip;
    private Stroke stroke;
    private Paint paint;
    private Font font = DEFAULT_FONT;
    Color xorColor = null;
    private final boolean offscreen;
    // Temporary fields for things we can't synthesize which are
    // usually never requested
    private FontRenderContext ctx;
    private BufferedImage dummyImage;
    private Graphics2D dummyImageGraphics;

    DummyGraphics(boolean offscreen) {
        this.offscreen = offscreen;
    }

    private void populateFontInfo() {
        if (ctx == null) {
            if (offscreen) {
                dummyImage = new BufferedImage(1, 1, GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
            } else {
                dummyImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(1, 1, TRANSLUCENT);
            }
            dummyImageGraphics = dummyImage.createGraphics();
            ctx = dummyImageGraphics.getFontRenderContext();
        }
    }


    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        populateFontInfo();
        return dummyImageGraphics.getDeviceConfiguration();
    }

    @Override
    public void draw(Shape s) {
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return true;
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
    }

    @Override
    public void drawString(String str, int x, int y) {
    }

    @Override
    public void drawString(String str, float x, float y) {
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
    }

    @Override
    public void fill(Shape s) {
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return s.intersects(rect);
    }

    @Override
    public void setComposite(Composite comp) {
        composite = comp;
    }

    @Override
    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    @Override
    public void setStroke(Stroke s) {
        this.stroke = s;
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        if (hintValue == null || hintKey == null) {
            return;
        }
        hints.put(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return hints.get(hintKey);
    }

    @Override
    public void setRenderingHints(Map hints) {
        hints.clear();
        addRenderingHints(hints);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addRenderingHints(Map hints) {
        hints.putAll(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return (RenderingHints) hints.clone();
    }

    @Override
    public void translate(int x, int y) {
        transform(AffineTransform.getTranslateInstance(x, y));
    }

    @Override
    public void translate(double tx, double ty) {
        transform(AffineTransform.getTranslateInstance(tx, ty));
    }

    @Override
    public void rotate(double theta) {
        transform(AffineTransform.getRotateInstance(theta));
    }

    @Override
    public void rotate(double theta, double x, double y) {
        transform(AffineTransform.getRotateInstance(theta, x, y));
    }

    @Override
    public void scale(double sx, double sy) {
        transform(AffineTransform.getScaleInstance(sx, sy));
    }

    @Override
    public void shear(double shx, double shy) {
        transform(AffineTransform.getShearInstance(shx, shy));
    }

    @Override
    public void transform(AffineTransform tx) {
        xform.concatenate(tx);
    }

    @Override
    public void setTransform(AffineTransform tx) {
        if (tx == null) {
            tx = AffineTransform.getTranslateInstance(0, 0);
        }
        xform = tx;
    }

    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(xform);
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public Composite getComposite() {
        return composite;
    }

    @Override
    public void setBackground(Color color) {
        background = color;
    }

    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    @Override
    public void clip(Shape s) {
        clip = s;
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return ctx;
    }

    @Override
    public Graphics create() {
        return new DummyGraphics(offscreen);
    }

    @Override
    public Color getColor() {
        return paint instanceof Color ? (Color) paint : null;
    }

    @Override
    public void setColor(Color c) {
        paint = c;
    }

    @Override
    public void setPaintMode() {
        if (composite == AlphaComposite.Xor) {
            composite = null;
        }
        xorColor = null;
    }

    @Override
    public void setXORMode(Color c1) {
        composite = AlphaComposite.Xor;
        xorColor = c1;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void setFont(Font font) {
        if (font == null) {
            font = DEFAULT_FONT;
        } else {
            this.font = font;
        }
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        populateFontInfo();
        return dummyImageGraphics.getFontMetrics(f);
    }

    @Override
    public Rectangle getClipBounds() {
        return clip == null ? null : clip.getBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        clip = new Rectangle(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        clip = new Rectangle(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        return clip;
    }

    @Override
    public void setClip(Shape clip) {
        this.clip = clip;
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return true;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return true;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return true;
    }

    @Override
    public void dispose() {
        if (ctx != null) {
            dummyImageGraphics.dispose();
            dummyImage.flush();
            dummyImage = null;
            dummyImageGraphics = null;
            ctx = null;
        }
    }

    @Override
    public Graphics create(int x, int y, int width, int height) {
        return new DummyGraphics(offscreen);
    }

    @Override
    public FontMetrics getFontMetrics() {
        return getFontMetrics(font);
    }

    @Override
    public boolean hitClip(int x, int y, int width, int height) {
        return clip == null ? false : clip.intersects(
                x, y, width, height);
    }

    @Override
    public Rectangle getClipBounds(Rectangle r) {
        if (clip != null) {
            r.setBounds(getClipBounds());
        }
        return r;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DummyGraphics;
    }

    @Override
    public String toString() {
        return "Dummy Graphics"; //NOI18N
    }
}
