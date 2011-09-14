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
import java.util.Set;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import net.dev.java.imagine.api.selection.Selection.Op;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public final class ObjectSelection<T> extends Selection<Collection<T>> {

    private final Set<T> contents = new HashSet<T>();
    private final Class<T> elementType;
    private final Universe<Collection<T>> universe;
    private final ShapeConverter<T> converter;

    public ObjectSelection(Class<T> type, Universe<Collection<T>> universe, ShapeConverter<T> converter) {
        super(Collection.class);
        elementType = type;
        this.universe = universe;
        this.converter = converter;
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
        return result;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    @Override
    public void add(Collection<T> content, Op op) {
        Set<T> old = new HashSet<T>(contents);
        switch (op) {
            case ADD:
                contents.addAll(content);
                break;
            case SUBTRACT:
                contents.removeAll(content);
                break;
            case INTERSECT:
                contents.retainAll(content);
                break;
            case REPLACE:
                contents.clear();
                contents.addAll(content);
                break;
            case XOR:
                Set<T> nue = new HashSet<T>(content);
                Set<T> newContents = new HashSet<T>();
                nue.addAll(contents);
                for (Iterator<T> it = nue.iterator(); it.hasNext();) {
                    T t = it.next();
                    boolean contentContains = content.contains(t);
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
            changed(new ObjectsUndoableEdit(old, contents, op));
        }
    }

    @Override
    public void clear() {
        contents.clear();
    }

    @Override
    public void paint(Graphics2D g, Rectangle bounds) {
        Shape shape = asShape();
        if (shape != null) {
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
        contents.clear();
        addIntersectingObjects(a, contents);
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
        contents.clear();
    }

    @Override
    public void invert(Rectangle bds) {
        Set<T> nue = new HashSet<T>();
        for (T t : universe.getAll()) {
            if (!contents.contains(t)) {
                Shape s = converter.toShape(t);
                if (bds.contains(s.getBounds())) {
                    nue.add(t);
                }
            }
        }

    }

    @Override
    public boolean contains(Collection<T> what) {
        return what == null ? false : contents.containsAll(what);
    }

    public String toString() {
        return super.toString() + "[" + contents + "]";
    }

    class ObjectsUndoableEdit implements UndoableEdit {

        private final Set<T> old;
        private final Set<T> nue;
        private final Op op;

        public ObjectsUndoableEdit(Set<T> old, Set<T> nue, Op op) {
            this.old = new HashSet<T>(old);
            this.nue = new HashSet<T>(nue);
            this.op = op;
        }

        @Override
        public void undo() throws CannotUndoException {
            contents.clear();
            contents.addAll(old);
        }

        @Override
        public boolean canUndo() {
            return canRedo();
        }

        @Override
        public void redo() throws CannotRedoException {
            contents.clear();
            contents.addAll(nue);
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
