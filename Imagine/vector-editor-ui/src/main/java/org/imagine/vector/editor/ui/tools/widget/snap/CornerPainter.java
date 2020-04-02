/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.snap;

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

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(wedge1);
        handle.repaintArea(wedge2);
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        SnapUISettings set = SnapUISettings.getInstance();
        g.setStroke(set.decorationStroke(SnapKind.CORNER, selected, zoom));
        g.setPaint(set.drawColor(SnapKind.CORNER, selected));
        g.draw(wedge1);
        g.draw(wedge2);
    }

    @Override
    public boolean onSnap(SnapPoint<ShapeSnapPointEntry> x, SnapPoint<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        CornerAngle ca = CornerAngle.decodeCornerAngle(x.value().sizeOrAngle);
        ca = ca.normalized().inverse().opposite();
        wedge1.setAngle(ca.aDegrees());
        wedge1.setExtent(ca.extent());
        wedge1.setCenter(x.coordinate(), y.coordinate());
        wedge1.setRadius(30);

        Adjustable adj = x.value().entry.item().as(Adjustable.class);
        if (adj == null) {
            return false;
        }
        int ct = adj.getControlPointCount();
        double[] pts = new double[ct * 2];
        adj.getControlPoints(pts);
        int ix = x.value().controlPoint1;
        int iy = x.value().controlPoint2;
        if (iy == -1) {
            iy = ct - 2;
        }
        ix *= 2;
        iy *= 2;

        wedge2.setAngle(ca.aDegrees());
        wedge2.setExtent(ca.extent());
        wedge2.setRadius(30);
        wedge2.setCenter(pts[iy], pts[iy + 1]);

        System.out.println("SNAP " + ca + " " + wedge1.getCenter() + " and "
                + wedge2.getCenter());

//        double[] pts = .
        return true;
    }

}
