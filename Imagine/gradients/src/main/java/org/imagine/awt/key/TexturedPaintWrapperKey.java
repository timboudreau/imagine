package org.imagine.awt.key;

import com.mastfrog.abstractions.Wrapper;
import java.awt.Dimension;
import java.awt.MultipleGradientPaint;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.util.Hasher;
import org.imagine.awt.util.IdPathBuilder;
import org.imagine.io.KeyWriter;
import org.imagine.io.KeyReader;

/**
 * For performance reasons, the GradientManager will wrap some linear and radial
 * paints in a TexturedPaint to avoid hangs while painting and poor performance.
 * This key allows us to keep wrap a key for the original in a key that points
 * to a texture paint that wrappers it, without recreating huge rasters on every
 * paint.
 *
 * @author Tim Boudreau
 */
public final class TexturedPaintWrapperKey<P extends MultiplePaintKey<T>, T extends MultipleGradientPaint>
        extends SizedPaintKey<TexturePaint> implements Wrapper<P> {

    public static final String ID_BASE = "gradient-texture";
    private final P delegate;
    private final int width;
    private final int height;

    public TexturedPaintWrapperKey(P delegate, int width, int height) {
        assert delegate != null : "Null delegate";
        assert width > 0 : "Width zero or less: " + width;
        assert height > 0 : "Height zero or less: " + height;
        this.delegate = delegate;
        this.width = width;
        this.height = height;
    }

    @Override
    public PaintKeyKind kind() {
        return delegate.kind();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaintKey<TexturePaint> createTransformedCopy(AffineTransform xform) {
        if (xform.isIdentity()) {
            return this;
        }
        double[] pts = new double[]{0, 0, width, height};
        xform.transform(pts, 0, pts, 0, 2);
        double newWidth = Math.abs(pts[2] - pts[0]);
        double newHeight = Math.abs(pts[3] - pts[1]);
        PaintKey<T> nue = delegate.createTransformedCopy(xform);
        return new TexturedPaintWrapperKey<>((P) nue,
                (int) Math.ceil(newWidth), (int) Math.ceil(newHeight));
    }

    public static <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint> TexturedPaintWrapperKey<P, T> create(P delegate, int width, int height) {
        return new TexturedPaintWrapperKey<>(delegate, width, height);
    }

    public TexturedPaintWrapperKey<P, T> withSize(int w, int h) {
        return new TexturedPaintWrapperKey<>(delegate, w, h);
    }

    public TexturedPaintWrapperKey<P, T> withSize(Dimension dim) {
        return withSize(dim.width, dim.height);
    }

    public P delegate() {
        return delegate;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    public Dimension size() {
        return new Dimension(width, height);
    }

    @Override
    protected Class<TexturePaint> type() {
        return TexturePaint.class;
    }

    @Override
    public TexturePaint toPaint() {
        return Accessor.paintForTextureKey(this);
    }

    @Override
    protected int computeHashCode() {
        return new Hasher()
                .add(width)
                .add(height)
                .add(delegate.hashCode())
                .hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof TexturedPaintWrapperKey<?, ?>) {
            TexturedPaintWrapperKey<?, ?> other = (TexturedPaintWrapperKey<?, ?>) o;
            return other.width == width && other.height == height
                    && other.delegate.equals(delegate);
        }
        return false;
    }

    @Override
    protected void buildId(IdPathBuilder bldr) {
        bldr.add(delegate.id())
                .add(width)
                .add(height);
    }

    @Override
    public P wrapped() {
        return delegate;
    }

    @Override
    public String idBase() {
        return ID_BASE;
    }

    @Override
    public void writeTo(KeyWriter writer) {
        writer.writeInt(width);
        writer.writeInt(height);
        delegate.writeTo(writer);
    }

    public static TexturedPaintWrapperKey read(KeyReader reader) throws IOException {
        int w = reader.readInt();
        int h = reader.readInt();
        if (w < 0 || h < 0) {
            throw new IOException("Read zero or negative size "
                    + w + "x" + h);
        }
        MultiplePaintKey<?> k = MultiplePaintKey.read(reader);
        return TexturedPaintWrapperKey.create(k, w, h);
    }

}
