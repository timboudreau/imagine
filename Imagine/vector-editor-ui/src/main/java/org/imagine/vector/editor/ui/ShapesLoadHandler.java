/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    /**
     * Original magic number, before layer canvas size was saved with the layer.
     */
    static final byte SHAPE_COLLECTION_MAGIC_1 = (byte) 37;
    /**
     * New magic number, indicates the layer canvas size should be written after the
     * shapes data is.
     */
    static final byte SHAPE_COLLECTION_MAGIC_1A = (byte) 38;
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
        ByteBuffer revCheck = ByteBuffer.allocate(1);
        channel.read(revCheck);
        channel.position(channel.position()-1);
        revCheck.flip();
        byte magic1 = revCheck.get();
        switch(magic1) {
            case SHAPE_COLLECTION_MAGIC_1 :
                break;
            case SHAPE_COLLECTION_MAGIC_1A :
                break;
            default :
                throw new IllegalArgumentException("Bad magic number for shape layer " + magic1
                    + " expected " + SHAPE_COLLECTION_MAGIC_1 + " or " + SHAPE_COLLECTION_MAGIC_1A);
        }

        KeyBinaryReader<C> rdr = new KeyBinaryReader<>(channel, magic1, SHAPE_COLLECTION_MAGIC_2);
        int size = rdr.readMagicAndSize();
        finer(() -> "Key reader reports shapes section length " + size);
        Shapes shapes = Shapes.load(rdr, HashInconsistencyBehavior.WARN);
        long end = channel.position();
        Dimension canvasSize = shapes.getBounds().getSize();
        switch(magic1) {
            case SHAPE_COLLECTION_MAGIC_1A :
                ByteBuffer sizeInfo = ByteBuffer.allocate(Integer.BYTES * 2);
                channel.read(sizeInfo);
                sizeInfo.flip();
                canvasSize = new Dimension(sizeInfo.getInt(), sizeInfo.getInt());
                break;
        }
        finer(() -> "Vector layer finished reading shapes @ " + end + " having read " + (end - start) + " bytes");
        VectorLayerFactoryImpl vlf = Lookup.getDefault().lookup(VectorLayerFactoryImpl.class);
        VectorLayer vl = new VectorLayer("-pending", handle, canvasSize, vlf, shapes);
        return vl;
    }
}
