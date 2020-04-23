/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.editing;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.beans.PropertyChangeListener;
import net.dev.java.imagine.api.selection.Selection;
import net.dev.java.imagine.api.tool.Tool;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.imagine.editor.api.Zoom;
import org.imagine.utils.java2d.GraphicsUtils;
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
        public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds, boolean showSelection, boolean ignoreVisibility, Zoom zoom) {
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
                return GraphicsUtils.noOpGraphics();
            }

            @Override
            public void setLocation(Point p) {
                // do nothing
            }

            @Override
            public Point getLocation() {
                return new Point(0, 0);
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
            public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle r, Zoom zoom) {
                // do nothing
                return false;
            }

            @Override
            public Dimension getSize() {
                // do nothing
                return new Dimension(0, 0);
            }
        }
    }
}
