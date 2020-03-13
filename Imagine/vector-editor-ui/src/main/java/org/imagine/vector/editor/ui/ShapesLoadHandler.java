/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.spi.io.LayerLoadHandler;
import org.imagine.io.KeyBinaryReader;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.io.HashInconsistencyBehavior;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = LayerLoadHandler.class, position = 10)
public class ShapesLoadHandler implements LayerLoadHandler<VectorLayer> {

    private static final Logger LOG = Logger.getLogger(ShapesLayerSave.class.getName());
    static final byte SHAPE_COLLECTION_MAGIC_1 = (byte) 37;
    static final byte SHAPE_COLLECTION_MAGIC_2 = (byte) 52;

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

    @Override
    public boolean recognizes(int layerTypeId) {
        return layerTypeId == ShapesLayerSave.SHAPES_LAYER_ID;
    }

    @Override
    public <C extends java.nio.channels.ReadableByteChannel & java.nio.channels.SeekableByteChannel> VectorLayer loadLayer(int type, long length, C channel, RepaintHandle handle, Map<String, String> loaderHints) throws IOException {
        assert recognizes(type) : "Wrong handler for type " + type;
        long start = channel.position();
        fine(() -> "Vector layer begin reading shapes @ " + start);
        KeyBinaryReader<C> rdr = new KeyBinaryReader<>(channel, SHAPE_COLLECTION_MAGIC_1, SHAPE_COLLECTION_MAGIC_2);
        int size = rdr.readMagicAndSize();
        finer(() -> "Key reader reports shapes section length " + size);
        Shapes shapes = Shapes.load(rdr, HashInconsistencyBehavior.WARN);
        long end = channel.position();
        finer(() -> "Vector layer finished reading shapes @ " + end + " having read " + (end - start) + " bytes");
        VectorLayerFactory vlf = Lookup.getDefault().lookup(VectorLayerFactory.class);
        VectorLayer vl = new VectorLayer("-pending", handle, vlf, shapes);
        return vl;
    }
}
