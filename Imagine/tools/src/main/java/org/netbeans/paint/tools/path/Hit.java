package org.netbeans.paint.tools.path;

/**
 * Returned if an existing point is clicked.
 *
 * @author Tim Boudreau
 */
interface Hit {

    final Hit NON_POINT = new Hit() {
    };

    default <T> T get(Class<T> type) {
        return type.isInstance(this) ? type.cast(this) : null;
    }

}
