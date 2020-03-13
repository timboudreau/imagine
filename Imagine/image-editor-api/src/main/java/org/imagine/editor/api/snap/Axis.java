package org.imagine.editor.api.snap;

import java.awt.geom.Point2D;

/**
 *
 * @author Tim Boudreau
 */
public enum Axis {
    X, Y;

    double value(Point2D p) {
        return this == X ? p.getX() : p.getY();
    }

    public static Axis forVertical(boolean vertical) {
        return vertical ? Y : X;
    }
}
