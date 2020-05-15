/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.tools.path;

import java.awt.geom.Point2D;
import org.imagine.geometry.path.PathElement;
import org.imagine.geometry.path.PointKind;

/**
 *
 * @author Tim Boudreau
 */
abstract class EnhPoint2D extends Point2D {

    public abstract PointKind kind();

    public abstract PathElement owner();

    public void setX(double x) {
        setLocation(x, getY());
    }

    public void setY(double y) {
        setLocation(getX(), y);
    }

    public void move(double dx, double dy) {
        setLocation(getX() + dx, getY() + dy);
    }

    public boolean ifHit(double x, double y, double radius, DistPointConsumer c) {
        double dist = distance(x, y, radius);
        boolean result = dist != java.lang.Double.MAX_VALUE;
        if (result) {
            c.hit(this, dist);
        }
        return result;
    }

    public double distance(double x, double y, double radius) {
        double dist = distance(x, y);
        if (dist > radius) {
            return java.lang.Double.MAX_VALUE;
        }
        return dist;
    }

    public boolean isDestination() {
        return kind().isDestination();
    }

}
