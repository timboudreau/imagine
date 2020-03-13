package org.imagine.vector.editor.ui.tools.widget;

import java.awt.Cursor;
import org.imagine.vector.editor.ui.tools.widget.painting.DesignerProperties;
import org.imagine.vector.editor.ui.tools.widget.painting.DecorationController;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import static org.imagine.utils.java2d.GraphicsUtils.setHighQualityRenderingHints;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.widget.actions.AdjustmentKeyActionHandler;
import org.imagine.vector.editor.ui.tools.widget.actions.DragHandler;
import org.imagine.vector.editor.ui.tools.widget.util.UIState;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
public final class ControlPointWidget extends Widget {

    private final ShapeControlPoint cp;
    private final Lookup lkp;
    private final DecorationController decorations;
    private boolean dragging;
    private ShapeControlPoint temp;
    private final UIState uiState;
    private final Lookup selection;

    @SuppressWarnings("LeakingThisInConstructor")
    ControlPointWidget(ShapeControlPoint pt, ShapesCollection shapes, Scene scene,
            DecorationController decorations, DragHandler dh,
            UIState uiState, AdjustmentKeyActionHandler keyActionHandler,
            Lookup selection) {
        super(scene);
        this.cp = pt;
        this.decorations = decorations;
        lkp = Lookups.fixed(pt, shapes, pt.owner(), this, dh, keyActionHandler);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(pt.index() + " - " + pt.kind());
        this.uiState = uiState;
        this.selection = selection;
    }

    private ShapeControlPoint cp() {
        if (temp != null) {
            return temp;
        }
        return cp;
    }

    @Override
    public String toString() {
        return "CP-" + cp().index() + "(" + cp().owner().getName() + ")";
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    public void setTempControlPoint(ShapeControlPoint temp) {
        this.temp = temp;
        revalidate();
    }

    public void clearTempControlPoint() {
        this.temp = null;
        revalidate();
    }

    public void onBeginDrag(ShapeControlPoint temp) {
        this.temp = temp;
        setDragging(true);
        revalidate();
    }

    public void onEndDrag() {
        this.temp = null;
        setDragging(false);
        revalidate();
    }

    void setDragging(boolean dragging) {
        if (dragging != this.dragging) {
            this.dragging = dragging;
            repaint();
        } else {
            throw new IllegalStateException(
                    "Set dragging called asymmetrically - " + dragging);
        }
    }

    private static final Rectangle emptyRect = new Rectangle();
    @Override
    protected Rectangle calculateClientArea() {
        if (!uiState.controlPointsVisible() && cp.isVirtual()) {
            emptyRect.x = emptyRect.y = emptyRect.width = emptyRect.height = 0;
            return emptyRect;
        }
        if (uiState.selectedShapeControlPointsVisible() && !selection.lookupAll(ShapeElement.class).contains(cp.owner())) {
            emptyRect.x = emptyRect.y = emptyRect.width = emptyRect.height = 0;
            return emptyRect;
        }
        Rectangle rect = controlPointShape(false).getBounds();
        Line2D.Double line = configureConnectorLine();
        if (line != null) {
            rect.add(line.x1, line.y1);
            rect.add(line.x2, line.y2);
        }
        return rect;
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (dragging) {
            return true;
        }
        if (!uiState.controlPointsVisible() && cp.isVirtual()) {
            return false;
        }
        if (uiState.selectedShapeControlPointsVisible() && !selection.lookupAll(ShapeElement.class).contains(cp.owner())) {
            return false;
        }
        Shape shape = controlPointShape(true);
        boolean hit = shape.contains(localLocation);
        if (!hit) {
            // In case of high zoom, the hit region can be < 1px,
            // which will still generate some hits, but not enough;
            // use a 6x6 square as the hit test in that case
            Rectangle2D bds = shape.getBounds2D();
            // XXX could probably do this without instantiating a
            // rectangle
            if (bds.getWidth() < 1 || bds.getHeight() < 1) {
                hit = localLocation.x > -3 && localLocation.y > -3
                        && localLocation.x < 3 && localLocation.y < 3;
            }
        }
        return hit;
    }

    private static final Line2D.Double scratchLine = new Line2D.Double();

    private void paintConnectorLine(Graphics2D g, DesignerProperties props) {
        if (!uiState.connectorLinesVisible()) {
            return;
        }
        Line2D line = configureConnectorLine();
        if (line != null) {
            g.setStroke(props.strokeForControlPointConnectorLine(getScene().getZoomFactor()));
            g.setPaint(props.colorForControlPointConnectorLine());
            g.draw(line);
        }
    }

    private Line2D.Double configureConnectorLine() {
        ShapeControlPoint c = cp();
        if (c.isVirtual() && c.kind().isSplinePoint()) {
            ShapeControlPoint[] sibs = c.family();
            ShapeControlPoint physicalSibling = null;
            for (int i = c.index() + 1; i < sibs.length; i++) {
                ShapeControlPoint sib = sibs[i];
                if (!sib.isVirtual()) {
                    physicalSibling = sib;
                    break;
                }
            }
            if (physicalSibling != null) {
                scratchLine.setLine(c.getX(), c.getY(), physicalSibling.getX(), physicalSibling.getY());
                return scratchLine;
            }
        }
        return null;
    }

    @Override
    protected void paintWidget() {
        if (!uiState.controlPointsVisible()) {
            return;
        }
        if (uiState.selectedShapeControlPointsVisible() && !selection.lookupAll(ShapeElement.class).contains(cp.owner())) {
            return;
        }
        Graphics2D g = getGraphics();
        if (!dragging) {
            setHighQualityRenderingHints(g);
        }
        DesignerProperties props = decorations.properties();
        paintConnectorLine(g, props);
        Shape shape = controlPointShape(true);
        ControlPointKind kind = cp().kind();
        double zoom = getScene().getZoomFactor();
        g.setStroke(props.strokeForControlPoint(zoom));
        g.setPaint(props.fillForControlPoint(kind, getState()));
        g.fill(shape);

        g.setPaint(props.drawForControlPoint(kind, getState()));
        g.draw(shape);
        paintDecorations(g, getState(), props, shape);
    }

    private void paintDecorations(Graphics2D g, ObjectState state, DesignerProperties props, Shape shape) {
        if (!uiState.focusDecorationsPainted()) {
            return;
        }
        if (state.isFocused()) {
            decorations.setupFocusedPainting(getScene().getZoomFactor(), g, g1 -> {
                g1.draw(shape);
            });
        } else if (state.isSelected()) {
            decorations.setupSelectedPainting(getScene().getZoomFactor(), g, g1 -> {
                g1.draw(shape);
            });
        }
    }

    Shape controlPointShape(boolean forPainting) {
        return decorations.properties().shapeForControlPoint(cp(), false,
                getScene().getZoomFactor(),
                !forPainting, getState());
    }
}
