package net.java.dev.imagine.api.vector.io;

import net.java.dev.imagine.api.vector.Primitive;

/**
 *
 * @author Tim Boudreau
 */
public interface VectorWriter {

    VectorWriter writeGeometry(double x, double y);

    VectorWriter writeGeometry(double x1, double y1, double x2, double y2);

    VectorWriter writeGeometry(double x1, double y1, double x2, double y2, double x3, double y3);

    VectorWriter writeGeometry(double[] values);

    VectorWriter writeDouble(double dbl);

    VectorWriter writeFloat(float dbl);

    VectorWriter writeInt(int dbl);

    VectorWriter writeShort(short dbl);

    VectorWriter writeBoolean(boolean val);

    VectorWriter writeBytes(byte[] bytes);

    default VectorWriter writeId(PrimitiveKind<?, ?> kind) {
        return writeInt(kind.ordinal());
    }

    default VectorWriter writeStartRecord() {
        return this;
    }

    default VectorWriter writeEndRecord() {
        return this;
    }

    default <T extends Primitive> VectorWriter begin(PrimitiveKind<T, ?> kind, T item, Runnable run) {
        writeStartRecord();
        writeId(kind);
        kind.accept(item, this);
        writeEndRecord();
        return this;
    }

}
