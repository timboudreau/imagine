/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt;

import org.imagine.awt.key.RadialPaintKey;
import org.imagine.awt.key.GradientPaintKey;
import org.imagine.awt.key.LinearPaintKey;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.imagine.awt.io.KeyWriter;

/**
 *
 * @author Tim Boudreau
 */
public class PaintKeyTest {

    @Test
    public void testLinearKeyNormalization() throws Throwable {
        float[] fracs = new float[]{0F, 0.13F, 0.75F, 1F};
        Color[] colors = new Color[]{
            new Color(128, 222, 255), new Color(0, 222, 255), new Color(128, 255, 100), new Color(128, 128, 255)};
        LinearGradientPaint gp = new LinearGradientPaint(
                new Point2D.Double(0, 0),
                new Point2D.Double(42, 47.3),
                fracs,
                colors);

        LinearPaintKey key1 = new LinearPaintKey(gp);

        LinearGradientPaint gp2 = new LinearGradientPaint(
                new Point2D.Double(-42, -47.3),
                new Point2D.Double(0, 0),
                fracs,
                colors,
                CycleMethod.NO_CYCLE,
                ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));

        LinearPaintKey key2 = new LinearPaintKey(gp2);

        assertEquals(0F, key1.centerX());
        assertEquals(0F, key2.centerX());
        assertEquals(0F, key1.centerY());
        assertEquals(0F, key2.centerY());

        assertEquals(42F, key1.focusX());
        assertEquals(42F, key2.focusX());
        assertEquals(47.3F, key1.focusY());
        assertEquals(47.3F, key2.focusY());

        assertArrayEquals(colors, key1.colors());
        assertArrayEquals(colors, key2.colors());

        assertArrayEquals(fracs, key1.fractions());
        assertArrayEquals(fracs, key2.fractions());
        assertEquals(CycleMethod.NO_CYCLE, key1.cycleMethod());
        assertEquals(CycleMethod.NO_CYCLE, key2.cycleMethod());
        assertEquals(ColorSpaceType.SRGB, key1.colorSpaceType());
        assertEquals(ColorSpaceType.SRGB, key2.colorSpaceType());
        assertEquals(AffineTransform.getTranslateInstance(0, 0),
                key1.transform());
        assertEquals(AffineTransform.getTranslateInstance(0, 0),
                key2.transform());

        assertEquals(key1.id(), key2.id());
        assertEquals(key1.hashCode(), key2.hashCode());
        assertEquals(key1, key2);
        assertTrue(key1.id().startsWith("linear/"));

