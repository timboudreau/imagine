package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapPoint;
import org.imagine.geometry.CornerAngle;
import org.imagine.geometry.PieWedge;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
public class CornerPainter extends OneTypePainter {

    private final PieWedge wedge1 = new PieWedge();
    private final PieWedge wedge2 = new PieWedge();
    private float lineWidth = 1;

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(wedge1, lineWidth);
        handle.repaintArea(wedge2, lineWidth);
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        SnapUISettings set = SnapUISettings.getInstance();
        BasicStroke strk = set.decorationStroke(SnapKind.CORNER, selected, zoom);
        g.setStroke(strk);
        lineWidth = strk.getLineWidth();
        g.setPaint(set.drawColor(SnapKind.CORNER, selected));
        g.draw(wedge1);
        g.draw(wedge2);
    }

    private static final double SIZE = 50;

    @Override
    public boolean onSnap(SnapPoint<ShapeSnapPointEntry> x, SnapPoint<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        CornerAngle ca = CornerAngle.decodeCornerAngle(x.value().sizeOrAngle);
        double sz = zoom.inverseScale(SIZE);
        wedge1.setRadius(sz);
        wedge1.setAngleAndExtent(ca.toSector());
        wedge1.setCenter(x.coordinate(), y.coordinate());

        Adjustable adj = x.value().entry.item().as(Adjustable.class);
        if (adj == null) {
            return false;
        }
        int ct = adj.getControlPointCount();
        double[] pts = new double[ct * 2];
        adj.getControlPoints(pts);
        int ix = x.value().controlPoint1;
        int iy = x.value().controlPoint2;
        ix *= 2;
        iy *= 2;

        wedge2.setAngleAndExtent(ca.toSector());
        wedge2.setRadius(sz);
        wedge2.setCenter(pts[ix], pts[ix + 1]);
        System.out.println("SNAP " + ca + " " + wedge1.getCenter() + " and "
                + wedge2.getCenter());
        return true;
    }

}
