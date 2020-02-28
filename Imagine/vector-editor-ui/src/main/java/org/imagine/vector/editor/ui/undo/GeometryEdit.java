/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.undo;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotUndoException;
import net.java.dev.imagine.ui.common.UndoMgr;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public final class GeometryEdit extends AbstractShapeEdit {

    private final ShapesCollection shapes;
    private final List<Runnable> undoers = new ArrayList<>();
    private List<Runnable> redoers;
    private final Rectangle2D.Double repaintBounds = new Rectangle2D.Double();
    private final RepaintHandle handle;

    public GeometryEdit(String name, ShapesCollection shapes, RepaintHandle handle) {
        super(name);
        this.shapes = shapes;
        this.handle = handle;
        for (ShapeElement se : shapes) {
            undoers.add(se.geometrySnapshot());
        }
        shapes.getBounds(repaintBounds);
    }

    public static AbstractShapeEdit createEditAdder(String name, ShapesCollection coll, RepaintHandle handle, Consumer<Runnable> editAdderConsumer) {
        UndoMgr mgr = Utilities.actionsGlobalContext().lookup(UndoMgr.class);
        if (mgr == null) {
            editAdderConsumer.accept(() -> {
            });
            return AbstractShapeEdit.DUMMY_EDIT;
        }
        GeometryEdit edit = new GeometryEdit(name, coll, handle);
        editAdderConsumer.accept(() -> {
            mgr.undoableEditHappened(new UndoableEditEvent(coll, edit));
        });
        return edit;
    }

    @Override
    protected void undoImpl() throws CannotUndoException {
        shapes.getBounds(repaintBounds);
        redoers = new ArrayList<>(undoers.size());
        for (ShapeElement se : shapes) {
            redoers.add(se.geometrySnapshot());
        }
        for (Runnable un : undoers) {
            un.run();
        }
        handle.repaintArea(repaintBounds);
    }

    @Override
    public boolean canUndo() {
        return redoers == null;
    }

    @Override
    protected void redoImpl() {
        for (Runnable redo : redoers) {
            redo.run();
        }
        redoers = null;
        handle.repaintArea(repaintBounds);
    }

    @Override
    public boolean canRedo() {
        return redoers != null;
    }

    @Override
    public void die() {
        redoers = null;
        undoers.clear();
    }
}
