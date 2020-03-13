package org.imagine.vector.editor.ui.palette;

import com.mastfrog.abstractions.Wrapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.imagine.io.KeyBinaryReader;
import org.imagine.io.KeyBinaryWriter;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.io.HashInconsistencyBehavior;
import org.imagine.vector.editor.ui.io.VectorIO;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
class ShapePaletteStorage extends PaletteStorage<ShapeElement> {

    public ShapePaletteStorage() {
        super(ShapeElement.class);
    }

    @Override
    protected void saveImpl(String name, ShapeElement obj) throws IOException {
        try (final FileChannel channel = writeChannel(obj.getName())) {
            ShapeEntry en = obj instanceof ShapeEntry
                    ? (ShapeEntry) obj : Wrapper.find(obj, ShapeEntry.class);
            if (en == null) {
                throw new IOException("Could not find a ShapeEntry instance to save");
            }
            KeyBinaryWriter kbw = new KeyBinaryWriter((byte) 32, (byte) 14);
            VectorIO vio = new VectorIO();
            en.writeTo(vio, kbw);
            kbw.finishRecord();
            for (ByteBuffer buf : kbw.get()) {
                buf.flip();
                channel.write(buf);
            }
            kbw.finishRecord();
        }
    }

    @Override
    protected ShapeEntry loadImpl(String name) throws IOException {
        FileChannel channel = readChannel(name);
        if (channel == null) {
            return null;
        }
        try {
            KeyBinaryReader reader = new KeyBinaryReader(channel);
            reader.readMagicAndSize();
            ShapeEntry result = ShapeEntry.read(new VectorIO().setHashInconsistencyBehavior(HashInconsistencyBehavior.WARN), reader);
            return result;
        } finally {
            channel.close();
        }
    }
}
