package net.dev.java.imagine.api.tool.aspects.snap;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Just provides a reification of a consumer so it can be looked up from a
 * Lookup by passing a Class object.
 *
 * @author Tim Boudreau
 */
public interface SnapPointsConsumer extends Consumer<Supplier<SnapPoints>> {

}
