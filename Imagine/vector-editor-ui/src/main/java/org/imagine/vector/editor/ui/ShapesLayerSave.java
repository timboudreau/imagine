package org.imagine.vector.editor.ui;

import java.awt.Rectangle;
import java.io.IOException;
import static java.lang.System.identityHashCode;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.spi.io.LayerSaveHandler;
import org.imagine.io.KeyBinaryWriter;
import static org.imagine.vector.editor.ui.ShapesLoadHandler.SHAPE_COLLECTION_MAGIC_1A;
import static org.imagine.vector.editor.ui.ShapesLoadHandler.SHAPE_COLLECTION_MAGIC_2;
import org.imagine.vector.editor.ui.spi.ShapesCollection;

/**
 *
 * @author Tim Boudreau
 */
public class ShapesLayerSave implements LayerSaveHandler<VectorLayer> {

    public static int SHAPES_LAYER_ID = 120909;

    static ShapesLayerSave INSTANCE = new ShapesLayerSave();
    private static final Logger LOG = Logger.getLogger(ShapesLayerSave.class.getName());

    static {
        LOG.setLevel(Level.ALL);
    }

    protected static void fine(Supplier<String> supp) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, supp.get());
        }
    }

    protected static void finer(Supplier<String> supp) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, supp.get());
        }
    }

    protected static void finest(Supplier<String> supp) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, supp.get());
        }
    }

    protected static void info(Supplier<String> supp) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, supp.get());
        }
    }

    @Override
    public <C extends WritableByteChannel & SeekableByteChannel> int saveTo(VectorLayer layer, C channel, Map<String, String> saveHints) throws IOException {
        ShapesCollection coll = layer.getLookup().lookup(ShapesCollection.class);
        Shapes shapes;
        if (coll instanceof Shapes) {
            shapes = (Shapes) coll;
        } else if (coll instanceof RepaintProxyShapes) {
            shapes = ((RepaintProxyShapes) coll).root();
        } else {
            throw new IOException("Could not find an instance of "
                    + "ShapesCollection in layer lookup of " + layer
                    + " that is either a " + Shapes.class.getName()
                    + " or a " + RepaintProxyShapes.class.getName());
        }

        long start = channel.position();
        fine(() -> "Vector layer write shapes starting at " + start);
        KeyBinaryWriter w = new KeyBinaryWriter(SHAPE_COLLECTION_MAGIC_1A, SHAPE_COLLECTION_MAGIC_2);
        shapes.writeTo(w);
        w.finishRecord();
        finer(() -> "Shapes writer reports bytes to write " + w.size());
        for (ByteBuffer buf : w.get()) {
            buf.flip();
            channel.write(buf);
        }
        long end = channel.position();
        finer(() -> "Vector layer wrote shapes starting at " + start
                + " ending at " + end + " of " + (end - start) + " bytes");
        ByteBuffer sizeInfo = ByteBuffer.allocate(Integer.BYTES * 2);
        Rectangle r = layer.getBounds();
        sizeInfo.putInt(r.width);
        sizeInfo.putInt(r.height);
        sizeInfo.flip();
        channel.write(sizeInfo);
        finer(() -> "Vector layer wrote size " + r.width + "," + r.height);

        return layerTypeId();
    }

    @Override
    public int layerTypeId() {
        return SHAPES_LAYER_ID;
    }

    @Override
    @SuppressWarnings("NonPublicExported")
    public Class<VectorLayer> layerType() {
        return VectorLayer.class;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "("
                + " layerTypeId " + SHAPES_LAYER_ID
                + " instance " + Long.toString(identityHashCode(this), 36)
                + ")";
    }

}
