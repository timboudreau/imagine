package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapCoordinate;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Arrow;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapeInfo.PointInfo;

/**
 *
 * @author Tim Boudreau
 */
public class AnglePainter extends OneTypePainter {

    private final Arrow arrow1 = new Arrow(0, 0, 1, 1);
    private final Arrow arrow2 = new Arrow(0, 0, 1, 1);
    private final TextPainter text = new TextPainter();
    private BasicStroke lastStroke;

    @Override
    protected boolean snapBoth(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        ShapeSnapPointEntry e = x.value();

        SnapUISettings set = SnapUISettings.getInstance();

        arrow1.headLength = arrow2.headLength =
                set.arrowHeadLength(zoom);

        if (x.basis() != Double.MIN_VALUE && y.basis() != Double.MIN_VALUE) {
            text.setDegrees(x.value().sizeOrAngle);
            PointInfo pi = e.entry.shapeInfo().forTrailingLineAngle(e.sizeOrAngle);
            boolean tryFlipped = pi == null;
            if (tryFlipped) {
                pi = e.entry.shapeInfo().forTrailingLineAngle(Angle.opposite(e.sizeOrAngle));
            }
            if (pi != null) {
                double offset = set.lineOffset(zoom);
                // XXX this should not be leadingLine here, but that
                // is getting the right result.
                EqLine ln = pi.vector.leadingLine();
                ln.shiftPerpendicular(offset);
                arrow1.setLine(ln);

                if (pi.vector.toTriangle().contains(ln.midPoint())) {
                    ln.shiftPerpendicular(offset * -2);
                }

                EqLine oth = new EqLine(x.coordinate(), y.coordinate(),
                        x.basis(), y.basis());

                oth.shiftPerpendicular(offset);
                if (selection != null) {
                    if (selection.shape().contains(oth.midPoint())) {
                        oth.shiftPerpendicular(offset * -2);
                    }
                }

                arrow2.setLine(oth);
                return true;
            }
        }

        double[] d = new double[e.entry.getControlPointCount() * 2];
        e.entry.item().as(Adjustable.class).getControlPoints(d);
        int ix1 = e.controlPoint1 * 2;
        int ix2 = e.controlPoint2 * 2;

        double dist = Point2D.distance(d[ix1], d[ix1 + 1], d[ix2], d[ix2 + 1]);
        double ang = e.sizeOrAngle;
        text.setDegrees(x.value().sizeOrAngle);
        Circle.positionOf(ang, x.coordinate() + 5, y.coordinate() + 5,
                dist, (xx, yy) -> {
                    arrow1.setPoints(x.coordinate() + 5, y.coordinate() + 5, xx, yy);
                });

        Circle.positionOf(ang, d[ix1] + 5, d[ix1 + 1] + 5, dist, (xx, yy) -> {
            arrow2.setPoints(d[ix1] + 5, d[ix1 + 1] + 5, xx, yy);
        });
        return true;
    }

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(arrow1, lastStroke);
        handle.repaintArea(arrow2, lastStroke);
        text.requestRepaint(handle);
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        text.paint(g, zoom, arrow2, true);
        SnapUISettings settings = SnapUISettings.getInstance();
        lastStroke = settings.decorationStroke(SnapKind.ANGLE, selected, zoom);
        g.setStroke(lastStroke);
        double shadowOffset = settings.drawShadowOffset(SnapKind.ANGLE, zoom);
        Circle.positionOf(Angle.perpendicularClockwise(arrow1.angle()), 0, 0, shadowOffset, (sx, sy) -> {
            g.translate(-sx, -sy);
            g.setPaint(settings.drawShadowColor(SnapKind.ANGLE, selected));
            g.draw(arrow1);
            g.draw(arrow2);
            g.translate(sx, sy);
        });
        g.setPaint(settings.drawColor(SnapKind.ANGLE, selected));
        g.draw(arrow1);
        g.draw(arrow2);
    }
}
