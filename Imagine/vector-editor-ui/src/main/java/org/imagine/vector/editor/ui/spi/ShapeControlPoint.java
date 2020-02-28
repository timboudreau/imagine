package org.imagine.vector.editor.ui.spi;

import net.java.dev.imagine.api.vector.design.ControlPoint;

/**
 *
 * @author Tim Boudreau
 */
public interface ShapeControlPoint extends ControlPoint {

    ShapeElement owner();
}
