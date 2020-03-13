package org.imagine.vector.editor.ui.tools.widget;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.imagine.editor.api.grid.Grid;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;

/**
 *
 * @author Tim Boudreau
 */
final class GridWidget extends LayerWidget implements ChangeListener {

    private final Grid grid;

    @SuppressWarnings(value = "LeakingThisInConstructor")
    GridWidget(Scene scene) {
        super(scene);
        setOpaque(false);
        grid = Grid.getInstance();
        setVisible(grid.isEnabled());
        // will be weakly referenced
        grid.addChangeListener(this);
    } // will be weakly referenced
    // will be weakly referenced
    // will be weakly referenced

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    @Override
    protected void paintWidget() {
        if (grid.isEnabled()) {
            Rectangle r = getScene().getBounds();
            double zoom = getScene().getZoomFactor();

            double gridSize = grid.size();
            double xOff = r.x % gridSize;
            double yOff = r.y % gridSize;

            int x = (int) Math.round((r.x + xOff) * zoom);
            int y = (int) Math.round((r.y + yOff) * zoom);

            Graphics2D g = getGraphics();
            if (zoom != 1D) {
                g.setPaint(grid.getGridPaint(zoom));
                double inv = 1D / zoom;
                g.scale(inv, inv);
            } else {
                g.setPaint(grid.getGridPaint());
            }
            g.fillRect(x, y, (int) Math.ceil(r.width * zoom),
                    (int) Math.ceil(r.height * zoom));
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        setVisible(grid.isEnabled());
        revalidate();
        repaint();
    }

}
