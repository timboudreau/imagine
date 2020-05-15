package org.imagine.vector.editor.ui.palette;

import com.mastfrog.abstractions.Wrapper;
import com.mastfrog.util.strings.Strings;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger LOG = Logger.getLogger(ShapePaletteStorage.class.getName());

    public ShapePaletteStorage() {
        super(ShapeElement.class);
    }

    @Override
    protected String synthesizeNamePrefix(ShapeElement obj) {
        if (obj.isNameExplicitlySet()) {
            return obj.getName();
        }
        return Strings.toPaddedHex(new long[]{obj.id()}, "-")
                + "-" + obj.item().getClass().getSimpleName();
    }

    @Override
    protected void saveImpl(String name, ShapeElement obj) throws IOException {
        LOG.log(Level.FINE, "Save {0} as {1}", new Object[]{name, obj});
        // XXX if overwrite, ask the user
        try (final FileChannel channel = writeChannel(name)) {
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
        } catch (IOException | RuntimeException | Error ex) {
            LOG.log(Level.SEVERE, "Exception saving paint key '" + name + "' for " + obj, ex);
            throw ex;
        }
    }

    @Override
    protected ShapeEntry loadImpl(String name) throws IOException {
        FileChannel channel = readChannel(name);
        LOG.log(Level.FINE, "Load shape {0} from {1}", new Object[]{name, channel});
        if (channel == null) {
            LOG.log(Level.SEVERE, "Null shape channel for {0}", name);
            return null;
        }
        try {
            KeyBinaryReader reader = new KeyBinaryReader(channel, (byte) 32, (byte) 14);
            reader.readMagicAndSize();
            ShapeEntry result = ShapeEntry.read(new VectorIO().setHashInconsistencyBehavior(
                    HashInconsistencyBehavior.WARN), reader);
            return result;
        } catch (IOException | RuntimeException | Error ex) {
            LOG.log(Level.SEVERE, "Exception saving paint key '" + name, ex);
            throw ex;
        } finally {
            channel.close();
        }
    }
}
