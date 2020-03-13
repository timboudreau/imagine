package org.imagine.io;

import com.mastfrog.util.strings.Strings;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.imagine.io.KeyStringWriter.BYTE_ARRAY_PREFIX;
import static org.imagine.io.KeyStringWriter.INT_ARRAY_PREFIX;
import static org.imagine.io.KeyStringWriter.LONG_ARRAY_PREFIX;
import static org.imagine.io.KeyStringWriter.MAGIC_1;
import static org.imagine.io.KeyStringWriter.MAGIC_2;

/**
 *
 * @author Tim Boudreau
 */
public final class KeyBinaryReader<C extends ReadableByteChannel & SeekableByteChannel> implements KeyReader {

    private static final int MIN_BUF = 20;
    private final C channel;
    private ByteBuffer buffer;
    private final byte m1, m2;

    public KeyBinaryReader(C channel, byte magic1, byte magic2) {
        this.channel = channel;
        this.m1 = magic1;
        this.m2 = magic2;
    }

    public KeyBinaryReader(C channel) {
        this.channel = channel;
        m1 = MAGIC_1;
        m2 = MAGIC_2;
    }

    public int readMagicAndSize() throws IOException {
        byte b1 = readByte();
        byte b2 = readByte();
        if (b1 != m1 || b2 != m2) {
            throw new IOException("Magic numbers do not match - expected "
                    + Strings.toPaddedHex(new byte[]{m1, m2})
                    + " but got "
                    + Strings.toPaddedHex(new byte[]{b1, b2})
            );
        }
        int size = readInt();
        if (size < 0) {
            throw new IOException("Read a record size of " + size);
        }
        return size;
    }

    @Override
    public String toString() {
        try {
            return "BinaryReader at position " + channel.position();
        } catch (IOException ex) {
            Logger.getLogger(KeyBinaryReader.class.getName()).log(Level.SEVERE, null, ex);
            return "Binary reader with dead channel";
        }
    }

    private ByteBuffer buffer(int size) {
        if (buffer == null || buffer.capacity() < size) {
            buffer = ByteBuffer.allocate(Math.max(MIN_BUF, size));
            buffer.limit(size);
        } else {
            buffer.position(0);
            buffer.limit(size);
        }
        return buffer;
    }

    @Override
    public int readInt() throws IOException {
        ByteBuffer buf = buffer(Integer.BYTES);
        int bytesRead = channel.read(buf);
        if (bytesRead < Integer.BYTES) {
            throw new IOException("Underflow - read " + bytesRead + " bytes");
        }
        buf.flip();
        int result = buf.getInt();
        return result;
    }

    @Override
    public byte readByte() throws IOException {
        ByteBuffer buf = buffer(1);
        if (channel.read(buf) < 1) {
            throw new IOException("Underflow - no bytes read");
        }
        buf.flip();
        return buf.get();
    }

    @Override
    public long readLong() throws IOException {
        ByteBuffer buf = buffer(Long.BYTES);
        if (channel.read(buf) != Long.BYTES) {
            throw new IOException("Underflow at " + channel.position()
                    + " reading long into buffer of " + buf.capacity());
        }
        buf.flip();
        return buf.getLong();
    }

    @Override
    public int[] readIntArray() throws IOException {
        int marker = readByte();
        if (marker != INT_ARRAY_PREFIX) {
            throw new IOException("Expected int array marker "
                    + Integer.toHexString(INT_ARRAY_PREFIX & 0xFF)
                    + " but got " + Integer.toHexString(marker & 0xFF));
        }
        int arraySize = readInt();
        if (arraySize == 0) {
            return new int[0];
        } else if (arraySize < 0) {
            throw new IOException("Invalid array size " + arraySize);
        }
        ByteBuffer buf = buffer(Integer.BYTES * arraySize);
        channel.read(buf);
        buf.flip();
        int[] result = new int[arraySize];
        for (int i = 0; i < result.length; i++) {
            result[i] = buf.getInt();
        }
        return result;
    }

    @Override
    public long[] readLongArray() throws IOException {
        int marker = readByte();
        if (marker != LONG_ARRAY_PREFIX) {
            throw new IOException("Expected long array marker "
                    + Integer.toHexString(LONG_ARRAY_PREFIX & 0xFF)
                    + " but got " + Integer.toHexString(marker & 0xFF));
        }
        int arraySize = readInt();
        if (arraySize == 0) {
            return new long[0];
        } else if (arraySize < 0) {
            throw new IOException("Invalid array size " + arraySize);
        }
        ByteBuffer buf = buffer(Long.BYTES * arraySize);
        channel.read(buf);
        buf.flip();
        long[] result = new long[arraySize];
        for (int i = 0; i < result.length; i++) {
            result[i] = buf.getLong();
        }
        return result;
    }

    @Override
    public byte[] readByteArray() throws IOException {
        int marker = readByte();
        if (marker != BYTE_ARRAY_PREFIX) {
            throw new IOException("Expected byte array marker "
                    + Integer.toHexString(BYTE_ARRAY_PREFIX & 0xFF)
                    + " but got " + Integer.toHexString(marker & 0xFF));
        }
        int arraySize = readInt();
        if (arraySize == 0) {
            return new byte[0];
        } else if (arraySize < 0) {
            throw new IOException("Invalid array size " + arraySize);
        }
        ByteBuffer buf = buffer(arraySize);
        channel.read(buf);
        buf.flip();
        byte[] result = new byte[arraySize];
        buf.get(result);
        return result;
    }
}
