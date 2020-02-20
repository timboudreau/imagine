/*
 * WrapperGraphics.java
 *
 * Created on October 15, 2005, 8:18 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.painting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.elements.Arc;
import net.java.dev.imagine.api.vector.elements.CharacterIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.CircleWrapper;
import net.java.dev.imagine.api.vector.elements.Clear;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.Line;
import net.java.dev.imagine.api.vector.elements.Oval;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.Polygon;
import net.java.dev.imagine.api.vector.elements.Polyline;
import net.java.dev.imagine.api.vector.elements.Rectangle;
import net.java.dev.imagine.api.vector.elements.RoundRect;
import net.java.dev.imagine.api.vector.elements.StringWrapper;
import net.java.dev.imagine.api.vector.elements.Text;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import net.java.dev.imagine.api.vector.graphics.AffineTransformWrapper;
import net.java.dev.imagine.api.vector.graphics.Background;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import net.java.dev.imagine.api.vector.graphics.ColorWrapper;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import net.java.dev.imagine.api.vector.graphics.GradientPaintWrapper;
import net.java.dev.imagine.api.vector.graphics.LinearPaintWrapper;
import net.java.dev.imagine.api.vector.graphics.RadialPaintWrapper;
import net.java.dev.imagine.api.vector.graphics.TexturePaintWrapper;
import org.imagine.geometry.Circle;
import org.imagine.geometry.Triangle;
import org.imagine.utils.java2d.GraphicsUtils;

/**
 * A Graphics2D which wrappers another Graphics2D, and produces objects
 * representing primitive graphics elements for all drawing operations performed
 * upon it.
 *
 * @author Timothy Boudreau
 */
public class VectorWrapperGraphics extends Graphics2D {

    private final Graphics2D other;
    private final VectorRepaintHandle handle;
    private final Point location;
    private final int w;
    private final int h;
    private Runnable onDispose;

    /**
     * Creates a new instance of WrapperGraphics
     */
    public VectorWrapperGraphics(VectorRepaintHandle handle, Graphics2D other, Point location, int w, int h) {
        this.w = w;
        this.h = h;
        this.other = (Graphics2D) other.create(0, 0, w, h); //so we can translate safely
        this.handle = handle;
        this.location = location;
        location.x = Math.min(0, location.x);
        location.y = Math.min(0, location.y);
        this.other.translate(-location.x, -location.y);
    }

    public void onDispose(Runnable run) {
        if (onDispose == null) {
            onDispose = run;
        } else {
            Runnable old = onDispose;
            onDispose = () -> {
                old.run();
                run.run();
            };
        }
    }

    public Dimension size() {
        return new Dimension(w, h);
    }

