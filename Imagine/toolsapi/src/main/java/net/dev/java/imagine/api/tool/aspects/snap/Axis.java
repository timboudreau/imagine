package net.dev.java.imagine.api.tool.aspects.snap;

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
}
