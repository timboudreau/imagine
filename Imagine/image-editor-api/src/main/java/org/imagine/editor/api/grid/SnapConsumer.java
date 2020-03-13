package org.imagine.editor.api.grid;

/**
 * Marker layers and tools can put in their lookup to indicate that
 * the UI/toolbar for snap settings should be visible when they are
 * active.
 *
 * @author Tim Boudreau
 */
public final class SnapConsumer {

    public static SnapConsumer instance() {
        return new SnapConsumer();
    }

    private SnapConsumer() {
        
    }
}
