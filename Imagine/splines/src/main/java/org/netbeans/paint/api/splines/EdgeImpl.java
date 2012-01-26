package org.netbeans.paint.api.splines;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
final class EdgeImpl implements Edge {

    private final ControlPoint a;
    private final ControlPoint b;
    private final boolean main;

    EdgeImpl(ControlPoint a, ControlPoint b, boolean main) {
        this.a = a;
        this.b = b;
        this.main = main;
    }

    @Override
    public ControlPoint getSourcePoint() {
        return a;
    }

    @Override
    public ControlPoint getTargetPoint() {
        return b;
    }

    @Override
    public boolean isMain() {
        return main;
    }

    @Override
    public Line2D toLine() {
        return new Line2D.Double(a, b);
    }

    @Override
    public Shape toStrokedShape(double width) {
        BasicStroke s = new BasicStroke((float) width);
        return s.createStrokedShape(path());
    }

    @Override
    public Rectangle2D getBounds2D() {
        double w = Math.abs(a.getX() - b.getX());
        double h = Math.abs(a.getY() - b.getY());
        double x = Math.min(a.getX(), b.getX());
        double y = Math.min(b.getY(), b.getY());
        return new Rectangle2D.Double(x, y, w, h).getBounds();
    }

    @Override
    public void translate(double xOff, double yOff) {
        for (ControlPoint n : a.getEntry().getControlPoints()) {
            n.setLocation(n.getX() + xOff, n.getY() + yOff);
        }
        for (ControlPoint n : b.getEntry().getControlPoints()) {
            n.setLocation(n.getX() + xOff, n.getY() + yOff);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EdgeImpl other = (EdgeImpl) obj;
        if (this.a != other.a && (this.a == null || !this.a.equals(other.a))) {
            return false;
        }
        if (this.b != other.b && (this.b == null || !this.b.equals(other.b))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.a != null ? this.a.hashCode() : 0);
        hash = 79 * hash + (this.b != null ? this.b.hashCode() : 0);
        return hash;
    }

    private GeneralPath path() {
        GeneralPath pth = new GeneralPath();
        if (isMain()) {
            Entry e = a.getEntry();
            ControlPoint[] pts = e.getControlPoints();
            pth.moveTo(pts[0].getX(), pts[0].getY());
            b.getEntry().perform(pth);
        } else {
            pth.moveTo(a.getX(), a.getY());
            pth.lineTo(b.getX(), b.getY());
        }
        return pth;
    }

    private Shape stroked() {
        return toStrokedShape(7);
    }

    @Override
    public Rectangle getBounds() {
        return path().getBounds();
    }

    @Override
    public boolean contains(double x, double y, double width) {
        return toStrokedShape(width).contains(x, y);
    }
    
    @Override
    public boolean contains(double x, double y) {
        return stroked().contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return stroked().contains(p);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return path().intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return path().intersects(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return stroked().contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return stroked().contains(r);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return path().getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return path().getPathIterator(at, flatness);
    }

    @Override
    public Edge[] getAdjacentEdges(boolean includeControlPoints) {
        Set<Edge> result = new HashSet<Edge>(Arrays.asList(a.getAdjacentEdges(includeControlPoints)));
        result.addAll(Arrays.asList(b.getAdjacentEdges(includeControlPoints)));
        result.remove(this);
        return result.toArray(new Edge[result.size()]);
    }

    @Override
    public Iterator<ControlPoint> iterator() {
        return Arrays.asList(a, b).iterator();
    }

    @Override
    public Point2D getLocation() {
        Rectangle2D r = getBounds2D();
        return new Point2D.Double(r.getX(), r.getY());
    }

    @Override
    public void setLocation(double x, double y) {
        Point2D old = getLocation();
        if (x != old.getX() || y != old.getY()) {
            double offX = x - old.getX();
            double offY = y - old.getY();
            translate (offX, offY);
        }
    }
    
    public void setLocation(Point2D point) {
        setLocation(point.getX(), point.getY());
    }
}
