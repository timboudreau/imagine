package org.imagine.io;

import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @author Tim Boudreau
 */
public interface KeyReader {

    int readInt() throws IOException;

    byte readByte() throws IOException;

    long readLong() throws IOException;

    int[] readIntArray() throws IOException;

    long[] readLongArray() throws IOException;

    byte[] readByteArray() throws IOException;

    default double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    default float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    default float[] readFloatArray() throws IOException {
        int[] ints = readIntArray();
        float[] floats = new float[ints.length];
        for (int i = 0; i < ints.length; i++) {
            floats[i] = Float.intBitsToFloat(ints[i]);
        }
        return floats;
    }

    default double[] readDoubleArray() throws IOException {
        long[] longs = readLongArray();
        double[] dbls = new double[longs.length];
        for (int i = 0; i < longs.length; i++) {
            dbls[i] = Double.longBitsToDouble(longs[i]);
        }
        return dbls;
    }

    default String readString() throws IOException {
        byte[] bytes = readByteArray();
        return new String(bytes, UTF_8);
    }

    default <T extends Enum<T>> T readEnum(Class<T> type) throws IOException {
        int ix = readInt();
        if (ix < 0) {
            throw new IOException("Enum index for " + type.getName()
                    + " < 0: " + ix);
        }
        T[] consts = type.getEnumConstants();
        if (ix >= consts.length) {
            throw new IOException("Enum constant index out of range: " + ix
                    + " of " + consts.length);
        }
        return consts[ix];
    }
}
