/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.geometry;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 *
 * @author Tim Boudreau
 */
public class EqPointDouble extends Point2D.Double {
    private static final double TOLERANCE = 0.0000001D;

    public EqPointDouble() {
    }

    public EqPointDouble(double x, double y) {
        super(x, y);
    }

    public EqPointDouble(Point2D p) {
        this(p.getX(), p.getY());
    }

    public static EqPointDouble of(Point2D p) {
        if (p == null) {
            return null;
        }
        if (p instanceof EqPointDouble) {
            return (EqPointDouble) p;
        }
        return new EqPointDouble(p);
    }

    @Override
    public String toString() {
        return "EqPoint[" + x + "," + y + "]";
    }

    public Point toPoint() {
        return new Point((int) x, (int) y);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof Point2D) {
            Point2D p = (Point2D) o;
            double ox = p.getX();
            double oy = p.getY();
            if (ox == x && oy == y) {
                return true;
            }
            double xdiff = Math.abs(ox - x);
            double ydiff = Math.abs(oy - y);
            return xdiff < TOLERANCE && ydiff < TOLERANCE;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int xx = (int) (x * Integer.MAX_VALUE);
        int yy = (int) (x * Integer.MAX_VALUE);
        return xx + 971 * yy;
    }
}
