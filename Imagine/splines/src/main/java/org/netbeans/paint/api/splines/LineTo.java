package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;


public class LineTo extends LocationEntry {
    private final ControlPoint cp = new ControlPointImpl(this, 0);
    
    public LineTo(double x, double y) {
        super (x, y);
    }

    public LineTo(Point2D p) {
        this (p.getX(), p.getY());
    }
    
    public LineTo(Point p) {
        this (p.x, p.y);
    }
    
    @Override
    public LineTo clone() {
        return new LineTo(getX(), getY());
    }
    
    @Override
    public void perform(GeneralPath path) {
        path.lineTo (getX(), getY());
    }

    @Override
    public void draw(Graphics2D g) {
        Rectangle r = getDrawBounds(null, 4);
        g.fillRect (r.x, r.y, r.width, r.height);
    }
    
    @Override
    public ControlPoint[] getControlPoints() {
        return new ControlPoint[]{ cp };
    }

    @Override
    public String toString() {
        return "gp.lineTo (" + getX() + "D, " + getY() +"D);\n";
    }
    
    @Override
    public boolean setControlPoint(int index, Point2D loc) {
        if (index != 0) {
            throw new IndexOutOfBoundsException(index + "");
        }
        boolean result = loc.getX() == x && loc.getY() == y;
        this.setLocation (loc);
        return result;
    }
    
    @Override
    public int size() {
        return 1;
    }
    
    @Override
    public int hashCode() {
        return new Point2D.Double(x, y).hashCode() * 13063;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LineTo && ((LineTo) obj).x == x && 
                ((LineTo) obj).y == y;
    }

    @Override
    public Kind kind() {
        return Kind.LineTo;
    }

    @Override
    protected double getControlPointX(int index) {
        assert index == 0;
        return getX();
    }

    @Override
    protected double getControlPointY(int index) {
        assert index == 0;
        return getY();
    }
}
