package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;


public final class CurveTo extends LocationEntry {
    private final Point2D.Double a;
    private final Point2D.Double b;
    private final ControlPointImpl mainPoint = new ControlPointImpl(this, 0);
    private final ControlPointImpl aPoint = new ControlPointImpl(this, 1);
    private final ControlPointImpl bPoint = new ControlPointImpl(this, 2);
    
    public CurveTo(double x1, double y1, double x2, double y2, double x, double y) {
        super (x, y);
        a = new Double(x1, y1);
        b = new Double(x2, y2);
    }
    
    @Override
    public CurveTo clone() {
        return new CurveTo (a.getX(), a.getY(), b.getX(), b.getY(), getX(), getY());
    }
    
    @Override
    public int size() {
        return 3;
    }
    
    public CurveTo(Point2D one, Point2D two, Point2D dest) {
        this (one.getX(), one.getY(), two.getX(), two.getY(), dest.getX(), dest.getY());
    }
    
    public CurveTo(Point one, Point two, Point dest) {
        this (one.x, one.y, two.x, two.y, dest.x, dest.y);
    }
    
    @Override
    public void perform(GeneralPath path) {
        path.curveTo(a.getX(), a.getY(), b.getX(), b.getY(), getX(), getY());
    }

    @Override
    public void draw(Graphics2D g) {
        new MoveTo(a.getX(), a.getY()).draw (g);
        new MoveTo(b.getX(), b.getY()).draw (g);
        new MoveTo(getX(), getY()).draw (g);
    }

    @Override
    protected void findDrawBounds(Rectangle r, int areaSize) {
        Rectangle x = new Rectangle();
        Rectangle y = new Rectangle();
        Rectangle z = new Rectangle();
        new MoveTo (a).findDrawBounds(x, areaSize);
        new MoveTo (this).findDrawBounds(y, areaSize);
        new MoveTo (b).findDrawBounds(z, areaSize);
        r.setBounds(x.union(y).union(z));
    }

    ControlPoint[] nodes;
    @Override
    public ControlPoint[] getControlPoints() {
        return new ControlPoint[] { mainPoint, aPoint, bPoint };
    }
    
    @Override
    public String toString() {
        return "gp.curveTo (" + a.getX() + "D, " + a.getY() + "D, " +
                b.getX() + "D, " + b.getY() + "D, " + getX() +"D, " +
                getY() +"D);\n";
    }

    @Override
    protected boolean setControlPoint(int index, Point2D loc) {
        Point2D.Double toSet;
        switch (index) {
            case 0:
                toSet = this;
                break;
            case 1:
                toSet = a;
                break;
            case 2 :
                toSet = b;
                break;
            default :
                throw new IndexOutOfBoundsException ("" + index);
        }
        
        boolean result = toSet.getX() != loc.getX() && toSet.getY() !=
                loc.getY();
        if (result) {
            toSet.setLocation (loc);
        }
        return result;
    }

    @Override
    public Kind kind() {
        return Kind.CurveTo;
    }
    
    private Point2D pointFor(int index) {
        Point2D result;
        switch (index) {
            case 0:
                result = this;
                break;
            case 1:
                result = a;
                break;
            case 2 :
                result = b;
                break;
            default :
                throw new IndexOutOfBoundsException ("" + index);
        }
        return result;
    }

    @Override
    protected double getControlPointX(int index) {
        return pointFor(index).getX();
    }

    @Override
    protected double getControlPointY(int index) {
        return pointFor(index).getY();
    }
}
