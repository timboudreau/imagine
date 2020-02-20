/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import com.mastfrog.abstractions.Wrapper;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.utils.ConvertList.Converter;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.CSGOperation;

/**
 * Wraps the actual shape model and triggers repaints on modification and
 * handles some undo support.
 *
 * @author Tim Boudreau
 */
final class RepaintProxyShapes implements ShapesCollection, Wrapper<Shapes> {

    private final Shapes shapes;
    private final RepaintHandle handle;

    RepaintProxyShapes(Shapes shapes, RepaintHandle handle) {
        this.shapes = shapes;
        this.handle = handle;
    }

    @Override
    public ShapeElement[] replaceShape(ShapeElement entry, Shaped... replacements) {
        entry = unwrap(entry);
        Rectangle r = entry.getBounds();
        for (Shaped s : replacements) {
            r.add(s.getBounds());
        }
        ShapeElement[] nue = shapes.replaceShape(entry, replacements);
        ShapeElement[] result = new ShapeElement[nue.length];
        handle.repaintArea(r);
        return wrap(result);
    }

    @Override
    public ShapeElement duplicate(ShapeElement el) {
        return wrap(shapes.duplicate(el));
    }

    @Override
    public boolean deleteShape(ShapeElement entry) {
        entry = unwrap(entry);
        Rectangle r = entry.getBounds();
        if (shapes.deleteShape(entry)) {
            handle.repaintArea(r);
            return true;
        }
        return false;
    }

    Converter<WrapperShapeEntry, ShapeEntry> converter = new Converter<WrapperShapeEntry, ShapeEntry>() {
        @Override
        public WrapperShapeEntry convert(ShapeEntry r) {
            return new WrapperShapeEntry(r);
        }

        @Override
        public ShapeEntry unconvert(WrapperShapeEntry t) {
            return t.entry;
        }
    };

    @Override
    public List<ShapeElement> shapesAtPoint(double x, double y) {
        List<? extends ShapeElement> l = shapes.shapesAtPoint(x, y);
        if (l.isEmpty()) {
            return Collections.emptyList();
        }
        List<ShapeElement> result = new ArrayList<>(l.size());
        for (ShapeElement e : l) {
            result.add(new WrapperShapeEntry((ShapeEntry) e));
        }
        return result;
    }

    private ShapeEntry unwrap(ShapeElement el) {
        if (el instanceof ShapeEntry) {
            return (ShapeEntry) el;
        }
        if (el instanceof WrapperShapeEntry) {
            return ((WrapperShapeEntry) el).entry;
        }
        return Wrapper.find(el, ShapeEntry.class);
    }

    private List<ShapeEntry> unwrap(List<? extends ShapeElement> all) {
        List<ShapeEntry> result = new ArrayList<>();
        for (ShapeElement el : all) {
            ShapeEntry un = unwrap(el);
            assert un != null : "Unknown in " + all;
            result.add(un);
        }
        return result;
    }

    private ShapeEntry[] unwrap(ShapeElement[] els) {
        ShapeEntry[] result = new ShapeEntry[els.length];
        for (int i = 0; i < els.length; i++) {
            ShapeEntry en = unwrap(els[i]);
            assert en != null : "Unknown at  " + i + ": " + els[i];
            result[i] = en;
        }
        return result;
    }

    @Override
    public boolean csg(CSGOperation op, ShapeElement lead, List<? extends ShapeElement> elements, BiConsumer<Set<ShapeElement>, ShapeElement> removedAndAdded) {
        Rectangle bds = new Rectangle(lead.getBounds());
        for (ShapeElement se : elements) {
            bds.add(se.getBounds());
        }
        return shapes.csg(op, unwrap(lead), unwrap(elements), (rem, add) -> {
            if (add != null) {
                bds.add(add.getBounds());
                handle.repaintArea(bds);
            }
        });
    }

    @Override
    public Shapes wrapped() {
        return shapes;
    }

