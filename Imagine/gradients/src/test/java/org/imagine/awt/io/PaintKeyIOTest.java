package org.imagine.awt.io;

import com.mastfrog.util.strings.Strings;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.imagine.awt.impl.Accessor;
import static org.imagine.awt.io.KeyStringReader.parseLongHex;
import static org.imagine.awt.io.KeyStringReader.toByte;
import static org.imagine.awt.io.KeyStringReader.toNibble;
import static org.imagine.awt.io.KeyStringWriter.BYTE_ARRAY_PREFIX;
import static org.imagine.awt.io.KeyStringWriter.INT_ARRAY_PREFIX;
import static org.imagine.awt.io.KeyStringWriter.LONG_ARRAY_PREFIX;
import static org.imagine.awt.io.KeyStringWriter.MAGIC_1;
import static org.imagine.awt.io.KeyStringWriter.MAGIC_2;
import org.imagine.awt.key.ColorKey;
import org.imagine.awt.key.GradientPaintKey;
import org.imagine.awt.key.LinearPaintKey;
import org.imagine.awt.key.ManagedTexturePaintKey;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.RadialPaintKey;
import org.imagine.awt.key.TexturePaintKey;
import org.imagine.awt.key.TexturedPaintWrapperKey;
import org.imagine.awt.key.UnknownPaintKey;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class PaintKeyIOTest {

    @Test
    public void testStringExtremeInts() throws Throwable {
        KeyStringWriter sw = new KeyStringWriter();
        int[] ints = {1, 2, -1, -10, Integer.MIN_VALUE, Integer.MAX_VALUE, 5, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, 0};
        sw.writeIntArray(ints);
        assertFalse(sw.toString().contains("-"), "Sign char should not be present");
        KeyStringReader reader = new KeyStringReader(sw.get());
        // skip magic header
        reader.readByte();
        reader.readByte();
        int[] vals = reader.readIntArray();
        assertNotNull(vals);
        assertArrayEquals(ints, vals);
    }

    @Test
    public void testStringExtremeLongs() throws Throwable {
        KeyStringWriter sw = new KeyStringWriter();
        long[] expected = {1, 2, -1, -10, Long.MIN_VALUE, Long.MAX_VALUE, 5, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, 0};
        sw.writeLongArray(expected);
        assertFalse(sw.toString().contains("-"), "Sign char should not be present");
        KeyStringReader reader = new KeyStringReader(sw.get());
        // skip magic header
        reader.readByte();
        reader.readByte();
        long[] got = reader.readLongArray();
        assertNotNull(got);
        assertArrayEquals(expected, got);
    }

    @Test
    public void testStringExtremeBytes() throws Throwable {
        KeyStringWriter sw = new KeyStringWriter();
        byte[] expected = {1, 2, -1, -10, Byte.MIN_VALUE, Byte.MAX_VALUE, 5, Byte.MIN_VALUE + 1, Byte.MAX_VALUE - 1, 0};
        sw.writeByteArray(expected);
        assertFalse(sw.toString().contains("-"), "Sign char should not be present");
        KeyStringReader reader = new KeyStringReader(sw.get());
        // skip magic header
        reader.readByte();
        reader.readByte();
        byte[] got = reader.readByteArray();
        assertNotNull(got);
        assertArrayEquals(expected, got);
    }

    @Test
    public void testUnsignedLongHexParsing() throws Throwable {
        char[] chs = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'
        };

        for (int i = 0; i < chs.length; i++) {
            char c = chs[i];
            int val = Integer.parseInt(new String(new char[]{c}), 16);
            int nib = toNibble(c);
            assertEquals(val, nib);
        }

        for (String s : new String[]{"12", "AB", "ab", "EF", "FF", "00"}) {
            assertEquals(Integer.parseInt(s, 16), toByte(s.charAt(0), s.charAt(1)),
                    "Wrong value for " + s);
        }

        for (long l : new long[]{10, 23, -1, -500, Long.MAX_VALUE, 232103901390L, Long.MIN_VALUE}) {
            String a = Long.toHexString(l);
            String str = Strings.toPaddedHex(new long[]{l}, "");
            assertEquals(l, parseLongHex(str));
            assertTrue(str.toUpperCase().endsWith(a.toUpperCase()));
        }
    }

    @Test
    public void testReadWriteBytesBinary() throws Throwable {
        KeyBinaryWriter wri = new KeyBinaryWriter();
        for (int i = 1; i < 5; i++) {
            wri.writeByte((byte) (i * 20));
        }
        wri.finish();
        byte[] bt = wri.toByteArray();
        ByteBuffer buf = ByteBuffer.wrap(bt);
        int m1 = buf.get();
        assertEquals(MAGIC_1, m1);
        int m2 = buf.get();
        assertEquals(MAGIC_2, m2);
        int len = buf.getInt();
        for (int i = 1; i < 5; i++) {
            byte val = buf.get();
            assertEquals((byte) (i * 20), val);
        }
        KeyBinaryReader reader = new KeyBinaryReader(new ByteArrayReadChannel(bt));
        byte m1a = reader.readByte();
        byte m2a = reader.readByte();
        int len2 = reader.readInt();

        byte[] gotInts = new byte[5];
        for (int i = 1; i < 5; i++) {
            byte val = reader.readByte();
//            assertEquals(i * 20, val);
            gotInts[i] = val;
        }
        for (int i = 1; i < 5; i++) {
            assertEquals((byte) (i * 20), gotInts[i]);
        }
        assertEquals(MAGIC_1, m1a);
        assertEquals(MAGIC_2, m2a);
        assertEquals(len, len2);

        assertEquals(10, len);
        assertEquals(10, bt.length,
                "Wrong buffer size");
    }

    @Test
    public void testReadWriteIntsBinary() throws Throwable {
        KeyBinaryWriter wri = new KeyBinaryWriter();
        for (int i = 1; i < 5; i++) {
            wri.writeInt(i * 20);
        }
        wri.finish();
        byte[] bt = wri.toByteArray();
        ByteBuffer buf = ByteBuffer.wrap(bt);
        int m1 = buf.get();
        assertEquals(MAGIC_1, m1);
        int m2 = buf.get();
        assertEquals(MAGIC_2, m2);
        int len = buf.getInt();
        for (int i = 1; i < 5; i++) {
            int val = buf.getInt();
            assertEquals(i * 20, val);
        }
        KeyBinaryReader reader = new KeyBinaryReader(new ByteArrayReadChannel(bt));
        byte m1a = reader.readByte();
        byte m2a = reader.readByte();
        int len2 = reader.readInt();

        int[] gotInts = new int[5];
        for (int i = 1; i < 5; i++) {
            int val = reader.readInt();
//            assertEquals(i * 20, val);
            gotInts[i] = val;
        }
        for (int i = 1; i < 5; i++) {
            assertEquals(i * 20, gotInts[i]);
        }
        assertEquals(MAGIC_1, m1a);
        assertEquals(MAGIC_2, m2a);
        assertEquals(len, len2);

        assertEquals(len, 2 + (Integer.BYTES * 5));
        assertEquals(2 + (Integer.BYTES * 5), bt.length,
                "Wrong buffer size");
    }

    @Test
    public void testReadWriteLongsBinary() throws Throwable {
        KeyBinaryWriter wri = new KeyBinaryWriter();
        for (int i = 1; i < 5; i++) {
            wri.writeLong(i * 20);
        }
        wri.finish();
        byte[] bt = wri.toByteArray();
        ByteBuffer buf = ByteBuffer.wrap(bt);
        int m1 = buf.get();
        assertEquals(MAGIC_1, m1);
        int m2 = buf.get();
        assertEquals(MAGIC_2, m2);
        int len = buf.getInt();
        for (int i = 1; i < 5; i++) {
            long val = buf.getLong();
            assertEquals(i * 20, val);
        }
        KeyBinaryReader reader = new KeyBinaryReader(new ByteArrayReadChannel(bt));
        byte m1a = reader.readByte();
        byte m2a = reader.readByte();
        int len2 = reader.readInt();

        assertEquals(MAGIC_1, m1a);
        assertEquals(MAGIC_2, m2a);
        assertEquals(len, len2);

        long[] gotLongs = new long[4];
        for (int i = 1; i < 5; i++) {
            long val = reader.readLong();
//            assertEquals(i * 20, val);
            gotLongs[i - 1] = val;
        }
        assertEquals(4, gotLongs.length);
        for (int i = 0; i < gotLongs.length; i++) {
            assertEquals((i + 1) * 20, gotLongs[i]);
        }

        assertEquals(
                (gotLongs.length * Long.BYTES) // array bytes
                + (Integer.BYTES + 2), // magic number and overall length
                len
        );
        assertEquals(len, bt.length,
                "Wrong buffer size");
    }

    @Test
    public void testReadWriteLongArrayBinary() throws Throwable {
        KeyBinaryWriter wri = new KeyBinaryWriter();
        long[] ints = new long[10];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = i + 23;
        }
        wri.writeLongArray(ints);
        wri.finish();

        ByteBuffer buf = ByteBuffer.wrap(wri.toByteArray());
        int m1 = buf.get();
        assertEquals(MAGIC_1, m1);
        int m2 = buf.get();
        assertEquals(MAGIC_2, m2);
        int len = buf.getInt();
        assertEquals(
                (ints.length * Long.BYTES) // array bytes
                + (Integer.BYTES + 2) // magic number and overall length
                + (Integer.BYTES + 1), len); // array marker and size

        byte b = buf.get();
        assertEquals(LONG_ARRAY_PREFIX, b);
        int sz = buf.getInt();
        assertEquals(sz, ints.length);
        for (int i = 0; i < ints.length; i++) {
            long val = buf.getLong();
            assertEquals(i + 23, val);
        }

        KeyBinaryReader reader = new KeyBinaryReader(new ByteArrayReadChannel(wri.toByteArray()));
        byte m1a = reader.readByte();
        assertEquals(m1, m1a);
        byte m2a = reader.readByte();
        assertEquals(m2, m2a);
        int len2 = reader.readInt();
        assertEquals(len, len2);

        long[] got = reader.readLongArray();
        assertArrayEquals(ints, got);
    }

    @Test
    public void testReadWriteIntArrayBinary() throws Throwable {
        KeyBinaryWriter wri = new KeyBinaryWriter();
        int[] ints = new int[10];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = i + 23;
        }
        wri.writeIntArray(ints);
        wri.finish();

        ByteBuffer buf = ByteBuffer.wrap(wri.toByteArray());
        int m1 = buf.get();
        assertEquals(MAGIC_1, m1);
        int m2 = buf.get();
        assertEquals(MAGIC_2, m2);
        int len = buf.getInt();
        assertEquals(
                (ints.length * Integer.BYTES) // array bytes
                + (Integer.BYTES + 2) // magic number and overall length
                + (Integer.BYTES + 1), len); // array marker and size

        byte b = buf.get();
        assertEquals(INT_ARRAY_PREFIX, b);
        int sz = buf.getInt();
        assertEquals(sz, ints.length);
        for (int i = 0; i < ints.length; i++) {
            int val = buf.getInt();
            assertEquals(i + 23, val);
        }

        KeyBinaryReader reader = new KeyBinaryReader(new ByteArrayReadChannel(wri.toByteArray()));
        byte m1a = reader.readByte();
        assertEquals(m1, m1a);
        byte m2a = reader.readByte();
        assertEquals(m2, m2a);
        int len2 = reader.readInt();
        assertEquals(len, len2);

        int[] got = reader.readIntArray();
        assertArrayEquals(ints, got);
    }

    @Test
    public void testReadWriteByteArrayBinary() throws Throwable {
        KeyBinaryWriter wri = new KeyBinaryWriter();
        byte[] ints = new byte[10];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = (byte) (i + 23);
        }
        wri.writeByteArray(ints);
        wri.finish();

        ByteBuffer buf = ByteBuffer.wrap(wri.toByteArray());
        int m1 = buf.get();
        assertEquals(MAGIC_1, m1);
        int m2 = buf.get();
        assertEquals(MAGIC_2, m2);
        int len = buf.getInt();
        assertEquals(
                (ints.length) // array bytes
                + (Integer.BYTES + 2) // magic number and overall length
                + (Integer.BYTES + 1), len); // array marker and size

        byte b = buf.get();
        assertEquals(BYTE_ARRAY_PREFIX, b);
        int sz = buf.getInt();
        assertEquals(sz, ints.length);
        for (int i = 0; i < ints.length; i++) {
            byte val = buf.get();
            assertEquals((byte) (i + 23), val);
        }

        KeyBinaryReader reader = new KeyBinaryReader(new ByteArrayReadChannel(wri.toByteArray()));
        byte m1a = reader.readByte();
        assertEquals(m1, m1a);
        byte m2a = reader.readByte();
        assertEquals(m2, m2a);
        int len2 = reader.readInt();
        assertEquals(len, len2);

        byte[] got = reader.readByteArray();
        assertArrayEquals(ints, got);
    }

    // String writers
    @Test
    public void testReadWriteBytesString() throws Throwable {
        KeyStringWriter wri = new KeyStringWriter();
        for (int i = 1; i < 5; i++) {
            wri.writeByte((byte) (i * 20));
        }
        String txt = wri.get();

        KeyStringReader reader = new KeyStringReader(0, txt);
        byte m1a = reader.readByte();
        byte m2a = reader.readByte();

        byte[] gotInts = new byte[5];
        for (int i = 1; i < 5; i++) {
            byte val = reader.readByte();
//            assertEquals(i * 20, val);
            gotInts[i] = val;
        }
        for (int i = 1; i < 5; i++) {
            assertEquals((byte) (i * 20), gotInts[i]);
        }
        assertEquals(MAGIC_1, m1a);
        assertEquals(MAGIC_2, m2a);
    }

    @Test
    public void testReadWriteIntsString() throws Throwable {
        KeyStringWriter wri = new KeyStringWriter();
        for (int i = 1; i < 5; i++) {
            wri.writeInt(i * 20);
        }
        KeyStringReader reader = new KeyStringReader(0, wri.get());
        byte m1a = reader.readByte();
        byte m2a = reader.readByte();

        int[] gotInts = new int[5];
        for (int i = 1; i < 5; i++) {
            int val = reader.readInt();
//            assertEquals(i * 20, val);
            gotInts[i] = val;
        }
        for (int i = 1; i < 5; i++) {
            assertEquals(i * 20, gotInts[i]);
        }
        assertEquals(MAGIC_1, m1a);
        assertEquals(MAGIC_2, m2a);
    }

    @Test
    public void testReadWriteLongsString() throws Throwable {
        KeyStringWriter wri = new KeyStringWriter();
        for (int i = 1; i < 5; i++) {
            wri.writeLong(i * 20);
        }

        KeyStringReader reader = new KeyStringReader(0, wri.get());
        byte m1a = reader.readByte();
        byte m2a = reader.readByte();

        assertEquals(MAGIC_1, m1a);
        assertEquals(MAGIC_2, m2a);

        long[] gotLongs = new long[4];
        for (int i = 1; i < 5; i++) {
            long val = reader.readLong();
//            assertEquals(i * 20, val);
            gotLongs[i - 1] = val;
        }
        assertEquals(4, gotLongs.length);
        for (int i = 0; i < gotLongs.length; i++) {
            assertEquals((i + 1) * 20, gotLongs[i]);
        }
    }

    @Test
    public void testReadWriteLongArrayString() throws Throwable {
        KeyBinaryWriter wri = new KeyBinaryWriter();
        long[] ints = new long[10];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = i + 23;
        }
        wri.writeLongArray(ints);
        wri.finish();

        ByteBuffer buf = ByteBuffer.wrap(wri.toByteArray());
        int m1 = buf.get();
        assertEquals(MAGIC_1, m1);
        int m2 = buf.get();
        assertEquals(MAGIC_2, m2);
        int len = buf.getInt();
        assertEquals(
                (ints.length * Long.BYTES) // array bytes
                + (Integer.BYTES + 2) // magic number and overall length
                + (Integer.BYTES + 1), len); // array marker and size

        byte b = buf.get();
        assertEquals(LONG_ARRAY_PREFIX, b);
        int sz = buf.getInt();
        assertEquals(sz, ints.length);
        for (int i = 0; i < ints.length; i++) {
            long val = buf.getLong();
            assertEquals(i + 23, val);
        }

        KeyBinaryReader reader = new KeyBinaryReader(new ByteArrayReadChannel(wri.toByteArray()));
        byte m1a = reader.readByte();
        assertEquals(m1, m1a);
        byte m2a = reader.readByte();
        assertEquals(m2, m2a);
        int len2 = reader.readInt();
        assertEquals(len, len2);

        long[] got = reader.readLongArray();
        assertArrayEquals(ints, got);
    }

    @Test
    public void testReadWriteIntArrayString() throws Throwable {
        KeyStringWriter wri = new KeyStringWriter();
        int[] ints = new int[10];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = i + 23;
        }
        wri.writeIntArray(ints);

        KeyStringReader reader = new KeyStringReader(0, wri.get());
        byte m1a = reader.readByte();
        assertEquals(MAGIC_1, m1a);
        byte m2a = reader.readByte();
        assertEquals(MAGIC_2, m2a);

        int[] got = reader.readIntArray();
        assertArrayEquals(ints, got);
    }

    @Test
    public void testReadWriteByteArrayString() throws Throwable {
        KeyStringWriter wri = new KeyStringWriter();
        byte[] ints = new byte[10];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = (byte) (i + 23);
        }
        wri.writeByteArray(ints);

        KeyStringReader reader = new KeyStringReader(0, wri.get());
        byte m1a = reader.readByte();
        assertEquals(MAGIC_1, m1a);
        byte m2a = reader.readByte();
        assertEquals(MAGIC_2, m2a);

        byte[] got = reader.readByteArray();
        assertArrayEquals(ints, got);
    }

    @Test
    public void testRadialKey() throws Throwable {
        float[] fracs = new float[]{0F, 0.13F, 0.75F, 1F};
        Color[] colors = new Color[]{
            new Color(128, 222, 255), new Color(0, 222, 255), new Color(128, 255, 100), new Color(128, 128, 255)};
        RadialGradientPaint gp = new RadialGradientPaint(
                new Point2D.Double(0, 0),
                67.5F,
                new Point2D.Double(42, 47.3),
                fracs,
                colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(0, 0)
        );

        RadialPaintKey expected = new RadialPaintKey(gp);

        byte[] bytes = PaintKeyIO.writeAsBytes(expected);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        PaintKey<?> got = PaintKeyIO.read(bytes);
        assertNotNull(got);
        assertTrue(got instanceof RadialPaintKey);
        assertEquals(expected, got);

        String s = PaintKeyIO.writeAsString(expected);
        assertNotNull(s);
        assertTrue(s.length() > 0);

        got = PaintKeyIO.read(s);
        assertNotNull(got);
        assertTrue(got instanceof RadialPaintKey);
        assertEquals(expected, got);
    }

    @Test
    public void testLinearKey() throws Throwable {
        float[] fracs = new float[]{0F, 0.13F, 0.75F, 1F};
        Color[] colors = new Color[]{
            new Color(128, 222, 255), new Color(0, 222, 255), new Color(128, 255, 100), new Color(128, 128, 255)};
        LinearGradientPaint gp = new LinearGradientPaint(
                new Point2D.Double(0, 0),
                new Point2D.Double(42, 47.3),
                fracs,
                colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(0, 0)
        );

        LinearPaintKey expected = new LinearPaintKey(gp);

        byte[] bytes = PaintKeyIO.writeAsBytes(expected);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        PaintKey<?> got = PaintKeyIO.read(bytes);
        assertNotNull(got);
        assertTrue(got instanceof LinearPaintKey);
        assertEquals(expected, got);

        String s = PaintKeyIO.writeAsString(expected);
        assertNotNull(s);
        assertTrue(s.length() > 0);

        got = PaintKeyIO.read(s);
        assertNotNull(got);
        assertTrue(got instanceof LinearPaintKey);
        assertEquals(expected, got);
    }

    @Test
    public void testGradientKey() throws Throwable {

        GradientPaint gp = new GradientPaint(23, 4, Color.GREEN, 55, 70, Color.RED, true);

        GradientPaintKey expected = new GradientPaintKey(gp);

        byte[] bytes = PaintKeyIO.writeAsBytes(expected);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        PaintKey<?> got = PaintKeyIO.read(bytes);
        assertNotNull(got);
        assertTrue(got instanceof GradientPaintKey);
        assertEquals(expected, got);

        String s = PaintKeyIO.writeAsString(expected);
        assertNotNull(s);
        assertTrue(s.length() > 0);

        got = PaintKeyIO.read(s);
        assertNotNull(got);
        assertTrue(got instanceof GradientPaintKey);
        assertEquals(expected, got);
    }

    @Test
    public void testColorKey() throws Throwable {

        Color gp = new Color(55, 80, 252, 180);

        ColorKey expected = new ColorKey(gp);

        byte[] bytes = PaintKeyIO.writeAsBytes(expected);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        PaintKey<?> got = PaintKeyIO.read(bytes);
        assertNotNull(got);
        assertTrue(got instanceof ColorKey);
        assertEquals(expected, got);

        String s = PaintKeyIO.writeAsString(expected);
        assertNotNull(s);
        assertTrue(s.length() > 0);

        got = PaintKeyIO.read(s);
        assertNotNull(got);
        assertTrue(got instanceof ColorKey);
        assertEquals(expected, got);
    }

    @Test
    public void testTextureWrapKey() throws Throwable {
        float[] fracs = new float[]{0F, 0.13F, 0.75F, 1F};
        Color[] colors = new Color[]{
            new Color(128, 222, 255), new Color(0, 222, 255), new Color(128, 255, 100), new Color(128, 128, 255)};
        LinearGradientPaint gp = new LinearGradientPaint(
                new Point2D.Double(0, 0),
                new Point2D.Double(42, 47.3),
                fracs,
                colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(0, 0)
        );

        LinearPaintKey delegate = new LinearPaintKey(gp);
        TexturedPaintWrapperKey expected = new TexturedPaintWrapperKey(delegate, 800, 600);

        byte[] bytes = PaintKeyIO.writeAsBytes(expected);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        PaintKey<?> got = PaintKeyIO.read(bytes);
        assertNotNull(got);
        assertTrue(got instanceof TexturedPaintWrapperKey);
        assertEquals(expected, got);

        String s = PaintKeyIO.writeAsString(expected);
        assertNotNull(s);
        assertTrue(s.length() > 0);

        got = PaintKeyIO.read(s);
        assertNotNull(got);
        assertTrue(got instanceof TexturedPaintWrapperKey);
        assertEquals(expected, got);
    }

    @Test
    public void testTexturePaint() throws Throwable {
        BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.BLUE);
            g.fillRect(10, 10, 15, 15);
        } finally {
            g.dispose();
        }
        TexturePaint tp = new TexturePaint(img, new Rectangle(0, 0, img.getWidth(), img.getHeight()));

        TexturePaintKey expected = new TexturePaintKey(tp);
        byte[] bytes = PaintKeyIO.writeAsBytes(expected);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        PaintKey<?> got = PaintKeyIO.read(bytes);
        assertNotNull(got);
        assertTrue(got instanceof TexturePaintKey);
        assertEquals(expected, got);

        String s = PaintKeyIO.writeAsString(expected);
        assertNotNull(s);
        assertTrue(s.length() > 0);

        got = PaintKeyIO.read(s);
        assertNotNull(got);
        assertTrue(got instanceof TexturePaintKey);
        assertEquals(expected, got);
        assertSame(tp, Accessor.rawPaintForPaintKey(expected));

        ManagedTexturePaintKey managed = expected.toManagedKey();
        bytes = PaintKeyIO.writeAsBytes(managed);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        got = PaintKeyIO.read(bytes);
        assertNotNull(got);
        assertTrue(got instanceof ManagedTexturePaintKey);
        assertEquals(managed, got);

        s = PaintKeyIO.writeAsString(managed);
        assertNotNull(s);
        assertTrue(s.length() > 0);

        got = PaintKeyIO.read(s);
        assertNotNull(got);
        assertTrue(got instanceof ManagedTexturePaintKey);
        assertEquals(managed, got);

        Paint paint = Accessor.rawPaintForPaintKey(got);
        assertTrue(paint instanceof TexturePaint);

        System.out.println("OTHER PAINT: " + ((TexturePaint)paint).getImage());

        assertTexturePaintsEqual(tp, paint);
    }

    private void assertTexturePaintsEqual(Paint a, Paint b) {
        BufferedImage ia = paintTestImage(a);
        BufferedImage ib = paintTestImage(b);
        assertRastersMatch(ia, ib);
    }

    private void assertRastersMatch(BufferedImage a, BufferedImage b) {
        int[] aPx = raster(a);
        int[] bPx = raster(b);
        assertArrayEquals(aPx, bPx, "Rasters do not match");
    }

    private int[] raster(BufferedImage img) {
        return img.getRaster().getPixels(0, 0, img.getWidth(), img.getHeight(), (int[]) null);
    }

    private BufferedImage paintTestImage(Paint p) {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setPaint(p);
            g.fillRect(5, 5, 90, 90);
        } finally {
            g.dispose();
        }
        return img;
    }

    @Test
    public void testUnknownKey() throws Throwable {

        FakePaint fp = new FakePaint("hello");
        UnknownPaintKey<FakePaint> expected = new UnknownPaintKey<>(fp);

        byte[] bytes = PaintKeyIO.writeAsBytes(expected);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        PaintKey<?> got = PaintKeyIO.read(bytes);
        assertNotNull(got);
        assertTrue(got instanceof UnknownPaintKey);
        assertEquals(expected, got);

        assertEquals(fp, Accessor.rawPaintForPaintKey(got));

        String s = PaintKeyIO.writeAsString(expected);
        assertNotNull(s);
        assertTrue(s.length() > 0);

        got = PaintKeyIO.read(s);
        assertNotNull(got);
        assertTrue(got instanceof UnknownPaintKey);
        assertEquals(expected, got);

        assertEquals(fp, Accessor.rawPaintForPaintKey(got));

    }

    static class FakePaint implements Serializable, Paint {

        private final String x;

        public FakePaint(String x) {
            this.x = x;
        }

        public int hashCode() {
            return x.hashCode();
        }

        public boolean equals(Object o) {
            return o instanceof FakePaint ? ((FakePaint) o).x.equals(x)
                    : false;
        }

        @Override
        public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getTransparency() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @BeforeEach
    public void setup() {
        Path pth = Paths.get(System.getProperty("java.io.tmpdir"));
        Path dir = pth.resolve("imagine-" + System.currentTimeMillis() + "-" + System.nanoTime());
        System.setProperty("textures.dir", dir.toString());
    }

}
