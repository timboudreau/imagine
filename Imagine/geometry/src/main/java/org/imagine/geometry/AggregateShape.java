package org.imagine.geometry;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A shape which simply merges a list of preexiting shapes, preserving the
 * originals and exposing a PathIterator that treats them as one. For
 * performance and memory footprint, {@link MinimalAggregateShapeDouble} or
 * {@link MinimalAggregateShapeFloat} may be preferable if you will not need to
 * recover the original Shape instances.
 *
 * @author Tim Boudreau
 */
public final class AggregateShape implements Shape, Iterable<Shape> {

    private final List<Shape> shapes;
    private final Rectangle2D.Double bounds = new Rectangle2D.Double();

    public AggregateShape(List<Shape> shapes) {
        this.shapes = shapes;
    }

    @Override
    public Rectangle getBounds() {
        return _getBounds2D().getBounds();
    }

    private void computeBounds() {
        for (Shape shape : shapes) {
            bounds.add(shape.getBounds2D());
        }
    }

    @Override
    public Rectangle2D getBounds2D() {
        return _getBounds2D().getBounds2D();
    }

    Rectangle2D _getBounds2D() {
        if (bounds.width == 0) {
            computeBounds();
        }
        return bounds;
    }

    @Override
    public boolean contains(double x, double y) {
        return _getBounds2D().contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return _getBounds2D().intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return _getBounds2D().contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return new MetaPathIterator(shapes.iterator(), at, 0, false);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new MetaPathIterator(shapes.iterator(), at, flatness, true);
    }

    @Override
    public Iterator<Shape> iterator() {
        return Collections.unmodifiableList(shapes).iterator();
    }

    private static class MetaPathIterator implements PathIterator {

        private final Iterator<Shape> shapes;
        private final AffineTransform xform;
        private final double flatness;
        private PathIterator curr;
        private final boolean useFlatness;

        public MetaPathIterator(Iterator<Shape> shapes, AffineTransform xform, double flatness, boolean useFlatness) {
            this.shapes = shapes;
            this.xform = xform;
            this.flatness = flatness;
            this.useFlatness = useFlatness;
            maybeNext();
        }

        private void maybeNext() {
            Shape shape = shapes.hasNext() ? shapes.next() : null;
            if (shape != null) {
                if (xform == null) {
                    curr = shape.getPathIterator(null);
                } else {
                    if (useFlatness) {
                        curr = shape.getPathIterator(xform, flatness);
                    } else {
                        curr = shape.getPathIterator(xform);
                    }
                }
            }
        }

        private PathIterator curr() {
            if (curr == null) {
                maybeNext();
            }
            if (curr != null) {
                if (curr.isDone()) {
                    curr = null;
                    maybeNext();
                }
            }
            return curr;
        }

        @Override
        public int getWindingRule() {
            PathIterator it = curr();
            return it == null ? PathIterator.WIND_NON_ZERO : it.getWindingRule();
        }

        @Override
        public boolean isDone() {
            return curr() == null;
        }

        @Override
        public void next() {
            PathIterator pi = curr();
            if (pi != null) {
                pi.next();
            }
        }

        @Override
        public int currentSegment(float[] coords) {
            PathIterator pi = curr();
            if (pi != null) {
                return pi.currentSegment(coords);
            }
            throw new IllegalStateException("Done");
        }

        @Override
        public int currentSegment(double[] coords) {
            PathIterator pi = curr();
            if (pi != null) {
                return pi.currentSegment(coords);
            }
            throw new IllegalStateException("Done");
        }
    }
}
