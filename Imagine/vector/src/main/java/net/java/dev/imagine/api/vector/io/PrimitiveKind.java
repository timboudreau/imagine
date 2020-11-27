/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.io;

import java.awt.geom.Arc2D;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.elements.Arc;
import net.java.dev.imagine.api.vector.elements.CircleWrapper;
import com.mastfrog.geometry.Circle;

/**
 *
 * @author Tim Boudreau
 */
public abstract class PrimitiveKind<T extends Primitive, P> implements BiConsumer<T, VectorWriter>, Function<VectorReader, T> {

    public static final PrimitiveKind<Arc, Arc2D> ARC = new ArcKind();
    public static final PrimitiveKind<CircleWrapper, Circle> CIRCLE = new CircleKind();

    private final Class<T> type;
    private final String name;
    private final int ordinal;

    PrimitiveKind(Class<T> type, String name, int ordinal) {
        this.type = type;
        this.name = name;
        this.ordinal = ordinal;
    }

    public final Class<T> type() {
        return type;
    }

    public final int ordinal() {
        return ordinal;
    }

    public final String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof PrimitiveKind<?, ?>) {
            PrimitiveKind<?, ?> pk = (PrimitiveKind<?, ?>) o;
            return pk.ordinal() == ordinal() && pk.getClass() == getClass();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ordinal * 71;
    }

    private static final class ArcKind extends PrimitiveKind<Arc, Arc2D> {

        ArcKind() {
            super(Arc.class, "arc", 0);
        }

        @Override
        public void accept(Arc t, VectorWriter u) {
            u.writeGeometry(t.x, t.y, t.width, t.height, t.arcAngle, t.startAngle);
        }

        @Override
        public Arc apply(VectorReader t) {
            return t.readGeometry4((x, y, w, h) -> {
                double aa = t.readDouble();
                double sa = t.readDouble();
                return new Arc(x, y, w, h, aa, sa, t.readBoolean());
            });
        }
    }

    private static final class CircleKind extends PrimitiveKind<CircleWrapper, Circle> {

        CircleKind() {
            super(CircleWrapper.class, "circle", 0);
        }

        @Override
        public void accept(CircleWrapper t, VectorWriter u) {
            u.writeGeometry(t.centerX, t.centerY);
            u.writeDouble(t.radius);
        }

        @Override
        public CircleWrapper apply(VectorReader t) {
            return t.<CircleWrapper>readGeometry2((x, y) -> {
                double rad = t.readDouble();
                return new CircleWrapper(x, y, rad);
            });
        }
    }
}
