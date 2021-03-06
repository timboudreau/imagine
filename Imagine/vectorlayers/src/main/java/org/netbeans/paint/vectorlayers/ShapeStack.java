/*
 * ShapeStack.java
 *
 * Created on October 25, 2006, 9:40 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.vectorlayers;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.api.vector.Aggregate;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.painting.VectorRepaintHandle;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Proxy;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.aggregate.PaintedPrimitive;
import net.java.dev.imagine.api.vector.aggregate.TransformedPrimitive;
import net.java.dev.imagine.api.vector.util.Pt;
import net.java.dev.imagine.api.vector.Vectors;

/**
 * Holder for the stack of vector shapes making up an image
 *
 * @author Tim Boudreau
 */
public final class ShapeStack implements VectorRepaintHandle {

    List<Primitive> primitives
            = new ArrayList<Primitive>();
    private List<Attribute<?>> currentAttributes = new ArrayList<>();
    private List<Primitive> unknown = new ArrayList<Primitive>();

    private static Logger logger = Logger.getLogger(ShapeStack.class.getName());
    private static final boolean log = logger.isLoggable(Level.FINE);

    static {
        logger.setLevel(Level.ALL);
    }
    private final RepaintHandle owner;

    public ShapeStack(RepaintHandle owner) {
        this.owner = owner;
    }

    public ShapeStack(ShapeStack other) {
        this.owner = other.owner;
        for (Primitive entry : other.primitives) {
            primitives.add(entry);
        }
    }

    public ShapeStack(ShapeStack other, RepaintHandle handle) {
        this.owner = handle;
        for (Primitive entry : other.primitives) {
            primitives.add(entry);
        }
    }

    public void repaintArea(int x, int y, int w, int h) {
        owner.repaintArea(x, y, w, h);
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

    public List<Primitive> getPrimitives() {
        return primitives; //XXX defensive copy if needed
    }
    Pt location = Pt.ORIGIN;
    private final Rectangle2D.Double scratch = new Rectangle2D.Double();

    PaintedPrimitive currentPrimitive = null;

    public void drawn(Primitive p) {
        if (log) {
            logger.log(Level.FINE, "Drawn: " + p);
        }
//        System.err.println("DRAWN: " + p);
        if (p instanceof Vectors) {
            Pt loc = ((Vectors) p).getLocation();
            double x = loc.x - location.x;
            double y = loc.y - location.y;
            ((Vectors) p).setLocation(x, y);
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
            currentAttributes.clear();
        }

        if (p instanceof Volume) {
            ((Volume) p).getBounds(scratch);
            java.awt.Rectangle r = scratch.getBounds();
            owner.repaintArea(r.x, r.y, r.width, r.height);
        }
    }

    void beginDraw() {
        if (currentPrimitive != null) {
            saveCurrentPrimitive();
        }
    }

    void endDraw() {
        if (currentPrimitive != null) {
            saveCurrentPrimitive();
        }
    }

    private void saveCurrentPrimitive() {
        Primitive p = currentPrimitive;
        if (p != null) {
            primitives.add(TransformedPrimitive.create(p));
        }
        currentPrimitive = null;
        currentAttributes.clear();
        if (p != null && p instanceof Volume) {
            ((Volume) p).getBounds(scratch);
            repaintArea((int) Math.floor(scratch.x), (int) Math.floor(scratch.y),
                    (int) Math.ceil(scratch.width), (int) Math.ceil(scratch.height));
        }
    }

    void cancelDraw() {
        currentAttributes.clear();
    }

    boolean isEmpty() {
        return primitives.isEmpty();
    }

    public void toFront(Primitive primitive) {
        assert primitives.contains(primitive);
        primitives.remove(primitive);
        primitives.add(primitive);
    }

    public void toBack(Primitive primitive) {
        assert primitives.contains(primitive);
        primitives.remove(primitive);
        primitives.add(0, primitive);
    }

    public void add(Primitive primitive) {
        primitives.add(primitive);
    }

    public void replace(List<? extends Primitive> toRemove, Primitive replaceWith) {
        int ix = Integer.MIN_VALUE;
        for (Primitive p : toRemove) {
            ix = Math.max(primitives.indexOf(p), ix);
        }
        primitives.removeAll(toRemove);
//        System.err.println("Replace " + toRemove + " with " + replaceWith);
        int newIdx = Math.max(0, ix - toRemove.size());
        primitives.add(newIdx, replaceWith);
    }

    public void draw(Primitive shape) {
        throw new UnsupportedOperationException();
    }

    public boolean paint(Graphics2D g) {
        Primitive[] shapes = (Primitive[]) primitives.toArray(new Primitive[primitives.size()]);
        try {
            for (int i = 0; i < shapes.length; i++) {
//            System.err.println("PAINT " + shapes[i]);
                shapes[i].paint(g);
            }
        } catch (InternalError err) {
            /* e.g., unimplemented on Linux - custom composites cannot be
               drawn directly to the screen, which effects applied to vector
               layers will try to do
	at sun.java2d.xr.XRSurfaceData.getRaster(XRSurfaceData.java:72)
	at sun.java2d.pipe.GeneralCompositePipe.renderPathTile(GeneralCompositePipe.java:100)
	at sun.java2d.pipe.AAShapePipe.renderTiles(AAShapePipe.java:201)
	at sun.java2d.pipe.AAShapePipe.fillParallelogram(AAShapePipe.java:102)
	at sun.java2d.pipe.PixelToParallelogramConverter.drawGeneralLine(PixelToParallelogramConverter.java:287)
	at sun.java2d.pipe.PixelToParallelogramConverter.draw(PixelToParallelogramConverter.java:139)
	at sun.java2d.pipe.ValidatePipe.draw(ValidatePipe.java:154)
	at sun.java2d.SunGraphics2D.draw(SunGraphics2D.java:2497)
	at net.java.dev.imagine.api.vector.elements.Line.paint(Line.java:52)
            */
            err.printStackTrace();
            if (true || "not implemented yet".equals(err.getMessage())) {
                Dimension dim = getSize();
                BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g1 = img.createGraphics();
                try {
                    for (int i = 0; i < shapes.length; i++) {
//            System.err.println("PAINT " + shapes[i]);
                        shapes[i].paint(g1);
                    }
                    g.drawRenderedImage(img, null);
                } finally {
                    g1.dispose();
                    img.flush();
                }
            }
        }
        return shapes.length > 0;
    }

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
}
