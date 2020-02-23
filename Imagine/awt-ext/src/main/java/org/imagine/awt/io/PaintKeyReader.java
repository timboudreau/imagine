package org.imagine.awt.io;

import java.io.IOException;

/**
 *
 * @author Tim Boudreau
 */
public interface PaintKeyReader {

    public int readInt() throws IOException;

    public byte readByte() throws IOException;

    public long readLong() throws IOException;

    public int[] readIntArray() throws IOException;

    public long[] readLongArray() throws IOException;

    public byte[] readByteArray() throws IOException;
}
