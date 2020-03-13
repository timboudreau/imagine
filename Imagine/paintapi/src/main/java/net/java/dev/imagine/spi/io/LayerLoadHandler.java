package net.java.dev.imagine.spi.io;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.utils.painting.RepaintHandle;

/**
 *
 * @author Tim Boudreau
 */
public interface LayerLoadHandler<L extends LayerImplementation> {

    boolean recognizes(int layerTypeId);

    <C extends ReadableByteChannel & SeekableByteChannel> L loadLayer(int type, long length, C channel, RepaintHandle handle, Map<String,String> loaderHints) throws IOException;
}
