package org.imagine.vector.editor.ui.tools;

import com.sun.glass.events.KeyEvent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import org.imagine.utils.Holder;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import static org.imagine.vector.editor.ui.tools.ControlPointWidget.BASE_SIZE;
import org.imagine.vector.editor.ui.tools.DMA.TranslateHandler;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
class ShapeWidget extends Widget implements DragNotifier {

    private final ShapeElement entry;
    private final WidgetController ctrllr;
    private final ShapesCollection shapes;
    private ShapeElement temp;
    private final Lookup lkp;

    @Messages({
        "MOVE_UP=Move Up",
        "MOVE_DOWN=Move Down",
        "MOVE_LEFT=Move Left",
        "MOVE_RIGHT=Move Right",
        "# {0} - degrees",
        "# {1} - name",
        "ROTATE_CLOCKWISE=Rotate {1} {0}\u00B0 Clockwise",
        "# {0} - degrees",
        "# {1} - name",
        "ROTATE_COUNTERCLOCKWISE=Rotate {1} {0}\u00B0 Counter-Clockwise",})
    ShapeWidget(Scene scene, ShapeElement entry, ShapesCollection shapes, Holder<PaintParticipant.Repainter> repainter, WidgetController ctrllr, MutableProxyLookup lookup, Runnable refresh) {
        super(scene);
        this.shapes = shapes;
        this.ctrllr = ctrllr;
        this.entry = entry;
        lkp = Lookups.fixed(entry, entry.item(), th, shapes, this);
        getActions().addAction(new DoubleMoveAction(DoubleMoveStrategy.FREE, DMA.INSTANCE));
        getActions().addAction(ActionFactory.createPopupMenuAction((Widget widget, Point localLocation) -> {
            Point2D scenePoint = DoubleMoveAction.convertLocalToScene(widget, localLocation);
            return ShapeActions.populatePopup(entry, shapes, refresh, widget, scenePoint);
        }));
        getActions().addAction(new KeyboardActions(entry));
    }

    private boolean isFocused() {
        return Utilities.actionsGlobalContext().lookupAll(ShapeWidget.class)
                .contains(this);
    }

    private WidgetAction.State rotateBy(double deg, String action) {
        Rectangle2D rect = new Rectangle2D.Double();
        entry.addToBounds(rect);
        AffineTransform xform = AffineTransform.getRotateInstance(Math.toRadians(deg), rect.getCenterX(), rect.getCenterY());
        shapes.edit(action, entry, () -> {
            entry.applyTransform(xform);
            revalidate();
            getScene().validate();
            repaint();

        }).hook(() -> {
            revalidate();
            getScene().validate();
            repaint();
        });
        return WidgetAction.State.CONSUMED;
    }

    private WidgetAction.State moveBy(int x, int y, String action) {
        double zoom = 1D / getScene().getZoomFactor();
        double dx = x * zoom;
        double dy = y * zoom;
        shapes.edit(action, entry, () -> {
            entry.translate(dx, dy);
            revalidate();
            getScene().validate();
            repaint();
        }).hook(() -> {
            revalidate();
            getScene().validate();
            repaint();
        });
        return WidgetAction.State.CONSUMED;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    private boolean dragInProgress;
    TranslateHandler th = new TranslateHandler() {
        @Override
        public void onStart(Point p) {
            dragInProgress = true;
        }

        @Override
        public void translate(double x, double y) {
            ShapeWidget.this.shapes.edit("Move", entry, () -> {
                entry.translate(x, y);
                revalidate();
                dragInProgress = false;
                getScene().repaint();
            }).hook(() -> {
                revalidate();
                getScene().revalidate();
                getScene().repaint();
            });
        }
    };

    @Override
    protected Rectangle calculateClientArea() {
        return entry().shape().getBounds();
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();
        if (!dragInProgress) {
            GraphicsUtils.setHighQualityRenderingHints(g);
        }
        boolean adjusting = isAdjusting();
        if (adjusting) {
            g.setXORMode(Color.LIGHT_GRAY);
        } else {
            GraphicsUtils.setHighQualityRenderingHints(g);
        }
        entry().paint(g, g.getClipBounds());
        if (adjusting) {
            g.setPaintMode();
        }
    }

    boolean isAdjusting() {
        return temp != null;
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        return entry().shape().contains(localLocation);
    }

    private ShapeElement entry() {
        if (temp != null) {
            return temp;
        }
        return entry;
    }

    @Override
    public ShapeElement onStartControlPointDrag() {
        return temp = entry.copy();
    }

    @Override
    public void onEndControlPointDrag() {
        temp = null;
        revalidate();
        repaint();
    }

    @Override
    public void onControlPointDragUpdate(int controlPointIndex, double offX, double offY) {
        if (temp != null) {
            ControlPoint[] pts = temp.controlPoints(BASE_SIZE, ignored -> {

            });
            ControlPoint pt = pts[controlPointIndex];
            pt.set(offX, offY);
        }
    }

    private class KeyboardActions extends WidgetAction.Adapter {

        private final ShapeElement entry;

        public KeyboardActions(ShapeElement entry) {
            this.entry = entry;
        }

        @Override
        public WidgetAction.State keyPressed(Widget widget, WidgetAction.WidgetKeyEvent event) {
            if (isFocused()) {
                if (!event.isControlDown() && !event.isAltDown()) {
                    int mult = event.isShiftDown() ? 10 : 1;
                    switch (event.getKeyCode()) {
                        case KeyEvent.VK_UP:
                            return moveBy(0, -1 * mult, Bundle.MOVE_UP());
                        case KeyEvent.VK_DOWN:
                            return moveBy(0, 1 * mult, Bundle.MOVE_DOWN());
                        case KeyEvent.VK_LEFT:
                            return moveBy(-1 * mult, 0, Bundle.MOVE_LEFT());
                        case KeyEvent.VK_RIGHT:
                            return moveBy(1 * mult, 0, Bundle.MOVE_RIGHT());
                    }
                } else if (event.isControlDown()) {
                    double mult = event.isShiftDown() ? 10 : 1;
                    String nm = entry.isNameSet() ? entry.getName()
                            : ShapeNames.nameOf(entry.item());
                    switch (event.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            return rotateBy(-1D * mult,
                                    Bundle.ROTATE_COUNTERCLOCKWISE(mult, nm));
                        case KeyEvent.VK_RIGHT:
                            return rotateBy(mult,
                                    Bundle.ROTATE_CLOCKWISE(mult, nm));
                    }
                }
            }
            return super.keyPressed(widget, event);
        }
    }
}
