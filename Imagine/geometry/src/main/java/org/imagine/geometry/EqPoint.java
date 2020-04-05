/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import java.awt.Point;
import java.awt.geom.Point2D;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;

/**
 * A Point2D.Float which provides a reasonable (tolerance based) implementation
 * of equals() and hashCode().
 *
 * @author Tim Boudreau
 */
public class EqPoint extends Point2D.Float implements Comparable<Point2D> {

    public EqPoint() {
    }

    public EqPoint(float x, float y) {
        super(x, y);
    }

    public EqPoint(double x, double y) {
        super((float) x, (float) y);
    }

    public EqPoint(Point2D p) {
        this(p.getX(), p.getY());
    }

    public static EqPoint of(Point2D p) {
        if (p == null) {
            return null;
        }
        if (p instanceof EqPoint) {
            return (EqPoint) p;
        }
        return new EqPoint(p);
    }

    public Point toPoint() {
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof Point2D) {
            Point2D p = (Point2D) o;
            return GeometryUtils.isSamePoint(p, this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return GeometryUtils.pointHashCode(x, y);
    }

    @Override
    public String toString() {
        return GeometryStrings.toString(x, y);
    }

    @Override
    public int compareTo(Point2D o) {
        int result = java.lang.Double.compare(y, o.getY());
        if (result == 0) {
            result = java.lang.Double.compare(x, o.getX());
        }
        return result;
    }
}
