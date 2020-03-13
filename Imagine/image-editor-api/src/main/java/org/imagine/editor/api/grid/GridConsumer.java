package org.imagine.editor.api.grid;

/**
 * Marker layers and tools can put in their lookup to indicate that
 * the UI/toolbar for snap settings should be visible when they are
 * active.
 *
 * @author Tim Boudreau
 */
public final class GridConsumer {

    public static GridConsumer instance() {
        return new GridConsumer();
    }

    private GridConsumer() {

    }
}
