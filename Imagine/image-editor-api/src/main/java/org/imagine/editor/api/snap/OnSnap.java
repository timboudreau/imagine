package org.imagine.editor.api.snap;

import java.util.function.BiPredicate;

/**
 *
 * @author Tim Boudreau
 */
public interface OnSnap<T> extends BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> {

    boolean onSnap(SnapCoordinate<T> x, SnapCoordinate<T> y);

    @Override
    default boolean test(SnapCoordinate<T> x, SnapCoordinate<T> y) {
        return onSnap(x, y);
    }
}