    static Triangle toTriangle(Shape shape, boolean fill) {
        PathIterator it = shape.getPathIterator(null);
        int count = 0;
        double[] scratch = new double[8];
        double[] pts = new double[6];
        while (!it.isDone()) {
            int type = it.currentSegment(scratch);
            if (count != 0 && type != PathIterator.SEG_LINETO) {
                return null;
            } else if (count == 0 && type != PathIterator.SEG_MOVETO) {
                return null;
            }
            pts[count * 2] = scratch[0];
            pts[(count * 2) + 1] = scratch[1];
            count++;
            if (count > 3) {
                return null;
            }
        }
        return new Triangle(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
    }

    public static Shaped primitiveFor(Shape shape, boolean fill) {
        if (shape instanceof java.awt.Rectangle) {
            java.awt.Rectangle r = (java.awt.Rectangle) shape;
            Rectangle rect = new Rectangle(r.x, r.y, r.width, r.height, fill);
            return rect;
        } else if (shape instanceof Circle) {
            Circle circ = (Circle) shape;
            return new CircleWrapper(circ, fill);
        } else if (shape instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) shape;
            Rectangle rect = new Rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight(), fill);
            return rect;
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D ell = (Ellipse2D) shape;
            if (ell.getWidth() == ell.getHeight()) {
                double cx = ell.getCenterX();
                double cy = ell.getCenterY();
                double rad = ell.getWidth() / 2;
                return new CircleWrapper(cx, cy, rad);
            }
            Oval ov = new Oval(ell.getX(), ell.getY(), ell.getWidth(), ell.getHeight(), fill);
            return ov;
        } else if (shape instanceof java.awt.Polygon) {
            java.awt.Polygon p = (java.awt.Polygon) shape;
            Polygon polygon = new Polygon(p.xpoints, p.ypoints, p.npoints, fill);
            return polygon;
        } else if (shape instanceof RoundRectangle2D) {
            RoundRectangle2D r = (RoundRectangle2D) shape;
            return new RoundRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), r.getArcWidth(), r.getArcHeight(), fill);
        } else if (shape instanceof Arc2D) {
            Arc2D arc = (Arc2D) shape;
            return new Arc(arc.getX(), arc.getY(), arc.getWidth(), arc.getHeight(), arc.getAngleStart(), arc.getAngleExtent(), fill);
        } else if (shape instanceof Line2D) {
            Line2D line = (Line2D) shape;
            return new Line(line.getX1(), line.getY1(), line.getX2(), line.getY2());
        } else if (shape instanceof Triangle) {
            Triangle t = (Triangle) shape;
            return new TriangleWrapper(t, fill);
        } else {
            Triangle tri = toTriangle(shape, fill);
            if (tri != null) {
                return new TriangleWrapper(tri, fill);
            }
            return new PathIteratorWrapper(shape.getPathIterator(null), fill);
        }
    }

    @Override
    public void draw(Shape s) {
        if (!receiving) {
            push(primitiveFor(s, false));
//            push(new PathIteratorWrapper(s.getPathIterator(
//                    AffineTransform.getTranslateInstance(0,0)),
//                    false));
        }
        other.draw(s);
        Stroke stroke = getStroke();
        java.awt.Rectangle r = s.getBounds();
        if (stroke == null || stroke instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke) stroke;
            if (bs == null || bs.getLineWidth() != 1) {
                r = s.getBounds();
                if (bs != null) {
                    int width = Math.round(bs.getLineWidth());
                    r.x -= width / 2;
                    r.y -= width / 2;
                    r.width += width;
                    r.height += width;
                }
            }
        }
        changed(r);
    }

    public void clear() {
        if (!receiving) {
            push(new Clear(0, 0, w, h));
        }
        clearRect(0, 0, w, h);
        changed(0, 0, w, h);
    }

    public void draw(Clear clear) {
        receiving = true;
        clear();
        receiving = false;
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
        if (strokeWidth > 1) {
            float half = strokeWidth / 2;
            x -= half;
            y -= half;
            w += strokeWidth;
            h += strokeWidth;
        }
        handle.repaintArea(x, y, w, h);
    }

    private void changed() {
        changed(-1, -1, -1, -1);
    }

    private void changed(java.awt.Rectangle r) {
        changed(r.x, r.y, r.width, r.height);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        boolean result = other.drawImage(img, xform, obs);
        if (result) {
            if (!receiving) {
                push(new ImageWrapper(xform.getTranslateX(), xform.getTranslateY(), img));
            }
            changed();
        }
        return result;
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        if (!receiving) {
            push(new ImageWrapper(img, x, y));
        }
        other.drawImage(img, op, x, y);
        changed(x, y, img.getWidth(), img.getHeight());
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        if (!receiving) {
            push(new ImageWrapper(img, xform.getTranslateX(), xform.getTranslateY()));
        }
        other.drawRenderedImage(img, xform);
        changed(0, 0, img.getWidth(), img.getHeight()); //XXX won't work on scale xform
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        if (!receiving) {
            push(new ImageWrapper(img, xform.getTranslateX(), xform.getTranslateY()));
        }
        other.drawRenderableImage(img, xform);
        changed(0, 0, (int) img.getWidth(), (int) img.getHeight()); //XXX won't work on scale xform
    }

    public void draw(ImageWrapper img) {
        receiving = true;
        drawRenderedImage(img.img, AffineTransform.getTranslateInstance(img.x, img.y));
        changed();
        receiving = false;
    }

    public void draw(StringWrapper sw) {
        receiving = true;
        sw.paint(this);
        receiving = false;
    }

    public void draw(Text sw) {
        receiving = true;
        sw.paint(this);
        receiving = false;
    }

    @Override
    public void drawString(String text, int x, int y) {
        if (!receiving) {
            push(new Text(text, getFont(), x, y));
        }
        other.drawString(text, x, y);
        changed();
    }

    @Override
    public void drawString(String text, float x, float y) {
        if (!receiving) {
            push(new Text(text, getFont(), x, y));
        }
        other.drawString(text, x, y);
        changed();
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        if (!receiving) {
            push(new CharacterIteratorWrapper(iterator, x, y));
        }
        other.drawString(iterator, x, y);
        changed();
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        if (!receiving) {
            push(new CharacterIteratorWrapper(iterator, x, y));
        }
        other.drawString(iterator, x, y);
        changed();
    }

    @Override
    public void drawGlyphVector(GlyphVector gv, float x, float y) {
        if (!receiving) {
            push(new PathIteratorWrapper(gv, x, y));
        }
        other.drawGlyphVector(gv, x, y);
        changed();
    }

    @Override
    public void fill(Shape s) {
        if (!receiving) {
            push(primitiveFor(s, true));
        }
        other.fill(s);
        changed(s.getBounds());
    }

    @Override
    public boolean hit(java.awt.Rectangle rect, Shape s, boolean onStroke) {
        return other.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return other.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp) {
        // XXX have a composite wrapper?
        other.setComposite(comp);
    }

    @Override
    public void setPaint(Paint paint) {
        if (!receiving) {
            if (paint != null) {
                if (paint.getClass() == GradientPaint.class) {
                    push(new GradientPaintWrapper((GradientPaint) paint));
                } else if (paint instanceof RadialGradientPaint) {
                    push(new RadialPaintWrapper((RadialGradientPaint) paint));
                } else if (paint instanceof TexturePaint) {
                    push(new TexturePaintWrapper((TexturePaint) paint));
                } else if (paint.getClass() == LinearGradientPaint.class) {
                    push(new LinearPaintWrapper((LinearGradientPaint) paint));
                }
            }
        }
        other.setPaint(paint);
    }

    float strokeWidth = 1;

    @Override
    public void setStroke(Stroke s) {
        if (!receiving) {
            if (s instanceof Primitive) {
                push((Primitive) s);
            } else if (s instanceof BasicStroke) {
                push(new BasicStrokeWrapper((BasicStroke) s));
            }
        }
        if (s instanceof BasicStroke) {
            strokeWidth = ((BasicStroke) s).getLineWidth();
        }
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
        if (!receiving) {
            push(new AffineTransformWrapper(x, y,
                    AffineTransformWrapper.TRANSLATE));
        }
    }

    @Override
    public void translate(double tx, double ty) {
        other.translate(tx, ty);
        if (!receiving) {
            push(new AffineTransformWrapper(tx, ty, AffineTransformWrapper.TRANSLATE));
        }
    }

    @Override
    public void rotate(double theta) {
        other.rotate(theta);
        if (!receiving) {
            push(new AffineTransformWrapper(theta));
        }
    }

    @Override
    public void rotate(double theta, double x, double y) {
        other.rotate(theta, x, y);
        if (!receiving) {
            push(new AffineTransformWrapper(theta, x, y));
        }
    }

    @Override
    public void scale(double sx, double sy) {
        other.scale(sx, sy);
        if (!receiving) {
            push(new AffineTransformWrapper(sx, sy, AffineTransformWrapper.SCALE));
        }
    }

    @Override
    public void shear(double shx, double shy) {
        other.shear(shx, shy);
        if (!receiving) {
            push(new AffineTransformWrapper(shx, shy, AffineTransformWrapper.SHEAR));
        }
    }

    @Override
    public void transform(AffineTransform tx) {
        other.transform(tx);
        if (!receiving) {
            push(new AffineTransformWrapper(tx));
        }
    }

    @Override
    public void setTransform(AffineTransform tx) {
        other.setTransform(tx);
        if (!receiving) {
            if (tx == null) {
                tx = AffineTransform.getTranslateInstance(0, 0);
            }
            push(new AffineTransformWrapper(tx));
        }
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
        if (!receiving) {
            push(new Background(color));
        }
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
        return new VectorWrapperGraphics(handle, (Graphics2D) other.create(),
                new Point(location), w, h);
    }

    @Override
    public Color getColor() {
        return other.getColor();
    }

    @Override
    public void setColor(Color c) {
        if (!receiving) {
            push(new ColorWrapper(c));
        }
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
        if (!receiving) {
            push(FontWrapper.create(font));
        }
        other.setFont(font);
    }

    public void setFont(FontWrapper w) {
        receiving = true;
        setFont(w.toFont());
        receiving = false;
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return other.getFontMetrics();
    }

    @Override
    public java.awt.Rectangle getClipBounds() {
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

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        if (!receiving) {
            push(new Line(x1, y1, x2, y2));
        }
        other.drawLine(x1, y1, x2, y2);
        int wid = Math.abs(x1 - x2);
        int ht = Math.abs(y1 - y2);
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        changed(x, y, wid, ht);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (!receiving) {
            push(new net.java.dev.imagine.api.vector.elements.Rectangle(x, y, width, height, true));
        }
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
        if (!receiving) {
            push(new RoundRect(x, y, width, height, arcWidth, arcHeight, false));
        }
        other.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        changed(x, y, width, height);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (!receiving) {
            push(new RoundRect(x, y, width, height, arcWidth, arcHeight, true));
        }
        other.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        changed(x, y, width, height);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        if (!receiving) {
            push(new Oval(x, y, width, height, false));
        }
        other.drawOval(x, y, width, height);
        changed(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        if (!receiving) {
            push(new Oval(x, y, width, height, true));
        }
        other.fillOval(x, y, width, height);
        changed(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (!receiving) {
            push(new Arc(x, y, width, height, startAngle, arcAngle, false));
        }
        other.drawArc(x, y, width, height, startAngle, arcAngle);
        changed(x, y, width, height);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (!receiving) {
            push(new Arc(x, y, width, height, startAngle, arcAngle, true));
        }
        other.fillArc(x, y, width, height, startAngle, arcAngle);
        changed(x, y, width, height);
    }

    @Override
    public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
        if (!receiving) {
            push(new Polyline(xPoints, yPoints, nPoints, false));
        }
        other.drawPolyline(xPoints, yPoints, nPoints);
        changed(xPoints, yPoints, nPoints);
    }

    private void push(Primitive serializable) {
        handle.drawn(serializable);
    }

    @Override
    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
        if (!receiving) {
            push(new Polygon(xPoints, yPoints, nPoints, false));
        }
        other.drawPolygon(xPoints, yPoints, nPoints);
        changed(xPoints, yPoints, nPoints);
    }

    private boolean receiving = false;

    @Override
    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
        if (!receiving) {
            push(new Polygon(xPoints, yPoints, nPoints, true));
        }
        other.fillPolygon(xPoints, yPoints, nPoints);
        changed(xPoints, yPoints, nPoints);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        if (!receiving) {
            push(new ImageWrapper(x, y, img));
        }
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
    }

    public void draw(java.awt.Rectangle rect) {
        receiving = true;
        drawRect(rect.x, rect.y, rect.width, rect.height);
        receiving = false;
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (width != 0 && height != 0) {
            if (!receiving) {
                push(new net.java.dev.imagine.api.vector.elements.Rectangle(x, y, width, height, false));
            }
            other.drawRect(x, y, width, height);
            changed(x, y, width + 1, height + 1);
        }
    }

    public void draw(Rectangle r) {
        receiving = true;
        r.draw(this);
        receiving = false;
    }

    public void receive(Primitive s) {
        if (!GraphicsUtils.isNoOpGraphics(other)) {
            other.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            other.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            other.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            other.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            other.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        receiving = true;
        try {
            s.paint(other);
        } finally {
            receiving = false;
        }
    }

    public void setFontThrough(Font f) {
        other.setFont(f);
    }
}
