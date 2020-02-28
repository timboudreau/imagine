/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.dev.java.imagine.api.tool.aspects.snap;

import java.awt.geom.Point2D;
import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
public class SnapPoint implements Comparable<SnapPoint> {

    final Axis axis;
    final double coordinate;

    public SnapPoint(Axis axis, double coordinate) {
        this.axis = axis;
        this.coordinate = coordinate;
    }

    public double coordinate() {
        return coordinate;
    }

    public Axis axis() {
        return axis;
    }

    public double distance(double x, double y) {
        double c = axis == Axis.X ? x : y;
        return Math.abs(c - coordinate);
    }

    public double distance(Point2D p) {
        double c = axis.value(p);
        return Math.abs(c - coordinate);
    }

    @Override
    public String toString() {
        return axis + ":" + coordinate;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.axis);
        hash = 83 * hash + (int) (Double.doubleToLongBits(Math.floor(this.coordinate) + 0.5) ^ (Double.doubleToLongBits(this.coordinate) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SnapPoint other = (SnapPoint) obj;
        if (Math.abs(this.coordinate - other.coordinate) < 0.5) {
            return true;
        }
        if (this.axis != other.axis) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(SnapPoint o) {
        int result = axis.compareTo(o.axis);
        if (result == 0) {
            result = Double.compare(coordinate, o.coordinate);
        }
        return result;
    }

}
