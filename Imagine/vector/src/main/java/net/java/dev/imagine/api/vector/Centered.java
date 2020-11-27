package net.java.dev.imagine.api.vector;

import com.mastfrog.geometry.EqPointDouble;

/**
 * Shape-like elements that have a center-point.
 *
 * @author Tim Boudreau
 */
public interface Centered {

    double centerX();

    double centerY();

    default EqPointDouble center() {
        return new EqPointDouble(centerX(), centerY());
    }
}
