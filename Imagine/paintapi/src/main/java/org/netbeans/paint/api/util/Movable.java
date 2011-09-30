package org.netbeans.paint.api.util;

import java.awt.Point;

/**
 * A thing which a move tool can move
 *
 * @author Tim Boudreau
 */
public interface Movable {
    public Point getLocation();
    public void setLocation(Point p);
}
