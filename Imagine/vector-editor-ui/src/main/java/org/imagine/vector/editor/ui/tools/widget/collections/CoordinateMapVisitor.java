package org.imagine.vector.editor.ui.tools.widget.collections;

/**
 *
 * @author Tim Boudreau
 */
public interface CoordinateMapVisitor<T> {

    void accept(double x, double y, T val);

}
