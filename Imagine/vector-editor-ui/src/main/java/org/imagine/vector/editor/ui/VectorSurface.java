/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImageOp;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.SnapPointsConsumer;
import net.java.dev.imagine.api.vector.painting.VectorWrapperGraphics;
import net.java.dev.imagine.spi.image.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import net.java.dev.imagine.ui.common.UndoMgr;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class VectorSurface extends SurfaceImplementation {

    private final Shapes shapes;
    private Tool tool;
    private final VectorLayer layer;
    private final RepaintHandle handle;
    private CollectionEngine engine;
    private Point location = new Point();

    VectorSurface(Shapes shapes, VectorLayer layer, RepaintHandle handle) {
        this.shapes = shapes;
        this.layer = layer;
        this.handle = handle;
        System.out.println("create a vector layer for " + layer.getName());
    }

    static class CollectionEngine implements RepaintHandle {

        final VectorWrapperGraphics graphics;
        private final String opName;
//        private final ShapeStack stack;
        private final ShapeCollector collector = new ShapeCollector();
        private final Rectangle bounds = new Rectangle();

        public CollectionEngine(
                String opName) {
            this.graphics = new VectorWrapperGraphics(collector,
                    GraphicsUtils.noOpGraphics(),
                    new Point(), 1, 1);
            this.opName = opName;
        }

        boolean hasItems() {
            return !collector.isEmpty();
        }

        Rectangle finish(Shapes shapes) {
            if (!collector.isEmpty()) {
                collector.replay(shapes);
            }
            return collector.getBounds();
        }

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            bounds.add(new Rectangle(x, y, w, h));
        }

        @Override
        public void setCursor(Cursor cursor) {
            // do nothing
        }
    }

    @Override
    public void setTool(Tool tool) {
        System.out.println("VECTOR LAYER SET TOOL " + tool);
        if (this.tool == tool) {
            System.out.println("already set.");
            return;
        }
        this.tool = tool;
        if (tool != null) {
            SnapPointsConsumer c = tool.getLookup().lookup(SnapPointsConsumer.class);
            if (c != null) {
                c.accept(shapes.snapPoints(5));
            }
        }
    }

    private CollectionEngine createEngine(boolean unexpected, String name) {
        ShapeStack tempStack = new ShapeStack();
        engine = new CollectionEngine(tool.getDisplayName());
        if (unexpected) {
            beginUndoableOperation(name == null ? tool.getDisplayName() : name);
        }
        tempStack.beginDraw();
        if (unexpected) {
            engine.graphics.onDispose(this::endUndoableOperation);
        }

        return engine;
    }

    @Override
    public Graphics2D getGraphics() {
        if (engine != null) {
            System.out.println("committing, returning existing graphics");
            return engine.graphics;
        } else if (tool != null) {
            System.out.println("not active, returning on-the-fly graphics");
            engine = createEngine(true, null);
            return engine.graphics;
        }
        System.out.println("no tool, using no-op graphics");
        return GraphicsUtils.noOpGraphics();
    }

    @Override
    public boolean canApplyComposite() {
        return false;
    }

    @Override
    public boolean canApplyBufferedImageOp() {
        return false;
    }

    @Override
    public void setLocation(Point p) {
        if (!p.equals(location)) {
            Rectangle bds = shapes.getBounds();
            Rectangle nue = new Rectangle(bds);
            nue.x = p.x;
            nue.y = p.y;
            bds.add(nue);
            location.x = p.x;
            location.y = p.y;
            handle.repaintArea(bds);
        }
    }

    @Override
    public Point getLocation() {
        return new Point(location);
    }

    @Override
    public void beginUndoableOperation(String name) {
        if (engine == null) {
            engine = createEngine(false, name);
        }
    }

    @Override
    public void endUndoableOperation() {
        if (engine != null) {
            try {
                if (engine.hasItems()) {
                    Shapes snapshot = shapes.snapshot();
                    Rectangle rect = engine.finish(shapes);
                    if (!rect.isEmpty()) {
                        handle.repaintArea(rect);
                    }
                    UndoMgr mgr = Utilities.actionsGlobalContext()
                            .lookup(UndoMgr.class);
                    if (mgr != null) {
                        ShapesUndoableEdit edit = new ShapesUndoableEdit(
                                engine.opName, shapes, handle, snapshot);
                        mgr.undoableEditHappened(new UndoableEditEvent(this, edit));
                    }
                }
            } finally {
                engine = null;
            }
        }
    }

    @Override
    public void cancelUndoableOperation() {
        engine = null;
        System.out.println("cancel undoable");
    }

    @Override
    public void setCursor(Cursor cursor) {
        handle.setCursor(cursor);
    }

    @Override
    public boolean paint(Graphics2D g, Rectangle r) {
        if (tool != null) {
            PaintParticipant pp = tool.lookup(PaintParticipant.class);
            if (pp != null) {
                pp.paint(g, r, false);
                return true;
            }
        }
        return false;
    }

    @Override
    public Dimension getSize() {
        return shapes.getBounds().getSize();
    }

    @Override
    public void applyComposite(Composite composite, Shape clip) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void applyBufferedImageOp(BufferedImageOp op, Shape clip) {
        throw new UnsupportedOperationException();
    }

    static class ShapesUndoableEdit extends AbstractUndoableEdit {

        private final String opName;
        private Shapes shapes;
        private Shapes redo;
        private Shapes undo;
        private RepaintHandle handle;

        public ShapesUndoableEdit(String opName, Shapes shapes, RepaintHandle handle, Shapes snapshot) {
            this.opName = opName;
            this.undo = snapshot;
            this.shapes = shapes;
            this.handle = handle;
        }

        @Override
        public String getPresentationName() {
            return opName;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public boolean canRedo() {
            return redo != null;
        }

        @Override
        public void redo() throws CannotRedoException {
            if (redo == null) {
                throw new CannotRedoException();
            }
            Rectangle repaint = shapes.restore(redo);
            handle.repaintArea(repaint);
        }

        @Override
        public boolean canUndo() {
            return undo != null;
        }

        @Override
        public void undo() throws CannotUndoException {
            if (undo == null) {
                throw new CannotUndoException();
            }
            redo = shapes.snapshot();
            shapes.restore(shapes);
        }

        @Override
        public void die() {
            undo = null;
            redo = null;
            shapes = null;
            handle = null;
        }
    }
}
