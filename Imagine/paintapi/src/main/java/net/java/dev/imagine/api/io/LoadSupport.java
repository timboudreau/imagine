package net.java.dev.imagine.api.io;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.api.image.Picture;
import static net.java.dev.imagine.api.io.SaveSupport.IO_REV;
import static net.java.dev.imagine.api.io.SaveSupport.LAYER_START_MARKER;
import static net.java.dev.imagine.api.io.SaveSupport.MAGIC_1;
import static net.java.dev.imagine.api.io.SaveSupport.MAGIC_2;
import static net.java.dev.imagine.api.io.SaveSupport.applyLayerProperties;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.PictureImplementation;
import net.java.dev.imagine.spi.io.LayerLoadHandler;
import org.imagine.utils.painting.RepaintHandle;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class LoadSupport<R extends RepaintHandle, D, I extends PictureImplementation> {

    private final PictureFactory<R, D, I> pictureFactory;
    private final int typeId;
    private final String fileExtension;
    private static final Logger LOG = Logger.getLogger(LoadSupport.class.getName());

    // XXX should support multiple factory types - saving does
    protected LoadSupport(PictureFactory<R, D, I> pictureFactory,
            int loaderId, String extension) {
        this.pictureFactory = pictureFactory;
        this.typeId = loaderId;
        this.fileExtension = extension;
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

    protected abstract String displayName();

    protected abstract void open(I pictureImpl);

    protected void populateLayerSaverHints(Map<? super String, ? super String> hints) {
        // do nothing
    }

    public BiFunction<Dimension, BiConsumer<RepaintHandle, Function<List<LayerImplementation>, Picture>>, Void> create(boolean open) {
        System.out.println("\nstart create sequence - open? " + open);
        return (Dimension dim, BiConsumer<RepaintHandle, Function<List<LayerImplementation>, Picture>> next) -> {
            System.out.println("  Called back with dim " + dim);
            R handle = pictureFactory.newRepaintHandle(dim, null);
            System.out.println("  Got dimension " + dim + " and repaint handle " + handle
                + ", calling " + next);
            next.accept(handle, (List<LayerImplementation> layers) -> {
                System.out.println("  create with " + layers.size() + " layers: " + layers);
                I pictureImpl = pictureFactory.toPicture(dim, handle, layers, null);
                System.out.println("     got picture impl " + pictureImpl);
                if (open) {
                    System.out.println("        opening " + pictureImpl);
                    open(pictureImpl);
                }
                return pictureImpl.getPicture();
            });
            return null;
        };
    }

    private Map<String, String> hints() {
        Map<String, String> hints = new HashMap<>();
        populateLayerSaverHints(hints);
        hints.put("extension", fileExtension);
        return hints;
    }

    public final String fileExtension() {
        return fileExtension;
    }

    public final int typeId() {
        return typeId;
    }

    public Picture open(Path path) throws IOException {
        return load(path, true);
    }

    public Picture load(Path path, boolean open) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            Picture result = doLoad(channel, open);
            if (result != null) {
                result.associateFile(path);
            }
            return result;
        }
    }

    protected <C extends ReadableByteChannel & SeekableByteChannel> I load(C channel,
            int type,
            int layerCount, Dimension pictureSize,
            List<Long> layerPositions) throws IOException {
        return defaultLoad(channel, type, layerCount, pictureSize, layerPositions);
    }

    public interface PictureFactory<R extends RepaintHandle, P, I extends PictureImplementation> {

        I toPicture(Dimension d, R r, List<LayerImplementation> layers, P customData);

        R newRepaintHandle(Dimension pictureSize, P customData);

        <C extends ReadableByteChannel & SeekableByteChannel> P loadCustomData(C channel) throws IOException;
    }

    static final <C extends ReadableByteChannel & SeekableByteChannel>
            Picture doLoad(C channel, boolean open) throws IOException {
        long startPos = channel.position();
        int headBufferSize = 3 + (Integer.BYTES * 4);
        ByteBuffer head = ByteBuffer.allocate(headBufferSize);
        fine(() -> "Load head at " + startPos + " loading " + head.capacity()
                + " bytes " + headBufferSize);
        channel.read(head);
        head.flip();
        byte m1 = head.get();
        if (m1 != MAGIC_1) {
            throw new IOException("Wrong magic number " + m1 + " expected " + MAGIC_1 + " at " + (channel.position() + head.position()));
        }
        byte m2 = head.get();
        if (m2 != MAGIC_2) {
            throw new IOException("Wrong magic number " + m1 + " expected " + MAGIC_2 + " at " + (channel.position() + head.position() - 1));
        }
        byte ioRev = head.get();
        if (ioRev != IO_REV) {
            throw new IOException("Unknown file format revision " + ioRev + " - expected " + IO_REV + " at " + (channel.position() + head.position() - 2));
        }
        int type = head.getInt();
        int layerCount = head.getInt();
        finer(() -> "Layer count " + layerCount);
        if (layerCount < 0) {
            throw new IOException("Negative layer count in file: " + layerCount + " at " + (channel.position() + head.position() - Integer.BYTES));
        }
        int pictureWidth = head.getInt();
        if (pictureWidth < 0) {
            throw new IOException("Invalid picture width " + pictureWidth + " at " + (channel.position() + head.position() - Integer.BYTES));
        }
        int pictureHeight = head.getInt();
        if (pictureHeight < 0) {
            throw new IOException("Invalid picture width " + pictureWidth + " at " + (channel.position() + head.position() - Integer.BYTES));
        }
        fine(() -> ("ioRev " + ioRev + " type " + type + " layerCount " + layerCount + " size " + pictureWidth + "x" + pictureHeight));
        Dimension pictureDim = new Dimension(pictureWidth, pictureHeight);
        ByteBuffer layerPositionBuffer = ByteBuffer.allocate(layerCount * Long.BYTES);
        List<Long> layerPositions = new ArrayList<>(layerCount);
        long lp = channel.position();
        fine(() -> "Read layer position at " + lp);
        channel.read(layerPositionBuffer);
        layerPositionBuffer.flip();

        for (int i = 0; i < layerCount; i++) {
            long layPos = layerPositionBuffer.getLong();
            int ix = i;
            finer(() -> "Expect layer " + ix + " @ " + layPos);
            layerPositions.add(layPos);
        }

        for (LoadSupport<?, ?, ?> ls : Lookup.getDefault().lookupAll(LoadSupport.class)) {

            if (ls.typeId() == type) {
                long lpos = channel.position();
                fine(() -> "Hand off layer reading to " + ls + " @ " + lpos);
                return loadWithLoader(ls, channel, type, open, layerCount, pictureDim, layerPositions).getPicture();
            }
        }
        throw new IOException("No LoadSupport recognizes type id " + type);
    }

    private static <C extends ReadableByteChannel & SeekableByteChannel, R extends RepaintHandle, D, I extends PictureImplementation>
            I loadWithLoader(LoadSupport<R, D, I> ls, C channel, int type, boolean open, int layerCount, Dimension pictureDim, List<Long> layerPositions) throws IOException {
        I result = ls.load(channel, type, layerCount, pictureDim, layerPositions);
        if (open) {
            ls.open(result);
        }
        return result;
    }

    private <C extends ReadableByteChannel & SeekableByteChannel> I defaultLoad(
            C channel,
            int type,
            int layerCount, Dimension pictureSize,
            List<Long> layerPositions) throws IOException {
        info(() -> this + ".defaultLoad() type " + type + " layerCount " + layerCount
                + " pictureSize " + pictureSize.width + "x" + pictureSize.height
                + " layerPositions " + layerPositions);
        List<LayerImplementation> layers = new ArrayList<>(layerCount);
        Collection<? extends LayerLoadHandler> handlers = Lookup.getDefault().lookupAll(LayerLoadHandler.class);
        if (handlers.isEmpty()) {
            throw new IOException("No LayerLoadHandlers installed at all. "
                    + "Missing plugin(s)?");
        }
        long cdStart = channel.position();
        D data = pictureFactory.loadCustomData(channel);
        long start = channel.position();
        finer(() -> "Loaded custom data at " + cdStart + " type " + type + " loaded " + (start - cdStart) + " bytes");
        finer(() -> "Starting load of layers at " + start + " by " + this);
        R repaintHandle = pictureFactory.newRepaintHandle(pictureSize, data);
        Map<String, String> hints = hints();
        Rectangle r = new Rectangle();
        for (int i = 0; i < layerCount; i++) {
            int layerHeaderBytes = Integer.BYTES * 3;
            int ix = i;
            long layerStart = channel.position();
            fine(() -> "Start reading layer " + ix + " @ " + layerStart
                    + " reading " + layerHeaderBytes + " byte header");
            ByteBuffer layerHeader = ByteBuffer.allocate(layerHeaderBytes);
            channel.read(layerHeader);
            layerHeader.flip();
            long layerHeaderEnd = channel.position();
            int startMarker = layerHeader.getInt();
            if (startMarker != LAYER_START_MARKER) {
                throw new IOException("Did not find layer start marker at "
                        + channel.position() + " - got " + startMarker);
            }
            int layerIndex = layerHeader.getInt();
            finer(() -> "Start marker " + startMarker
                    + " ok. Checking layer index expecting " + layerIndex);
            if (layerIndex != i) {
                throw new IOException("Layer index " + layerIndex
                        + " does not match current " + i);
            }
            int layerType = layerHeader.getInt();
            fine(() -> "Layer index " + layerIndex + " ok. Layer type is " + layerType);
            LayerLoadHandler h = null;
            for (LayerLoadHandler handler : handlers) {
                if (handler.recognizes(layerType)) {
                    finer(() -> "Recog by " + handler);
                    h = handler;
                    break;
                } else {
                    finer(() -> "Layer type " + layerType
                            + " not recognized by " + handler);
                }
            }
            if (h == null) {
                info(() -> "Uh oh. No LayerLoad handler recogizes layer type "
                        + layerType + ", out of " + handlers + ". Cannot "
                        + "load this stream.");
                // XXX - the right way to do this would be to simply
                // create a dummy layer type which is undisplayable but
                // preserves the bytes from the unreadable layer, and
                // writes them back out in the same position on save,
                // so that files with unreadable layers could be edited
                // without destroying data.
                throw new IOException("No layer handler recognizes layer type "
                        + layerType);
            }
            long len;
            if (i < layerCount - 1) {
                len = layerPositions.get(i + 1) - layerPositions.get(i);
                finer(() -> "Expected layer data length based on index: " + len);
            } else {
                len = (channel.size() - Integer.BYTES) - layerPositions.get(i);
                finer(() -> "Expected layer data length based on stream position: "
                        + len);
            }
            LayerLoadHandler theHandler = h;
            fine(() -> "Hand off layer loading to " + theHandler + " at "
                    + layerHeaderEnd);
            LayerImplementation layer = h.loadLayer(layerType, len, channel, repaintHandle, hints);
            layers.add(layer);
            long lpStart = channel.position();
            finer(() -> "Loaded layer " + ix + " (" + layer
                    + ") successfully using " + theHandler
                    + ". Will read standard layer properties @ "
                    + lpStart);
            applyLayerProperties(i, layers.get(i), channel);
            long lpEnd = channel.position();
            finer(() -> "After standard layer properties load, stream position "
                    + lpEnd);
            Rectangle bds = layer.getBounds();
            if (r.isEmpty()) {
                r.setFrame(bds);
                finer(() -> "Init picture bounds " + bds.x
                        + ", " + bds.y + " " + bds.width + "x"
                        + bds.height);
            } else {
                r.add(bds);
                finer(() -> "Add to picture bounds " + bds.x
                        + ", " + bds.y + " " + bds.width + "x"
                        + bds.height + " to get " + r.x
                        + ", " + r.y + " " + r.width + "x"
                        + r.height);
            }
        }
        long tailStart = channel.position();
        finer(() -> "Read tail at " + tailStart);
        ByteBuffer tail = ByteBuffer.allocate(Integer.BYTES);
        channel.read(tail);
        tail.flip();
        int activeLayer = tail.getInt();
        I result = pictureFactory.toPicture(pictureSize, repaintHandle,
                layers, data);
        fine(() -> "Final hand off of layer set to " + pictureFactory
                + " created " + result
                + ". Setting active layer to saved "
                + activeLayer + " if > 0.");
        if (activeLayer >= 0 && activeLayer < layers.size()) {
            result.setActiveLayer(layers.get(activeLayer));
        }
        return result;
    }
}
