/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Base class for shapes containing common logic for computing
 * things.
 *
 * @author Tim Boudreau
 */
public abstract class AbstractShape implements Shape {

    protected AbstractShape() {
    }

    /**
     * Add the bounds of this shape into the passed rectangle.
     * If the rectangle is empty, set its frame to this shape's bounding
     * rectangle, otherwise add its bounds to it.
     *
     * @param <T> The rectangle type
     * @param into The rectangle
     * @return the passed rectangle
     */
    public abstract <T extends Rectangle2D> T addToBounds(T into);

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        if (contains(x, y)
                || contains(x, y + h)
                || contains(x + w, y)
                || contains(x + w, y + h)) {
            return true;
        }
        Rectangle2D bds = getBounds2D();
        if (bds.contains(x, y, w, h)) {
            return true;
        } else if (new Rectangle2D.Double(x, y, w, h).contains(bds)) {
            return true;
        }
        return false;
    }

    @Override
    public final Rectangle getBounds() {
        return addToBounds(new Rectangle());
    }

    @Override
    public final Rectangle2D getBounds2D() {
        return addToBounds(new Rectangle2D.Double());
    }

    @Override
    public final boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return contains(x, y) && contains(x, y + h)
                && contains(x + w, y) && contains(x + w, y + h);
    }

    @Override
    public final boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }
}
