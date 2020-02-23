package org.imagine.awt.io;

import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
public interface PaintKeyWriter<T> extends Supplier<T> {

    PaintKeyWriter writeInt(int value);

    PaintKeyWriter writeIntArray(int[] ints);

    PaintKeyWriter writeLong(long value);

    PaintKeyWriter writeLongArray(long[] ints);

    PaintKeyWriter writeByte(byte b);

    PaintKeyWriter writeByteArray(byte[] bytes);

    byte[] toByteArray();
}
