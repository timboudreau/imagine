package org.imagine.vector.editor.ui;

import org.imagine.vector.editor.ui.undo.SingleShapeEdit;
import com.mastfrog.abstractions.Wrapper;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.snap.SnapPoints;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import org.imagine.awt.key.PaintKey;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.OnSnap;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.utils.Holder;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapeInfo;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.CSGOperation;
import org.imagine.vector.editor.ui.undo.ContentsEdit;
import org.imagine.vector.editor.ui.undo.GeometryEdit;
import org.imagine.vector.editor.ui.undo.UndoRedoHookable;

/**
 * Wraps the actual shape model and triggers repaints on modification and
 * handles some undo support.
 *
 * @author Tim Boudreau
 */
public final class RepaintProxyShapes implements ShapesCollection, Wrapper<Shapes> {

    private final Shapes shapes;
    private final RepaintHandle handle;

    public RepaintProxyShapes(Shapes shapes, RepaintHandle handle) {
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
    public boolean contains(ShapeElement el) {
        return shapes.contains(unwrap(el));
    }

    @Override
    public ShapeElement get(int index) {
        return wrap(shapes.get(index));
    }

    @Override
    public int indexOf(ShapeElement el) {
        return shapes.indexOf(unwrap(el));
    }

    @Override
    public int size() {
        return shapes.size();
    }

    @Override
    public void addToBounds(Rectangle2D b) {
        shapes.addToBounds(b);
    }

    @Override
    public UndoRedoHookable contentsEdit(String name, Runnable r) {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return ContentsEdit.addEdit(name, this, handle, () -> {
            r.run();
            addToBounds(bds);
            handle.repaintArea(bds);
        });
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

    @Override
    public List<? extends ShapeElement> shapesAtPoint(double x, double y) {
        List<? extends ShapeElement> l = shapes.shapesAtPoint(x, y);
        return wrap(l);
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

    @Override
    public Supplier<SnapPoints> snapPoints(double radius, OnSnap onSnap) {
        return shapes.snapPoints(radius, onSnap);
    }

    @Override
    public UndoRedoHookable geometryEdit(String name, Runnable r) {
        return GeometryEdit.createEditAdder(name, this, handle, adder -> {
            r.run();
            adder.run();
        });
    }

    @Override
    public void getBounds(Rectangle2D into) {
        shapes.getBounds(into);
    }

    @Override
    public boolean csg(CSGOperation op, ShapeElement lead, List<? extends ShapeElement> elements, BiConsumer<? super Set<? extends ShapeElement>, ? super ShapeElement> removedAndAdded) {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        lead.addToBounds(bds);
        for (ShapeElement se : elements) {
            se.addToBounds(bds);
        }
        SingleShapeEdit.includeContentsSnapshotInCurrentEdit();
        return shapes.csg(op, unwrap(lead), unwrap(elements), (rem, add) -> {
            if (add != null) {
                add.addToBounds(bds);
                handle.repaintArea(bds);
            }
            removedAndAdded.accept(wrap(rem), wrap(add));
        });
    }

    @Override
    public Shapes wrapped() {
        return shapes;
    }

    @Override
    public ShapeElement add(Shaped vect, Paint bg, Paint fg, BasicStroke stroke, PaintingStyle style) {
        return new WrapperShapeEntry(shapes.add(vect, bg, fg, stroke, style));
    }

    @Override
    public ShapeElement add(Shaped vect, PaintKey<?> bg, PaintKey<?> fg, BasicStroke stroke, PaintingStyle style) {
        return new WrapperShapeEntry(shapes.add(vect, bg, fg, stroke, style));
    }

    public ShapeElement add(Shaped vect, Paint bg, Paint fg, BasicStroke stroke, boolean draw, boolean fill) {
        return new WrapperShapeEntry(shapes.add(vect, bg, fg, stroke, draw, fill));
    }

    @Override
    public void addAll(ShapesCollection shapes, Shape clip) {
        shapes.addAll(shapes, clip);
    }

    @Override
    public ShapesCollection applyTransform(AffineTransform xform) {
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
    public Runnable contentsSnapshot() {
        Runnable r = shapes.contentsSnapshot();
        Rectangle bds = getBounds();
        return () -> {
            bds.add(getBounds());
            r.run();
            handle.repaintArea(bds);
        };
    }

    @Override
    public UndoRedoHookable edit(String name, ShapeElement el, Runnable editPerformer) {
        Rectangle oldBounds = el.getBounds();
        return SingleShapeEdit.maybeAddEdit(name, el, this, handle, (Runnable editAdder) -> {
            shapes.edit(name, unwrap(el), () -> {
                editPerformer.run();
                oldBounds.add(el.getBounds());
                handle.repaintArea(oldBounds);
                editAdder.run();
            });
        });
    }

    @Override
    public Rectangle getBounds() {
        return shapes.getBounds();
    }

    @Override
    public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds, Zoom zoom, AspectRatio ratio) {
        return shapes.paint(goal, g, bounds, zoom, ratio);
    }

    @Override
    public ShapesCollection snapshot() {
        return new RepaintProxyShapes(shapes.snapshot(), handle);
    }

    @Override
    public ShapeElement addForeign(ShapeElement element) {
        return new WrapperShapeEntry(shapes.addForeign(element));
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
        if (l == null || l.isEmpty()) {
            return Collections.emptyList();
        }
        List<WrapperShapeEntry> result = new ArrayList(l.size());
        for (ShapeElement sh : l) {
            result.add(wrap(sh));
        }
        return result;
    }

    private Set<WrapperShapeEntry> wrap(Set<? extends ShapeElement> l) {
        if (l == null || l.isEmpty()) {
            return Collections.emptySet();
        }
        Set<WrapperShapeEntry> result = new HashSet<>(l.size());
        for (ShapeElement sh : l) {
            result.add(wrap(sh));
        }
        return result;
    }

    @Override
    public Set<? extends ShapeElement> absent(Collection<? extends ShapeElement> from) {
        return wrap(shapes.absent(from));
    }

    private WrapperShapeEntry[] wrap(ShapeElement[] l) {
        if (l == null || l.length == 0) {
            return new WrapperShapeEntry[0];
        }
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
        public boolean setStroke(BasicStroke stroke) {
            return entry.setStroke(stroke);
        }

        @Override
        public boolean setStroke(BasicStrokeWrapper wrapper) {
            boolean result;
            EnhRectangle2D rect = new EnhRectangle2D();
            addToBounds(rect);
            if (result = entry.setStroke(wrapper)) {
                addToBounds(rect);
                handle.repaintArea(rect);
            }
            return result;
        }

        @Override
        public boolean isNameExplicitlySet() {
            return entry.isNameExplicitlySet();
        }

        @Override
        public ShapeInfo shapeInfo() {
            return entry.shapeInfo();
        }

        @Override
        public int getControlPointCount() {
            return entry.getControlPointCount();
        }

        @Override
        public void addToBounds(Rectangle2D r) {
            entry.addToBounds(r);
        }

        @Override
        public Runnable restorableSnapshot() {
            Runnable snap = entry.restorableSnapshot();
            return () -> {
                snap.run();
                handle.repaintArea(getBounds());
            };
        }

        @Override
        public Runnable geometrySnapshot() {
            return entry.geometrySnapshot();
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
        public ShapeControlPoint[] controlPoints(double size, Consumer<ShapeControlPoint> c) {
            Rectangle bds = getBounds();
            Holder<ShapeControlPoint[]> h = c == null ? null : Holder.create();
            ShapeControlPoint[] result = entry.controlPoints(size, (cp) -> {
                if (c != null) {
                    c.accept(h.get()[cp.index()]);
                }
                Rectangle nue = getBounds();
                bds.add(nue);
                handle.repaintArea(bds);
                bds.setFrame(nue);
            });
            if (c != null) {
                h.set(result);
            }
            // Shouldn't be needed, but having trouble with
            // Shape identity
            return WrapperControlPoint.wrap(result, this);
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
        public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle clip, AspectRatio ratio) {
            return entry.paint(goal, g, clip, ratio);
        }

        @Override
        public boolean isNameSet() {
            return entry.isNameSet();
        }

        @Override
        public void setName(String name) {
            entry.setName(name);
        }

        @Override
        public String getName() {
            return entry.getName();
        }

        @Override
        public void setShape(Shape shape) {
            wrapMutation(() -> {
                entry.setShape(shape);
            });
        }

        @Override
        public Paint fill(AspectRatio ratio) {
            return entry.fill(ratio);
        }

        @Override
        public Paint outline(AspectRatio ratio) {
            return entry.outline(ratio);
        }

        @Override
        public void translate(double x, double y) {
            wrapMutation(() -> {
                entry.translate(x, y);
            });
        }

        private void wrapMutation(Runnable mutation) {
            EnhRectangle2D r = new EnhRectangle2D();
            addToBounds(r);
            mutation.run();
            addToBounds(r);
            BasicStroke stroke = stroke();
            if (stroke != null) {
                r.x -= stroke.getLineWidth();
                r.y -= stroke.getLineWidth();
                r.width += stroke.getLineWidth() * 2;
                r.height += stroke.getLineWidth() * 2;
            }
            handle.repaintArea(r);
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
            wrapMutation(() -> {
                entry.setPaintingStyle(style);
            });
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
        public void setFill(PaintKey<?> fill) {
            wrapMutation(() -> {
                entry.setFill(fill);
            });
        }

        @Override
        public void setDraw(PaintKey<?> draw) {
            wrapMutation(() -> {
                entry.setDraw(draw);
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

        public PaintKey<?> getFillKey() {
            return entry.getFillKey();
        }

        @Override
        public Paint getDraw() {
            return entry.getDraw();
        }

        public PaintKey<?> getDrawKey() {
            return entry.getDrawKey();
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
                return ((ShapeEntry) o) == entry || ((ShapeEntry) o).equals(entry)
                        || ((ShapeEntry) o).id() == entry.id();
            } else if (o instanceof WrapperShapeEntry) {
                return ((WrapperShapeEntry) o).entry == entry
                        || ((WrapperShapeEntry) o).entry.equals(entry);
            } else if (o instanceof ShapeElement) {
                return ((ShapeElement) o).id() == entry.id();
//                ShapeEntry e = Wrapper.find(o, ShapeEntry.class);
//                if (e != null) {
//                    return e == entry || e.equals(entry);
//                }
            }
            return false;
        }
    }

    static class WrapperControlPoint implements ShapeControlPoint {

        private final ShapeControlPoint delegate;
        private final WrapperShapeEntry owner;
        private final ShapeControlPoint[] family;

        public WrapperControlPoint(ShapeControlPoint delegate, WrapperShapeEntry owner, ShapeControlPoint[] family) {
            this.delegate = delegate;
            this.owner = owner;
            this.family = family;
        }

        static ShapeControlPoint[] wrap(ShapeControlPoint[] originals, WrapperShapeEntry owner) {
            ShapeControlPoint[] result = new ShapeControlPoint[originals.length];
            for (int i = 0; i < originals.length; i++) {
                result[i] = new WrapperControlPoint(originals[i], owner, result);
            }
            return result;
        }

        @Override
        public ShapeElement owner() {
            return owner;
        }

        @Override
        public ShapeControlPoint[] family() {
            return family;
        }

        @Override
        public ControlPointKind kind() {
            return delegate.kind();
        }

        @Override
        public int index() {
            return delegate.index();
        }

        @Override
        public Adjustable getPrimitive() {
            return delegate.getPrimitive();
        }

        @Override
        public Point2D.Double location() {
            return delegate.location();
        }

        @Override
        public boolean isValid() {
            return delegate.isValid();
        }

        @Override
        public boolean move(double dx, double dy) {
            return delegate.move(dx, dy);
        }

        @Override
        public double getX() {
            return delegate.getX();
        }

        @Override
        public double getY() {
            return delegate.getY();
        }

        @Override
        public boolean set(double newX, double newY) {
            return delegate.set(newX, newY);
        }

        @Override
        public boolean delete() {
            return delegate.delete();
        }

        @Override
        public boolean canDelete() {
            return delegate.canDelete();
        }

        @Override
        public boolean isVirtual() {
            return delegate.isVirtual();
        }

        @Override
        public boolean hit(double hx, double hy) {
            return delegate.hit(hx, hy);
        }

        @Override
        public Set<ControlPointKind> availableControlPointKinds() {
            return delegate.availableControlPointKinds();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == delegate) {
                return true;
            } else if (o instanceof WrapperControlPoint) {
                return ((WrapperControlPoint) o).delegate
                        == delegate
                        || ((WrapperControlPoint) o).delegate.equals(delegate);
            } else if (o instanceof ShapeControlPoint) {
                ShapeControlPoint other = (ShapeControlPoint) o;
                return other.owner().id() == owner.id()
                        && other.index() == index();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return "W(" + delegate + ")";
        }

    }

}
