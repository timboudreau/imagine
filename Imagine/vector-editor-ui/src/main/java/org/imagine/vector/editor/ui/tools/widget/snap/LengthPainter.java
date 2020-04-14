/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapCoordinate;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Arrow;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapeInfo;

/**
 *
 * @author Tim Boudreau
 */
final class LengthPainter extends OneTypePainter {

    private final Arrow arrow1 = new Arrow();
    private final Arrow arrow2 = new Arrow();
    private final EqLine dotted1 = new EqLine();
    private final EqLine dotted2 = new EqLine();
    private final EqLine dotted3 = new EqLine();
    private final EqLine dotted4 = new EqLine();
    private final TextPainter text = new TextPainter();
    private BasicStroke lastStroke;

    LengthPainter() {
        arrow1.headAngleA = arrow1.headAngleB
                = arrow2.headAngleA = arrow2.headAngleB = 90;
    }

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(arrow1, lastStroke);
        handle.repaintArea(arrow2, lastStroke);
        handle.repaintArea(dotted1, lastStroke);
        handle.repaintArea(dotted2, lastStroke);
        handle.repaintArea(dotted3, lastStroke);
        handle.repaintArea(dotted4, lastStroke);
        text.requestRepaint(handle);
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        text.paint(g, zoom, arrow2, true);
        SnapUISettings set = SnapUISettings.getInstance();
        g.setStroke(lastStroke = set.decorationStroke(SnapKind.LENGTH, selected, zoom));
        double shadowOffset = set.drawShadowOffset(SnapKind.LENGTH, zoom);
        Circle.positionOf(Angle.perpendicularClockwise(arrow1.angle()), 0, 0, shadowOffset, (sx, sy) -> {
            g.translate(-sx, -sy);
            g.setPaint(set.drawShadowColor(SnapKind.LENGTH, selected));
            g.draw(arrow1);
            g.draw(arrow2);
            g.translate(sx, sy);
        });

        g.setPaint(set.drawColor(SnapKind.LENGTH, selected));
        g.draw(arrow1);
        g.draw(arrow2);
        g.setStroke(set.indicatorStroke(SnapKind.LENGTH, selected, zoom));
        g.setPaint(set.indicatorLineColor(SnapKind.LENGTH, selected));
        g.draw(dotted1);
        g.draw(dotted2);
        g.draw(dotted3);
        g.draw(dotted4);
    }

    @Override
    protected boolean snapBoth(SnapCoordinate<ShapeSnapPointEntry> x,
            SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom,
            ShapeElement selection) {

        SnapUISettings set = SnapUISettings.getInstance();

        arrow1.headLength = arrow2.headLength = set.sizingHeadLength(zoom);

        ShapeSnapPointEntry sspe = x.value();
        ShapeEntry en = sspe.entry;
        double len = sspe.sizeOrAngle;
        text.setValue(len);

        ShapeInfo info = en.shapeInfo();
        ShapeInfo.PointInfo pt1 = info.forControlPoint(sspe.controlPoint1);
        ShapeInfo.PointInfo pt2 = info.forControlPoint(sspe.controlPoint2);

        EqLine line = new EqLine(x.coordinate(), y.coordinate(), x.basis(),
                y.basis());

        EqLine line2 = new EqLine(pt2.vector.apex(), pt1.vector.apex());

        double offset = set.lineOffset(zoom);
        if (line2.relativeCCW(x.coordinate(), y.coordinate()) == 1) {
            offset = -offset;
        }

        line.shiftPerpendicular(offset);
        line2.shiftPerpendicular(offset);

        arrow1.setLine(line2);
        arrow2.setLine(line);

        double originPerp = Angle.perpendicularClockwise(line.angle());
        double originOpposite = Angle.opposite(originPerp);

        double draggingPerp = Angle.perpendicularClockwise(line2.angle());
        double draggingOpposite = Angle.opposite(draggingPerp);

        double dist = zoom.inverseScale(60);
        Circle.positionOf(draggingPerp, pt1.vector.apexX(), pt1.vector.apexY(),
                dist, (lx1, ly1) -> {
                    Circle.positionOf(draggingOpposite, pt1.vector.apexX(),
                            pt1.vector.apexY(), dist, (lx2, ly2) -> {
                        dotted1.setLine(lx1, ly1, lx2, ly2);
                    });
                });
        Circle.positionOf(draggingPerp, pt2.vector.apexX(), pt2.vector.apexY(),
                dist, (lx1, ly1) -> {
                    Circle.positionOf(draggingOpposite, pt2.vector.apexX(),
                            pt2.vector.apexY(), dist, (lx2, ly2) -> {
                        dotted2.setLine(lx1, ly1, lx2, ly2);
                    });
                });
        Circle.positionOf(originPerp, line.x1, line.y1, dist,
                (lx1, ly1) -> {
                    Circle.positionOf(originOpposite, line.x1, line.y1, dist,
                            (lx2, ly2) -> {
                                dotted3.setLine(lx1, ly1, lx2, ly2);
                            });
                });
        Circle.positionOf(originPerp, line.x2, line.y2, dist,
                (lx1, ly1) -> {
                    Circle.positionOf(originOpposite, line.x2, line.y2, dist,
                            (lx2, ly2) -> {
                                dotted4.setLine(lx1, ly1, lx2, ly2);
                            });
                });
        return true;
    }

}
