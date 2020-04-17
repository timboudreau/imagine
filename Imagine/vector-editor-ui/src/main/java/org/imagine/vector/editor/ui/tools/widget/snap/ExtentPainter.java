package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapCoordinate;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.geometry.Circle;
import org.imagine.geometry.CornerAngle;
import org.imagine.geometry.PieWedge;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapeInfo;

/**
 *
 * @author Tim Boudreau
 */
public class ExtentPainter extends OneTypePainter {

    private final PieWedge wedge1 = new PieWedge();
    private final PieWedge wedge2 = new PieWedge();
    private final TextPainter text = new TextPainter();
    private BasicStroke lastStroke;

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(wedge1, lastStroke);
        handle.repaintArea(wedge2, lastStroke);
        handle.repaintArea(0, 0, 1000, 1000);
        text.requestRepaint(handle);
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        SnapUISettings set = SnapUISettings.getInstance();
        lastStroke = set.decorationStroke(SnapKind.EXTENT, selected, zoom);
        g.setStroke(lastStroke);

        double shadowOffset = set.drawShadowOffset(SnapKind.EXTENT, zoom);

        Circle.positionOf(wedge1.angle(), 0, 0, shadowOffset, (sx, sy) -> {
            g.translate(-sx, -sy);
            g.setPaint(set.drawShadowColor(SnapKind.EXTENT, selected));
            g.draw(wedge1);
            g.draw(wedge2);
            g.translate(sx, sy);
        });

        g.setPaint(set.drawColor(SnapKind.EXTENT, selected));
        g.draw(wedge1);
        g.draw(wedge2);

        text.paint(g, zoom, wedge1);
    }

    @Override
    protected boolean snapBoth(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        ShapeSnapPointEntry se = x.value();
        ShapeElement en = se.entry;
        double trailingAngle = x.basis();
        double leadingAngle = y.basis();

        ShapeInfo info = en.shapeInfo();
        int cpPrev = se.controlPoint1;
        int cpTarget = cpPrev == info.size() - 1 ? 0 : cpPrev + 1;

        ShapeInfo.PointInfo pt = info.forControlPoint(cpTarget);

        SnapUISettings set = SnapUISettings.getInstance();

        CornerAngle ca = new CornerAngle(trailingAngle, leadingAngle);

        double ext = ca.extent();
        if (ext < 0) {
            ext += 360;
        }
        text.setDegrees(ext);

        wedge1.setCenter(x.coordinate(), y.coordinate());
        double rad = set.wedgeSize(SnapKind.EXTENT, zoom);
        wedge1.setRadius(rad);
        wedge1.setAngleAndExtent(ca);

        if (pt != null) {
            wedge2.setAngleAndExtent(pt.angle);
            wedge2.setCenter(pt.vector.apexX(), pt.vector.apexY());
            wedge2.setRadius(rad);
        } else {
            wedge2.setRadius(0);
        }
        return true;
    }
}
