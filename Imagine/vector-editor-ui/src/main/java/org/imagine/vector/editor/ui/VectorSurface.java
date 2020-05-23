package org.imagine.vector.editor.ui;

import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.function.BiConsumer;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.image.RenderingGoal;
import org.imagine.editor.api.snap.SnapPointRendering;
import org.imagine.editor.api.snap.SnapPoints;
import org.imagine.editor.api.snap.SnapPointsConsumer;
import org.imagine.editor.api.snap.SnapPointsSupplier;
import net.java.dev.imagine.api.image.ToolCommitPreference;
import net.java.dev.imagine.effects.api.EffectReceiver;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.imagine.editor.api.Zoom;
import org.imagine.vector.editor.ui.spi.WidgetSupplier;
import org.imagine.vector.editor.ui.undo.ContentsEdit;

/**
 *
 * @author Tim Boudreau
 */
public class VectorSurface extends SurfaceImplementation implements SnapPointsSupplier {

    private final Shapes shapes;
    private Tool tool;
    private final RepaintHandle handle;
    private CollectionEngine engine;
    private final Point location = new Point();
    private BiConsumer<Tool, Tool> onToolChange;
    private BiConsumer<Point, Point> onPositionChange;
    private final SnapLinesPainter snapPainter;

    VectorSurface(Shapes shapes, RepaintHandle handle) {
        this.shapes = shapes;
        this.handle = handle;
        snapPainter = new SnapLinesPainter(handle, shapes::getBounds);
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

    @Override
    public SnapPoints<?> get() {
        SnapPoints result = shapes.snapPoints(SnapPointRendering.visualSize(),
                snapPainter).get();
        return result;
    }

    @Override
    public BufferedImage getImage() {
        return null;
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
                c.accept(this);
            }
        }
    }

    boolean toolIsPaintingScene() {
        if (tool == null) {
            return false;
        }
        WidgetSupplier supp = tool.lookup(WidgetSupplier.class);
        if (supp != null) {
            return supp.takesOverPaintingScene();
        }
        return false;
    }

    private CollectionEngine createEngine(boolean unexpected, String name) {
        Dimension d = shapes.getBounds().getSize();
        String nm = tool == null ? name : tool.getDisplayName();
        engine = new CollectionEngine(nm, handle, location, d.width, d.height);
        if (unexpected) {
            beginUndoableOperation(nm);
            engine.graphics.onDispose(this::endUndoableOperation);
        }
        return engine;
    }

    @Override
    public Graphics2D getGraphics() {
        if (engine != null) {
            return engine.graphics();
//        } else if (tool != null) {
        } else {
            engine = createEngine(true, null);
            return engine.graphics();
        }
//        return GraphicsUtils.noOpGraphics();
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
                } else {
                    System.out.println("No items painted");
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
    public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle r, Zoom zoom) {
        if (tool != null) {
            WidgetSupplier supp = tool.getLookup().lookup(WidgetSupplier.class);
            if (supp != null && supp.takesOverPaintingScene() && goal.isEditing()) {
//                if (r == null) {
//                    snapPainter.paint(g, zoom);
//                }
                return false;
            }
            if (goal.isEditing()) {
                PaintParticipant pp = tool.lookup(PaintParticipant.class);
                if (pp != null) {
//                    if (!zoom.isOneToOne()) {
//                        double inv = zoom.inverseScale(1);
//                        g.scale(inv, inv);
//                    }
                    pp.paint(g, r, false);
//                    if (!zoom.isOneToOne()) {
//                        double sc = zoom.scale(1);
//                        g.scale(sc, sc);
//                    }
                    return true;
                }
                if (snapPainter != null) {
                    snapPainter.paint(g, zoom);
                }
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

    @Override
    public boolean canApplyComposite() {
        return false;
    }

    @Override
    public boolean canApplyBufferedImageOp() {
        return false;
    }
}
