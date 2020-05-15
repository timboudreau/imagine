package net.dev.java.imagine.api.tool.aspects;

import java.awt.Shape;

/**
 * Implemented by a customizer that can preview a transform of a shape,
 * to provide a context-sensitive preview.
 *
 * @author Tim Boudreau
 */
public interface ShapePreview {

    void setShape(Shape shape);
}
