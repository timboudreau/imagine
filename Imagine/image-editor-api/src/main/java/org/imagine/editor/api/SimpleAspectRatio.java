package org.imagine.editor.api;

import java.awt.Dimension;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
final class SimpleAspectRatio implements AspectRatio {

    private final Supplier<Dimension> dimension;
    private final BooleanSupplier flex;

    SimpleAspectRatio(Supplier<Dimension> dimension, BooleanSupplier flex) {
        this.dimension = dimension;
        this.flex = flex;
    }

    @Override
    public boolean isFlexible() {
        return flex == null ? false : flex.getAsBoolean();
    }

    @Override
    public double width() {
        return dimension.get().width;
    }

    @Override
    public double height() {
        return dimension.get().height;
    }

    @Override
    public String toString() {
        return "AspectRatio(" + fraction() + ")";
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.dimension);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SimpleAspectRatio)) {
            return false;
        }
        final SimpleAspectRatio other
                = (SimpleAspectRatio) obj;
        return Objects.equals(this.dimension,
                other.dimension);
    }
}
