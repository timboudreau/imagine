package org.imagine.vector.editor.ui.tools.widget;

import org.imagine.vector.editor.ui.tools.widget.painting.DecorationController;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.PathText;
import org.imagine.utils.java2d.GraphicsUtils;
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
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Tim Boudreau
 */
public class OneShapeWidget extends Widget {

    private final ShapeElement element;
    private ShapeElement temp;
    private final DecorationController decorationPainting;
    private final ShapesCollection shapes;
    private final InstanceContent content = new InstanceContent();
    private final AbstractLookup lkp = new AbstractLookup(content);
    private final UIState uiState;

    @SuppressWarnings("LeakingThisInConstructor")
    public OneShapeWidget(Scene scene, ShapeElement el, ShapesCollection coll,
            DecorationController decorationPainting, DragHandler shapeDrag,
            UIState uiState, AdjustmentKeyActionHandler keyActionHandler) {
        super(scene);
        this.uiState = uiState;
        this.shapes = coll;
        this.element = el;
        el.changed();
        this.decorationPainting = decorationPainting;
        content.add(this);
        content.add(coll);
        content.add(shapeDrag);
        content.add(keyActionHandler);
        content.add("shape", new CVT());
    }

    public ShapeElement realShape() {
        return element;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    class CVT implements InstanceContent.Convertor<String, ShapeElement> {

        @Override
        public ShapeElement convert(String t) {
            if (temp != null) {
                return temp;
            }
            return element;
        }

        @Override
        public Class<? extends ShapeElement> type(String t) {
            return ShapeElement.class;
        }

        @Override
        public String id(String t) {
            return t;
        }

        @Override
        public String displayName(String t) {
            return t;
        }
    }

    @Override
    public String toString() {
        return "OneShapeWidget(" + element.getName() + ")";
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    private ShapeElement element() {
        if (temp != null) {
            return temp;
        }
        return element;
    }

    @Override
    protected Rectangle calculateClientArea() {
        return element().getBounds();
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (isDragInProgress()) {
            return true;
        }
        return element().shape().contains(localLocation);
    }

    private boolean draggingControlPoint;

    private ShapeElement setControlPointDragInProgress(boolean value) {
        if (value && draggingShape) {
            throw new IllegalStateException("Dragging control point and "
                    + "dragging shape simultaneously is impossible");
        }
        if (value != this.draggingControlPoint) {
            this.draggingControlPoint = value;
            if (value) {
                return switchToShapeCopy();
            } else {
                return clearShapeCopy();
            }
        }
        return null;
    }

    private boolean draggingShape;

    private ShapeElement setShapeDragInProgress(boolean value) {
        if (value && draggingControlPoint) {
            throw new IllegalStateException("Dragging control point and "
                    + "dragging shape simultaneously is impossible");
        }
        if (value != this.draggingShape) {
            this.draggingShape = value;
            if (value) {
                return switchToShapeCopy();
            } else {
                return clearShapeCopy();
            }
        }
//        if (value && temp != null) {
//            return temp;
//        }
        return null;
    }

    private boolean isDragInProgress() {
        return draggingControlPoint || draggingShape;
    }

    private Exception lastBegin;

    public ShapeElement onBeginDrag() {
        ShapeElement result = setShapeDragInProgress(true);
        if (result == null && temp != null) {
            Exception e1 = lastBegin;
            lastBegin = null;
            throw new IllegalStateException("Previous drag operation not ended", e1);
        }
        lastBegin = new Exception();
        return result;
    }

    @Messages({
        "# {0} - shape",
        "DRAG_SHAPE=Move {0}"
    })
    public void onEndDrag(boolean commit) {
        ShapeElement tp = setShapeDragInProgress(false);
        assert tp != null : "No temp shape removed - not dragging?";
        if (commit) {
            // Wrap our change in an edit, so undo support will
            // kick in
            shapes.edit(Bundle.DRAG_SHAPE(element.getName()), element, () -> {
                element.setShape(tp.item());
            });
        }
    }

    private ShapeElement switchToShapeCopy() {
        assert temp == null;
        temp = element.copy();
        temp.setName(temp.getName() + "-tempcopy");
        return temp;
    }

    private ShapeElement clearShapeCopy() {
        assert temp != null;
        ShapeElement old = temp;
        temp = null;
        return old;
    }

    public ShapeControlPoint onBeginControlPointDrag(ShapeControlPoint pt) {
        setControlPointDragInProgress(true);
        return tempControlPoint(pt.index());
    }

    public void onControlPointMove(ShapeControlPoint pt) {
        revalidate();
    }

    private ShapeControlPoint tempControlPoint(int index) {
        assert temp != null : "Not dragging - no temporary shape";
        return temp.controlPoints(9, cp -> {
            revalidate();
            getScene().validate();
        })[index];
    }

    public void onCancelControlPointDrag() {
        setControlPointDragInProgress(false);
        revalidate();
        repaint();
    }

    @Messages("MOVE_CONTROL_POINT=Move Control Point")
    public void onEndControlPointDrag(ShapeControlPoint pt, boolean commit) {
        ShapeElement tp = setControlPointDragInProgress(false);
        assert pt.owner().equals(tp);
        assert tp != null : "CP End drag without start";
        if (commit) {
            // Find the real control point (actually, creating a new one
            // with the same identity for mapping to a widget), and
            // set its position to that of the dragged control point
            // copy
            ShapeControlPoint real = element.controlPoints(9, cp -> {
                revalidate();
//                getScene().validate();
            })[pt.index()];
            shapes.edit(Bundle.MOVE_CONTROL_POINT(), element, () -> {
                real.set(pt.getX(), pt.getY());
            });
        } else {
            // Ensure if we were moved, we repaint in the right place
            revalidate();
//            getScene().validate();
        }
    }

    Rectangle fudge = new Rectangle();

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();
        if (!isDragInProgress()) {
            GraphicsUtils.setHighQualityRenderingHints(g);
        }
        ShapeElement el = element();

        fudge.x = fudge.y = fudge.width = fudge.height = 0;
        el.addToBounds(fudge);
        if (!g.hitClip(fudge.x, fudge.y, fudge.width, fudge.height)) {
            return;
        }
        if (el.item().is(ImageWrapper.class)) {
            el.item().as(ImageWrapper.class).paint(g);
        } else if (el.item().is(PathText.class)) {
            if (el.isFill()) {
                g.setPaint(el.fill());
                el.item().as(PathText.class).paint(g);
            }
            if (el.isDraw()) {
                g.setPaint(el.outline());
                g.setStroke(el.stroke());
                el.item().as(PathText.class).draw(g);
            }

            if (getState().isFocused()) {
                el.item().as(PathText.class, pt -> {
                    Shape shape = pt.shape().toShape();
                    if (pt.transform() != null) {
                        shape = pt.transform().createTransformedShape(shape);
                    }
                    g.draw(shape);
                });
            }
        } else {
            if (el.isFill()) {
                Paint bg = el.getFill();
                if (bg != null) {
                    g.setPaint(bg);
                    g.fill(el.shape());
                }
            }
            if (el.isDraw()) {
                Paint fg = el.getDraw();
                if (fg != null) {
                    g.draw(el.shape());
                }
            }
        }
        paintDecorations(g);
    }

    private void paintDecorations(Graphics2D g) {
        if (!uiState.focusDecorationsPainted()) {
            return;
        }
        ObjectState state = getState();
        if (state.isFocused()) {
            decorationPainting.setupFocusedPainting(getScene().getZoomFactor(), g, g1 -> {
                g1.draw(element().shape());
            });
        } else if (state.isSelected()) {
            decorationPainting.setupFocusedPainting(getScene().getZoomFactor(), g, g1 -> {
                g1.draw(element().shape());
            });
        }
    }
}
