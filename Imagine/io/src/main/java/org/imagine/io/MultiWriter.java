/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.io;

import com.mastfrog.function.throwing.io.IOBiConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
final class MultiWriter {

    private final Iterable<Writable> keysIterable;
    static final int DISTANCE = 72;
    static int fillValue = 0b1010110111001101;
    static int preIndexFill = 0b0110001000110010;

    public MultiWriter(Iterable<Writable> keysIterable) {
        this.keysIterable = keysIterable;
    }

    public <C extends WritableByteChannel & SeekableByteChannel> void writeTo(C channel,
            IOBiConsumer<IOException, Writable> notify) throws IOException {

        // Okay, the format here:
        // 8 byte header consisting of the entry count and the
        // file offset of the index
        //
        // First item comes immediately after the header; after that
        // they are spaced on 72 byte boundaries, padded with fillValue.
        // Basically the aim is to guarantee that if the file is corrupted,
        // the data is recoverable
        List<IndexEntry> index = new ArrayList<>();
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
        sizeBuffer.putInt(0);
        sizeBuffer.putInt(0);
        sizeBuffer.flip();
        channel.write(sizeBuffer);
        int count = 0;
        for (Writable pk : keysIterable) {
            IndexEntry en = new IndexEntry(pk.typeId(), channel.position(), count);
            try {
//                PaintKeyIO.write(channel, pk);
                int pos = (int) channel.position();
                int nextPosition = (pos % DISTANCE) + DISTANCE;
                int fillLength = (nextPosition - pos);
                channel.write(ofInts(fillValue, fillLength));
                notify.accept(null, pk);
                count++;
                index.add(en);
            } catch (IOException ex) {
                notify.accept(ex, pk);
            }
        }
        int indexStart = (int) channel.position();
        int nextPosition = (indexStart % DISTANCE) + DISTANCE;
        int fillLength = (nextPosition - indexStart);
        channel.write(ofInts(preIndexFill, fillLength));
        for (IndexEntry en : index) {
            en.write(channel);
        }
        sizeBuffer.putInt(count);
        sizeBuffer.putInt(indexStart);
        long end = channel.position();
        channel.position(0);
        sizeBuffer.flip();
        channel.write(sizeBuffer);
        channel.position(end);
    }

    static class IndexEntry implements Comparable<IndexEntry> {

        private final String id;
        private final long fileOffset;
        private final int itemIndex;

        public IndexEntry(String id, long index, int itemIndex) {
            this.id = id;
            this.fileOffset = index;
            this.itemIndex = itemIndex;
        }

        @Override
        public int compareTo(IndexEntry o) {
            return id.compareToIgnoreCase(o.id);
        }

        public int itemIndex() {
            return itemIndex;
        }

        public long index() {
            return fileOffset;
        }

        public String id() {
            return id;
        }

        <C extends WritableByteChannel & SeekableByteChannel> void write(C channel) {
            byte[] idBytes = id.getBytes(US_ASCII);
            int len = idBytes.length + Long.BYTES;
            ByteBuffer bts = ByteBuffer.allocate(len);
            bts.putInt(itemIndex);
            bts.putLong(fileOffset);
            bts.putInt(idBytes.length);
            bts.put(idBytes);
        }
    }

    private static ByteBuffer ofInts(int val, int length) {
        ByteBuffer buf = ByteBuffer.allocate(length * Integer.BYTES);
        for (int i = 0; i < 10; i++) {
            buf.putInt(val);
        }
        buf.flip();
        return buf;
    }

}