    @Override
    public ShapeElement add(Shaped vect, Paint bg, Paint fg, BasicStroke stroke, boolean draw, boolean fill) {
        return new WrapperShapeEntry(shapes.add(vect, bg, fg, stroke, draw, fill));
    }

    @Override
    public void addAll(ShapesCollection shapes, Shape clip) {
        shapes.addAll(shapes, clip);
    }

    @Override
    public ShapesCollection applyTransform(AffineTransform xform) {
        // XXX undo support
        shapes.applyTransform(xform);
        return this;
    }

    @Override
    public boolean canApplyTransform(AffineTransform xform) {
        return shapes.canApplyTransform(xform);
    }

    @Override
    public ShapesCollection clip(Shape shape) {
        return new RepaintProxyShapes(shapes.clip(shape), handle);
    }

    @Override
    public void edit(String name, ShapeElement el, Runnable r) {
        Rectangle oldBounds = el.getBounds();
        shapes.edit(name, unwrap(el), () -> {
            r.run();
            oldBounds.add(el.getBounds());
            handle.repaintArea(oldBounds);
        });
    }

    @Override
    public Rectangle getBounds() {
        return shapes.getBounds();
    }

    @Override
    public boolean paint(Graphics2D g, Rectangle bounds) {
        return shapes.paint(g, bounds);
    }

    @Override
    public ShapesCollection snapshot() {
        return new RepaintProxyShapes(shapes.snapshot(), handle);
    }

    @Override
    public void hibernate() {
        shapes.hibernate();
    }

    @Override
    public void wakeup(boolean immediately, Runnable notify) {
        shapes.wakeup(immediately, notify);
    }

    @Override
    public Iterator<ShapeElement> iterator() {
        return new It(shapes.iterator());
    }

    // XXX add calls to UNWRAP on passed objects
    // or everything will be broken
    @Override
    public boolean toBack(ShapeElement en) {
        ShapeEntry e = unwrap(en);
        if (shapes.toBack(e)) {
            Rectangle r = e.getBounds();
            for (ShapeElement se : possiblyOverlapping(e)) {
                r.add(se.getBounds());
            }
            handle.repaintArea(r);
            return true;
        }
        return false;
    }

    @Override
    public boolean toFront(ShapeElement en) {
        ShapeEntry e = unwrap(en);
        if (shapes.toFront(e)) {
            Rectangle r = e.getBounds();
            for (ShapeElement se : possiblyOverlapping(e)) {
                r.add(se.getBounds());
            }
            handle.repaintArea(r);
            return true;
        }
        return false;
    }

    private List<WrapperShapeEntry> wrap(List<? extends ShapeElement> l) {
        List<WrapperShapeEntry> result = new ArrayList(l.size());
        for (ShapeElement sh : l) {
            result.add(wrap(sh));
        }
        return result;
    }

    private WrapperShapeEntry[] wrap(ShapeElement[] l) {
        WrapperShapeEntry[] result = new WrapperShapeEntry[l.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = wrap(unwrap(l[i]));
        }
        return result;
    }

    private WrapperShapeEntry wrap(ShapeElement el) {
        if (el instanceof WrapperShapeEntry) {
            return (WrapperShapeEntry) el;
        }
        return new WrapperShapeEntry(unwrap(el));
    }

    @Override
    public List<? extends ShapeElement> possiblyOverlapping(ShapeElement el) {
        return wrap(shapes.possiblyOverlapping(unwrap(el)));
    }

    class It implements Iterator<ShapeElement> {

        private final Iterator<ShapeElement> orig;

        public It(Iterator<ShapeElement> orig) {
            this.orig = orig;
        }

        @Override
        public boolean hasNext() {
            return orig.hasNext();
        }

        @Override
        public ShapeElement next() {
            return new WrapperShapeEntry((ShapeEntry) orig.next());
        }

    }

    class WrapperShapeEntry implements ShapeElement, Wrapper<ShapeEntry> {

        private final ShapeEntry entry;

        public WrapperShapeEntry(ShapeEntry entry) {
            this.entry = entry;
        }

        @Override
        public String toString() {
            return "Wrap(" + entry.toString() + ")";
        }

