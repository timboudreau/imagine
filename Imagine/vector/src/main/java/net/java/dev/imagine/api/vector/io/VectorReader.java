package net.java.dev.imagine.api.vector.io;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleBiFunction;
import com.mastfrog.function.DoubleQuadConsumer;
import com.mastfrog.function.DoubleQuadFunction;
import com.mastfrog.function.DoubleTriConsumer;
import com.mastfrog.function.DoubleTriFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author Tim Boudreau
 */
public interface VectorReader {

    VectorReader readBeginRecord();

    int readInt();

    double readDouble();

    float readFloat();

    byte readByte();

    boolean readBoolean();

    default VectorReader readDoubles2(DoubleBiConsumer bi) {
        bi.accept(readDouble(), readDouble());
        return this;
    }

    default VectorReader readDoubles3(DoubleTriConsumer tri) {
        tri.accept(readDouble(), readDouble(), readDouble());
        return this;
    }

    default VectorReader readDoubles4(DoubleQuadConsumer quad) {
        quad.accept(readDouble(), readDouble(), readDouble(), readDouble());
        return this;
    }

    default VectorReader readDoubles(int expected, Consumer<double[]> doubles) {
        if (expected == -1) {
            int count = readInt();
            double[] result = new double[count];
            for (int i = 0; i < result.length; i++) {
                result[i] = readDouble();
            }
            doubles.accept(result);
        } else {
            double[] result = new double[expected];
            for (int i = 0; i < result.length; i++) {
                result[i] = readDouble();
            }
            doubles.accept(result);
        }
        return this;
    }

    default <X> X readGeometry2(DoubleBiFunction<X> bi) {
        return bi.apply(readDouble(), readDouble());
    }

    default <X> X readGeometry3(DoubleTriFunction<X> tri) {
        return tri.apply(readDouble(), readDouble(), readDouble());
    }

    default <X> X readGeometry4(DoubleQuadFunction<X> quad) {
        return quad.apply(readDouble(), readDouble(), readDouble(), readDouble());
    }

    default <X> X readGeometry(int expected, Function<double[], X> doubles) {
        if (expected == -1) {
            int count = readInt();
            double[] result = new double[count];
            for (int i = 0; i < result.length; i++) {
                result[i] = readDouble();
            }
            return doubles.apply(result);
        } else {
            double[] result = new double[expected];
            for (int i = 0; i < result.length; i++) {
                result[i] = readDouble();
            }
            return doubles.apply(result);
        }
    }
}
