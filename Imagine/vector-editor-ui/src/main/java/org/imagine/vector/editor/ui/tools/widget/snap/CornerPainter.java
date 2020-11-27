package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapCoordinate;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.CornerAngle;
import com.mastfrog.geometry.LineVector;
import com.mastfrog.geometry.PieWedge;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapeInfo.PointInfo;

/**
 *
 * @author Tim Boudreau
 */
public class CornerPainter extends OneTypePainter {

    private final PieWedge wedge1 = new PieWedge();
    private final PieWedge wedge2 = new PieWedge();
    private final TextPainter text = new TextPainter();
    private float lineWidth = 1;

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(wedge1, lineWidth);
        handle.repaintArea(wedge2, lineWidth);
        text.requestRepaint(handle);
        handle.repaintArea(0, 0, 1000, 1000);
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        SnapUISettings set = SnapUISettings.getInstance();
        BasicStroke strk = set.decorationStroke(SnapKind.CORNER, selected, zoom);
        g.setStroke(strk);
        lineWidth = strk.getLineWidth();
        g.setPaint(set.drawShadowColor(SnapKind.CORNER, selected));
        double shadowOffset = set.drawShadowOffset(SnapKind.EXTENT, zoom);
        Circle.positionOf(wedge1.angle(), 0, 0, shadowOffset, (sx, sy) -> {
            g.translate(-sx, -sy);
            g.setPaint(set.drawShadowColor(SnapKind.EXTENT, selected));
            g.draw(wedge1);
            g.draw(wedge2);
            g.translate(sx, sy);
        });

        g.setPaint(set.drawColor(SnapKind.CORNER, selected));
        g.draw(wedge1);
        g.draw(wedge2);
        text.paint(g, zoom, wedge2);
    }

    private static final double SIZE = 50;

    @Override
    public boolean onSnap(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        CornerAngle ca = CornerAngle.decodeCornerAngle(x.value().sizeOrAngle);
        double sz = SnapUISettings.getInstance().wedgeSize(SnapKind.CORNER, zoom);

        if (x.value() != null && x.basis() != Double.MIN_VALUE && x.value().entry != null) {
            PointInfo orig = x.value().entry.shapeInfo().forControlPoint(x.value().controlPoint1);
            //x.value().entry.shapeInfo().forCornerAngle(x.value().sizeOrAngle);
            if (orig != null) {
                LineVector v = orig.vector;
                wedge1.setFrom(v);
                wedge1.setRadius(sz);

                LineVector v2
                        = v.withApex(x.coordinate(), y.coordinate());
//                LineVector v2
//                        = orig.angle.toLineVector(
//                                v.trailingPoint(),
//                                v.leadingPoint());

                wedge2.setFrom(v2);
                wedge2.setRadius(sz);

                text.setDegrees(orig.angle.extent());

                return true;
            }
        }

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
        return true;
    }

}
