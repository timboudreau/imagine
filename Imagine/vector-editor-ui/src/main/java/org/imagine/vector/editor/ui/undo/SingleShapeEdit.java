package org.imagine.vector.editor.ui.undo;

import com.mastfrog.function.state.Bool;
import java.awt.Rectangle;
import java.util.function.Consumer;
import javax.swing.event.UndoableEditEvent;
import net.java.dev.imagine.ui.common.UndoMgr;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public final class SingleShapeEdit extends AbstractShapeEdit {

    private final Runnable undo;
    private Runnable redo;
    private final ShapeElement el;
    private final ShapesCollection coll;
    private Runnable contentsSnapshot;
    private Runnable redoSnapshot;
    private static final ThreadLocal<SingleShapeEdit> CURR_EDIT
            = new ThreadLocal<>();
    private final Rectangle repaintBounds = new Rectangle();
    private final RepaintHandle handle;

    public SingleShapeEdit(String name, Runnable undo, ShapeElement el, ShapesCollection coll, RepaintHandle handle) {
        super(name);
        this.undo = undo;
        this.el = el;
        this.coll = coll;
        this.handle = handle;
        el.addToBounds(repaintBounds);
    }

    public static void includeContentsSnapshotInCurrentEdit() {
        SingleShapeEdit edit = CURR_EDIT.get();
        if (edit != null) {
            edit.includeContentsSnapshot();
        }
    }

    public static AbstractShapeEdit maybeAddEdit(String name, ShapeElement el, ShapesCollection coll, RepaintHandle handle, Consumer<Runnable> editAdderConsumer) {
        UndoMgr mgr = Utilities.actionsGlobalContext().lookup(UndoMgr.class);
        if (mgr != null) {
            SingleShapeEdit se = new SingleShapeEdit(name, el.restorableSnapshot(), el, coll, handle);
            CURR_EDIT.set(se);
            Runnable editAdder = () -> {
                mgr.undoableEditHappened(new UndoableEditEvent(el, se));
                CURR_EDIT.remove();
            };
            editAdderConsumer.accept(editAdder);
            return se;
        } else {
            editAdderConsumer.accept(() -> {
            });
            return AbstractShapeEdit.DUMMY_EDIT;
        }
    }

    public static AbstractShapeEdit maybeAddAbortableEdit(String name, ShapeElement el, ShapesCollection coll, RepaintHandle handle, Consumer<Abortable> editAdderConsumer) {
        UndoMgr mgr = Utilities.actionsGlobalContext().lookup(UndoMgr.class);
        if (mgr != null) {
            SingleShapeEdit se = new SingleShapeEdit(name, el.restorableSnapshot(), el, coll, handle);
            CURR_EDIT.set(se);

            Bool hasEdits = Bool.create();
            Consumer<Consumer<Abortable>> runner = c -> {
                boolean aborted = AbortableImpl.SHARED_INSTANCE.borrow(c);
                if (!aborted) {
                    hasEdits.set();
                }
            };
            runner.accept(editAdderConsumer);
            hasEdits.ifTrue(() -> {
                mgr.undoableEditHappened(new UndoableEditEvent(el, se));
            });

            return hasEdits.getAsBoolean() ? se : AbstractShapeEdit.DUMMY_EDIT;
        } else {
            editAdderConsumer.accept(() -> {
            });
            return AbstractShapeEdit.DUMMY_EDIT;
        }
    }

    public void includeContentsSnapshot() {
        if (redo != null) {
            throw new IllegalStateException("Cannot alter state "
                    + "after edit has been used");
        }
        if (contentsSnapshot != null) {
            contentsSnapshot = coll.contentsSnapshot();
        }
    }

    @Override
    protected void undoImpl() {
        if (contentsSnapshot != null) {
            redoSnapshot = coll.contentsSnapshot();
        }
        el.addToBounds(repaintBounds);
        redo = el.restorableSnapshot();
        if (contentsSnapshot != null) {
            contentsSnapshot.run();
        }
        undo.run();
        handle.repaintArea(repaintBounds);
    }

    @Override
    public boolean canUndo() {
        return redo == null;
    }

    @Override
    protected void redoImpl() {
        if (redoSnapshot != null) {
            redoSnapshot.run();
            redoSnapshot = null;
        }
        redo.run();
        redo = null;
        handle.repaintArea(repaintBounds);
    }

    @Override
    public boolean canRedo() {
        return redo != null;
    }

    @Override
    public void die() {
        redo = null;
        redoSnapshot = null;
    }
}
