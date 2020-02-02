/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.editing;

import java.awt.AlphaComposite;
import static java.awt.AlphaComposite.SRC_OVER;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
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
import java.beans.PropertyChangeListener;
import java.text.AttributedCharacterIterator;
import java.util.Collections;
import java.util.Map;
import net.dev.java.imagine.api.selection.Selection;
import net.dev.java.imagine.api.tool.Tool;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
final class NoOpLayer extends LayerFactory {

    public NoOpLayer(String name, String displayName) {
        super(name, displayName);
    }

    @Override
    public LayerImplementation createLayer(String name, RepaintHandle handle, Dimension size) {
        return new LI(this, true);
    }

    @Override
    public boolean canConvert(Layer other) {
        return false;
    }

    @Override
    public LayerImplementation convert(Layer other) {
        return new LI(this);
    }
    
    static class Sel extends Selection<Object> {

        public Sel() {
            super(Object.class);
        }

        @Override
        public Selection<Object> clone() {
            return this;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public void add(Object content, Op op) {
            // do nothing
        }

        @Override
        public void clear() {
            // do nothing
        }

        @Override
        public void paint(Graphics2D g, Rectangle bounds) {
            // do nothing
        }

        @Override
        public void addShape(Shape shape, Op op) {
            // do nothing
        }

        @Override
        public void translateFrom(Selection selection) {
            // do nothing
        }

        @Override
        public Shape asShape() {
            return new Rectangle();
        }

        @Override
        public void clearNoUndo() {
            // do nothing
        }

        @Override
        public void invert(Rectangle bds) {
            
        }

        @Override
        public boolean contains(Object what) {
            return false;
        }
    }

    static class LI extends LayerImplementation {

        public LI(LayerFactory factory) {
            super(factory);
        }

        public LI(LayerFactory factory, boolean resolutionIndependent) {
            super(factory, resolutionIndependent);
        }

        @Override
        protected Lookup createLookup() {
            return Lookups.fixed(this, new SI(), new Sel());
        }

        @Override
        public Rectangle getBounds() {
            return new Rectangle(0, 0, 640, 480);
        }

        @Override
        public String getName() {
            return "No LayerFactory Installed";
        }

        @Override
        public void resize(int width, int height) {

        }

        @Override
        public void setName(String name) {

        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
        }

        @Override
        public void setVisible(boolean visible) {
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public float getOpacity() {
            return 1;
        }

        @Override
        public void setOpacity(float f) {
        }

        @Override
        public void commitLastPropertyChangeToUndoHistory() {
        }

        @Override
        public boolean paint(Graphics2D g, Rectangle bounds, boolean showSelection) {
            g.setColor(new Color(136, 50, 50));
            Rectangle r = getBounds();
            g.fill(r);
            g.draw(r);
            String msg = "No Modules Providing a LayerFactory are Installed";
            g.setColor(new Color(255, 180, 180));
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font f = new Font("SansSerif", Font.BOLD, 16);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int sw = fm.stringWidth(msg);
            int th = fm.getMaxDescent() + fm.getMaxAscent();
            int tx = (r.width / 2) - (sw / 2);
            int ty = (r.height / 2) - (th / 2);
            g.drawString(msg, tx, ty);
            return true;
        }

        @Override
        public LayerImplementation clone(boolean isUserCopy, boolean deepCopy) {
            return this;
        }

        static class SI extends SurfaceImplementation {

            @Override
            public Graphics2D getGraphics() {
                // do nothing
                return new G();
            }

            @Override
            public void setLocation(Point p) {
                // do nothing
            }

            @Override
            public Point getLocation() {
                return new Point(0,0);
            }

            @Override
            public void beginUndoableOperation(String name) {
                // do nothing
            }

            @Override
            public void endUndoableOperation() {
                // do nothing
            }

            @Override
            public void cancelUndoableOperation() {
                // do nothing
            }

            @Override
            public void setCursor(Cursor cursor) {
                // do nothing
            }

            @Override
            public void setTool(Tool tool) {
                // do nothing
            }

            @Override
            public void applyComposite(Composite composite, Shape clip) {
                // do nothing
            }

            @Override
            public boolean paint(Graphics2D g, Rectangle r) {
                // do nothing
                return false;
            }

            @Override
            public Dimension getSize() {
                // do nothing
                return new Dimension(0,0);
            }

            static class G extends Graphics2D {

                @Override
                public void draw(Shape s) {
                    // do nothing
                }

                @Override
                public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
                    // do nothing
                    return false;
                }

                @Override
                public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
                    // do nothing
                }

                @Override
                public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
                    // do nothing
                }

                @Override
                public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
                    // do nothing
                }

                @Override
                public void drawString(String str, int x, int y) {
                    // do nothing
                }

                @Override
                public void drawString(String str, float x, float y) {
                    // do nothing
                }

                @Override
                public void drawString(AttributedCharacterIterator iterator, int x, int y) {
                    // do nothing
                }

                @Override
                public void drawString(AttributedCharacterIterator iterator, float x, float y) {
                    // do nothing
                }

