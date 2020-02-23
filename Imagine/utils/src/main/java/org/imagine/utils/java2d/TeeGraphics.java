/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
 *
 * @author Tim Boudreau
 */
final class TeeGraphics extends Graphics2D {

    private final Graphics2D a;
    private final Graphics2D b;
    private final Runnable onDispose;

    TeeGraphics(Graphics2D a, Graphics2D b, Runnable onDispose) {
        this.a = a;
        this.b = b;
        this.onDispose = onDispose;
    }

    @Override
    public void draw(Shape s) {
        a.draw(s);
        b.draw(s);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        // bitwise and on purpose
        return a.drawImage(img, xform, obs) & b.drawImage(img, xform, obs);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        a.drawImage(img, op, x, y);
        b.drawImage(img, op, x, y);
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        a.drawRenderedImage(img, xform);
        b.drawRenderedImage(img, xform);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        a.drawRenderableImage(img, xform);
        b.drawRenderableImage(img, xform);
    }

    @Override
    public void drawString(String str, int x, int y) {
        a.drawString(str, x, y);
        b.drawString(str, x, y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        a.drawString(str, x, y);
        b.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        a.drawString(iterator, x, y);
        b.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        a.drawString(iterator, x, y);
        b.drawString(iterator, x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        a.drawGlyphVector(g, x, y);
        b.drawGlyphVector(g, x, y);
    }

    @Override
    public void fill(Shape s) {
        a.fill(s);
        b.fill(s);
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return a.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return a.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp) {
        a.setComposite(comp);
        b.setComposite(comp);
    }

    @Override
    public void setPaint(Paint paint) {
        a.setPaint(paint);
        b.setPaint(paint);
    }

    @Override
    public void setStroke(Stroke s) {
        a.setStroke(s);
        b.setStroke(s);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        a.setRenderingHint(hintKey, hintValue);
        b.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return a.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        a.setRenderingHints(hints);
        b.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        a.addRenderingHints(hints);
        b.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return a.getRenderingHints();
    }

    @Override
    public void translate(int x, int y) {
        a.translate(x, y);
        b.translate(x, y);
    }

    @Override
    public void translate(double x, double y) {
        a.translate(x, y);
        b.translate(x, y);
    }

    @Override
    public void rotate(double theta) {
        a.rotate(theta);
        b.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        a.rotate(theta);
        b.rotate(theta);
    }

    @Override
    public void scale(double sx, double sy) {
        a.scale(sx, sy);
        b.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        a.shear(shx, shy);
        b.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform Tx) {
        a.transform(Tx);
        b.transform(Tx);
    }

    @Override
    public void setTransform(AffineTransform Tx) {
        a.setTransform(Tx);
        b.setTransform(Tx);
    }

    @Override
    public AffineTransform getTransform() {
        return a.getTransform();
    }

    @Override
    public Paint getPaint() {
        return a.getPaint();
    }

    @Override
    public Composite getComposite() {
        return a.getComposite();
    }

    @Override
    public void setBackground(Color color) {
        a.setBackground(color);
        b.setBackground(color);
    }

    @Override
    public Color getBackground() {
        return a.getBackground();
    }

    @Override
    public Stroke getStroke() {
        return a.getStroke();
    }

    @Override
    public void clip(Shape s) {
        a.clip(s);
        b.clip(s);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return a.getFontRenderContext();
    }

    @Override
    public Graphics create() {
        return new TeeGraphics((Graphics2D) a.create(), (Graphics2D) b.create(),
                null);
    }

    @Override
    public Color getColor() {
        return a.getColor();
    }

    @Override
    public void setColor(Color c) {
        a.setColor(c);
    }

    @Override
    public void setPaintMode() {
        a.setPaintMode();
        b.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        a.setXORMode(c1);
        b.setXORMode(c1);
    }

    @Override
    public Font getFont() {
        return a.getFont();
    }

    @Override
    public void setFont(Font font) {
        a.setFont(font);
        b.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return a.getFontMetrics(f);
    }

    @Override
    public Rectangle getClipBounds() {
        return a.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        a.clipRect(x, y, width, height);
        b.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        a.setClip(x, y, width, height);
        b.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        return a.getClip();
    }

    @Override
    public void setClip(Shape clip) {
        a.setClip(clip);
        b.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        a.copyArea(x, y, width, height, dx, dy);
        b.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        a.drawLine(x1, y1, x2, y2);
        b.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        a.fillRect(x, y, width, height);
        b.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        a.fillRect(x, y, width, height);
        b.fillRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        a.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        b.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        a.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        b.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        a.drawOval(x, y, width, height);
        b.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        a.fillOval(x, y, width, height);
        b.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        a.drawArc(x, y, width, height, startAngle, arcAngle);
        b.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        a.fillArc(x, y, width, height, startAngle, arcAngle);
        b.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        a.drawPolyline(xPoints, yPoints, nPoints);
        b.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        a.drawPolygon(xPoints, yPoints, nPoints);
        b.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        a.fillPolygon(xPoints, yPoints, nPoints);
        b.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return a.drawImage(img, x, y, observer)
                & b.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return a.drawImage(img, x, y, width, height, observer)
                & b.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return a.drawImage(img, x, y, bgcolor, observer)
                & a.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return a.drawImage(img, x, y, width, height, bgcolor, observer)
                & a.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return a.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer)
                & a.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return a.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer)
                & a.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    @Override
    public void dispose() {
        a.dispose();
        b.dispose();
        if (onDispose != null) {
            onDispose.run();
        }
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        a.fill3DRect(x, y, width, height, raised);
        b.fill3DRect(x, y, width, height, raised);
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        a.draw3DRect(x, y, width, height, raised);
        b.draw3DRect(x, y, width, height, raised);
    }

    @Override
    public Rectangle getClipBounds(Rectangle r) {
        return a.getClipBounds();
    }

    @Override
    public boolean hitClip(int x, int y, int width, int height) {
        return a.hitClip(x, y, width, height);
    }

    @Override
    public Rectangle getClipRect() {
        return a.getClipRect();
    }

    @Override
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        a.drawBytes(data, offset, length, x, y);
        b.drawBytes(data, offset, length, x, y);
    }

    @Override
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        a.drawChars(data, offset, length, x, y);
        b.drawChars(data, offset, length, x, y);
    }

    @Override
    public void fillPolygon(Polygon p) {
        a.fillPolygon(p);
        b.fillPolygon(p);
    }

    @Override
    public void drawPolygon(Polygon p) {
        a.drawPolygon(p);
        b.drawPolygon(p);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        a.drawRect(x, y, width, height);
        b.drawRect(x, y, width, height);
    }

    @Override
    public FontMetrics getFontMetrics() {
        return a.getFontMetrics();
    }

    @Override
    public Graphics create(int x, int y, int width, int height) {
        return new TeeGraphics((Graphics2D) a.create(x, y, width, height),
                (Graphics2D) b.create(x, y, width, height), null);
    }
}
