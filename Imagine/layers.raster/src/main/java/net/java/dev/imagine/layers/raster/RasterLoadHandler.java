/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.layers.raster;

import java.io.IOException;
import static java.lang.System.identityHashCode;
import java.util.Map;
import static net.java.dev.imagine.layers.raster.RasterLayerSave.RASTER_LAYER_ID;
import net.java.dev.imagine.spi.io.LayerLoadHandler;
import org.imagine.utils.painting.RepaintHandle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = LayerLoadHandler.class, position = 10)
public class RasterLoadHandler implements LayerLoadHandler<RasterLayerImpl> {

    @Override
    public boolean recognizes(int layerTypeId) {
        return layerTypeId == RasterLayerSave.INSTANCE.layerTypeId();
    }

    @Override
    public <C extends java.nio.channels.ReadableByteChannel & java.nio.channels.SeekableByteChannel> RasterLayerImpl loadLayer(int type, long length, C channel, RepaintHandle handle, Map<String, String> loaderHints) throws IOException {
        return RasterLayerSave.INSTANCE.load(handle, channel, loaderHints);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(rev=" + RasterLayerSave.IO_REV
                + " layerTypeId " + RASTER_LAYER_ID
                + " instance " + Long.toString(identityHashCode(this), 36)
                + ")";
    }

}