                @Override
                public void drawGlyphVector(GlyphVector g, float x, float y) {
                    // do nothing
                }

                @Override
                public void fill(Shape s) {
                    // do nothing
                }

                @Override
                public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
                    // do nothing
                    return false;
                }

                @Override
                public GraphicsConfiguration getDeviceConfiguration() {
                    // do nothing
                    return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
                }

                @Override
                public void setComposite(Composite comp) {
                    // do nothing
                }

                @Override
                public void setPaint(Paint paint) {
                    // do nothing
                }

                @Override
                public void setStroke(Stroke s) {
                    // do nothing
                }

                @Override
                public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
                    // do nothing
                }

                @Override
                public Object getRenderingHint(RenderingHints.Key hintKey) {
                    return null;
                }

                @Override
                public void setRenderingHints(Map<?, ?> hints) {
                    // do nothing
                }

                @Override
                public void addRenderingHints(Map<?, ?> hints) {
                    // do nothing
                }

                @Override
                public RenderingHints getRenderingHints() {
                    return new RenderingHints(Collections.emptyMap());
                }

                @Override
                public void translate(int x, int y) {
                    // do nothing
                }

                @Override
                public void translate(double tx, double ty) {
                    // do nothing
                }

                @Override
                public void rotate(double theta) {
                    // do nothing
                }

                @Override
                public void rotate(double theta, double x, double y) {
                    // do nothing
                }

                @Override
                public void scale(double sx, double sy) {
                    // do nothing
                }

                @Override
                public void shear(double shx, double shy) {
                    // do nothing
                }

                @Override
                public void transform(AffineTransform Tx) {
                    // do nothing
                }

                @Override
                public void setTransform(AffineTransform Tx) {
                    // do nothing
                }

                @Override
                public AffineTransform getTransform() {
                    // do nothing
                    return AffineTransform.getTranslateInstance(0, 0);
                }

                @Override
                public Paint getPaint() {
                    // do nothing
                    return Color.BLACK;
                }

                @Override
                public Composite getComposite() {
                    // do nothing
                    return AlphaComposite.getInstance(SRC_OVER);
                }

                @Override
                public void setBackground(Color color) {
                    // do nothing
                }

                @Override
                public Color getBackground() {
                    // do nothing
                    return Color.WHITE;
                }

                @Override
                public Stroke getStroke() {
                    return new BasicStroke(1);
                }

                @Override
                public void clip(Shape s) {
                    // do nothing
                }

                @Override
                public FontRenderContext getFontRenderContext() {
                    // do nothing
                    return new FontRenderContext() {
                    };
                }

                @Override
                public Graphics create() {
                    // do nothing
                    return this;
                }

                @Override
                public Color getColor() {
                    // do nothing
                    return Color.BLACK;
                }

                @Override
                public void setColor(Color c) {
                    // do nothing
                }

                @Override
                public void setPaintMode() {
                    // do nothing
                }

                @Override
                public void setXORMode(Color c1) {
                    // do nothing
                }

                @Override
                public Font getFont() {
                    // do nothing
                    return new Font("SansSerif", Font.PLAIN, 12);
                }

                @Override
                public void setFont(Font font) {
                    // do nothing
                }

                @Override
                public FontMetrics getFontMetrics(Font f) {
                    // do nothing
                    return new FontMetrics(f) {
                    };
                }

                @Override
                public Rectangle getClipBounds() {
                    // do nothing
                    return new Rectangle(0,0,0,0);
                }

                @Override
                public void clipRect(int x, int y, int width, int height) {
                    // do nothing
                }

                @Override
                public void setClip(int x, int y, int width, int height) {
                    // do nothing
                }

                @Override
                public Shape getClip() {
                    return getClipBounds();
                }

                @Override
                public void setClip(Shape clip) {
                    // do nothing
                }

                @Override
                public void copyArea(int x, int y, int width, int height, int dx, int dy) {
                    // do nothing
                }

                @Override
                public void drawLine(int x1, int y1, int x2, int y2) {
                    // do nothing
                }

                @Override
                public void fillRect(int x, int y, int width, int height) {
                    // do nothing
                }

                @Override
                public void clearRect(int x, int y, int width, int height) {
                    // do nothing
                }

                @Override
                public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
                    // do nothing
                }

                @Override
                public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
                    // do nothing
                }

                @Override
                public void drawOval(int x, int y, int width, int height) {
                    // do nothing
                }

                @Override
                public void fillOval(int x, int y, int width, int height) {
                    // do nothing
                }

                @Override
                public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
                    // do nothing
                }

                @Override
                public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
                    // do nothing
                }

                @Override
                public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
                    // do nothing
                }

                @Override
                public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
                    // do nothing
                }

                @Override
                public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
                    // do nothing
                }

                @Override
                public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
                    return false;
                }

                @Override
                public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
                    return false;
                }

                @Override
                public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
                    return false;
                }

                @Override
                public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
                    return false;
                }

                @Override
                public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
                    return false;
                }

                @Override
                public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
                    return false;
                }

                @Override
                public void dispose() {
                    // do nothing
                }
            }
        }
    }
}
