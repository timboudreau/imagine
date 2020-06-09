/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.markdown.util;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
public class RectangleCollection implements Shape {

    private List<Rectangle2D.Float> rects = new ArrayList<>();

    public static Rectangle2D.Float copy(Rectangle2D.Float rect) {
        return new Rectangle2D.Float(rect.x, rect.y, rect.width, rect.height);
    }

    private Rectangle2D.Float last() {
        return rects.isEmpty() ? null : rects.get(rects.size() - 1);
    }

    private boolean replaceLastIfMatches(Rectangle2D.Float curr) {
        Rectangle2D.Float last = last();
        if (last == null) {
            rects.add(curr);
            return false;
        }
        if (last.x + last.width <= curr.x && last.y == curr.y && last.height >= curr.height) {
            last = copy(last);
            last.add(curr);
            rects.set(rects.size() - 1, last);
            return true;
        }
        rects.add(curr);
        return false;
    }

    public void addCopy(Rectangle2D.Float rect) {
        add(copy(rect));
    }

    public void add(Rectangle2D.Float rect) {
        replaceLastIfMatches(rect);
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        if (rects.isEmpty()) {
            return new Rectangle();
        }
        Rectangle2D.Float rect = null;
        for (Rectangle2D.Float r : rects) {
            if (rect == null) {
                rect = copy(r);
            } else {
                rect.add(r);
            }
        }
        return rect;
    }

    @Override
    public boolean contains(double x, double y) {
        for (Rectangle2D r : rects) {
            if (r.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        for (Rectangle2D r : rects) {
            if (r.intersects(x, y, w, h)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return getBounds2D().contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public Shape toShape() {
        return new AggregateShape(rects);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return new AggregateShape(rects).getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    public boolean isEmpty() {
        if (rects.isEmpty()) {
            return true;
        }
        for (Rectangle2D r : rects) {
            if (!r.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
