package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

public final class QuadTo extends LocationEntry {

    private Point2D.Double cp = new Point2D.Double();
    private ControlPoint locPoint = new ControlPointImpl(this, 0);
    private ControlPoint conPoint = new ControlPointImpl(this, 1);

    public QuadTo(double cx, double cy, double x, double y) {
        super(x, y);
        cp.setLocation(cx, cy);
    }

    private Point2D point(int index) {
        switch (index) {
            case 0:
                return this;
            case 1:
                return cp;
            default:
                throw new IndexOutOfBoundsException(index + "");
        }
    }

    @Override
    protected double getControlPointX(int index) {
        return point(index).getX();
    }

    @Override
    protected double getControlPointY(int index) {
        return point(index).getY();
    }

    @Override
    protected boolean setControlPoint(int index, Point2D loc) {
        Point2D p = point(index);
        boolean result = p.getX() != loc.getX() || p.getY() != loc.getY();
        if (result) {
            p.setLocation(loc);
        }
        return result;
    }

    @Override
    public void perform(GeneralPath path) {
        path.quadTo(cp.getX(), cp.getY(), getX(), getY());
    }

    @Override
    public void draw(Graphics2D g) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ControlPoint[] getControlPoints() {
        return new ControlPoint[]{locPoint, conPoint};
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public Kind kind() {
        return Kind.QuadTo;
    }

    @Override
    public String toString() {
        return "gp.quadTo (" + getX() + "D," + getY() + "D, " + cp.getX() + "D, " + cp.getY() + ");\n";
    }
}
