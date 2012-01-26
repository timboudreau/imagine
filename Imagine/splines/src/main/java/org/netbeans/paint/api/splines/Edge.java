package org.netbeans.paint.api.splines;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * An edge between two nodes
 *
 * @author Tim Boudreau
 */
public interface Edge extends Shape, Iterable<ControlPoint> {

    ControlPoint getSourcePoint();

    ControlPoint getTargetPoint();

    boolean isMain();

    void translate(double xOff, double yOff);

    Line2D toLine();

    Shape toStrokedShape(double width);

    Edge[] getAdjacentEdges(boolean includeControlPoints);

    public boolean contains(double x, double y, double strokeWidth);

    public Point2D getLocation();

    public void setLocation(double x, double y);

    public void setLocation(Point2D point);
}
