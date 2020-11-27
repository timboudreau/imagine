/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.Supplier;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapCoordinate;
import com.mastfrog.geometry.Arrow;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
final class DistancePainter extends OneTypePainter {

    private final Arrow arrowX1 = new Arrow();
    private final Arrow arrowX2 = new Arrow();
    private final Arrow arrowY1 = new Arrow();
    private final Arrow arrowY2 = new Arrow();

    private boolean xActive;
    private boolean yActive;
    private boolean hasXb;
    private boolean hasYb;
    private final Supplier<Rectangle> viewBounds;

    DistancePainter(Supplier<Rectangle> viewBounds) {
        arrowX1.headAngleA = arrowX1.headAngleB
                = arrowX2.headAngleA = arrowX2.headAngleB = 135;
        this.viewBounds = viewBounds;
    }

    @Override
    protected void requestRepaint(RepaintHandle handle) {
//        if (xActive) {
        handle.repaintArea(arrowX1, lastStroke);
//            if (hasXb) {
        handle.repaintArea(arrowX2, lastStroke);
//            }
//        }
//        if (yActive) {
        handle.repaintArea(arrowY1, lastStroke);
//            if (hasYb) {
        handle.repaintArea(arrowY2, lastStroke);
//            }
//        }
    }

    BasicStroke lastStroke;

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        SnapUISettings s = SnapUISettings.getInstance();
//        lastStroke = s.indicatorStroke(SnapKind.DISTANCE, selected, zoom);
        lastStroke = s.decorationStroke(SnapKind.DISTANCE, selected, zoom);

        g.setStroke(lastStroke);
        g.setPaint(s.drawColor(SnapKind.DISTANCE, selected));
        if (xActive) {
            g.draw(arrowX1);
        }
        if (yActive) {
            g.draw(arrowY1);
        }
        g.setPaint(s.drawColor(SnapKind.DISTANCE, selected));
        if (xActive && hasXb) {
            g.draw(arrowX2);
        }
        if (yActive && hasYb) {
            g.draw(arrowY2);
        }
    }

    @Override
    protected boolean snapBoth(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        xActive = snapOne(x, zoom, selection);
        yActive = snapOne(y, zoom, selection);
        return xActive || yActive;
    }

    @Override
    protected boolean snapOne(SnapCoordinate<ShapeSnapPointEntry> pt, Zoom zoom, ShapeElement selection) {
        ShapeSnapPointEntry e = pt.value();
        double margin = zoom.inverseScale(5);
        Arrow arrowA, arrowB;
        boolean hasControlPoints = e.controlPoint1 >= 0 && e.controlPoint2 >= 0;
        switch (pt.axis()) {
            case X:
                arrowA = arrowX1;
                arrowB = arrowX2;
                hasXb = hasControlPoints;
                break;
            case Y:
                arrowA = arrowY1;
                arrowB = arrowY2;
                hasYb = hasControlPoints;
                break;
            default:
                throw new AssertionError(pt.axis());
        }

        Rectangle b = viewBounds.get();
        switch (pt.axis()) {
            case X:
                arrowA.x1 = arrowA.x2 = pt.coordinate();
                arrowA.y1 = b.y + (b.height / 2);
                arrowA.setLengthFromPoint1(pt.value().sizeOrAngle);
                break;
            case Y:
                arrowA.y1 = arrowA.y2 = pt.coordinate();
                arrowA.x1 = b.x + (b.width / 2);
                arrowA.setLengthFromPoint1(pt.value().sizeOrAngle);
                break;

        }
        if (hasControlPoints) {
            double[] d = new double[e.entry.getControlPointCount() * 2];
            e.entry.item().as(Adjustable.class).getControlPoints(d);
            int ix1 = e.controlPoint1 * 2;
            int ix2 = e.controlPoint2 * 2;
            arrowB.x1 = d[ix1];
            arrowB.x2 = d[ix2];
            arrowB.y1 = d[ix1 + 1];
            arrowB.y2 = d[ix2 + 1];
        } else {
            arrowB.setPoints(0, 0, 0, 0);
        }
        switch (pt.axis()) {
            case X:
                arrowA.y1 += margin;
                arrowA.y2 += margin;
                if (hasControlPoints) {
                    arrowB.y1 += margin;
                    arrowB.y2 += margin;
                }
                break;
            case Y:
                arrowA.x1 += margin;
                arrowA.x2 += margin;
                if (hasControlPoints) {
                    arrowB.x1 += margin;
                    arrowB.x2 += margin;
                }
        }
        return true;
    }
}
