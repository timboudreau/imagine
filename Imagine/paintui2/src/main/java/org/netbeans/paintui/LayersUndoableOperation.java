/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paintui;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
class LayersUndoableOperation implements UndoableEdit {
    private LayersState state;
    private boolean deepCopy;
    LayersState redoState = null;
    String opName = NbBundle.getMessage(PictureScene.PI.class, "LBL_UNKNOWN_UNDOABLE_OP");

    LayersUndoableOperation(LayersState state, boolean deepCopy) {
        this.state = new LayersState(state, deepCopy);
        this.deepCopy = deepCopy;
    }

    boolean isDeepCopy() {
        return deepCopy;
    }

    public void undo() throws CannotUndoException {
        redoState = new LayersState(state.getOwner().getState(), deepCopy);
        state.restore();
        isRedo = true;
    }

    void becomeDeepCopy() {
        state.becomeDeepCopy();
        deepCopy = true;
    }

    public boolean canUndo() {
        return !isRedo;
    }

    public void redo() throws CannotRedoException {
        redoState.restore();
        isRedo = false;
    }
    boolean isRedo = false;

    public boolean canRedo() {
        return isRedo;
    }

    public void die() {
        state = null;
        redoState = null;
    }

    public boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    public boolean replaceEdit(UndoableEdit anEdit) {
        return false;
    }

    public boolean isSignificant() {
        return true;
    }

    public String getPresentationName() {
        return opName;
    }

    public String getUndoPresentationName() {
        return opName;
    }

    public String getRedoPresentationName() {
        return opName;
    }
    
}
