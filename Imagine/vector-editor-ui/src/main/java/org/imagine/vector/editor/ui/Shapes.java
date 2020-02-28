package org.imagine.vector.editor.ui;

import com.mastfrog.abstractions.Wrapper;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoint;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoints;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.painting.VectorWrapperGraphics;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.HitTester;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.tools.CSGOperation;
import org.imagine.vector.editor.ui.undo.AbstractShapeEdit;
import org.imagine.vector.editor.ui.undo.UndoRedoHookable;

/**
 * The underlying collection of shapes.
 *
 * @author Tim Boudreau
 */
public class Shapes implements HitTester, ShapesCollection {

    private final List<ShapeEntry> shapes;

    public Shapes(boolean log) {
        shapes = log ? new LoggingList(20) : new ArrayList<>(20);
    }

    public Shapes() {
        this(false);
    }

    public Shapes(Shapes orig) {
        this(false);
        for (ShapeEntry se : orig.shapes) {
            shapes.add(se.copy());
        }
    }

    @Override
    public ShapeElement get(int index) {
        return shapes.get(index);
    }

    @Override
    public int size() {
        return shapes.size();
    }

    @Override
    public void addToBounds(Rectangle2D b) {
        for (ShapeEntry se : shapes) {
            se.addToBounds(b);
        }
    }

    @Override
    public UndoRedoHookable geometryEdit(String name, Runnable r) {
        r.run();
        onChange();
        return AbstractShapeEdit.noOp();
    }

    @Override
    public void getBounds(Rectangle2D into) {
        for (ShapeEntry se : shapes) {
            se.vect.addToBounds(into);
        }
    }

    public Runnable contentsSnapshot() {
        List<ShapeEntry> l = new ArrayList<>(shapes);
        return () -> {
            shapes.clear();
            shapes.addAll(l);
            onChange();
        };
    }

    @Override
    public UndoRedoHookable contentsEdit(String name, Runnable r) {
        r.run();
        onChange();
        return AbstractShapeEdit.noOp();
    }

    public Iterator<ShapeElement> iterator() {
        return new NMI<>(shapes.iterator());
    }

    @Override
    public boolean toFront(ShapeElement en) {
        int ix = indexOf(en);
        if (ix < 0 || ix == shapes.size() - 1) {
            return false;
        } else {
            en = shapes.get(ix);
        }
        shapes.remove(ix);
        shapes.add((ShapeEntry) en);
        return true;
    }

    @Override
    public boolean toBack(ShapeElement en) {
        int ix = indexOf(en);
        if (ix <= 0) {
            return false;
        } else {
            en = shapes.get(ix);
        }
        shapes.remove(ix);
        shapes.add(0, (ShapeEntry) en);
        return true;
    }

    public boolean csg(CSGOperation op, ShapeElement lead, List<? extends ShapeElement> elements, BiConsumer<? super Set<? extends ShapeElement>, ? super ShapeElement> removedAndAdded) {
        List<Shape> shapes = new ArrayList<>();
        if (elements.isEmpty()) {
            System.out.println("nothing to csg");
            return false;
        }
        if (!contains(lead)) {
            System.out.println("lead shape not present");
            return false;
        }
        for (ShapeElement el : elements) {
            if (el == lead) {
                continue;
            }
            if (!contains(el)) {
                System.out.println("tried to csg not present item");
                return false;
            }
            shapes.add(el.shape());
        }
        if (shapes.isEmpty()) {
            System.out.println("nothing to csg 2");
            return false;
        }
        Shape leadShape = lead.shape();
        Shape nue = op.apply(leadShape, shapes.toArray(new Shape[shapes.size()]));
        Shaped shaped = VectorWrapperGraphics.primitiveFor(nue, true);
        replaceShape(lead, shaped);
        Set<ShapeElement> s = new HashSet<>(elements);
        for (ShapeElement s1 : s) {
            if (s1 != lead) {
                int ix = indexOf(s1);
                if (ix >= 0) {
                    this.shapes.remove(ix);
                }
            }
        }
        removedAndAdded.accept(s, lead);
        return true;
    }

    @Override
    public Set<? extends ShapeElement> absent(Collection<? extends ShapeElement> from) {
        Set<? extends ShapeElement> all = new LinkedHashSet<>(shapes);
        all.removeAll(from);
        return all;
    }

    public boolean contains(ShapeElement el) {
        return shapes.contains(el) || indexOf(el) >= 0;
    }

