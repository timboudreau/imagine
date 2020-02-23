package org.imagine.awt.io;

import com.mastfrog.function.throwing.io.IOFunction;
import java.awt.Paint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.imagine.awt.impl.Accessor;
import static org.imagine.awt.io.KeyStringWriter.MAGIC_1;
import static org.imagine.awt.io.KeyStringWriter.MAGIC_2;
import org.imagine.awt.key.ColorKey;
import org.imagine.awt.key.GradientPaintKey;
import org.imagine.awt.key.LinearPaintKey;
import org.imagine.awt.key.ManagedTexturePaintKey;
import org.imagine.awt.key.MultiplePaintKey;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.RadialPaintKey;
import org.imagine.awt.key.TexturePaintKey;
import org.imagine.awt.key.TexturedPaintWrapperKey;
import org.imagine.awt.key.UnknownPaintKey;

/**
 *
 * @author Tim Boudreau
 */
public final class PaintKeyIO {

    public static KeyWriter<String> stringWriter(PaintKey<?> key) {
        KeyStringWriter sw = new KeyStringWriter();
        sw.writeInt(key.idBase().hashCode());
        sw.writeInt(key.hashCode());
        return sw;
    }

    public static KeyWriter<List<ByteBuffer>> binaryWriter(PaintKey<?> key) {
        KeyBinaryWriter sw = new KeyBinaryWriter();
        sw.writeInt(key.idBase().hashCode());
        sw.writeInt(key.hashCode());
        return sw;
    }

    public static String writeAsString(PaintKey<?> key) throws IOException {
        KeyStringWriter w = new KeyStringWriter();
        w.writeInt(key.idBase().hashCode());
        w.writeInt(key.hashCode());
        key.writeTo(w);
        return w.get();
    }

    public static byte[] writeAsBytes(PaintKey<?> key) throws IOException {
        KeyBinaryWriter sw = new KeyBinaryWriter();
        sw.writeInt(key.idBase().hashCode());
        sw.writeInt(key.hashCode());
        key.writeTo(sw);
        sw.finish();
        return sw.toByteArray();
    }

    public static void write(PaintKey<?> key, StringBuilder into) throws IOException {
        KeyStringWriter w = new KeyStringWriter(into);
        w.writeInt(key.idBase().hashCode());
        w.writeInt(key.hashCode());
        key.writeTo(w);
    }

    public static PaintKey<?> read(CharSequence seq) throws IOException {
        return read(seq, 0);
    }

    public static PaintKey<?> read(CharSequence seq, int position) throws IOException {
        KeyStringReader reader = new KeyStringReader(position, seq);
        byte m1 = reader.readByte();
        if (MAGIC_1 != m1) {
            throw new IOException("Wrong first magic number byte " + m1 + " should be " + MAGIC_1);
        }
        byte m2 = reader.readByte();
        if (MAGIC_2 != m2) {
            throw new IOException("Wrong second magic number byte " + m2 + " should be " + MAGIC_2);
        }
        int idBase = reader.readInt();
        int hash = reader.readInt();
        Class<? extends PaintKey<?>> type = findType(idBase);
        if (type == null) {
            throw new IOException("Could nt find a paint key type for " + idBase
                    + " among " + Accessor.allSupportedTypes());
        }
        return readOne((Class) type, reader, hash); // XXX
    }

    public static <C extends WritableByteChannel & SeekableByteChannel> void write(C channel, PaintKey<?> key) throws IOException {
        KeyBinaryWriter sw = new KeyBinaryWriter();
        sw.writeInt(key.idBase().hashCode());
        sw.writeInt(key.hashCode());
        key.writeTo(sw);
        sw.finish();
        for (ByteBuffer buf : sw.get()) {
            channel.write(buf);
        }
    }

    public static PaintKey<?> read(byte[] bytes) throws IOException {
        return read(new ByteArrayReadChannel(bytes));
    }

    public static <C extends ReadableByteChannel & SeekableByteChannel> PaintKey<?> read(C channel) throws IOException {
        KeyBinaryReader<C> reader = new KeyBinaryReader<>(channel);

        byte m1 = reader.readByte();
        if (MAGIC_1 != m1) {
            throw new IOException("Wrong first magic number byte " + m1 + " should be " + MAGIC_1);
        }
        byte m2 = reader.readByte();
        if (MAGIC_2 != m2) {
            throw new IOException("Wrong second magic number byte " + m2 + " should be " + MAGIC_2);
        }

        int len = reader.readInt();
        if (channel.size() - channel.position() < len - 16) {
            throw new IOException("Record size reported " + len + " but only "
                    + (channel.size() - channel.position()) + " bytes remain"
                    + " in channel");
        }
        int idBase = reader.readInt();
        int hash = reader.readInt();

        Class<? extends PaintKey<?>> type = findType(idBase);
        if (type == null) {
            throw new IOException("Could nt find a paint key type for " + idBase);
        }
        return readOne((Class) type, reader, hash); // XXX
    }

    private static <K extends PaintKey<P>, P extends Paint> K readOne(Class<K> type, KeyReader reader, int hash) throws IOException {
        IOFunction<? super KeyReader, ? extends K> rdr = readerFunction(type);
        K result = rdr.apply(reader);
        if (result != null) {
            int foundHash = result.hashCode();
            if (foundHash != hash) {
                throw new IOException("Saved and current hash codes do not "
                        + "match for " + result + " - expected " + hash
                        + " got " + foundHash);
            }
        }
        return result;
    }

    private static <K extends PaintKey<P>, P extends Paint>
            IOFunction<? super KeyReader, ? extends K> readerFunction(Class<K> type) {
        Function<KeyReader, K> result = Accessor.readerForKeyType(type);
        if (result != null) {
            return rdr -> {
                return result.apply(rdr);
            };
        }
        if (type == ColorKey.class) {
            return rdr -> {
                return type.cast(ColorKey.read(rdr));
            };
        } else if (type == GradientPaintKey.class) {
            return rdr -> {
                return type.cast(GradientPaintKey.read(rdr));
            };
        } else if (type == LinearPaintKey.class || type == RadialPaintKey.class) {
            return rdr -> {
                return type.cast(MultiplePaintKey.read(rdr));
            };
        } else if (type == (Class) TexturedPaintWrapperKey.class) {
            return rdr -> {
                return type.cast(TexturedPaintWrapperKey.read(rdr));
            };
        } else if (type == TexturePaintKey.class) {
            return rdr -> {
                return type.cast(TexturePaintKey.read(rdr));
            };
        } else if (type == ManagedTexturePaintKey.class) {
            return rdr -> {
                return type.cast(ManagedTexturePaintKey.read(rdr));
            };
        }
        return rdr -> {
            return type.cast(UnknownPaintKey.read(rdr));
        };
    }

    private static Class<? extends PaintKey<?>> findType(int idBase) throws IOException {
        for (Map.Entry<String, Class<?>> e : Accessor.allSupportedTypes().entrySet()) {
            int hc = e.getKey().hashCode();
            if (hc == idBase) {
                return (Class<? extends PaintKey<?>>) e.getValue();
            }
        }
        if (idBase == UnknownPaintKey.ID_BASE.hashCode()) {
            return (Class) UnknownPaintKey.class;
        }
        throw new IOException("No known type for " + idBase);
    }

    private PaintKeyIO() {
        throw new AssertionError();
    }
}
