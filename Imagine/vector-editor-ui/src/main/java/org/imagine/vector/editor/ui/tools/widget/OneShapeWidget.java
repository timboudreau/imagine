package org.imagine.vector.editor.ui.tools.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import org.imagine.vector.editor.ui.tools.widget.painting.DecorationController;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.PathText;
import net.java.dev.imagine.api.vector.elements.Text;
import org.imagine.geometry.PieWedge;
import org.imagine.geometry.Polygon2D;
import org.imagine.geometry.RotationDirection;
import org.imagine.geometry.util.GeometryStrings;
import com.mastfrog.function.state.Int;
import org.imagine.geometry.CornerAngle;
import org.imagine.geometry.LineVector;
import org.imagine.geometry.analysis.VectorVisitor;
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

        System.out.println("\nsetCpDragInProgress " + value);
//        new Exception("setCpDragInProgress " + value).printStackTrace();
        if (value != this.draggingControlPoint) {
            this.draggingControlPoint = value;
            if (value) {
                ShapeElement result = switchToShapeCopy();
                System.out.println("switch to copy " + result.getName());
                return result;
            } else {
                ShapeElement result = clearShapeCopy();
                System.out.println("switch back to real " + result.getName());
                return result;
            }
        } else {
            System.out.println("NOT A CHANGE");
            if (true) {
                return temp;
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
            ShapeElement result;
            if (value) {
                result = switchToShapeCopy();
            } else {
                result = clearShapeCopy();
            }
            revalidate();
            getScene().validate();
            repaint();
            return result;
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
        System.out.println("begin drag " + pt.index() + " on " + pt.owner().getName());
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
        Shape shape = el.shape();

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
//                el.item().as(PathText.class).paint(g);
                g.fill(shape);
            }
            if (el.isDraw()) {
                g.setPaint(el.outline());
                g.setStroke(el.stroke());
//                el.item().as(PathText.class).draw(g);
                g.fill(shape);
            }

            if (getState().isFocused()) {
                el.item().as(PathText.class, pt -> {
                    Shape sp = pt.shape().toShape();
                    if (pt.transform() != null) {
                        sp = pt.transform().createTransformedShape(sp);
                    }
                    g.draw(sp);
                });
            }
        } else {
            if (el.isFill()) {
                Paint bg = el.getFill();
                if (bg != null) {
                    g.setPaint(bg);
                    g.fill(shape);
                }
            }
            if (el.isDraw()) {
                Paint fg = el.getDraw();
                if (fg != null) {
                    g.draw(shape);
                }
            }
            if (!el.item().is(Text.class)) {
//                debugPaintCorners(g, el, shape);
            }
        }
        paintDecorations(g, el, shape);
    }

    private final PieWedge wedge = new PieWedge();

    private void debugPaintCorners(Graphics2D g, ShapeElement el, Shape shape) {
        Font f = g.getFont();
        double scale = 1D / getScene().getZoomFactor();
        g.setFont(f.deriveFont(AffineTransform.getScaleInstance(scale, scale)));
        Rectangle r = shape.getBounds();

        BasicStroke scaleStroke = new BasicStroke((float) scale);
        g.setStroke(scaleStroke);
        wedge.setRadius((Math.min(r.width, r.height) / 6) * scale);

        Int lastIdHash = Int.of(-1);
        VectorVisitor vv = (int pointIndex, LineVector vect, int subpathIndex, RotationDirection subpathRotationDirection, Polygon2D approximate, int prevPointIndex, int nextPointIndex) -> {
            double x = vect.apexX();
            double y = vect.apexY();
            CornerAngle corner = vect.corner();
            wedge.setCenter(x, y);
//            corner = corner.normalized();
            wedge.setAngleAndExtent(corner.toSector());

            int idHash = System.identityHashCode(approximate);
            lastIdHash.ifUpdate(idHash, () -> {
                System.out.println("POLY POINTS " + approximate.pointCount()
                        + " for " + el.getName());
                g.setColor(new Color(20, 150, 20, 200));
                g.setStroke(new BasicStroke((float) (scale * 3)));
                g.draw(approximate);
                g.setStroke(scaleStroke);
            });
            float txtOffset = (float) (scale * 5);
            g.setColor(new Color(40, 40, 240));
            g.drawString(Integer.toString(pointIndex)
                    + " - " + GeometryStrings.toShortString(corner.extent()) + "\u00B0"
                    + corner.toShortString(),
                    (float) (x + txtOffset), (float) (y + txtOffset)
            );
            g.draw(wedge);
        };
        RotationDirection dir = vv.analyze(shape);


        /*
        RotationDirection dir = CornerAngle.forShape(shape, (int apexPointIndexWithinShape, CornerAngle angle, double x, double y, int isects) -> {
        });

        CornerAngle.forShape(shape, (int apexPointIndexWithinShape, CornerAngle angle, double x, double y, int isects) -> {
            wedge.setCenter(x, y);
            RotationDirection d2 = angle.direction();
            if (isects % 2 != 0) {
                d2 = d2.opposite();
            }
            if (d2 != dir) {
                System.out.println("Invert " + apexPointIndexWithinShape + " for " + angle);
                g.setColor(new Color(255, 40, 40));
                angle = angle.normalized();
                if (isects % 2 == 1) {
                    g.setColor(new Color(40, 220, 40));
                    wedge.setAngleAndExtent(angle.bDegrees(), 360 - Math.abs(angle.extent()));
                } else {
                    wedge.setAngleAndExtent(angle.bDegrees(), 360 - Math.abs(angle.extent()));
//                    wedge.setAngleAndExtent(angle.aDegrees(), angle.extent());
                }
            } else {
                angle = angle.normalized();
                wedge.setAngleAndExtent(angle.aDegrees(), angle.extent());
//                angle = angle.normalized();
                g.setColor(new Color(40, 40, 255));
            }
            float txtOffset = (float) (scale * 5);
            g.drawString(Integer.toString(apexPointIndexWithinShape)
                    + " - " + GeometryUtils.toShortString(angle.extent()) + "\u00B0"
                    + " i:" + isects,
                    (float) (x + txtOffset), (float) (y + txtOffset)
            );
            g.draw(wedge);
//            g.setColor(Color.RED);
//            g.draw(GeometryUtils.approximate(shape));
        });
         */

 /*
        new MinimalAggregateShapeDouble(shape).visitAnglesWithArcs(new ArcsVisitor() {
            @Override
            public void visit(int index, double angle1, double x1, double y1,
                    double angle2, double x2, double y2, Rectangle2D bounds,
                    double apexX, double apexY, double offsetX,
                    double offsetY, double midAngle) {
                CornerAngle ca = new CornerAngle(angle1, angle2);
                if (ca.direction() == dir) {
                    ca = ca.opposite();
                }
                String txt = GeometryUtils.toShortString(ca.normalized().extent()) + "\u00B0";
                g.drawString(txt, (float) offsetX, (float) offsetY);
                g.setColor(Color.BLUE.brighter());
                g.draw(ca.toShape(apexX, apexY, 20));
            }
        }, 25);
         */
    }

    private void paintDecorations(Graphics2D g, ShapeElement el, Shape shape) {
        if (!uiState.focusDecorationsPainted()) {
            return;
        }
        ObjectState state = getState();
        if (state.isFocused()) {
            decorationPainting.setupFocusedPainting(getScene().getZoomFactor(), g, g1 -> {
                g1.draw(shape);
            });
        } else if (state.isSelected()) {
            decorationPainting.setupFocusedPainting(getScene().getZoomFactor(), g, g1 -> {
                g1.draw(shape);
            });
        }
    }
}
