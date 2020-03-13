package org.netbeans.paintui;

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
public final class GridWidget extends LayerWidget implements ChangeListener {

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

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    @Override
    protected void paintWidget() {
        if (grid.isEnabled()) {
            double zoom = getScene().getZoomFactor();
            Graphics2D g = getGraphics();
            if (zoom != 1D) {
                g.setPaint(grid.getGridPaint(zoom));
                double inv = 1D / zoom;
                g.scale(inv, inv);
            } else {
                g.setPaint(grid.getGridPaint());
            }
            Rectangle r = getScene().getBounds();
            g.fillRect(0, 0, (int) Math.ceil(r.width * zoom), (int) Math.ceil(r.height * zoom));
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        setVisible(grid.isEnabled());
        revalidate();
        repaint();
    }

}
