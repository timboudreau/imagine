package org.netbeans.paint.api.splines;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
final class ControlPointImpl extends ControlPoint {

    public final int index;
    public final LocationEntry entry;
    private transient List<Edge> edges;
    
    public ControlPointImpl(LocationEntry entry, int ix) {
        this.index = ix;
        this.entry = entry;
    }
    
    public Entry getEntry() {
        return entry;
    }

    @Override
    public Edge[] getEdges() {
        if (index == 0) {
            if (edges == null || edges.isEmpty()) {
                if (edges == null) {
                    edges = new ArrayList<Edge>();
                }
                ControlPoint[] n = entry.getControlPoints();
                for (ControlPoint nn : n) {
                    if (nn != this) {
                        edges.add(new EdgeImpl(this, nn, false));
                    }
                }
            }
            return edges.toArray(new Edge[edges.size()]);
        } else {
            return new EdgeImpl[0];
        }
    }

    @Override
    public void setLocation(Point2D p) {
        setLocation(p.getX(), p.getY());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof ControlPoint) {
            ControlPoint other = (ControlPoint) o;
            return other.getEntry().equals(getEntry()) && 
                    other.getIndex() == getIndex();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return entry.hashCode() + (103591 * getIndex());
//        int xx = (int) getX() * 1000;
//        int yy = (int) getY() * 1000;
//        return (xx * yy * (index + 1)) ^ entry.hashCode();
    }

    @Override
    public boolean match(Point2D pt) {
        return pt.getX() == getX() && pt.getY() == getY();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int x() {
        return (int) entry.getControlPointX(index);
    }

    @Override
    public int y() {
        return (int) entry.getControlPointY(index);
    }

    @Override
    public Point toPoint() {
        return new Point(x(), y());
    }

    @Override
    public void paint(Graphics2D g2d, boolean selected) {
        //XXX handle zooming - points should not grow
        Rectangle2D.Double r = new Rectangle2D.Double(getX() - 3, getY() - 3, 6, 6);
        Color c = g2d.getColor();
        g2d.setColor(selected ? Color.ORANGE : Color.WHITE);
        g2d.fill(r);
        g2d.setColor(Color.BLACK);
        g2d.draw(r);
        g2d.setColor(c);
    }

    @Override
    public double getX() {
        return entry.getControlPointX(index);
    }

    @Override
    public double getY() {
        return entry.getControlPointY(index);
    }

    @Override
    public void setLocation(double x, double y) {
        entry.setControlPoint(index, new Point2D.Double(x, y));
    }

    @Override
    public Edge[] getAdjacentEdges(boolean includeControlPoints) {
        List<Edge> result = new ArrayList<Edge>(5);
        if (includeControlPoints) {
            result.addAll(Arrays.asList(getEdges()));
        }
        if (index == 0 && entry.model() != null) {
            for (Edge e : entry.model().getPathEdges()) {
                if (e.getSourcePoint().getEntry() == entry || e.getTargetPoint().getEntry() == entry) {
                    result.add(e);
                }
            }
        }
        return result.toArray(new Edge[result.size()]);
    }

    @Override
    public void translate(double offX, double offY) {
        double newX = getX() + offX;
        double newY = getY() + offY;
        setLocation(newX, newY);
    }
}
