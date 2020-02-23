/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import static org.imagine.awt.io.KeyStringWriter.BYTE_ARRAY_PREFIX;
import static org.imagine.awt.io.KeyStringWriter.INT_ARRAY_PREFIX;
import static org.imagine.awt.io.KeyStringWriter.LONG_ARRAY_PREFIX;
import static org.imagine.awt.io.KeyStringWriter.MAGIC_1;
import static org.imagine.awt.io.KeyStringWriter.MAGIC_2;

/**
 *
 * @author Tim Boudreau
 */
final class KeyBinaryWriter implements KeyWriter<List<ByteBuffer>> {

    private static final int MIN_BUFFER = 96;
    private final List<ByteBuffer> buffers = new ArrayList<>();
    private ByteBuffer currBuffer;

    KeyBinaryWriter() {
        this(MAGIC_1, MAGIC_2);
    }

    KeyBinaryWriter(byte magic1, byte magic2) {
        writeByte(magic1);
        writeByte(magic2);
        writeInt(0); // length placeholder
    }

    @Override
    public String toString() {
        if (buffers.isEmpty()) {
            return "Empty binary writer";
        } else {
            return "Binary writer on buffer " + (buffers.size() - 1)
                    + " @ " + buffers.get(buffers.size() - 1).position();
        }
    }

    @Override
    public KeyWriter finishRecord() {
        finish();
        return this;
    }

    void finish() {
        if (buffers.size() > 0) {
            int sz = size();
            ByteBuffer first = buffers.get(0);
            int pos = first.position();
            first.position(2);
            try {
                first.putInt(sz);
            } finally {
                first.position(pos);
            }
        }
    }

    private int size() {
        int result = 0;
        for (ByteBuffer buf : buffers) {
            result += buf.position();
        }
        return result;
    }

    public List<ByteBuffer> get() {
        List<ByteBuffer> result = new ArrayList<>(buffers.size());
        for (ByteBuffer b : buffers) {
            ByteBuffer copy = b.duplicate();
            copy.flip();
            result.add(b);
        }
        return result;
    }

    @Override
    public byte[] toByteArray() {
        int size = 0;
        for (ByteBuffer buf : buffers) {
            size += buf.position();
        }
        byte[] result = new byte[size];
        int cursor = 0;
        for (ByteBuffer buf : buffers) {
            buf = buf.duplicate();
            buf.flip();
            int len = buf.remaining();
            buf.get(result, cursor, len);
            cursor += len;
        }
        return result;
    }

    private ByteBuffer buffer(int projectedBytes) {
        if (currBuffer == null) {
            currBuffer = ByteBuffer.allocate(Math.max(MIN_BUFFER, projectedBytes));
            buffers.add(currBuffer);
        } else if (currBuffer.remaining() < projectedBytes) {
            currBuffer = ByteBuffer.allocate(Math.max(MIN_BUFFER, projectedBytes));
            buffers.add(currBuffer);
        }
        return currBuffer;
    }

    @Override
    public KeyBinaryWriter writeInt(int value) {
        buffer(Integer.BYTES).putInt(value);
        return this;
    }

    @Override
    public KeyBinaryWriter writeLong(long value) {
        buffer(Long.BYTES).putLong(value);
        return this;
    }

    @Override
    public KeyBinaryWriter writeIntArray(int[] ints) {
        writeByte(INT_ARRAY_PREFIX);
        writeInt(ints.length);
        ByteBuffer buf = buffer(ints.length * Integer.BYTES);
        for (int i = 0; i < ints.length; i++) {
            buf.putInt(ints[i]);
        }
        return this;
    }

    @Override
    public KeyBinaryWriter writeLongArray(long[] ints) {
        writeByte(LONG_ARRAY_PREFIX);
        writeInt(ints.length);
        ByteBuffer buf = buffer((ints.length * Long.BYTES) + 5);
        for (int i = 0; i < ints.length; i++) {
            buf.putLong(ints[i]);
        }
        return this;
    }

    @Override
    public KeyBinaryWriter writeByte(byte b) {
        ByteBuffer buf = buffer(1);
        buf.put(b);
        return this;
    }

    @Override
    public KeyBinaryWriter writeByteArray(byte[] bytes) {
        ByteBuffer buf = buffer(bytes.length + 5);
        writeByte(BYTE_ARRAY_PREFIX);
        writeInt(bytes.length);
        buf.put(bytes);
        return this;
    }
}
