/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget;

import java.util.HashSet;
import java.util.Set;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.tools.widget.collections.Refiner;

/**
 *
 * @author Tim Boudreau
 */
public class ShapeControlPointRefiner implements Refiner<Set<ShapeControlPoint>> {

    public static final ShapeControlPointRefiner INSTANCE = new ShapeControlPointRefiner();

    private final Set<ShapeControlPoint> result = new HashSet<>(100);

    @Override
    public Set<ShapeControlPoint> refine(double tMinX, double tMaxX, double tMinY,
            double tMaxY, Set<ShapeControlPoint> pts) {
        if (pts == null || pts.isEmpty()) {
            return pts;
        }
        result.clear();
//        System.out.println("Refine " + tMinX + " - " + tMaxX + " / " + tMinY
//                + " - " + tMaxY);
        if (pts != null) {
            for (ShapeControlPoint pt : pts) {
                double x = pt.getX();
                if (x >= tMinX && x <= tMaxX) {
                    double y = pt.getY();
                    if (y >= tMinY && y <= tMaxY) {
                        result.add(pt);
                    }
                }
            }
        }
        return result;
    }
}
