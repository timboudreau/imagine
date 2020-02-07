/*
 * ShapeStack.java
 *
 * Created on October 25, 2006, 9:40 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.api.vector.Aggregate;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Proxy;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.aggregate.PaintedPrimitive;
import net.java.dev.imagine.api.vector.painting.VectorRepaintHandle;
import net.java.dev.imagine.api.vector.util.Pt;
import net.java.dev.imagine.spi.image.RepaintHandle;

/**
 * Holder for the stack of vector shapes making up an image
 *
 * @author Tim Boudreau
 */
public final class ShapeStack implements VectorRepaintHandle {

    List<PaintedPrimitive> primitives
            = new ArrayList<PaintedPrimitive>();
    private List<Attribute<?>> currentAttributes = new ArrayList<Attribute<?>>();

    private static Logger logger = Logger.getLogger(ShapeStack.class.getName());

    static {
        logger.setLevel(Level.ALL);
    }
    private static final boolean log = logger.isLoggable(Level.FINE);

    private final MetaRepaintHandle owner = new MetaRepaintHandle();

    public ShapeStack() {
        this(null, null);
    }

    public ShapeStack(ShapeStack other) {
        this(other, null);
    }

    public ShapeStack(ShapeStack other, RepaintHandle handle) {
        if (handle != null) {
            owner.add(handle);
        }
        if (other != null) {
            for (PaintedPrimitive entry : other.primitives) {
                primitives.add(entry);
            }
        }
    }

    public void addRepaintHandle(RepaintHandle h) {
        owner.add(h);
    }

    public void repaint(Primitive p) {
        if (p instanceof Volume) {
            Volume v = (Volume) p;
            v.getBounds(scratch);
            repaintArea(scratch.x, scratch.y, scratch.width, scratch.height);
        } else {
            Dimension d = getSize();
            repaintArea(0, 0, d.width, d.height);
        }
    }

    public void repaintArea(double x, double y, double w, double h) {
        owner.repaintArea((int) Math.floor(x), (int) Math.floor(y),
                (int) (Math.ceil(w + x) - Math.floor(x)),
                (int) (Math.ceil(h + y) - Math.floor(y)));
    }

    public void repaintArea(int x, int y, int w, int h) {
        owner.repaintArea(x, y, w, h);
    }

    public void repaint(double addX, double addY) {
        Rectangle2D.Double r = new Rectangle2D.Double();
        Rectangle2D.Double scratch = new Rectangle2D.Double();
        for (Primitive primitive : primitives) {
            if (primitive instanceof Volume) {
                ((Volume) primitive).getBounds(scratch);
            }
            r.union(r, scratch, r);
        }
        if (addX != 0 || addY != 0) {
            r.x -= addX;
            r.y -= addY;
            r.width += addX * 2;
            r.height += addY * 2;
        }
        repaintArea(r.x, r.y, r.width, r.height);
    }

    public void repaint() {
        repaint(0, 0);
    }

    public Dimension getSize() {
        Rectangle2D.Double r = new Rectangle2D.Double();
        Rectangle2D.Double scratch = new Rectangle2D.Double();
        for (Primitive primitive : primitives) {
            if (primitive instanceof Volume) {
                ((Volume) primitive).getBounds(scratch);
            }
            r.union(r, scratch, r);
        }
        return new Dimension((int) (r.x + r.width), (int) (r.y + r.height));
    }

    public List<PaintedPrimitive> getPrimitives() {
        return primitives; //XXX defensive copy if needed
    }
    Pt location = Pt.ORIGIN;
    private final Rectangle2D.Double scratch = new Rectangle2D.Double();

    PaintedPrimitive currentPrimitive = null;
    Consumer<Primitive> onDraw = this::onDrawn;

    public void onDraw(Consumer<Primitive> c) {
        onDraw = c;
    }

    public void drawn(Primitive p) {
        onDraw.accept(p);
    }

