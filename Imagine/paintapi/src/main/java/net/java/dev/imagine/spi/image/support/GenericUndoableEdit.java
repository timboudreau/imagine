package net.java.dev.imagine.spi.image.support;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 *
 * @author Tim Boudreau
 */
final class GenericUndoableEdit implements UndoableEdit {

    private final String name;

    private Runnable undo;
    private Runnable redo;

    public GenericUndoableEdit(String name, Runnable undo, Runnable redo) {
        this.name = name;
        this.undo = undo;
        this.redo = redo;
    }

    @Override
    public void undo() throws CannotUndoException {
        if (undo == null) {
            throw new CannotUndoException();
        }
    }

    @Override
    public boolean canUndo() {
        return undo != null;
    }

    @Override
    public void redo() throws CannotRedoException {
        if (redo == null) {
            throw new CannotRedoException();
        }
        redo.run();
    }

    @Override
    public boolean canRedo() {
        return redo != null;
    }

    @Override
    public void die() {
        undo = null;
        redo = null;
    }

    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    @Override
    public boolean replaceEdit(UndoableEdit anEdit) {
        return false;
    }

    @Override
    public boolean isSignificant() {
        return true;
    }

    @Override
    public String getPresentationName() {
        return name;
    }

    @Override
    public String getUndoPresentationName() {
        return getPresentationName();
    }

    @Override
    public String getRedoPresentationName() {
        return getPresentationName();
    }
}
