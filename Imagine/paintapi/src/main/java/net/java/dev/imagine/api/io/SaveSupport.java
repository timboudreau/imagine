/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.io;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.Accessor;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.PictureImplementation;
import net.java.dev.imagine.spi.io.LayerSaveHandler;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class SaveSupport<P, I extends PictureImplementation> {

    static final byte IO_REV = 1;
    static final byte MAGIC_1 = 97;
    static final byte MAGIC_2 = 28;
    static final int LAYER_START_MARKER = 301903915;
    static final int LAYER_PROPS_MARKER = 232379737;
    private final Class<I> implType;
    private final int typeId;
    private final String ext;
    private static final Logger LOG = Logger.getLogger(SaveSupport.class.getName());

    static {
        LOG.setLevel(Level.FINER);
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

    protected SaveSupport(Class<I> implType, int typeId, String ext) {
        this.implType = implType;
        this.typeId = typeId;
        this.ext = ext;
    }

    int typeId() {
        return typeId;
    }

    public abstract String displayName();

    protected void populateLayerSaverHints(Map<? super String, ? super String> hints) {
        // do nothing
    }

    private Map<String, String> hints() {
        Map<String, String> hints = new HashMap<>();
        populateLayerSaverHints(hints);
        hints.put("extension", ext);
        return hints;
    }

    public String fileExtension() {
        return ext;
    }

    public static Set<String> supportedExtensions() {
        Set<String> result = new HashSet<>();
        for (SaveSupport supp : Lookup.getDefault().lookupAll(SaveSupport.class)) {
            result.add(supp.fileExtension());
        }
        return result;
    }

    public static <C extends WritableByteChannel & SeekableByteChannel> long save(Picture picture, Path path) throws IOException {
        List<String> avail = new ArrayList<>(5);
        for (SaveSupport supp : Lookup.getDefault().lookupAll(SaveSupport.class)) {
            avail.add(supp.ext);
            boolean canSave = supp.canSavePicture(picture);
            boolean extMatches = path.getFileName().toString().endsWith("." + supp.ext);
            fine(() -> "Try save support " + supp + " can save? " + canSave + " ext " + supp.ext + " path " + path
                    + " ext matches " + extMatches);
            if (canSave && extMatches) {
                return supp.doSave(picture, path);
            }
        }
        throw new IOException("No SaveSupport can save " + Accessor.pictureImplFor(picture)
                + " to the file extension of " + path + ". Available extensions: " + avail);
    }

    public static <C extends WritableByteChannel & SeekableByteChannel> long save(Picture picture, C channel) throws IOException {
        for (SaveSupport supp : Lookup.getDefault().lookupAll(SaveSupport.class)) {
            if (supp.canSavePicture(picture)) {
                return supp.callSaveImpl(picture, channel);
            }
        }
        throw new IOException("Could not find an installed SaveSupport that "
                + "can save " + Accessor.pictureImplFor(picture));
    }

    public static boolean canSave(Picture picture) {
        Collection<? extends SaveSupport> all = Lookup.getDefault().lookupAll(SaveSupport.class);
        if (all.isEmpty()) {
            return false;
        }
        for (SaveSupport ss : all) {
            if (ss.canSavePicture(picture)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSavePicture(Picture picture) {
        return canSavePictureImpl(Accessor.pictureImplFor(picture));
    }

    private boolean canSavePictureImpl(PictureImplementation impl) {
        boolean result = implType.isInstance(impl);
        info(() -> "Can save picture " + impl + " looking for " + implType.getName()
                + " = " + result);
        return result;
    }

    /**
     * Opens a channel for the file and calls save.
     *
     * @param picture The picture
     * @param path The file
     * @return The number of bytes saved
     * @throws IOException If something goes wrong
     */
    private long doSave(Picture picture, Path path) throws IOException {
        try (FileChannel out = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            return callSaveImpl(picture, out);
        }
    }

    protected abstract <C extends WritableByteChannel & SeekableByteChannel> P saveCustomData(I picture, C channel) throws IOException;

    private <C extends WritableByteChannel & SeekableByteChannel> long callSaveImpl(Picture picture, C channel) throws IOException {
        I impl = implType.cast(Accessor.pictureImplFor(picture));
        return saveImpl(impl, channel);
    }

    protected <C extends WritableByteChannel & SeekableByteChannel> long saveImpl(I picture, C c) throws IOException {
        return defaultSaveImpl(picture, new DebugWrapperChannel(c));
    }

    private final <C extends WritableByteChannel & SeekableByteChannel> long defaultSaveImpl(I picture, C c) throws IOException {
        long initialPosition = c.position();
        int headLength = 3 + (Integer.BYTES * 4);
        fine(() -> "Start write at " + initialPosition + " ready " + headLength + " bytes");
        try {
            List<LayerImplementation> layers = picture.getLayers();
            // 1.  Write the file header:
            //  - Two bytes magic number
            //  - One byte save support file type revision
            //  - One int - the id of this loader for lookup on load attempts
            //  - One int - The layer count
            //  - Two ints - The width and height of the picture
            //    (layers may be larger - the visual window)

            ByteBuffer buf = ByteBuffer.allocate(headLength);
            buf.put(MAGIC_1);
            buf.put(MAGIC_2);
            buf.put(IO_REV);
            buf.putInt(typeId());
            buf.putInt(layers.size());
            Dimension sz = picture.getSize();
            buf.putInt(sz.width);
            buf.putInt(sz.height);
            buf.flip();
            c.write(buf);

            // For error recovery (so one layer loader can fail without the file
            // being completely unopenable), we will record the file-offset of
            // each layer here; for now we just write zeros, then seek backward
            // to layerInfoPosition after all layers have been written and
            // rewrite this section
            long layerInfoPosition = c.position();
            // Write placeholders - we go back and overwrite with real data later
            int layerInfoLength = Long.BYTES * layers.size();
            ByteBuffer layerInfo = ByteBuffer.allocate(layerInfoLength);
            finer(() -> "Will write initial info at " + layerInfoPosition + " of " + layerInfoLength + " bytes");
            for (int i = 0; i < layers.size(); i++) {
                layerInfo.putLong(0);
            }
            layerInfo.flip();
            c.write(layerInfo);

            // The picture type may have its own data that it wants to save;
            // Do that here.
            P data = saveCustomData(picture, c);
            finer(() -> "Saved custom data " + data);

            // A list to collect layer file offsets in for later overwriting
            // at layerInfoPosition
            List<Long> layerPositions = new ArrayList<>();
            long lastLayerEnd = 0;

            // Hints allow customization of the file format for future-proofing,
            // such as signalling to save in an older version, JSON, whatever
            Map<String, String> hints = hints();
            // Now write each layer in order
            for (int i = 0; i < layers.size(); i++) {
                LayerImplementation layer = layers.get(i);
                // The layer must have a LayerSaveHandler that can write it to
                // the file
                LayerSaveHandler h = layer.getLookup().lookup(LayerSaveHandler.class);
                if (h == null) {
                    // Future - warn the user and offer to continue?
                    throw new IOException("Layer " + layer + " does not support saving");
                }
                long start = c.position();
                int ix = i;
                layerPositions.add(start);
                // Write the layer header, including some sanity-checking
                // information
                // a. One int - layer start marker - if missing, the file is
                //    corrupt
                // b. This layer's index, as an additional integrity check
                // c. The type id of this layer loader, to look up the right one
                //    when loading
                int layerHeaderLength = Integer.BYTES * 3;
                ByteBuffer layerHeader = ByteBuffer.allocate(layerHeaderLength);
                finer(() -> "Begin layer " + ix + " header of "
                        + layerHeaderLength + " bytes @ " + start);
                layerHeader.putInt(LAYER_START_MARKER);
                layerHeader.putInt(i);
                layerHeader.putInt(h.layerTypeId());
                layerHeader.flip();
                c.write(layerHeader);
                // Now we hand off writing of the file to the layer writer
                long writerStart = c.position();
                fine(() -> "Hand off layer writing to " + h + " @ " + writerStart);
                h.saveTo(layer, c, hints);
                // Ensure the layer writer didn't seek the stream somewhere
                // weird
                if (c.position() < lastLayerEnd) {
                    throw new IOException("Layer saver " + h + " repositioned stream");
                }
                if (c.position() <= writerStart) {
                    throw new IOException("Layer saver " + h + " repositioned stream");
                }
                long lle = c.position();
                finer(() -> "Return from " + h + " @ " + lle + " having written " + (lle - start) + " bytes");

                // Save generic layer properties (visibility, opacity, name if assigned)
                long layerPropsStart = c.position();
                finer(() -> "Save standard layer properties for " + layer + " @ " + layerPropsStart);
                saveLayerProperties(i, layer, c);

                lastLayerEnd = lle;
            }
            // Lastly, save the index of the active layer
            long tailStart = c.position();
            ByteBuffer tail = ByteBuffer.allocate(Integer.BYTES);
            finer(() -> "Write tail @ " + tailStart + " of " + tail.capacity() + " bytes");
            tail.putInt(layers.indexOf(picture.getActiveLayer()));
            tail.flip();
            c.write(tail);
            long tailEnd = c.position();
            finer(() -> "End of data at " + tailEnd + " rewinding to " + layerInfoPosition + " to update layer offset table");
            // Now go back and overwrite the layer file offsets now that we know
            // what they are
            layerInfo.rewind();
            for (int i = 0; i < layers.size(); i++) {
                layerInfo.putLong(layerPositions.get(i));
            }
            layerInfo.flip();
            c.position(layerInfoPosition);
            c.write(layerInfo);
            c.position(tailEnd);
            return tailEnd;
        } catch (IOException | RuntimeException | Error ioe) {
            // try to restore the stream state
            if (c.isOpen()) {
                c.position(initialPosition);
            }
            throw ioe;
        }
    }

    static <C extends ReadableByteChannel & SeekableByteChannel> void applyLayerProperties(int layerIndex, LayerImplementation layer, C channel) throws IOException {
        // Read the standard layer properties opacity, visibility and name and set them on the
        // newly loaded layer
        long lpStart = channel.position();

        LoadSupport.finer(() -> "Read generic layer properties for layer "
                + layerIndex + "(" + layer + ") @ " + lpStart);
        // This section is variable-length because of the name, so start by
        // reading the byte count for the section, by itself
        ByteBuffer len = ByteBuffer.allocate(Integer.BYTES);
        channel.read(len);
        len.flip();
        int length = len.getInt();
        LoadSupport.finer(() -> "Will allocate a buffer of " + length + " for generic properties of layer "
                + layerIndex + "(" + layer + ")");
        // Ensure the read length is sane
        int minLength = Float.BYTES + 1 + Integer.BYTES;
        if (length <= minLength) {
            throw new IOException("Invalid length for generic layer properties " + length + " expecting minimum " + minLength
                    + " at " + lpStart + " for layer " + layerIndex
                    + " (" + layer + ")");
        }
        // Now read the actual data:
        // 1. 1-int - layer start marker sanity check
        // 2. 1-float - opacity
        // 3. 1-byte - visibility (non-zero = visible)
        // 4. The name length (hmm, could subtract and avoid this field)
        // 5. The name bytes as UTF-8
        ByteBuffer buf = ByteBuffer.allocate(length);
        channel.read(buf);
        buf.flip();

        int marker = buf.getInt();
        if (marker != LAYER_PROPS_MARKER) {
            throw new IOException("Did not find layer properties marker "
                    + " " + marker + " at " + (lpStart + Integer.BYTES));
        }
        float opa = buf.getFloat();
        boolean visible = buf.get() != 0;
        int nameLength = buf.getInt();
        int expectedNameLength = (Integer.BYTES * 2) + Float.BYTES + 1;
        if (nameLength != length - expectedNameLength) {
            throw new IOException("Name length " + nameLength
                    + " does not match remaining byte count "
                    + (length - expectedNameLength));
        }
        LoadSupport.finer(() -> "Opacity " + opa + " visible " + visible + " name length " + nameLength);
        // Configure the layer with the read properties
        if (nameLength > 0) {
            byte[] bytes = new byte[nameLength];
            buf.get(bytes);
            String name = new String(bytes, UTF_8);
            layer.setName(name);
        }
        layer.setOpacity(opa);
        layer.setVisible(visible);
    }

    static <C extends WritableByteChannel & SeekableByteChannel> void saveLayerProperties(int layerIndex, LayerImplementation layer, C c) throws IOException {
        byte[] bts = layer.getName().getBytes(UTF_8);
        // Compute the section length
        int length = Float.BYTES + 1 + bts.length + (Integer.BYTES * 2);
        long start = c.position();
        fine(() -> "Save generic layer properties for layer " + layerIndex + " (" + layer + ") of length " + length + " @ " + start);
        // Write the info:
        // 1. 1-int - layer start marker sanity check
        // 2. 1-float - opacity
        // 3. 1-byte - visibility (non-zero = visible)
        // 4. The name length (hmm, could subtract and avoid this field)
        // 5. The name bytes as UTF-8
        ByteBuffer buf = ByteBuffer.allocate(length + Integer.BYTES);
        buf.putInt(length);
        buf.putInt(LAYER_PROPS_MARKER);
        buf.putFloat(layer.getOpacity());
        buf.put(layer.isVisible() ? 1 : (byte) 0);
        buf.putInt(bts.length);
        buf.put(bts);
        buf.flip();
        c.write(buf);
    }
}