        @Override
        public ShapeElement duplicate() {
            return new WrapperShapeEntry(entry.duplicate());
        }

        public long id() {
            return entry.id();
        }

        @Override
        public ShapeElement copy() {
            return new WrapperShapeEntry(entry.copy());
        }

        @Override
        public Rectangle getBounds() {
            return entry.getBounds();
        }

        @Override
        public boolean isDraw() {
            return entry.isDraw();
        }

        @Override
        public boolean isFill() {
            return entry.isFill();
        }

        @Override
        public boolean isPaths() {
            return entry.isPaths();
        }

        @Override
        public Shaped item() {
            return entry.item();
        }

        @Override
        public Shape shape() {
            return entry.shape();
        }

        @Override
        public BasicStroke stroke() {
            return entry.stroke();
        }

        @Override
        public void toPaths() {
            entry.toPaths();
        }

        @Override
        public ControlPoint[] controlPoints(double size, Consumer<ControlPoint> c) {
            Rectangle bds = getBounds();
            return entry.controlPoints(size, (cp) -> {
                c.accept(cp);
                Rectangle nue = getBounds();
                bds.add(nue);
                handle.repaintArea(bds);
                bds.setFrame(nue);
            });
        }

        @Override
        public void changed() {
            Rectangle r = getBounds();
            entry.changed();
            r.add(getBounds());
            handle.repaintArea(r);
        }

        @Override
        public void applyTransform(AffineTransform xform) {
            wrapMutation(() -> {
                entry.applyTransform(xform);
            });
        }

        @Override
        public boolean canApplyTransform(AffineTransform xform) {
            return entry.canApplyTransform(xform);
        }

        @Override
        public boolean paint(Graphics2D g, Rectangle clip) {
            return entry.paint(g, clip);
        }

        @Override
        public Paint fill() {
            return entry.fill();
        }

        @Override
        public Paint outline() {
            return entry.outline();
        }

        @Override
        public void translate(double x, double y) {
            wrapMutation(() -> {
                entry.translate(x, y);
            });
        }

        private void wrapMutation(Runnable mutation) {
            Rectangle bds = entry.getBounds();
            mutation.run();
            bds.add(entry.getBounds());
            handle.repaintArea(bds);
        }

        @Override
        public DoubleConsumer wrap(DoubleConsumer c) {
            return ShapeElement.super.wrap(v -> {
                wrapMutation(() -> {
                    c.accept(v);
                });
            });
        }

        @Override
        public void setPaintingStyle(PaintingStyle style) {
            Rectangle bds = getBounds();
            entry.setPaintingStyle(style);
            handle.repaintArea(bds);
        }

        @Override
        public PaintingStyle getPaintingStyle() {
            return entry.getPaintingStyle();
        }

        @Override
        public void setFill(Paint fill) {
            wrapMutation(() -> {
                entry.setFill(fill);
            });
        }

        @Override
        public void setDraw(Paint draw) {
            wrapMutation(() -> {
                entry.setDraw(draw);
            });
        }

        @Override
        public Paint getFill() {
            return entry.getFill();
        }

        @Override
        public Paint getDraw() {
            return entry.getDraw();
        }

        @Override
        public void setShape(Shaped shape) {
            wrapMutation(() -> {
                entry.setShape(shape);
            });
        }

        @Override
        public ShapeEntry wrapped() {
            return entry;
        }

        @Override
        public int hashCode() {
            return entry.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o == this) {
                return true;
            } else if (o instanceof ShapeEntry) {
                return ((ShapeEntry) o) == entry || ((ShapeEntry) o).equals(entry);
            } else if (o instanceof WrapperShapeEntry) {
                return ((WrapperShapeEntry) o).entry == entry
                        || ((WrapperShapeEntry) o).entry.equals(entry);
            } else if (o instanceof ShapeElement) {
                ShapeEntry e = Wrapper.find(o, ShapeEntry.class);
                if (e != null) {
                    return e == entry || e.equals(entry);
                }
            }
            return false;
        }
    }
}