    private void onDrawn(Primitive p) {
        if (log) {
//            logger.log(Level.FINE, "Drawn: " + p);
            System.out.println("DRAWN: " + p);
//            Thread.dumpStack();
        }
//        System.err.println("DRAWN: " + p);
        if (p instanceof Vector && location.x != 0 && location.y != 0) {
            Pt loc = ((Vector) p).getLocation();
            double x = loc.x - location.x;
            double y = loc.y - location.y;
            ((Vector) p).setLocation(x, y);
        }

        if (p instanceof Attribute) {
            currentAttributes.add((Attribute) p);
        } else {
            if (currentPrimitive == null) {
                currentPrimitive = PaintedPrimitive.create(p, currentAttributes);
            } else if (currentPrimitive.matchesDrawnObject(p)) {
                boolean fill = p instanceof Fillable ? ((Fillable) p).isFill() : false;
                currentPrimitive.add(currentAttributes, fill);
            } else { //if currentPrimitive != null && !currentPrimitive.matchesDrawnObject (p)
//                System.err.println("Go to a new primitive");
                primitives.add(currentPrimitive);
                currentPrimitive = PaintedPrimitive.create(p, currentAttributes);
            }
            currentAttributes = new ArrayList<>();
//            currentAttributes.clear();
//            currentAttributes = new ArrayList<>(currentAttributes);
        }

        if (p instanceof Volume) {
            ((Volume) p).getBounds(scratch);
            java.awt.Rectangle r = scratch.getBounds();
            owner.repaintArea(r.x, r.y, r.width, r.height);
        }
    }

    public void beginDraw() {
        if (currentPrimitive != null) {
            saveCurrentPrimitive();
        }
    }

    public void endDraw() {
        if (currentPrimitive != null) {
            saveCurrentPrimitive();
        }
    }

    private void saveCurrentPrimitive() {
        PaintedPrimitive p = currentPrimitive;
        if (p != null) {
            primitives.add(p);
        }
        currentPrimitive = null;
//        currentAttributes.clear();
        if (p != null && p instanceof Volume) {
            ((Volume) p).getBounds(scratch);
            repaintArea((int) Math.floor(scratch.x), (int) Math.floor(scratch.y),
                    (int) Math.ceil(scratch.width), (int) Math.ceil(scratch.height));
        }
    }

    public void cancelDraw() {
        currentAttributes.clear();
    }

    public boolean isEmpty() {
        return primitives.isEmpty();
    }

    public void toFront(PaintedPrimitive primitive) {
        assert primitives.contains(primitive);
        primitives.remove(primitive);
        primitives.add(primitive);
    }

    public void toBack(PaintedPrimitive primitive) {
        assert primitives.contains(primitive);
        primitives.remove(primitive);
        primitives.add(0, primitive);
    }

    public void add(PaintedPrimitive primitive) {
        primitives.add(primitive);
    }

    public void setCursor(Cursor cursor) {
        owner.setCursor(cursor);
    }

    public void replace(List<? extends PaintedPrimitive> toRemove, PaintedPrimitive replaceWith) {
        int ix = Integer.MIN_VALUE;
        for (Primitive p : toRemove) {
            ix = Math.max(primitives.indexOf(p), ix);
        }
        if (ix != Integer.MIN_VALUE) {
            if (!primitives.removeAll(toRemove)) {
                System.out.println("Remove failed, try proxies");
                List<Primitive> prims = new ArrayList<>(toRemove.size());
                for (Primitive p : prims) {
                    while (p instanceof Proxy && !primitives.contains(p)) {
                        p = ((Proxy) p).getProxiedPrimitive();
                    }
                    prims.add(p);
                }
                if (!primitives.removeAll(prims)) {
                    System.out.println("Still failed to remove old prims");
                }
            }
        }
        System.err.println("Replace " + toRemove + " with " + replaceWith);
        int newIdx = Math.max(1, ix - toRemove.size());
        primitives.add(newIdx, replaceWith);
    }

    public boolean paint(Graphics2D g) {
        Primitive[] shapes = (Primitive[]) primitives.toArray(new Primitive[primitives.size()]);
        for (int i = 0; i < shapes.length; i++) {
//            System.err.println("PAINT " + shapes[i]);
            shapes[i].paint(g);
        }
        return shapes.length > 0;
    }

    @Override
    public String toString() {
        return super.toString() + primitives;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder("\n----------\n");
        for (Primitive p : primitives) {
            dumpPrimitive(p, 0, sb);
        }
        sb.append("----------\n");
        return sb.toString();
    }

    private void dumpPrimitive(Primitive p, int depth, StringBuilder sb) {
        char[] c = new char[depth * 2];
        Arrays.fill(c, ' ');
        sb.append(new String(c) + " ITEM:");
        sb.append(p);
        sb.append('\n');
        if (p instanceof Aggregate) {
            int max = ((Aggregate) p).getPrimitiveCount();
            for (int i = 0; i < max; i++) {
                Primitive pp = ((Aggregate) p).getPrimitive(i);
                dumpPrimitive(pp, depth + 1, sb);
            }
        } else if (p instanceof Proxy) {
            dumpPrimitive(((Proxy) p).getProxiedPrimitive(), depth + 1, sb);
        }
    }

    @Override
    public void draw(Primitive shape) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
