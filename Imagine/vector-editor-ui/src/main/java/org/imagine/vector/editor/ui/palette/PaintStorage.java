package org.imagine.vector.editor.ui.palette;

import java.io.IOException;
import java.nio.channels.FileChannel;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
class PaintStorage extends PaletteStorage<PaintKey<?>> {

    public PaintStorage() {
        super(PaintKey.class);
    }

    @Override
    protected void saveImpl(String name, PaintKey obj) throws IOException {
        try (final FileChannel channel = writeChannel(obj.getClass().getSimpleName() + "-" + obj.hashCode())) {
            PaintKeyIO.write(channel, obj);
        }
    }

    @Override
    protected PaintKey loadImpl(String name) throws IOException {
        FileChannel channel = readChannel(name);
        if (channel == null) {
            return null;
        }
        try {
            PaintKey<?> key = PaintKeyIO.read(channel);
            return key;
        } finally {
            channel.close();
        }
    }
}
