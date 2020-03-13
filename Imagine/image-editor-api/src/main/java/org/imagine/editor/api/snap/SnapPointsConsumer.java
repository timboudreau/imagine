package org.imagine.editor.api.snap;

import java.util.function.Consumer;

/**
 * Just provides a reification of a consumer so it can be looked up from a
 * Lookup by passing a Class object.
 *
 * @author Tim Boudreau
 */
public interface SnapPointsConsumer extends Consumer<SnapPointsSupplier> {

}
