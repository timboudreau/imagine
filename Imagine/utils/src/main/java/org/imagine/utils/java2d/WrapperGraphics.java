/*
 * WrapperGraphics.java
 *
 * Created on October 15, 2005, 8:18 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.imagine.utils.java2d;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import org.imagine.utils.painting.RepaintHandle;

/**
 * A Graphics2D which wrappers another Graphics2D and pushes repaint requests
 * for the modified area out to the component, whenever a drawing operation is
 * performed.
 *
 * @author Timothy Boudreau
 */
public class WrapperGraphics extends TrackingGraphics {

    private final Graphics2D other;
    private final RepaintHandle handle;
    private final Point location;
    private final int w;
    private final int h;

    public WrapperGraphics(RepaintHandle handle, Point location, int w, int h) {
        this(handle, GraphicsUtils.noOpGraphics(true), location, w, h);
    }

    public WrapperGraphics(RepaintHandle handle, Graphics2D other, Point location, int w, int h) {
        this.w = w;
        this.h = h;
        this.other = (Graphics2D) other.create(0, 0, w, h); //so we can translate safely
        this.handle = handle;
        this.location = location;
        location.x = Math.min(0, location.x);
        location.y = Math.min(0, location.y);
        this.other.translate(-location.x, -location.y);
    }

    @Override
    public void draw(Shape s) {
        other.draw(s);
        Rectangle r = s.getBounds();
        changed(r);
    }

