package net.java.dev.imagine.api.vector.design;

import net.java.dev.imagine.api.vector.util.Size;

/**
 *
 * @author Tim Boudreau
 */
public interface ControlPointController {

    public void changed(ControlPoint pt);

    public Size getControlPointSize();

    default void deleted(ControlPoint pt) {
    }

}
