package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapPoint;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Arrow;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
public class AnglePainter extends OneTypePainter {

    private final Arrow arrow1 = new Arrow(0, 0, 1, 1);
    private final Arrow arrow2 = new Arrow(0, 0, 1, 1);

    @Override
    protected boolean snapBoth(SnapPoint<ShapeSnapPointEntry> x, SnapPoint<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        ShapeSnapPointEntry e = x.value();
        double[] d = new double[e.entry.getControlPointCount() * 2];
        e.entry.item().as(Adjustable.class).getControlPoints(d);

        arrow1.setPointAndAngle(x.coordinate() + 5,
                y.coordinate() + 5,
                Angle.opposite(e.sizeOrAngle), 130);

        int ix1 = e.controlPoint1 * 2;
        int ix2 = e.controlPoint2 * 2;
        arrow2.setPointAndAngle(d[ix1] + 5, d[ix1 + 1] + 5,
                Angle.opposite(e.sizeOrAngle), 130);

//        arrow1.
/*
        arrow1.x1 = d[ix1] + 5;
        arrow1.y1 = d[ix1 + 1] + 5;
        arrow1.x2 = d[ix2] + 5;
        arrow1.y2 = d[ix2 + 1] + 5;

        double xDiff = arrow1.x1 - arrow1.x2;
        double yDiff = arrow1.y1 - arrow1.y2;
        arrow2.x1 = x.coordinate();
        arrow2.y1 = y.coordinate();
        arrow2.x2 = arrow2.x1 - xDiff;
        arrow2.y2 = arrow2.x1 - yDiff;
         */
        return true;
    }

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(arrow1);
        handle.repaintArea(arrow2);
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        SnapUISettings settings = SnapUISettings.getInstance();
        BasicStroke stroke = settings.decorationStroke(SnapKind.ANGLE, selected, zoom);
        g.setStroke(stroke);
        g.setPaint(settings.drawColor(SnapKind.ANGLE, selected));
        g.draw(arrow1);
        g.draw(arrow2);
    }
}
