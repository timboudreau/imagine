package net.java.dev.imagine.layers.raster;

import java.awt.Point;
import java.awt.image.BufferedImage;
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
import org.imagine.utils.painting.RepaintHandle;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class RasterLayerSave implements LayerSaveHandler<RasterLayerImpl> {

    static final int RASTER_LAYER_ID = -5605;
    static final byte IO_REV = 1;
    static final RasterLayerSave INSTANCE = new RasterLayerSave();
    private static final Logger LOG = Logger.getLogger(LayerSaveHandler.class.getName());

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(rev=" + IO_REV
                + " layerTypeId " + RASTER_LAYER_ID
                + " instance " + Long.toString(identityHashCode(this), 36)
                + ")";
    }

    static {
        LOG.setLevel(Level.ALL);
    }

    protected static void fine(Supplier<String> supp) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, supp.get());
        }
    }

    protected static void finest(Supplier<String> supp) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, supp.get());
        }
    }

    protected static void info(Supplier<String> supp) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, supp.get());
        }
    }

    public <C extends java.nio.channels.ReadableByteChannel & java.nio.channels.SeekableByteChannel> RasterLayerImpl load(RepaintHandle handle, C channel, Map<String, String> hints) throws IOException {
        long layerStart = channel.position();
        int layerHeaderLength = 1 + (Integer.BYTES * 6);
        info(() -> "Beging reading raster layer @ " + layerStart + " with header of " + layerHeaderLength);
        ByteBuffer buf = ByteBuffer.allocate(layerHeaderLength);
        channel.read(buf);
        buf.flip();
        byte rev = buf.get();
        if (rev != 1) {
            throw new IOException("Unexpected format revision " + rev + " expected " + IO_REV);
        }
        int type = buf.getInt();
        switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_BYTE_BINARY:
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_BYTE_INDEXED:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_565_RGB:
            case BufferedImage.TYPE_USHORT_GRAY:
                break;
            default:
                throw new IOException("Unknown buffered image type " + type);
        }
        int width = buf.getInt();
        if (width < 0) {
            throw new IOException("Invalid image width " + width);
        }
        int height = buf.getInt();
        if (height < 0) {
            throw new IOException("Invalid image height " + height);
        }
        int x = buf.getInt();
        int y = buf.getInt();
        int pixelArraySize = buf.getInt();
        if (pixelArraySize < 0) {
            throw new IOException("Invalid pixel array size " + pixelArraySize);
        }
        finest(() -> "Loaded raster layer properties size " + width + "x" + height
                + " type " + type + " pos " + x + "," + y + " pixel array size "
                + pixelArraySize);
        int[] pixels = new int[pixelArraySize];
        buf = ByteBuffer.allocate(pixels.length * Integer.BYTES);
        channel.read(buf);
        buf.flip();
        long pxPos = channel.position();
        finest(() -> "After load of " + pixels.length + " channel position " + pxPos);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = buf.getInt();
        }
        BufferedImage img = new BufferedImage(width, height, type);
        img.getRaster().setPixels(0, 0, width, height, pixels);
        RasterLayerFactory rlf = Lookup.getDefault().lookup(RasterLayerFactory.class);
        if (rlf == null) {
            throw new IOException("No RasterLayerFactory in default lookup");
        }
        RasterLayerImpl impl = new RasterLayerImpl(rlf, handle, img);
        impl.surface().setLocation(new Point(x, y));
        return impl;
    }

    @Override
    public <C extends WritableByteChannel & SeekableByteChannel> int saveTo(RasterLayerImpl layer, C channel, Map<String, String> saveHints) throws IOException {
        long start = channel.position();
        int headerSize = 1 + (Integer.BYTES * 5);
        RasterLayerImpl raster = layer.getLookup().lookup(RasterLayerImpl.class);
        if (raster == null) {
            throw new IOException("No raster layer in " + layer);
        }
        RasterSurfaceImpl surface = raster.surface();
        BufferedImage img = surface.getImage();
        Point p = surface.getLocation();

        info(() -> "Begin save raster layer @ " + start + " with " + headerSize + " byte header");

        ByteBuffer buf = ByteBuffer.allocate(headerSize);
        buf.put(IO_REV);
        buf.putInt(img.getType());
        buf.putInt(img.getWidth());
        buf.putInt(img.getHeight());
        buf.putInt(p.x);
        buf.putInt(p.y);
        buf.flip();
        channel.write(buf);

        long pixelsStart = channel.position();
        int[] pixels = img.getRaster().getPixels(0, 0, img.getWidth(), img.getHeight(), (int[]) null);

        // XXX RLE encoding would probably be good enough
        // to substantially reduce size in the application.
        // Or use ImageIO + png
        int pixelArrayWithSizeLength = Integer.BYTES * (pixels.length + 1);
        finest(() -> "Write " + pixels.length + " pixels + length to buffer of "
                + pixelArrayWithSizeLength + " @ " + pixelsStart);
        buf = ByteBuffer.allocateDirect(pixelArrayWithSizeLength);
        buf.putInt(pixels.length);
        for (int i = 0; i < pixels.length; i++) {
            buf.putInt(pixels[i]);
        }
        buf.flip();
        channel.write(buf);
        return layerTypeId();
    }

    @Override
    public int layerTypeId() {
        return RASTER_LAYER_ID;
    }

    @Override
    public Class<RasterLayerImpl> layerType() {
        return RasterLayerImpl.class;
    }

}
