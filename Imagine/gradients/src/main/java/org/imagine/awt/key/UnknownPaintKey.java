package org.imagine.awt.key;

import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.imagine.awt.util.IdPathBuilder;
import org.imagine.io.KeyWriter;
import org.imagine.io.KeyReader;

/**
 *
 * @author Tim Boudreau
 */
public final class UnknownPaintKey<T extends Paint> extends PaintKey<T> {

    public static final String ID_BASE = "unknown";
    private final T paint;

    public UnknownPaintKey(T paint) {
        assert paint != null : "Paint null";
        this.paint = paint;
    }

    @Override
    public PaintKeyKind kind() {
        return StandardPaintKeyKinds.UNKNOWN;
    }

    @Override
    protected Class<T> type() {
        return (Class<T>) paint.getClass();
    }

    @Override
    public T toPaint() {
        return paint;
    }

    @Override
    public PaintKey<T> createTransformedCopy(AffineTransform xform) {
        return this;
    }

    @Override
    protected int computeHashCode() {
        return paint.hashCode();
    }

    @Override
    public String idBase() {
        return ID_BASE;
    }

    @Override
    protected void buildId(IdPathBuilder bldr) {
        bldr.add(paint.hashCode());
    }

    @Override
    public String toString() {
        return "Unknown(" + paint + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof UnknownPaintKey<?>) {
            UnknownPaintKey<?> other = (UnknownPaintKey<?>) o;
            return other.paint.equals(this.paint);
        }
        return false;
    }

    @Override
    public void writeTo(KeyWriter writer) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oout = new ObjectOutputStream(out)) {
                oout.writeObject(paint);
            }
            writer.writeByteArray(out.toByteArray());
        }
    }

    public static PaintKey<?> read(KeyReader reader) throws IOException {
        byte[] bytes = reader.readByteArray();
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream oin = new ObjectInputStream(in)) {
                return new UnknownPaintKey((Paint) oin.readObject());
            } catch (ClassNotFoundException ex) {
                throw new IOException(ex);
            }
        }
    }
}
