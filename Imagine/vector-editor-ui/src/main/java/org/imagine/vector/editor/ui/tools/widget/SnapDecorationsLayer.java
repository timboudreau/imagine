package org.imagine.vector.editor.ui.tools.widget;

import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.OnSnap;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.SnapLinesPainter;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;

/**
 *
 * @author Tim Boudreau
 */
class SnapDecorationsLayer extends LayerWidget implements RepaintHandle {

    private final SnapLinesPainter painter;

    public SnapDecorationsLayer(Scene scene) {
        super(scene);
        painter = new SnapLinesPainter(this, () -> {
            return getScene().getBounds();
        });
    }

    public OnSnap<ShapeSnapPointEntry> snapListener() {
        return painter;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    @Override
    protected void paintWidget() {
        painter.paint(getGraphics(), z);
    }

    private final Z z = new Z();

    class Z implements Zoom {

        @Override
        public float getZoom() {
            return (float) getScene().getZoomFactor();
        }

        @Override
        public void setZoom(float val) {
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
        }
    }

    @Override
    public void repaintArea(int x, int y, int w, int h) {
        JComponent v = getScene().getView();
        if (v != null) {
            Rectangle rect = new Rectangle(x, y, w, h);
            getScene().convertSceneToView(rect);
            v.repaint(rect);
        }
    }
}
