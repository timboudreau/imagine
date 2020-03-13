package org.imagine.editor.api.snap;

import java.util.function.Supplier;

/**
 * A reified <code>Supplier&lt;SnapPoints&gt;</code> for use in
 * <code>Lookup</code>s.
 *
 * @author Tim Boudreau
 */
public interface SnapPointsSupplier extends Supplier<SnapPoints<?>> {

    public static SnapPointsSupplier NONE = () -> SnapPoints.EMPTY;
}