    public List<? extends ShapeElement> possiblyOverlapping(ShapeElement el) {
        Rectangle2D bds = new Rectangle2D.Double();
        el.addToBounds(bds);
        List<ShapeElement> result = new ArrayList<>(7);
        for (ShapeEntry e : shapes) {
            if (e == el) {
                continue;
            }
            if (e.getBounds().intersects(bds)) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
    public List<? extends ShapeElement> shapesAtPoint(double x, double y) {
        List<ShapeEntry> result = new ArrayList<>(shapes.size() / 2);
        for (ShapeEntry e : shapes) {
            if (e.shape().contains(x, y)) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
    public boolean deleteShape(ShapeElement entry) {
        if (!(entry instanceof ShapeEntry)) {
            entry = Wrapper.find(entry, ShapeEntry.class);
        }
        int ix = indexOf(entry);
        if (ix < 0) {
            System.out.println("Shape entry not present");
            return false;
        }
        shapes.remove(ix);
        return true;
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public ShapeEntry[] replaceShape(ShapeElement entry, Shaped... replacements) {
        int ix = indexOf(entry);
        if (ix < 0) {
            System.out.println("Shape entry not present");
            return new ShapeEntry[0];
        }
        entry = shapes.get(ix);
        if (replacements.length == 0) {
            shapes.remove(entry);
            return new ShapeEntry[0];
        }
        entry.setShape(replacements[0]);
        List<ShapeEntry> nue = new ArrayList<>(replacements.length);
        for (int i = 1; i < replacements.length; i++) {
            ShapeEntry copy = (ShapeEntry) entry.duplicate();
            copy.setShape(replacements[i]);
            shapes.add(ix + 1, copy);
            nue.add(copy);
        }
        return nue.toArray(new ShapeEntry[nue.size()]);
    }

    @Override
    public Shapes applyTransform(AffineTransform xform) {
        Shapes result = snapshot();
        for (ShapeEntry en : shapes) {
            en.applyTransform(xform);
        }
        return result;
    }

    @Override
    public boolean canApplyTransform(AffineTransform xform) {
        if (shapes.isEmpty()) {
            return false;
        }
        if (xform.getDeterminant() < 0.01) {
            return false;
        }
        boolean result = false;
        if (result) {
            for (ShapeEntry se : shapes) {
                if (se.canApplyTransform(xform)) {
                    result = true;
                }
            }
        }
        return result;
    }

    @Override
    public ShapeEntry add(Shaped vect, Paint bg, Paint fg, BasicStroke stroke, boolean draw, boolean fill) {
//        System.out.println("ADD TO SHAPES " + vect + " " + bg + " " + fg + " " + fill + " " + draw);
        if (bg == null && fg == null) {
            bg = Color.BLACK;
            fg = Color.BLACK;
        }

        // XXX think this coalescing is now done elsewhere
        ShapeEntry prev = shapes.isEmpty() ? null : shapes.get(0);
        if (prev != null) {
            if (vect.equals(prev.vect)) {
                if (prev.isFill() && !fill) {
                    prev.setOutline(fg);
                    return prev;
                }
            }
        }
        ShapeEntry se = new ShapeEntry(vect, bg, fg, stroke, draw, fill);
        shapes.add(0, se);
        pts = null;
        return se;
    }

    @Override
    public Shapes clip(Shape shape) {
        Shapes nue = new Shapes();
        Rectangle2D shapeBounds = shape.getBounds2D();
        for (ShapeEntry e : shapes) {
            Shape otherShape = e.vect.toShape();
            Rectangle2D otherBounds = otherShape.getBounds2D();
            if (shapeBounds.contains(otherBounds)) {
                nue.shapes.add(e.copy());
            } else if (otherShape.intersects(otherBounds)) {
                Area area = new Area(otherShape);
                area.intersect(new Area(shape));
                // XXX replace the shape
                ShapeEntry n = e.copy();
                n.vect = new PathIteratorWrapper(area.getPathIterator(null), e.isFill());
                nue.shapes.add(n);
            }
        }
        return nue;
    }

    private ShapeEntry toShapeEntryOrCopy(ShapeElement el) {
        if (el instanceof ShapeEntry) {
            return (ShapeEntry) el;
        }
        ShapeEntry e = Wrapper.find(el, ShapeEntry.class);
        if (e != null) {
            return e;
        }
        ShapeEntry en = new ShapeEntry(el.item(), el.fill(),
                el.outline(), el.stroke(), el.isFill(), el.isDraw());
        return en;
    }

    @Override
    public void addAll(ShapesCollection shapes, Shape clip) {
        if (shapes == this) {
            throw new IllegalArgumentException("Adding to self");
        }
        if (clip != null) {
            shapes = shapes.clip(clip);
            for (ShapeElement e : shapes) {
                this.shapes.add(toShapeEntryOrCopy(e));
            }
        } else {
            for (ShapeElement e : shapes) {
                this.shapes.add(toShapeEntryOrCopy(e.duplicate()));
            }
        }
    }

    private boolean isChanged(ShapeEntry a, ShapeEntry b) {
        return !Objects.equals(a, b);
    }

    public int indexOf(ShapeElement el) {
        int ix = shapes.indexOf(el);
        if (ix < 0) {
            ShapeElement el2 = Wrapper.find(el, ShapeEntry.class);
            if (el2 != null) {
                ix = shapes.indexOf(el2);
                if (ix >= 0) {
                    return ix;
                }
            }
            if (el2 != null) {
                el = el2;
            }
            for (int i = 0; i < shapes.size(); i++) {
                ShapeEntry e = shapes.get(i);
                if (e.id() == el.id()) {
                    return i;
                }
            }
            for (int i = 0; i < shapes.size(); i++) {
                ShapeEntry e = shapes.get(i);
                if (e.vect == el.item()) {
                    return i;
                }
            }
            for (int i = 0; i < shapes.size(); i++) {
                ShapeEntry e = shapes.get(i);
                if (e.vect.equals(el.item())) {
                    return i;
                }
            }
        }
        return ix;
    }

    @Override
    public ShapeElement duplicate(ShapeElement el) {
        int ix = indexOf(el);
        if (ix < 0) {
            throw new IllegalArgumentException("Not present "
                    + el + " in " + shapes);
        } else {
            ShapeEntry en = shapes.get(ix);
            ShapeEntry dup = en.duplicate();
            shapes.add(ix, dup);
            onChange();
            return dup;
        }
    }

    @Override
    public UndoRedoHookable edit(String name, ShapeElement el, Runnable r) {
        int ix = indexOf(el);
        if (ix < 0) {
            throw new IllegalArgumentException("Not present: " + el
                    + " in " + shapes);
        } else {
            el = shapes.get(ix);
        }
        r.run();
        onChange();
        return AbstractShapeEdit.noOp();
    }

    @Override
    public Shapes snapshot() {
        Shapes result = new Shapes(false);
        for (ShapeEntry e : shapes) {
            result.shapes.add(e.copy());
        }
        return result;
    }

    public Rectangle restore(Shapes snapshot) {
        Rectangle bds = getBounds();

        shapes.clear();
        shapes.addAll(snapshot.shapes);

        bds.add(getBounds());
        return bds;
    }

    public int hits(Point2D pt, Consumer<? super ShapeElement> c) {
        int count = 0;
        for (ShapeEntry se : shapes) {
            if (se.contains(pt)) {
                c.accept(se);
                count++;
            }
        }
        return count;
    }

    @Override
    public void hibernate() {
        shapes.forEach(ShapeEntry::hibernate);
    }

    @Override
    public void wakeup(boolean immediately, Runnable notify) {
        for (ShapeEntry sh : shapes) {
            sh.wakeup(immediately, null);
        }
    }

    @Override
    public boolean paint(Graphics2D g, Rectangle bounds) {
        GraphicsUtils.setHighQualityRenderingHints(g);
        int max = shapes.size() - 1;
        boolean result = false;
        for (int i = max; i >= 0; i--) {
            ShapeEntry se = shapes.get(i);
            result |= se.paint(g, bounds);
        }
        return result;
    }

    @Override
    public Rectangle getBounds() {
        Rectangle2D.Double result = new Rectangle.Double();
        for (ShapeEntry se : shapes) {
            se.addToBounds(result);
        }
        return result.getBounds();
    }

    void onChange() {
        pts = null;
    }

    private SnapPoints pts;

    public Supplier<SnapPoints> snapPoints(double radius, BiConsumer<SnapPoint, SnapPoint> onSnap) {
        return () -> {
            SnapPoints result = pts;
            if (result != null) {
                return result;
            }
            SnapPoints.Builder b = SnapPoints.builder(radius);
            for (ShapeEntry se : shapes) {
                se.addTo(b);
            }
            return pts = b
                    .notifying(onSnap)
                    .build();
        };
    }

    public Supplier<SnapPoints> snapPoints(double radius) {
        return () -> {
            SnapPoints result = pts;
            if (result != null) {
                return result;
            }
            SnapPoints.Builder b = SnapPoints.builder(radius);
            for (ShapeEntry se : shapes) {
                se.addTo(b);
            }
            return pts = b.build();
        };
    }

    static class NMI<T> implements Iterator<T> {

        private final Iterator<? extends T> iter;

        public NMI(Iterator<? extends T> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public T next() {
            return iter.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class LoggingList extends ArrayList<ShapeEntry> {

        LoggingList() {

        }

        LoggingList(int size) {
            super(size);
        }

        @Override
        public boolean add(ShapeEntry e) {
            System.out.println("ADD " + e.id() + " - " + System.identityHashCode(e) + " - " + e);
            return super.add(e);
        }

        @Override
        public boolean remove(Object o) {
            ShapeElement se = (ShapeElement) o;
            System.out.println("REM by IDENT " + se.id() + " - " + System.identityHashCode(o) + " - " + se);
            return super.remove(o);
        }

        @Override
        public ShapeEntry remove(int index) {
            ShapeEntry se = super.remove(index);
            System.out.println("REM by IX " + se.id() + " - " + System.identityHashCode(se) + " - " + se);
            return se;
        }
    }
}
