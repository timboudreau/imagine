/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.java.dev.imagine.ui.common;

import java.util.ArrayList;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import org.netbeans.paint.api.editing.UndoManager;
import org.openide.awt.UndoRedo;

/**
 *
 * @author Tim Boudreau
 */
public final class UndoMgr extends UndoRedo.Manager implements UndoManager {

    public List getEdits() {
        return new ArrayList(super.edits);
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        return super.addEdit(anEdit);
    }

    @Override
    public synchronized void redo() throws CannotRedoException {
        super.redo();
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        super.undo();
    }

}
