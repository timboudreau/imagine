/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.api.selection;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import net.dev.java.imagine.api.selection.Selection.Op;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Tim Boudreau
 */
public final class ObjectSelection<T> extends Selection<Collection<T>> implements Lookup.Provider {

    private final Set<T> contents = new LinkedHashSet<>();
    private final Class<T> elementType;
    private final Universe<Collection<T>> universe;
    private final ShapeConverter<T> converter;
    private final InstanceContent content = new InstanceContent();
    private final AbstractLookup lookup = new AbstractLookup(content);

    public ObjectSelection(Class<T> type, Universe<Collection<T>> universe, ShapeConverter<T> converter) {
        super(Collection.class);
        elementType = type;
        this.universe = universe;
        this.converter = converter;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public boolean containsObject(Object o) {
        return contents.contains(o);
    }

    ShapeConverter<T> converter() {
        return converter;
    }

    public Class<T> elementType() {
        return elementType;
    }

    @Override
    public Collection<T> get() {
        return Collections.unmodifiableSet(contents);
    }

    public ObjectSelection<T> clone() {
        ObjectSelection<T> result = new ObjectSelection<T>(elementType, universe, converter);
        result.contents.addAll(contents);
        for (T obj : contents) {
            result.content.add(obj);
        }
        result.storedShape = storedShape;

        return result;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    @Override
    public void add(Collection<T> toAdd, Op op) {
        Set<T> old = new HashSet<>(contents);
        switch (op) {
            case ADD:
                contents.addAll(toAdd);
                break;
            case SUBTRACT:
                contents.removeAll(toAdd);
                break;
            case INTERSECT:
                contents.retainAll(toAdd);
                break;
            case REPLACE:
                contents.clear();
                contents.addAll(toAdd);
                break;
            case XOR:
                Set<T> nue = new HashSet<>(toAdd);
                Set<T> newContents = new HashSet<>();
                nue.addAll(contents);
                for (Iterator<T> it = nue.iterator(); it.hasNext();) {
                    T t = it.next();
                    boolean contentContains = toAdd.contains(t);
                    boolean oldContains = contents.contains(t);
                    if (contentContains != oldContains) {
                        newContents.add(t);
                    }
                }
                contents.clear();
                contents.addAll(newContents);
                break;
        }
        if (!old.equals(contents)) {
            changed(new ObjectsUndoableEdit(old, contents, op, storedShape));
            updateContents(old, contents);
        }
    }

    void diff(Set<T> old, Set<T> nue, BiConsumer<Set<T>, Set<T>> receiver) {
        Set<T> added = new HashSet<>(nue);
        added.removeAll(old);
        Set<T> removed = new HashSet<>(old);
        removed.retainAll(nue);
        receiver.accept(removed, added);
    }

    @Override
    public void clear() {
        clearContents();
    }

    @Override
    public void paint(Graphics2D g, Rectangle bounds) {
        Shape shape = asShape();
        if (shape != null && shape.intersects(bounds)) {
            paintSelectionAsShape(g, shape, bounds);
        }
    }
    private Shape storedShape;

    Shape getStoredShape() {
        return storedShape;
    }

    protected void onChange() {
        storedShape = null;
    }

    @Override
    public void translateFrom(Selection selection) {
        Shape s = null;
        if (selection instanceof ObjectSelection<?> && ((ObjectSelection<?>) selection).getStoredShape() != null) {
            storedShape = ((ObjectSelection<?>) selection).getStoredShape();
        }
        if (s == null) {
            s = selection.asShape();
        }
        storedShape = s;
        Area a = toArea(s);
        clearContents();
        addIntersectingObjects(a, contents);
    }

    private void clearContents() {
        for (T obj : contents) {
            content.remove(obj);
        }
        contents.clear();
    }

    private void addIntersectingObjects(Area a, Collection<T> addTo) {
        if (a != null) {
            for (T t : universe.getAll()) {
                Shape s1 = converter.toShape(t);
                Area a1 = toArea(s1);
                Area a3 = (Area) a.clone();
                a3.intersect(a1);
                Rectangle b = a3.getBounds();
                if (b.width + b.height > 0) {
                    addTo.add(t);
                }
            }
        }
    }

    @Override
    public void addShape(Shape shape, Op op) {
        Set<T> objs = new HashSet<T>();
        addIntersectingObjects(toArea(shape), objs);
        add(objs, op);
    }

    private Area toArea(Shape s) {
        return s == null ? null : s instanceof Area ? (Area) s : new Area(s);
    }

    @Override
    public Shape asShape() {
        if (contents.isEmpty()) {
            return null;
        } else if (contents.size() == 1) {
            return converter.toShape(contents.iterator().next());
        } else {
            Area a = new Area();
            for (T t : contents) {
                a.intersect(toArea(converter.toShape(t)));
            }
            return a;
        }
    }

    @Override
    public void clearNoUndo() {
        clearContents();
    }

    @Override
    public void invert(Rectangle bds) {
        Set<T> nue = new HashSet<>();
        Area area = new Area(bds);
        if (storedShape != null) {
            area.subtract(new Area(storedShape));
        }
        for (T t : universe.getAll()) {
            if (!contents.contains(t)) {
                Shape s = converter.toShape(t);
                if (s != null) {
                    area.subtract(new Area(s));
                }
                if (bds.contains(s.getBounds())) {
                    nue.add(t);
                }
            }
        }
        if (!nue.equals(contents)) {
            Set<T> old = new HashSet<>(contents);
            updateContents(contents, nue);
            ObjectsUndoableEdit ed = new ObjectsUndoableEdit(old, nue, Op.INVERT, storedShape);
            changed(ed);
            storedShape = null;
        }
    }

    @Override
    public boolean contains(Collection<T> what) {
        return what == null ? false : contents.containsAll(what);
    }

    @Override
    public String toString() {
        return super.toString() + "[" + contents + "]";
    }

    private void updateContents(Set<T> old, Set<T> nue) {
        diff(old, nue, (removed, added) -> {
            for (T rem : removed) {
                content.remove(rem);
                if (nue != contents) {
                    contents.remove(rem);
                }
            }
            for (T add : added) {
                if (nue != contents) {
                    contents.add(add);
                }
                content.add(add);
            }
        });
    }

    class ObjectsUndoableEdit implements UndoableEdit {

        private final Set<T> old;
        private final Set<T> nue;
        private final Op op;
        private final Shape shape;
        private Shape otherShape;

        public ObjectsUndoableEdit(Set<T> old, Set<T> nue, Op op, Shape shape) {
            this.old = new HashSet<T>(old);
            this.nue = new HashSet<T>(nue);
            this.op = op;
            this.shape = shape;
        }

        @Override
        public void undo() throws CannotUndoException {
            otherShape = storedShape;
            updateContents(contents, old);
            storedShape = shape;
        }

        @Override
        public boolean canUndo() {
            return canRedo();
        }

        @Override
        public void redo() throws CannotRedoException {
            updateContents(contents, nue);
            storedShape = otherShape;
        }

        @Override
        public boolean canRedo() {
            return !old.isEmpty() || !nue.isEmpty();
        }

        @Override
        public void die() {
            old.clear();
            nue.clear();
        }

        @Override
        public boolean addEdit(UndoableEdit anEdit) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean replaceEdit(UndoableEdit anEdit) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        public String getPresentationName() {
            if (op == null) {
                return NbBundle.getMessage(ShapeSelection.class,
                        "CLEAR_EDIT_NAME"); //NOI18N
            }
            return NbBundle.getMessage(ShapeSelection.class, "SEL_EDIT_NAME", //NOI18N
                    op.toString());
        }

        public String getUndoPresentationName() {
            if (op == null) {
                return NbBundle.getMessage(ShapeSelection.class,
                        "CLEAR_UNDO_NAME"); //NOI18N
            }
            return NbBundle.getMessage(ShapeSelection.class, "UNDO_NAME", //NOI18N
                    op.toString());
        }

        public String getRedoPresentationName() {
            if (op == null) {
                return NbBundle.getMessage(ShapeSelection.class,
                        "CLEAR_REDO_NAME"); //NOI18N
            }
            return NbBundle.getMessage(ShapeSelection.class, "REDO_NAME", //NOI18N
                    op.toString());
        }
    }
}
