package org.imagine.vector.editor.ui.undo;

/**
 *
 * @author Tim Boudreau
 */
public interface UndoRedoHookable {

    void hook(Runnable r);
}
