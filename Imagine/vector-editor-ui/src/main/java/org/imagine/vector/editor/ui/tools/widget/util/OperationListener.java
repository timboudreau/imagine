package org.imagine.vector.editor.ui.tools.widget.util;

import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public interface OperationListener {

    void onDragStarted(Lookup lkp);

    void onDragCompleted(Lookup lkp);

    void onDragCancelled(Lookup lkp);

}
