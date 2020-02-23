package org.imagine.awt.io;

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
}
