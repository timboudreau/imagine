package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapCoordinate;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.EqLine;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
public class GridPainter extends OneTypePainter {

    private final Circle circ = new Circle();
    private final EqLine line = new EqLine();
    private BasicStroke stroke;

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(circ, stroke);
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        SnapUISettings settings = SnapUISettings.getInstance();
        stroke = settings.decorationStroke(SnapKind.GRID, selected, zoom);
        g.setStroke(stroke);
        g.setPaint(settings.fillColor(SnapKind.GRID, selected));
        g.fill(circ);
        g.setPaint(settings.drawColor(SnapKind.GRID, selected));
        g.draw(circ);
        double rad2 = (circ.radius() / 2) + zoom.inverseScale(3);
        line.setLine(circ.centerX(), circ.centerY() - rad2, circ.centerX(), circ.centerY() + rad2);
        g.draw(line);
        line.setLine(circ.centerX() - rad2, circ.centerY(), circ.centerX() + rad2, circ.centerY());
    }

    @Override
    protected boolean snapBoth(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        circ.setCenter(x.coordinate(), y.coordinate());
        circ.setRadius(zoom.inverseScale(SnapUISettings.getInstance().targetPointRadius(SnapKind.GRID, selection)));
        return true;
    }
}
