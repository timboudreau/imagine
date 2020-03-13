package org.imagine.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
public interface KeyWriter<T> extends Supplier<T> {

    KeyWriter writeInt(int value);

    KeyWriter writeIntArray(int[] ints);

    KeyWriter writeLong(long value);

    KeyWriter writeLongArray(long[] ints);

    KeyWriter writeByte(byte b);

    KeyWriter writeByteArray(byte[] bytes);

    KeyWriter finishRecord();

    byte[] toByteArray();

    default KeyWriter writeString(String txt) {
        return writeByteArray(txt.getBytes(UTF_8));
    }

    default KeyWriter writeDouble(double val) {
        return writeLong(Double.doubleToLongBits(val));
    }

    default KeyWriter writeFloat(float val) {
        return writeInt(Float.floatToIntBits(val));
    }

    default KeyWriter writeDoubleArray(double[] dbls) {
        long[] longs = new long[dbls.length];
        for (int i = 0; i < dbls.length; i++) {
            longs[i] = Double.doubleToLongBits(dbls[i]);
        }
        return writeLongArray(longs);
    }

    default KeyWriter writeFloatArray(float[] floats) {
        int[] ints = new int[floats.length];
        for (int i = 0; i < floats.length; i++) {
            ints[i] = Float.floatToIntBits(floats[i]);
        }
        return writeIntArray(ints);
    }

    default <T extends Enum<T>> KeyWriter writeEnum(T enumValue) {
        return writeInt(enumValue.ordinal());
    }
}