    private void changed(int[] xPoints, int[] yPoints, int nPoints) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < nPoints; i++) {
            minX = Math.min(minX, xPoints[i]);
            minY = Math.min(minY, yPoints[i]);
            maxX = Math.max(maxX, xPoints[i]);
            maxY = Math.max(maxY, yPoints[i]);
        }
        changed(minX, minY, maxX - minX, maxY - minY);
    }

    private void changed(int x, int y, int w, int h) {
        //System.err.println("CHANGED " + handle+" "+ x  + "," + y  + "," + w  + "," + h);
        handle.repaintArea(x, y, w, h);
    }

    private void changed() {
        changed(-1, -1, -1, -1);
    }

    private void changed(Rectangle r) {
        changed(r.x, r.y, r.width, r.height);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        boolean result = other.drawImage(img, xform, obs);
        if (result) {
            changed();
        }
        return result;
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        other.drawImage(img, op, x, y);
        changed(x, y, img.getWidth(), img.getHeight());
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        other.drawRenderedImage(img, xform);
        Point scratch = new Point(0, 0);
        Point topLeft = new Point();
        xform.transform(scratch, topLeft);
        Point bottomRight = new Point();
        scratch.x = img.getWidth();
        scratch.y = img.getHeight();
        xform.transform(scratch, bottomRight);
        changed(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        other.drawRenderableImage(img, xform);
        Point scratch = new Point(0, 0);
        Point topLeft = new Point();
        xform.transform(scratch, topLeft);
        Point bottomRight = new Point();
        scratch.x = (int) Math.ceil(img.getWidth());
        scratch.y = (int) Math.ceil(img.getHeight());
        xform.transform(scratch, bottomRight);
        changed(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
    }

    @Override
    public void drawString(String str, int x, int y) {
        other.drawString(str, x, y);
        FontMetrics fm = getFontMetrics();
        Rectangle2D r = fm.getStringBounds(str, other);
        changed(x, y, (int) Math.round(r.getWidth()), (int) Math.round(r.getHeight()));
    }

    @Override
    public void drawString(String s, float x, float y) {
        other.drawString(s, x, y);
        FontMetrics fm = getFontMetrics();
        Rectangle2D r = fm.getStringBounds(s, other);
        changed((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.ceil(r.getWidth()), (int) Math.ceil(r.getHeight()));
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        other.drawString(iterator, x, y);
        FontMetrics fm = getFontMetrics();
        Rectangle2D r = fm.getStringBounds(iterator, 0, iterator.last(), other);
        changed(x, y, (int) Math.ceil(r.getWidth()), (int) Math.ceil(r.getHeight()));
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        other.drawString(iterator, x, y);
        FontMetrics fm = getFontMetrics();
        Rectangle2D r = fm.getStringBounds(iterator, 0, iterator.last(), other);
        changed((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(r.getWidth()), (int) Math.ceil(r.getHeight()));
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        other.drawGlyphVector(g, x, y);
        Rectangle2D r = g.getLogicalBounds();
        r.setFrame(r.getX() + x, r.getY() + y, w, h);
        AffineTransform xform = getTransform();
        if (xform != null && !xform.isIdentity()) {
            Point2D.Double topLeft = new Point2D.Double(r.getX(), r.getY());
            Point2D.Double bottomRight = new Point2D.Double(r.getX() + r.getWidth(), r.getY() + r.getHeight());
            xform.transform(topLeft, topLeft);
            xform.transform(bottomRight, bottomRight);
            r.setFrame(topLeft.getX(), topLeft.getY(),
                    bottomRight.getX() - topLeft.getX(),
                    bottomRight.getY() - topLeft.getY());
        }
        changed(r.getBounds());
    }

    @Override
    public void fill(Shape s) {
        other.fill(s);
        changed(s.getBounds());
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return other.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return other.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp) {
        other.setComposite(comp);
    }

    @Override
    public void setPaint(Paint paint) {
        other.setPaint(paint);
    }

    @Override
    public void setStroke(Stroke s) {
        other.setStroke(s);
    }

    @Override
    public void setRenderingHint(Key hintKey, Object hintValue) {
        other.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(Key hintKey) {
        return other.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map hints) {
        other.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map hints) {
        other.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return other.getRenderingHints();
    }

    @Override
    public void translate(int x, int y) {
        other.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty) {
        other.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        other.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        other.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        other.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        other.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform tx) {
        other.transform(tx);
    }

    @Override
    public void setTransform(AffineTransform tx) {
        other.setTransform(tx);
    }

    @Override
    public AffineTransform getTransform() {
        return other.getTransform();
    }

    @Override
    public Paint getPaint() {
        return other.getPaint();
    }

    @Override
    public Composite getComposite() {
        return other.getComposite();
    }

    @Override
    public void setBackground(Color color) {
        other.setBackground(color);
    }

    @Override
    public Color getBackground() {
        return other.getBackground();
    }

    @Override
    public Stroke getStroke() {
        return other.getStroke();
    }

    @Override
    public void clip(Shape s) {
        other.clip(s);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return other.getFontRenderContext();
    }

    @Override
    public Graphics create() {
        return new WrapperGraphics(handle, (Graphics2D) other.create(),
                new Point(location), w, h);
    }

    @Override
    public Color getColor() {
        return other.getColor();
    }

    @Override
    public void setColor(Color c) {
        other.setColor(c);
    }

    @Override
    public void setPaintMode() {
        other.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        other.setXORMode(c1);
    }

    @Override
    public Font getFont() {
        return other.getFont();
    }

    @Override
    public void setFont(Font font) {
        other.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return other.getFontMetrics();
    }

    @Override
    public Rectangle getClipBounds() {
        return other.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        other.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        other.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        return other.getClip();
    }

    @Override
    public void setClip(Shape clip) {
        other.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        other.copyArea(x, y, width, height, dx, dy);
    }

    private final Rectangle scratchRect = new Rectangle();
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        other.drawLine(x1, y1, x2, y2);
        scratchRect.setFrameFromDiagonal(x1, y1, x2, y2);
        changed(scratchRect);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        other.fillRect(x, y, width, height);
        changed(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        other.clearRect(x, y, width, height);
        changed(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        other.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        changed(x, y, width, height);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        other.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        changed(x, y, width, height);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        other.drawOval(x, y, width, height);
        changed(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        other.fillOval(x, y, width, height);
        changed(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        other.drawArc(x, y, width, height, startAngle, arcAngle);
        changed(x, y, width, height);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        other.fillArc(x, y, width, height, startAngle, arcAngle);
        changed(x, y, width, height);
    }

    @Override
    public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
        other.drawPolyline(xPoints, yPoints, nPoints);
        changed(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
        other.drawPolygon(xPoints, yPoints, nPoints);
        changed(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
        other.fillPolygon(xPoints, yPoints, nPoints);
        changed(xPoints, yPoints, nPoints);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        boolean result = other.drawImage(img, x, y, observer);
        if (result) {
            changed();
        }
        return result;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        boolean result = other.drawImage(img, x, y, width, height, observer);
        if (result) {
            changed();
        }
        return result;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        boolean result = other.drawImage(img, x, y, bgcolor, observer);
        if (result) {
            changed();
        }
        return result;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        boolean result = other.drawImage(img, x, y, width, height, bgcolor, observer);
        if (result) {
            changed();
        }
        return result;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        boolean result = other.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
        if (result) {
            changed();
        }
        return result;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        boolean result = other.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
        if (result) {
            changed();
        }
        return result;
    }

    @Override
    public void dispose() {
        other.dispose();
        onDispose();
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (width != 0 || height != 0) {
            other.drawRect(x, y, width, height);
            changed(x, y, width + 1, height + 1);
        }
    }

    @Override
    public void areaModified(int x, int y, int w, int h) {
        changed(x, y, w, h);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + other + ")";
    }
}
