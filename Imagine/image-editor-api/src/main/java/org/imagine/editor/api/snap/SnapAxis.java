package org.imagine.editor.api.snap;

import java.awt.geom.Point2D;

/**
 *
 * @author Tim Boudreau
 */
public enum SnapAxis {
    X, Y;

    double value(Point2D p) {
        return this == X ? p.getX() : p.getY();
    }

    public static SnapAxis forVertical(boolean vertical) {
        return vertical ? Y : X;
    }
}
