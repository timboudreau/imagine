/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

/**
 * A file channel over a byte array.
 *
 * @author Tim Boudreau
 */
final class ByteArrayReadChannel implements ReadableByteChannel, SeekableByteChannel, WritableByteChannel {

    private byte[] bytes;
    private int cursor;

    ByteArrayReadChannel(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] toByteArray() {
        if (cursor == bytes.length) {
            return bytes;
        }
        return Arrays.copyOf(bytes, cursor);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int rem = dst.remaining();
        if (rem == 0) {
            return 0;
        }
        int max = Math.min(bytes.length - cursor, rem);
        if (max <= 0) {
            return 0;
        }
        dst.put(bytes, cursor, max);
        cursor += max;
        return max;
    }

    @Override
    public boolean isOpen() {
        return cursor < bytes.length;
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int amt = src.remaining();
        if (amt > 0) {
            if (bytes.length - cursor < amt) {
                bytes = Arrays.copyOf(bytes, cursor + amt);
            }
            src.get(bytes, cursor, amt);
            cursor += amt;
            return amt;
        }
        return 0;
    }

    @Override
    public long position() throws IOException {
        return cursor;
    }

    @Override
    public ByteArrayReadChannel position(long newPosition) throws IOException {
        if (newPosition > bytes.length) {
            throw new IOException("Position past end");
        }
        cursor = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        return bytes.length;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        if (size > bytes.length) {
            return this;
        }
        bytes = Arrays.copyOf(bytes, (int) size);
        return this;
    }
}
