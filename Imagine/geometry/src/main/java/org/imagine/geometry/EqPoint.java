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
public class EqPoint extends Point2D.Float {
    private static final double TOLERANCE = 0.00001D;

    public EqPoint() {
    }

    public EqPoint(float x, float y) {
        super(x, y);
    }

    public EqPoint(Point2D.Float p) {
        this(p.x, p.y);
    }

    public static EqPoint of(Point2D.Float p) {
        if (p == null) {
            return null;
        }
        if (p instanceof EqPoint) {
            return (EqPoint) p;
        }
        return new EqPoint(p);
    }

    public String toString() {
        return "EqPoint[" + x + "," + y + "]";
    }

    public Point toPoint() {
        return new Point((int) x, (int) y);
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof Point2D) {
            Point2D p = (Point2D) o;
            double ox = p.getX();
            double oy = p.getY();
            double x = getX();
            double y = getY();
            if (ox == x && oy == y) {
                return true;
            }
            double xdiff = Math.abs(ox - x);
            double ydiff = Math.abs(oy - y);
            return xdiff < TOLERANCE && ydiff < TOLERANCE;
        }
        return false;
    }

    public int hashCode() {
        int xx = (int) (x * Integer.MAX_VALUE);
        int yy = (int) (x * Integer.MAX_VALUE);
        return xx + 971 * yy;
    }
}
