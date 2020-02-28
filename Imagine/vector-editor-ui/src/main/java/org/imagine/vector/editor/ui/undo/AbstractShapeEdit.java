package org.imagine.vector.editor.ui.undo;

import java.util.LinkedList;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AbstractShapeEdit implements UndoableEdit, UndoRedoHookable {

    private final String name;
    private List<Runnable> hooks;

    public static AbstractShapeEdit noOp() {
        return DUMMY_EDIT;
    }

    protected AbstractShapeEdit(String name) {
        this.name = name;
    }

    public void hook(Runnable r) {
        if (hooks == null) {
            hooks = new LinkedList<>();
        }
        hooks.add(r);
    }

    @Override
    public final void undo() throws CannotUndoException {
        if (!canUndo()) {
            throw new CannotUndoException();
        }
        onBeforeUndo();
        undoImpl();
        if (hooks != null) {
            for (Runnable r : hooks) {
                r.run();
            }
        }
    }

    @Override
    public final void redo() throws CannotRedoException {
        if (!canRedo()) {
            throw new CannotRedoException();
        }
        onBeforeRedo();
        redoImpl();
        if (hooks != null) {
            for (Runnable r : hooks) {
                r.run();
            }
        }
    }

    protected void onBeforeUndo() {

    }

    protected void onBeforeRedo() {
    }

    protected abstract void undoImpl();

    protected abstract void redoImpl();

    @Override
    public final boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    @Override
    public final boolean replaceEdit(UndoableEdit anEdit) {
        return false;
    }

    @Override
    public final boolean isSignificant() {
        return true;
    }

    @Override
    public final String getPresentationName() {
        return name;
    }

    @Override
    public final String getUndoPresentationName() {
        return getPresentationName();
    }

    @Override
    public final String getRedoPresentationName() {
        return getPresentationName();
    }

    static AbstractShapeEdit DUMMY_EDIT = new AbstractShapeEdit("x") {
        @Override
        protected void undoImpl() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected void redoImpl() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean canUndo() {
            return false;
        }

        @Override
        public boolean canRedo() {
            return false;
        }

        @Override
        public void die() {
            // do nothing
        }

        @Override
        public void hook(Runnable r) {
            // do nothing
        }
    };

}
