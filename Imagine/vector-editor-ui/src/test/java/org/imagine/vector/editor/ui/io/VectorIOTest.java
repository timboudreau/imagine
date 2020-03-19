/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.io;

import com.mastfrog.function.throwing.io.IOConsumer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.elements.CircleWrapper;
import net.java.dev.imagine.api.vector.elements.Line;
import net.java.dev.imagine.api.vector.elements.Oval;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.PathText;
import net.java.dev.imagine.api.vector.elements.Polygon;
import net.java.dev.imagine.api.vector.elements.Polyline;
import net.java.dev.imagine.api.vector.elements.Rectangle;
import net.java.dev.imagine.api.vector.elements.RoundRect;
import net.java.dev.imagine.api.vector.elements.StringWrapper;
import net.java.dev.imagine.api.vector.elements.Text;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import org.imagine.awt.key.PaintKey;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.io.KeyBinaryReader;
import org.imagine.io.KeyBinaryWriter;
import org.imagine.io.KeyStringReader;
import org.imagine.io.KeyStringWriter;
import static org.imagine.io.KeyStringWriter.MAGIC_1;
import static org.imagine.io.KeyStringWriter.MAGIC_2;
import org.imagine.vector.editor.ui.Shapes;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class VectorIOTest {

    private Primitive[] primitives;

    @Test
    public void testStrokeSerialization() throws Throwable {
        BasicStroke simple = new BasicStroke(1);
        System.out.println("DASH ARRAY SIMPLE: " + Arrays.toString(simple.getDashArray()));
        testOneStroke(simple);
    }

    private void testOneStroke(BasicStroke stroke) throws Throwable {
        VectorIO io = new VectorIO().setHashInconsistencyBehavior(HashInconsistencyBehavior.THROW);
        BasicStrokeWrapper wrapper = new BasicStrokeWrapper(stroke);
        BasicStroke reconstituted = wrapper.toStroke();
        BasicStrokeWrapper reconstitutedWrapper = new BasicStrokeWrapper(reconstituted);
        assertEquals(wrapper, reconstitutedWrapper);
        assertEquals(stroke, reconstituted);
        withWriteChannel(ch -> {
            KeyBinaryWriter kbw = new KeyBinaryWriter();
            io.writeShape(wrapper, kbw);
            io.writeShape(wrapper, kbw);
            kbw.finishRecord();
            for (ByteBuffer buf : kbw.get()) {
                buf.flip();
                ch.write(buf);
            }
            ch.force(true);
        });
        withReadChannel(ch -> {
            KeyBinaryReader r = new KeyBinaryReader(ch);
            int size = r.readMagicAndSize();
            assertTrue(size > 0);
            BasicStrokeWrapper got = (BasicStrokeWrapper) io.readShape(r, HashInconsistencyBehavior.WARN);
            compareStrokes(wrapper, got);
            BasicStrokeWrapper read = (BasicStrokeWrapper) io.readShape(r);
            assertNotNull(read);
            assertEquals(wrapper.hashCode(), read.hashCode());
            assertEquals(wrapper, read);
            assertEquals(wrapper.toStroke(), read.toStroke());
        });
    }

    private void compareStrokes(BasicStrokeWrapper exp, BasicStrokeWrapper got) {
        StringBuilder sb = new StringBuilder();
        if (exp.endCap != got.endCap) {
            sb.append("End caps differ ").append(exp.endCap).append(" and ").append(got.endCap);
        }
        if (exp.lineJoin != got.lineJoin) {
            sb.append("\nLine join differs ").append(exp.lineJoin).append(" and ").append(got.lineJoin);
        }
        if (exp.lineWidth != got.lineWidth) {
            sb.append("\nLine width differs ").append(exp.lineWidth).append(" and ").append(got.lineWidth);
        }
        if (exp.dashPhase != got.dashPhase) {
            sb.append("\nDash phase differs ").append(exp.dashPhase).append(" and ").append(got.dashPhase);
        }
        if (exp.miterLimit != got.miterLimit) {
            sb.append("\nMiter limit differs ").append(exp.miterLimit).append(" and ").append(got.miterLimit);
        }
        if (!Arrays.equals(exp.dashArray, got.dashArray)) {
            sb.append("\nDash array differs: ").append(Arrays.toString(exp.dashArray)).append(" and ").append(Arrays.toString(got.dashArray));
        }
        if (exp.hashCode() != got.hashCode()) {
            sb.append("\nhash codes differ: ").append(exp.hashCode()).append(" and ").append(got.hashCode());
        }
        if (!exp.toStroke().equals(got.toStroke())) {
            sb.append("\nGenerated strokes differ: ").append(exp.toStroke()).append(" and ").append(got.toStroke());
        }
        if (sb.length() > 0) {
            throw new AssertionError(sb);
        }
    }

    @Test
    public void testBinary() throws IOException {
        VectorIO io = new VectorIO().setHashInconsistencyBehavior(HashInconsistencyBehavior.THROW);
        withWriteChannel(ch -> {
            KeyBinaryWriter kbw = new KeyBinaryWriter();
            for (Primitive p : primitives) {
                io.writeShape(p, kbw);
            }
            kbw.finishRecord();
            for (ByteBuffer buf : kbw.get()) {
                buf.flip();
                ch.write(buf);
            }
            ch.force(true);
        });
        List<Primitive> all = new ArrayList<>();
        withReadChannel(ch -> {
            KeyBinaryReader r = new KeyBinaryReader(ch);
            int size = r.readMagicAndSize();
            assertNotEquals(0, size, "Size recorded in record is zero");
//            assertEquals((int) ch.size(), size);
            while (ch.position() < ch.size()) {
                Primitive p = io.readShape(r);
                all.add(p);
            }
        });
        assertEquals(primitives.length, all.size());
    }

    @Test
    public void testString() throws IOException {
        VectorIO io = new VectorIO().setHashInconsistencyBehavior(HashInconsistencyBehavior.WARN);
        StringBuilder sb = new StringBuilder();
        KeyStringWriter w = new KeyStringWriter(sb);
        int textIndex = 0;
        Text txt = null;
        for (int i = 0; i < primitives.length; i++) {
            Primitive p = primitives[i];
            if (p instanceof Text) {
                textIndex = i;
                txt = (Text) p;
            }
            io.writeShape(p, w);
        }
        w.finishRecord();

        List<Primitive> all = new ArrayList<>();
        KeyStringReader r = new KeyStringReader(sb);
        byte m1 = r.readByte();
        byte m2 = r.readByte();
        assertEquals(MAGIC_1, m1);
        assertEquals(MAGIC_2, m2);
        int item = 0;
        while (r.cursor() < sb.length()) {
            Primitive p = io.readShape(r);
            if (item == textIndex) {
                System.out.println("ORIG TEXT: " + txt);
                System.out.println("GOT  TEXT: " + p);
            }
            all.add(p);
            assertEquals(primitives[item], p);
            assertEquals(primitives[item].hashCode(), p.hashCode(),
                    " hash codes do not match: " + primitives[item] + " and " + p);
            item++;
        }
        assertEquals(primitives.length, all.size());
    }

    @Test
    public void testShapesBinary() throws IOException {
        withWriteChannel(ch -> {
            KeyBinaryWriter kbw = new KeyBinaryWriter();
            shapes.writeTo(kbw);
            kbw.finishRecord();
            for (ByteBuffer buf : kbw.get()) {
                buf.flip();
                ch.write(buf);
            }
            ch.force(true);
            System.out.println("SHAPES BINARY SIZE " + ch.size());
        });
        List<Primitive> all = new ArrayList<>();
        withReadChannel(ch -> {
            KeyBinaryReader r = new KeyBinaryReader(ch);
            int size = r.readMagicAndSize();
            assertNotEquals(0, size, "Size recorded in record is zero");
            Shapes nue = Shapes.load(r);
            assertEquals(primitives.length, shapes.size());
            Set<ShapeElement> l = new HashSet<>(shapes.size());
            Set<ShapeElement> got = new HashSet<>(l.size());
            for (ShapeElement e : nue) {
                got.add(e);
                all.add(e.item());
            }
            for (ShapeElement e : shapes) {
                l.add(e);
            }

            assertEquals(l, got);
            assertEquals(Arrays.asList(primitives), all);
        });
    }

    @Test
    public void testShapesString() throws IOException {
        StringBuilder sb = new StringBuilder();
        KeyStringWriter kbw = new KeyStringWriter(sb);
        shapes.writeTo(kbw);
        kbw.finishRecord();

        System.out.println("SHAPES STRING SIZE " + sb.length());

        KeyStringReader r = new KeyStringReader(sb);
        r.readMagic();
        Shapes nue = Shapes.load(r);
        assertEquals(primitives.length, shapes.size());
        List<Primitive> all = new ArrayList<>();
//        assertEquals(shapes, nue);
        Set<ShapeElement> l = new HashSet<>(shapes.size());
        Set<ShapeElement> got = new HashSet<>(l.size());
        for (ShapeElement e : nue) {
            got.add(e);
            all.add(e.item());
        }
        for (ShapeElement e : shapes) {
            l.add(e);
        }

        System.out.println("SHAPES: " + sb);
    }

    private void withWriteChannel(IOConsumer<FileChannel> c) throws IOException {
        try (FileChannel str = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            c.accept(str);
        }
    }

    private void withReadChannel(IOConsumer<FileChannel> c) throws IOException {
        try (FileChannel str = FileChannel.open(file, StandardOpenOption.READ)) {
            c.accept(str);
        }
    }

    @BeforeEach
    public void setup() {
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_EVEN_ODD);
        path.moveTo(100, 107.3);
        path.lineTo(200, 23.5);
        path.curveTo(28, 54.20101, 32.5, 482.01013, 17.20202, 19);
        path.lineTo(50, 421.222);
        path.quadTo(1010, 36.7, 85.12, 101);
        path.closePath();

        primitives = new Primitive[]{
            new CircleWrapper(10, 10, 5),
            new Rectangle(100, 100, 20.5, 20.325, false),
            new RoundRect(200, 210, 7.5, 8.32, 31.5, 37.8, false),
            new Oval(300, 301, 15.183, 82.27, false),
            new Line(2.7, 3.21, 77.32, 18.5),
            new Polygon(new int[]{5, 8, 7}, new int[]{13, 12, 8}, 3, false),
            new Polyline(new int[]{5, 8, 7}, new int[]{13, 12, 8}, 3, true),
            new TriangleWrapper(57, 61, 35, 33.2, 105.3, -1000.31109),
            new Text("Hello world", new Font("Times New Roman", Font.PLAIN, 17)
            .deriveFont(AffineTransform.getRotateInstance(Math.toRadians(30))),
            17.3, 17.5),
            new PathIteratorWrapper(path),
            new PathText(new PathIteratorWrapper(path), new StringWrapper("Foober", 7, 3.3), FontWrapper.create("Times New Roman", 13.5F, Font.BOLD),
            AffineTransform.getRotateInstance(Math.toRadians(33.5)))
        };
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        file = tmp.resolve("VectorIOTest-" + Long.toString(System.currentTimeMillis(), 36)
                + "_" + Long.toString(ThreadLocalRandom.current().nextLong(), 36));
        shapes = new Shapes();
        rnd = new Random(92309320L);
        for (Primitive p : primitives) {
            Shaped shaped = (Shaped) p;
            Color bg, fg;
            PaintingStyle s;
            bg = fg = null;
            switch (rnd.nextInt(3)) {
                case 0:
                    bg = randomColor();
                    s = PaintingStyle.FILL;
                    break;
                case 1:
                    fg = randomColor();
                    s = PaintingStyle.OUTLINE;
                    break;
                case 2:
                    fg = randomColor();
                    bg = randomColor();
                    s = PaintingStyle.OUTLINE_AND_FILL;
                    break;
                default:
                    throw new AssertionError();
            }
            PaintKey<?> bgk = bg == null ? null : PaintKey.forPaint(bg);
            PaintKey<?> fgk = fg == null ? null : PaintKey.forPaint(fg);
            BasicStroke strk;
            if (rnd.nextBoolean()) {
                strk = new BasicStroke(1);
            } else {
                strk = randomStroke();
                BasicStrokeWrapper bsw = new BasicStrokeWrapper(strk);
                bsw.toStroke();
            }
            String name = rnd.nextBoolean() ? randomString() : null;
            shapes.add(shaped, bgk, fgk, s, strk, name);
        }
    }

    private static final String ALPHA
            = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private String randomString() {
        StringBuilder sb = new StringBuilder();
        int ct = 5 + rnd.nextInt(10);
        for (int i = 0; i < ct; i++) {
            sb.append(ALPHA.charAt(rnd.nextInt(ALPHA.length())));
        }
        return sb.toString();
    }

    private BasicStroke randomStroke() {
        float[] pat = new float[rnd.nextInt(5) + 2];
        for (int i = 0; i < pat.length; i++) {
            pat[i] = 1 + (rnd.nextFloat() * 10);
        }
        int cap;
        switch (rnd.nextInt(3)) {
            case 0:
                cap = BasicStroke.CAP_BUTT;
                break;
            case 1:
                cap = BasicStroke.CAP_ROUND;
                break;
            case 2:
                cap = BasicStroke.CAP_SQUARE;
                break;
            default:
                throw new AssertionError();
        }
        int join;
        switch (rnd.nextInt(3)) {
            case 0:
                join = BasicStroke.JOIN_BEVEL;
                break;
            case 1:
                join = BasicStroke.JOIN_MITER;
                break;
            case 2:
                join = BasicStroke.JOIN_ROUND;
                break;
            default:
                throw new AssertionError();
        }
        /*
    public BasicStroke(float width, int cap, int join, float miterlimit,
                       float dash[], float dash_phase) {
         */
        return new BasicStroke(rnd.nextFloat() * 5, cap, join,
                (rnd.nextFloat() * 3) + 1F, pat, rnd.nextFloat() * 2);
    }

    private Color randomColor() {
        return new Color(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255));
    }

    private Random rnd;
    private Path file;
    private Shapes shapes;

    @AfterEach
    public void tearDown() throws IOException {
        if (file != null && Files.exists(file)) {
            Files.delete(file);
        }
    }
}
