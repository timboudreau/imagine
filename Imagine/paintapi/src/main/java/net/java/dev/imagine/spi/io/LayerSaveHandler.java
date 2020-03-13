package net.java.dev.imagine.spi.io;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import net.java.dev.imagine.spi.image.LayerImplementation;

/**
 *
 * @author Tim Boudreau
 */
public interface LayerSaveHandler<L extends LayerImplementation> {

    /**
     * Save the content in some form to the passed channel, returning the
     * layer type id that will be used at load time.
     * 
     * @param <C> The channel type
     * @param channel The channel
     * @return The id used to identify this layer type for loading later
     * @throws IOException 
     */
    <C extends WritableByteChannel & SeekableByteChannel> int saveTo(L layer, C channel, Map<String,String> saveHints) throws IOException;

    int layerTypeId();

    Class<L> layerType();
}
