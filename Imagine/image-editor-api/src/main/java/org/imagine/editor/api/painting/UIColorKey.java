package org.imagine.editor.api.painting;

import java.awt.Color;

/**
 *
 * @author Tim Boudreau
 */
public interface UIColorKey<V> {

    default Color outline() {
        return Color.LIGHT_GRAY;
    }

    default Color interior() {
        return Color.WHITE;
    }

    default Color shadow() {
        return Color.DARK_GRAY;
    }
}
