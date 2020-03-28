package org.imagine.vector.editor.ui.tools.widget.collections;

/**
 *
 * @author Tim Boudreau
 */
public interface Refiner<T> {

    T refine(double tMinX, double tMaxX, double tMinY, double tMaxY, T obj);

    public static Refiner<Object> NONE =
            (double tMinX, double tMaxX, double tMinY, double tMaxY, Object obj) -> obj;
}