        LinearGradientPaint gp3 = new LinearGradientPaint(
                new Point2D.Double(-42, -47.3),
                new Point2D.Double(10, 0),
                fracs,
                colors,
                CycleMethod.NO_CYCLE,
                ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));

        assertNotEquals(key1.id(), new LinearPaintKey(gp3).id());
        assertNotEquals(key1.hashCode(), new LinearPaintKey(gp3).hashCode());
        assertNotEquals(key1, new LinearPaintKey(gp3));

        gp3 = new LinearGradientPaint(
                new Point2D.Double(-42, -47.3),
                new Point2D.Double(0, 0),
                fracs,
                colors,
                CycleMethod.REFLECT,
                ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));

        assertNotEquals(key1.id(), new LinearPaintKey(gp3).id());
        assertNotEquals(key1.hashCode(), new LinearPaintKey(gp3).hashCode());
        assertNotEquals(key1, new LinearPaintKey(gp3));

        this.<LinearGradientPaint, LinearPaintKey>testSerialization(key1);
    }

    @Test
    public void testRadialKeyNormalization() throws Throwable {
        float[] fracs = new float[]{0F, 0.13F, 0.75F, 1F};
        Color[] colors = new Color[]{
            new Color(128, 222, 255), new Color(0, 222, 255), new Color(128, 255, 100), new Color(128, 128, 255)};
        RadialGradientPaint gp = new RadialGradientPaint(
                new Point2D.Double(0, 0),
                67.5F,
                new Point2D.Double(42, 47.3),
                fracs,
                colors,
                CycleMethod.NO_CYCLE, ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(0, 0)
        );

        RadialPaintKey key1 = new RadialPaintKey(gp);

        KeyWriter<String> kw = PaintKeyIO.stringWriter(key1);
        key1.writeTo(kw);
        System.out.println("WRITTEN: " + kw);
        System.out.println("BYTES: " + kw.toByteArray().length);

        KeyWriter<?> ww = PaintKeyIO.binaryWriter(key1);
        key1.writeTo(ww);
        byte[] bb = ww.toByteArray();
        System.out.println("BINARY: " + new String(bb));
        System.out.println("BYTES: " + bb.length);

        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oo = new ObjectOutputStream(o)) {
                oo.writeObject(key1);
            }
            System.out.println("SERL: " + new String(o.toByteArray()));
            System.out.println("BYTES: " + o.toByteArray().length);
        }


        RadialGradientPaint gp2 = new RadialGradientPaint(
                new Point2D.Double(-42, -47.3),
                67.5F,
                new Point2D.Double(0, 0),
                fracs,
                colors,
                CycleMethod.NO_CYCLE, ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));

        RadialPaintKey key2 = new RadialPaintKey(gp2);

        assertEquals(0F, key1.centerX());
        assertEquals(0F, key2.centerX());
        assertEquals(0F, key1.centerY());
        assertEquals(0F, key2.centerY());

        assertEquals(42F, key1.focusX());
        assertEquals(42F, key2.focusX());
        assertEquals(47.3F, key1.focusY());
        assertEquals(47.3F, key2.focusY());
        assertEquals(67.5F, key1.radius());
        assertEquals(67.5F, key2.radius());

        assertArrayEquals(colors, key1.colors());
        assertArrayEquals(colors, key2.colors());

        assertArrayEquals(fracs, key1.fractions());
        assertArrayEquals(fracs, key2.fractions());
        assertEquals(CycleMethod.NO_CYCLE, key1.cycleMethod());
        assertEquals(CycleMethod.NO_CYCLE, key2.cycleMethod());
        assertEquals(ColorSpaceType.SRGB, key1.colorSpaceType());
        assertEquals(ColorSpaceType.SRGB, key2.colorSpaceType());
        assertEquals(AffineTransform.getTranslateInstance(0, 0),
                key1.transform());
        assertEquals(AffineTransform.getTranslateInstance(0, 0),
                key2.transform());

        assertEquals(key1.id(), key2.id());
        assertEquals(key1.hashCode(), key2.hashCode());
        assertEquals(key1, key2);
        assertTrue(key1.id().startsWith("radial/"));

        RadialGradientPaint gp3 = new RadialGradientPaint(
                new Point2D.Double(-42, -47.3),
                67.5F,
                new Point2D.Double(10, 0),
                fracs,
                colors,
                CycleMethod.NO_CYCLE,
                ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));

        RadialPaintKey key3 = new RadialPaintKey(gp3);
        assertEquals(52F, key3.focusX());
        assertNotEquals(key1, key3, "\n" + key1 + "\n" + key3);
        assertNotEquals(key1.id(), key3.id(), "\n" + key1 + "\n" + key3);
        assertNotEquals(key1.hashCode(), key3.hashCode(), "\n" + key1 + "\n" + key3);
        assertNotEquals(key1.hashCode(), key3.hashCode());

        gp3 = new RadialGradientPaint(
                new Point2D.Double(-42, -47.3),
                67.5F,
                new Point2D.Double(0, 0),
                fracs,
                colors,
                CycleMethod.REFLECT, ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));
        System.out.println("k2hc " + key3.hashCode());
        key3 = new RadialPaintKey(gp3);
        assertNotEquals(key1.id(), key3.id());
        assertNotEquals(key1.hashCode(), key3.hashCode());
        assertNotEquals(key1, new RadialPaintKey(gp3));

        System.out.println("k1hc " + key1.hashCode());
        System.out.println("k2hc " + key3.hashCode());
        gp3 = new RadialGradientPaint(
                new Point2D.Double(-42, -47.3),
                67.5F,
                new Point2D.Double(1, 0),
                fracs,
                colors,
                CycleMethod.NO_CYCLE, ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));
        key3 = new RadialPaintKey(gp3);
        assertNotEquals(key1.id(), key3.id());
        assertNotEquals(key1.hashCode(), key3.hashCode());
        assertNotEquals(key1, new RadialPaintKey(gp3));
        System.out.println("k3hc " + key3.hashCode());
        gp3 = new RadialGradientPaint(
                new Point2D.Double(-42, -47.3),
                67.5F,
                new Point2D.Double(0, 1),
                fracs,
                colors,
                CycleMethod.NO_CYCLE, ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));
        key3 = new RadialPaintKey(gp3);
        assertNotEquals(key1.id(), key3.id());
        assertNotEquals(key1.hashCode(), key3.hashCode());
        assertNotEquals(key1, new RadialPaintKey(gp3));
        gp3 = new RadialGradientPaint(
                new Point2D.Double(-42, -47.3),
                67.5F,
                new Point2D.Double(1, 1),
                fracs,
                colors,
                CycleMethod.NO_CYCLE, ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));
        key3 = new RadialPaintKey(gp3);
        assertNotEquals(key1.id(), key3.id());
        assertNotEquals(key1.hashCode(), key3.hashCode());
        assertNotEquals(key1, new RadialPaintKey(gp3));

        gp3 = new RadialGradientPaint(
                new Point2D.Double(-42, -47.3),
                67.5F,
                new Point2D.Double(0, 0.00001),
                fracs,
                colors,
                CycleMethod.NO_CYCLE, ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));
        key3 = new RadialPaintKey(gp3);
        assertNotEquals(key1.id(), key3.id());
        assertNotEquals(key1.hashCode(), key3.hashCode());
        assertNotEquals(key1, new RadialPaintKey(gp3));

        gp3 = new RadialGradientPaint(
                new Point2D.Double(-42, -47.3),
                67.5F,
                new Point2D.Double(0, 0.000001), // below the resolution
                fracs,
                colors,
                CycleMethod.NO_CYCLE, ColorSpaceType.SRGB,
                AffineTransform.getTranslateInstance(42, 47.3));
        key3 = new RadialPaintKey(gp3);
        assertEquals(key1.id(), key3.id());
        assertEquals(key1.hashCode(), key3.hashCode());
        assertEquals(key1, new RadialPaintKey(gp3));
        testSerialization(key1);
    }

    @Test
    public void testGradientPaintKeyNormalization() throws Throwable {
        GradientPaint gp1 = new GradientPaint(
                new Point2D.Float(42, 43.75F),
                Color.BLUE,
                new Point2D.Double(7, 3.2),
                Color.ORANGE,
                true);

        GradientPaint gp2 = new GradientPaint(
                new Point2D.Double(7, 3.2),
                Color.ORANGE,
                new Point2D.Float(42, 43.75F),
                Color.BLUE,
                true);

        GradientPaintKey key1 = new GradientPaintKey(gp1);
        GradientPaintKey key2 = new GradientPaintKey(gp2);
        assertEquals(7F, key1.x1());
        assertEquals(3.2F, key1.y1());
        assertEquals(42F, key1.x2());
        assertEquals(43.75F, key1.y2());
        assertEquals(Color.ORANGE, key1.color1());
        assertEquals(Color.BLUE, key1.color2());
        assertTrue(key1.isCyclic());

        assertEquals(7F, key2.x1());
        assertEquals(3.2F, key2.y1());
        assertEquals(42F, key2.x2());
        assertEquals(43.75F, key2.y2());
        assertEquals(Color.ORANGE, key2.color1());
        assertEquals(Color.BLUE, key2.color2());
        assertTrue(key2.isCyclic());

        assertEquals(key1.hashCode(), key2.hashCode());
        assertEquals(key1.id(), key2.id());
        assertEquals(key1, key2);

        GradientPaint gp3 = new GradientPaint(
                new Point2D.Double(7, 3.2),
                Color.ORANGE,
                new Point2D.Float(42, 43.75F),
                Color.BLUE,
                false);

        assertNotEquals(key1.id(), new GradientPaintKey(gp3).id());
        assertNotEquals(key1, new GradientPaintKey(gp3));
        gp3 = new GradientPaint(
                new Point2D.Double(6.5, 3.2),
                Color.ORANGE,
                new Point2D.Float(42, 43.75F),
                Color.BLUE,
                true);
        assertNotEquals(key1.id(), new GradientPaintKey(gp3).id());
        assertNotEquals(key1, new GradientPaintKey(gp3));

        gp3 = new GradientPaint(
                new Point2D.Double(7, 3.2),
                Color.BLACK,
                new Point2D.Float(42, 43.75F),
                Color.GREEN,
                true);

        assertNotEquals(key1.id(), new GradientPaintKey(gp3).id());
        assertNotEquals(key1, new GradientPaintKey(gp3));

        gp3 = new GradientPaint(
                new Point2D.Double(7, 3.2),
                Color.BLUE,
                new Point2D.Float(42, 43.75F),
                Color.ORANGE,
                true);

        assertNotEquals(key1.id(), new GradientPaintKey(gp3).id());
        assertNotEquals(key1, new GradientPaintKey(gp3));

        testSerialization(key1);
    }

    <P extends Paint, K extends PaintKey<P>> void testSerialization(K key) throws IOException, ClassNotFoundException {
        K nue = serializeAndDeserialize(key);
        assertNotNull(nue);
        assertSame(key.getClass(), nue.getClass());
        assertEquals(key, nue);
    }

    private <P extends Paint, K extends PaintKey<P>> K serializeAndDeserialize(K key) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oout = new ObjectOutputStream(out)) {
            oout.writeObject(key);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (ObjectInputStream oin = new ObjectInputStream(in)) {
            return (K) oin.readObject();
        }
    }

}
