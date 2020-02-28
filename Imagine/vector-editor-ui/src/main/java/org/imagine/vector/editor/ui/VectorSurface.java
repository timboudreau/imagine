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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImageOp;
import java.util.function.BiConsumer;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPointsConsumer;
import net.java.dev.imagine.api.image.ToolCommitPreference;
import net.java.dev.imagine.effects.api.EffectReceiver;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.WidgetSupplier;
import org.imagine.vector.editor.ui.undo.ContentsEdit;

/**
 *
 * @author Tim Boudreau
 */
public class VectorSurface extends SurfaceImplementation {

    private final Shapes shapes;
    private Tool tool;
    private final RepaintHandle handle;
    private CollectionEngine engine;
    private final Point location = new Point();
    private BiConsumer<Tool, Tool> onToolChange;
    private BiConsumer<Point, Point> onPositionChange;

    VectorSurface(Shapes shapes, RepaintHandle handle) {
        this.shapes = shapes;
        this.handle = handle;
    }

    void onToolChange(BiConsumer<Tool, Tool> c) {
        onToolChange = c;
    }

    void onPositionChange(BiConsumer<Point, Point> onPositionChange) {
        this.onPositionChange = onPositionChange;
    }

    private final ER er = new ER();

    EffectReceiver<AffineTransform> transformReceiver() {
        return er;
    }

    class ER extends EffectReceiver<AffineTransform> {

        ER() {
            super(AffineTransform.class);
        }

        @Override
        protected boolean onApply(AffineTransform effect) {
            shapes.geometryEdit("Transform", () -> {
                shapes.applyTransform(effect);
            });
            return true;
        }

        @Override
        public Dimension getSize() {
            return shapes.getBounds().getSize();
        }
    }

    @Override
    public ToolCommitPreference toolCommitPreference() {
        return ToolCommitPreference.COLLECT_GEOMETRY;
    }

    @Override
    public void setTool(Tool tool) {
        if (this.tool == tool) {
            return;
        }
        Tool old = this.tool;
        this.tool = tool;
        if (onToolChange != null) {
            onToolChange.accept(old, tool);
        }
        if (tool != null) {
            SnapPointsConsumer c = tool.getLookup().lookup(SnapPointsConsumer.class);
            if (c != null) {
                c.accept(shapes.snapPoints(5));
            }
        }
    }

    private CollectionEngine createEngine(boolean unexpected, String name) {
        Dimension d = shapes.getBounds().getSize();
        engine = new CollectionEngine(tool.getDisplayName(), handle, location, d.width, d.height);
        if (unexpected) {
            beginUndoableOperation(name == null ? tool.getDisplayName() : name);
            engine.graphics.onDispose(this::endUndoableOperation);
        }
        return engine;
    }

    @Override
    public Graphics2D getGraphics() {
        if (engine != null) {
            return engine.graphics();
        } else if (tool != null) {
            engine = createEngine(true, null);
            return engine.graphics();
        }
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
            Point old = getLocation();
            Rectangle bds = shapes.getBounds();
            Rectangle nue = new Rectangle(bds);
            nue.x = p.x;
            nue.y = p.y;
            bds.add(nue);
            location.x = p.x;
            location.y = p.y;
            if (onPositionChange != null) {
                onPositionChange.accept(old, getLocation());
            }
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
                    ContentsEdit.addEdit(engine.opName, shapes, handle, () -> {
                        Rectangle rect = engine.finish(shapes);
                        if (!rect.isEmpty()) {
                            handle.repaintArea(rect);
                        }
                    });
                }
            } finally {
                engine = null;
            }
        }
    }

    @Override
    public void cancelUndoableOperation() {
        engine = null;
    }

    @Override
    public void setCursor(Cursor cursor) {
        handle.setCursor(cursor);
    }

    @Override
    public boolean paint(Graphics2D g, Rectangle r) {
        if (tool != null) {
            WidgetSupplier supp = tool.getLookup().lookup(WidgetSupplier.class);
            if (supp != null && supp.takesOverPaintingScene()) {
                return false;
            }
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
