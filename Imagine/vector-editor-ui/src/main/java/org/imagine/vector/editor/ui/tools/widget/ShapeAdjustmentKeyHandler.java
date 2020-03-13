package org.imagine.vector.editor.ui.tools.widget;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.widget.actions.AdjustmentKeyActionHandler;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class ShapeAdjustmentKeyHandler implements AdjustmentKeyActionHandler {

    @Override
    public WidgetAction.State moveBy(Widget widget, int x, int y, String action) {
        ShapeElement entry = widget.getLookup().lookup(ShapeEntry.class);
        ShapesCollection shapes = widget.getLookup().lookup(ShapesCollection.class);
        if (entry != null && shapes != null) {
            double zoom = 1D / widget.getScene().getZoomFactor();
            double dx = x * zoom;
            double dy = y * zoom;
            shapes.edit(action, entry, () -> {
                entry.translate(dx, dy);
                widget.revalidate();
                widget.getScene().validate();
                widget.repaint();
            }).hook(() -> {
                widget.revalidate();
                widget.getScene().validate();
                widget.repaint();
            });
            return WidgetAction.State.CONSUMED;
        }
        return WidgetAction.State.REJECTED;
    }

    @Override
    public WidgetAction.State rotateBy(Widget widget, double deg, String action) {
        ShapeElement entry = widget.getLookup().lookup(ShapeEntry.class);
        ShapesCollection shapes = widget.getLookup().lookup(ShapesCollection.class);
        if (entry != null && shapes != null) {
            Rectangle2D rect = new Rectangle2D.Double();
            entry.addToBounds(rect);
            AffineTransform xform = AffineTransform.getRotateInstance(Math.toRadians(deg), rect.getCenterX(), rect.getCenterY());
            shapes.edit(action, entry, () -> {
                entry.applyTransform(xform);
                widget.revalidate();
                widget.getScene().validate();
                widget.repaint();
            }).hook(() -> {
                widget.revalidate();
                widget.getScene().validate();
                widget.repaint();
            });
            return WidgetAction.State.CONSUMED;
        }
        return WidgetAction.State.REJECTED;
    }

}
