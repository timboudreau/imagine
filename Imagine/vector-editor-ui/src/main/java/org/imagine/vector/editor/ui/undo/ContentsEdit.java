package org.imagine.vector.editor.ui.undo;

import java.awt.geom.Rectangle2D;
import javax.swing.event.UndoableEditEvent;
import net.java.dev.imagine.ui.common.UndoMgr;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public final class ContentsEdit extends AbstractShapeEdit {

    private final ShapesCollection coll;
    private Runnable undoSnapshot;
    private Runnable redoSnapshot;
    private final RepaintHandle handle;
    private final Rectangle2D repaintBounds = new Rectangle2D.Double();

    public ContentsEdit(ShapesCollection coll, Runnable undoSnapshot, String name, RepaintHandle handle) {
        super(name);
        this.coll = coll;
        this.undoSnapshot = undoSnapshot;
        this.handle = handle;
        coll.getBounds(repaintBounds);
    }

    public static AbstractShapeEdit addEdit(String name, ShapesCollection coll, RepaintHandle handle, Runnable performer) {
        UndoMgr mgr = Utilities.actionsGlobalContext().lookup(UndoMgr.class);
        if (mgr == null) {
            performer.run();
            return AbstractShapeEdit.DUMMY_EDIT;
        } else {
            ContentsEdit edit = new ContentsEdit(coll, coll.contentsSnapshot(), name, handle);
            performer.run();
            mgr.undoableEditHappened(new UndoableEditEvent(performer, edit));
            return edit;
        }
    }

    @Override
    protected void onBeforeUndo() {
        coll.getBounds(repaintBounds);
        redoSnapshot = coll.contentsSnapshot();
    }

    @Override
    public void undoImpl() {
        undoSnapshot.run();
        handle.repaintArea(repaintBounds);
    }

    @Override
    public boolean canUndo() {
        return redoSnapshot == null;
    }

    @Override
    public void redoImpl() {
        redoSnapshot.run();
        redoSnapshot = null;
        handle.repaintArea(repaintBounds);
    }

    @Override
    public boolean canRedo() {
        return redoSnapshot == null;
    }

    @Override
    public void die() {
        redoSnapshot = null;
        undoSnapshot = null;
    }
}
