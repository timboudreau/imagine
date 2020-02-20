/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.imagine.geometry;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

/**
 *
 * @author Tim Boudreau
 */
public final class EqLine extends Line2D.Double {

    private static final DecimalFormat FMT = new DecimalFormat(
            "#####################0.0#####################");

    public EqLine() {
    }

    public EqLine(double d, double d1, double d2, double d3) {
        super(d, d1, d2, d3);
    }

    public EqLine(Point2D pd, Point2D pd1) {
        super(pd, pd1);
    }

    public void translate(double x, double y) {
        x1 += x;
        y1 += y;
        x2 += x;
        y2 += y;
    }

    public EqLine rotatedBy(double angle) {
        if (angle == 0D || angle == 360D || angle % 360D == 0) {
            return this;
        }
        Rectangle2D bds = getBounds2D();
        Circle circ = new Circle(bds.getCenterX(), bds.getCenterY(), length() / 2D);
        return circ.line(angle);
    }

    public EqLine translated(double x, double y) {
        return new EqLine(x1 + x, y1 + y, x2 + x, y2 + y);
    }

    public boolean fuzzyEquals(Line2D other) {
        return equals(other, 0.00000001);
    }

    public boolean equals(Line2D other, double tolerance) {
        if (equals(other)) {
            return true;
        }
        return (Math.abs(x1 - other.getX1()) < tolerance)
                && (Math.abs(x2 - other.getX2()) < tolerance)
                && (Math.abs(y1 - other.getY1()) < tolerance)
                && (Math.abs(y2 - other.getY2()) < tolerance);
    }

    public double length() {
        return Point2D.distance(x1, y1, x2, y2);
    }

    public String toString() {
        return FMT.format(x1) + "," + FMT.format(y1) + " <-> "
                + FMT.format(x2) + "," + FMT.format(y2);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof Line2D) {
            Line2D other = (Line2D) o;
            return other.getX1() == x1 && other.getX2() == x2
                    && other.getY1() == y1 && other.getY2() == y2;
        }
        return false;
    }

    public int hashCode() {
        long l1 = java.lang.Double.doubleToLongBits(x1);
        long l2 = java.lang.Double.doubleToLongBits(y1);
        long l3 = java.lang.Double.doubleToLongBits(x2);
        long l4 = java.lang.Double.doubleToLongBits(y2);
        int hash = 5;
        hash += l1 * 37;
        hash += l2 * 91;
        hash += l3 * 51;
        hash += l4 * 3;
        return hash;
    }
}
