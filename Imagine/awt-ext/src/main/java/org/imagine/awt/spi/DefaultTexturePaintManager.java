package org.imagine.awt.spi;

import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.key.ManagedTexturePaintKey;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.TexturePaintKey;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
final class DefaultTexturePaintManager extends TexturePaintManager {

    private final Map<String, TexturePaint> paintForKey = Collections.synchronizedMap(new WeakHashMap<>());
    public static final String SYS_PROP_TEXTURES_DIR = "textures.dir";
    private Path cacheDir() {
        String dir = System.getProperty(SYS_PROP_TEXTURES_DIR);
        if (dir == null) {
            return Paths.get("user.home").resolve(".imagine-textures");
        } else {
            return Paths.get(dir);
        }
    }

    @Override
    protected PaintKey<TexturePaint> keyFor(TexturePaint paint) {
        if (isManaged(paint)) {
            HashedBufferedImage img = (HashedBufferedImage) paint.getImage();
            return img.key();
        }
        return new TexturePaintKey(paint);
    }

    @Override
    protected TexturePaint paintFor(ManagedTexturePaintKey key) {
        TexturePaint paint = paintForKey.get(key.id());
        if (paint != null) {
            return paint;
        }
        BufferedImage img = imageFor(key.id(), key);
        TexturePaint result = new TexturePaint(img, key.anchorRect());
        paintForKey.put(key.id(), result);
        return result;
    }

    private void ensureImageSaved(TexturePaintKey key, ManagedTexturePaintKey managed) throws IOException {
        Path pth = cacheDir().resolve(managed.id() + ".rgba");
        if (Files.exists(pth)) {
            return;
        }
        TexturePaint p = Accessor.rawPaintForPaintKey(key);
        BufferedImage img = p.getImage();
        if (img instanceof HashedBufferedImage) {
            return;
        }
        saveImage(managed.id() + ".rgba", img);
    }

    public ManagedTexturePaintKey toManagedKey(TexturePaintKey key) {
        ManagedTexturePaintKey result = new ManagedTexturePaintKey(key.rawX(), key.rawY(), key.rawW(), key.rawH(), key.hash());
        try {
            ensureImageSaved(key, result);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return result;
    }

    private void saveImage(String id, BufferedImage img) throws IOException {
//        Path pth = cacheDir().resolve(id + ".rgba");
        Path pth = cacheDir().resolve(id);
        if (!Files.exists(pth.getParent())) {
            Files.createDirectories(pth.getParent());
        }
        int[] rast = rasterContents(img);
        try (FileChannel out = FileChannel.open(pth, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            int byteCount = 17 + (rast.length * 4);
            ByteBuffer buf = ByteBuffer.allocate(byteCount);
            buf.putInt(img.getWidth());
            buf.putInt(img.getHeight());
            buf.put((byte) img.getTransparency());
            for (int i = 0; i < rast.length; i++) {
                buf.putInt(rast[i]);
            }
            buf.flip();
            out.write(buf);
            out.force(true);
        }
    }

    @Override
    protected BufferedImage imageFor(String id, ManagedTexturePaintKey key) {
        Path pth = cacheDir().resolve(id + ".rgba");
        int[] arr = null;
        int w = 0;
        int h = 0;
        byte xpar = Transparency.OPAQUE;
        if (Files.exists(pth)) {
            try (FileChannel ch = FileChannel.open(pth, StandardOpenOption.READ)) {
                ByteBuffer buf = ByteBuffer.allocate((int) ch.size());
                ch.read(buf);
                buf.flip();
                w = buf.getInt();
                h = buf.getInt();
                xpar = buf.get();
                arr = new int[buf.remaining() / 4];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = buf.getInt();
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else {
            System.out.println("file does not exist: " + pth);
        }
        if (arr == null) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }
        BufferedImage img = new HashedBufferedImage(w, h, BufferedImage.TYPE_INT_ARGB, key);
        img.getRaster().setPixels(0, 0, w, h, arr);
        return img;
    }

    @Override
    protected byte[] hashAndSave(BufferedImage img) {
        int[] rasterContents = rasterContents(img);
        byte[] hash = hash(rasterContents);
        return hash;
    }

    private static int[] rasterContents(BufferedImage img) {
        Raster raster = img.getRaster();
        return raster.getPixels(0, 0, img.getWidth(), img.getHeight(), (int[]) null);
    }

    private static byte[] hash(int[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("sha1");
            byte[] arr = new byte[bytes.length * 4];
            ByteBuffer b = ByteBuffer.wrap(arr);
            for (int i = 0; i < bytes.length; i++) {
                b.putInt(bytes[i]);
            }
            digest.update(b);
            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(ex);
        }
    }

    static class HashedBufferedImage extends BufferedImage {

        private final ManagedTexturePaintKey key;

        public HashedBufferedImage(int i, int i1, int i2, ManagedTexturePaintKey key) {
            super(i, i1, i2);
            this.key = key;
        }

        public ManagedTexturePaintKey key() {
            return key;
        }

        @Override
        public String toString() {
            return "HashedBufferedImage(" + key + ")";
        }
    }

}
