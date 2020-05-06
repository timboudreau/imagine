package org.imagine.awt.key;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.util.Hasher;
import org.imagine.awt.util.IdPathBuilder;
import org.imagine.io.KeyWriter;
import org.imagine.io.KeyReader;

/**
 * A texture paint key which embeds the raster data for the texture - these are
 * useful for newly created instances; ManagedTexturePaintKey provides for a
 * variant where a service stores the images, and they are identified by hash.
 * TexturePaints require careful management to avoid creating infinite copies of
 * the same image on the heap; a TexturePaintKey contains the entire raster data;
 * a ManagedTexturePaint key keeps a reference to a disk-cached copy of the data,
 * and is what should be used in, say, copy/paste serialization.
 *
 * @author Tim Boudreau
 */
public final class TexturePaintKey extends PaintKey<TexturePaint> {

    public static final String ID_BASE = "texture";
    private final int x, y, w, h, imageW, imageH, imageType;
    private transient byte[] hash;
    private final int[] raster;

    public TexturePaintKey(int x, int y, int w, int h, int imageW, int imageH, int imageType, byte[] hash, int[] raster) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.imageW = imageW;
        this.imageH = imageH;
        this.imageType = imageType;
        this.hash = hash;
        this.raster = raster;
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public TexturePaintKey(TexturePaint paint) {
        Rectangle2D rect = paint.getAnchorRect();
        x = floatToIntBits(roundOff(rect.getX()));
        y = floatToIntBits(roundOff(rect.getY()));
        w = floatToIntBits(roundOff(rect.getWidth()));
        h = floatToIntBits(roundOff(rect.getHeight()));
        BufferedImage img = paint.getImage();
        imageType = img.getType();
        imageW = img.getWidth();
        imageH = img.getHeight();
        raster = rasterContents(img);
        hash = hash(raster);
        paintForKey.put(this, paint);
    }

    @Override
    public TexturePaintKey createTransformedCopy(AffineTransform xform) {
        if (xform.isIdentity()) {
            return this;
        }
        double[] points = new double[] {x, y, x + w, y+h, 0, 0, imageW, imageH};
        xform.transform(points, 0, points, 0, x);

        BufferedImage origImage = getImage();
        double newImageWidth = points[6] - points[4];
        double newImageHeight = points[7] - points[5];
        if (newImageWidth < 0) {
            points[4] += newImageWidth;
            newImageWidth = -newImageWidth;
        }
        if (newImageHeight < 0) {
            points[5] += newImageHeight;
            newImageHeight = -newImageHeight;
        }
        Rectangle2D.Double newImageBounds = new Rectangle2D.Double(points[4], points[5],
                newImageWidth, newImageHeight);
        Rectangle ibds = newImageBounds.getBounds();
        BufferedImage nue = new BufferedImage(ibds.width, ibds.height, origImage.getType());
        Graphics2D g = nue.createGraphics();
        try {
            g.translate(-newImageBounds.x, -newImageBounds.y);
            g.drawRenderedImage(origImage, xform);
        } finally {
            g.dispose();
        }
        TexturePaint paint = new TexturePaint(nue, newImageBounds);
        return new TexturePaintKey(paint);
    }

    @Override
    public PaintKeyKind kind() {
        return StandardPaintKeyKinds.TEXTURE;
    }

    public int rawX() {
        return x;
    }

    public int rawY() {
        return y;
    }

    public int rawH() {
        return h;
    }

    public int rawW() {
        return w;
    }

    public float x() {
        return intBitsToFloat(x);
    }

    public float y() {
        return intBitsToFloat(x);
    }

    public float w() {
        return intBitsToFloat(x);
    }

    public float h() {
        return intBitsToFloat(x);
    }

    public int imageW() {
        return imageW;
    }

    public int imageH() {
        return imageW;
    }

    public int imageType() {
        return imageType;
    }

    public ManagedTexturePaintKey toManagedKey() {
        return Accessor.managedKeyFor(this);
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

    @Override
    public void writeTo(KeyWriter writer) throws IOException {
        writer.writeInt(x)
                .writeInt(y)
                .writeInt(w)
                .writeInt(h)
                .writeInt(imageType)
                .writeInt(imageW)
                .writeInt(imageH)
                .writeIntArray(raster);
    }

    public static TexturePaintKey read(KeyReader reader) throws IOException {
        int x = reader.readInt();
        int y = reader.readInt();
        int w = reader.readInt();
        int h = reader.readInt();
        int imageType = reader.readInt();
        int imageW = reader.readInt();
        int imageH = reader.readInt();
        int[] raster = reader.readIntArray();
        byte[] hash = hash(raster);
        return new TexturePaintKey(x, y, w, h, imageW, imageH, imageType, hash, raster);
    }

    @Override
    protected Class<TexturePaint> type() {
        return TexturePaint.class;
    }

    static Map<TexturePaintKey, TexturePaint> paintForKey
            = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public TexturePaint toPaint() {
        TexturePaint result = paintForKey.get(this);
        if (result == null) {
            result = constructPaint();
        }
        return result;
    }

    public Rectangle2D anchorRect() {
        return new Rectangle2D.Float(intBitsToFloat(x), intBitsToFloat(y),
                intBitsToFloat(w), intBitsToFloat(h));
    }

    private TexturePaint constructPaint() {
        BufferedImage img = getImage();
        TexturePaint result = new TexturePaint(img, anchorRect());
        paintForKey.put(this, result);
        return result;
    }

    private BufferedImage getImage() {
        BufferedImage result = Accessor.imageForTextureKeyId(id(),
                new ManagedTexturePaintKey(x, y, w, h, hash));
        if (result == null) {
            result = new BufferedImage(imageW, imageH, imageType);
            WritableRaster r = result.getRaster();
            r.setPixels(0, 0, imageW, imageH, raster);
            return result;
        }
        return result;
    }

    @Override
    protected int computeHashCode() {
        return new Hasher().add(hash()).add(imageW).add(imageH)
                .add(x).add(y).add(w).add(h)
                .hashCode();
    }

    @Override
    public String idBase() {
        return ID_BASE;
    }

    public byte[] hash() {
        if (hash == null) {
            hash = hash(raster);
        }
        return hash;
    }

    @Override
    protected void buildId(IdPathBuilder bldr) {
        ByteBuffer buf = ByteBuffer.wrap(hash());
        while (buf.remaining() > 0) {
            bldr.add(buf.getInt());
        }
        bldr.add(x).add(y).add(w).add(h);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        }
        if (!(obj instanceof TexturePaintKey)) {
            return false;
        }
        final TexturePaintKey other = (TexturePaintKey) obj;
        if (this.x != other.x) {
            return false;
        } else if (this.y != other.y) {
            return false;
        } else if (this.w != other.w) {
            return false;
        } else if (this.h != other.h) {
            return false;
        } else if (this.imageW != other.imageW) {
            return false;
        } else if (this.imageH != other.imageH) {
            return false;
        } else if (this.imageType != other.imageType) {
            return false;
        }
        return Arrays.equals(this.hash(), other.hash());
    }

    @Override
    public String toString() {
        return id();
    }

}
