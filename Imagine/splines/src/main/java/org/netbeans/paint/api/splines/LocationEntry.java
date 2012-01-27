package org.netbeans.paint.api.splines;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Iterator;

abstract class LocationEntry extends Point2D.Double implements Entry {
    
    DefaultPathModel model;
    
    public LocationEntry(double x, double y) {
        super (x, y);
    }
    public Point2D getLocation() {
        return this;
    }
    
    protected abstract double getControlPointX(int index);
    protected abstract double getControlPointY(int index);
    protected abstract boolean setControlPoint(int index, Point2D loc);
    
    DefaultPathModel model() {
        return model;
    }

    @Override
    public Iterator<ControlPoint> iterator() {
        return Arrays.asList(getControlPoints()).iterator();
    }

    @Override
    public String toString() {
        return '[' + getClass().getName() + getX() + ',' + getY() + ']';
    }
    
    public int hit (Point2D pt, double areaSize) {
        Rectangle2D test = new Rectangle2D.Double(pt.getX(), pt.getY(), areaSize, areaSize);
        ControlPoint[] p = getControlPoints();
        
        double sz = areaSize / 2D;
        for (int i = 0; i < p.length; i++) {
            test.setRect(p[i].getX() - sz, p[i].getY() - sz, areaSize, areaSize);
            boolean match = test.contains(pt);
            if (match) {
                return i;
            }
        }
        return -1;
    }

    public final Rectangle getDrawBounds(Rectangle r, int areaSize) {
        r = cr(r);
        findDrawBounds (r, areaSize);
        return r;
    }
    
    protected void findDrawBounds (Rectangle r, int areaSize) {
        r.x = (int) getX()  - areaSize;
        r.y = (int) getY()  - areaSize;
        r.width = areaSize;
        r.height = areaSize;
    }
    
    static final Point p (Point2D.Double d) {
        return new Point ((int) d.x, (int) d.y);
    }
    
    static final Rectangle cr(Rectangle r) {
        return r == null ? new Rectangle() : r;
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        } else if (o == this) {
            return true;
        }
        LocationEntry other = (LocationEntry) o;
        if (other.model == model) {
            int mine = index();
            int others = other.index();
            return mine == others && kind() == other.kind();
        }
        return false;
    }
    
    @Override
    public final int hashCode() {
        return 99317 * index() + (5821 * kind().ordinal());
    }
    
    private int index() {
        return model == null ? -1 : model.indexOf(this);
    }
    
}