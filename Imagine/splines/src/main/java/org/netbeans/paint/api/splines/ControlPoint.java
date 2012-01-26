package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * A control point
 *
 * @author Tim Boudreau
 */
public abstract class ControlPoint extends Point2D {

    public abstract Edge[] getEdges();

    public abstract int getIndex();

    public abstract boolean match(Point2D pt);

    public abstract void paint(Graphics2D g2d, boolean selected);

    public abstract Point toPoint();

    public abstract void setLocation(Point2D p);

    public abstract int x();

    public abstract int y();
 
    public abstract Entry getEntry();
    
    public abstract Edge[] getAdjacentEdges(boolean includeControlPoints);
    
    public abstract void translate(double offX, double offY);
    
    public boolean isMain() {
        return getIndex() == 0;
    }
}
