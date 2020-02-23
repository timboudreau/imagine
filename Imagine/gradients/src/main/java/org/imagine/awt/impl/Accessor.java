/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.impl;

import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.imagine.awt.key.ManagedTexturePaintKey;
import org.imagine.awt.key.MultiplePaintKey;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.TexturePaintKey;
import org.imagine.awt.key.TexturedPaintWrapperKey;
import org.imagine.awt.spi.PaintKeyFactory;
import org.imagine.awt.io.KeyReader;

/**
 *
 * @author Tim Boudreau
 */
public abstract class Accessor {

    public static Accessor DEFAULT;
    public static PaintKeyAccessor DEFAULT_PAINT_KEY;

    static PaintKeyAccessor getDefaultPaintKey() {
        if (DEFAULT_PAINT_KEY != null) {
            return DEFAULT_PAINT_KEY;
        }
        Class<?> type = PaintKey.class;
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Accessor.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
        assert DEFAULT_PAINT_KEY != null : "The DEFAULT_PAINT_KEY field must be initialized";
        return DEFAULT_PAINT_KEY;
    }

    static Accessor getDefault() {
        if (DEFAULT != null) {
            return DEFAULT;
        }
        Class<?> type = PaintKeyFactory.class;
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Accessor.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
        assert DEFAULT != null : "The DEFAULT field must be initialized";
        return DEFAULT;
    }

    public static <T extends Paint> PaintKey<T> paintKeyFor(T paint) {
        return getDefault().paintKeyForPaint(paint);
    }

    public static <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint>
            BufferedImage imageForTextureKey(TexturedPaintWrapperKey<P, T> key) {
        return getDefault().imageFor(key);
    }

    public static <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint>
            TexturePaint paintForTextureKey(TexturedPaintWrapperKey<P, T> key) {
        return getDefault().paintFor(key);
    }

    public static Map<String, Class<?>> allSupportedTypes() {
        return getDefault().supportedTypes();
    }

    public static boolean isSupportedPaintType(Paint p) {
        return getDefault().isSupported(p);
    }

    public static BufferedImage thumbnailForPaintKey(PaintKey<?> key, int w, int h) {
        return getDefault().thumbnailFor(key, w, h);
    }

    public static <K extends PaintKey<P>, P extends Paint> P rawPaintForPaintKey(K key) {
        return getDefaultPaintKey().toRawPaint(key);
    }

    public static <K extends PaintKey<P>, P extends Paint>
            Function<KeyReader, K> readerForKeyType(Class<? extends K> type) {
        return getDefault().readerFor(type);
    }

    public static TexturePaint texturePaintForManaged(ManagedTexturePaintKey key) {
        return getDefault().texturePaintFor(key);
    }

    public static BufferedImage imageForTextureKeyId(String id, ManagedTexturePaintKey key) {
        return getDefault().imageForId(id, key);
    }

    public static ManagedTexturePaintKey managedKeyFor(TexturePaintKey key) {
        return getDefault().toManagedKey(key);
    }

    protected abstract boolean isManaged(TexturePaint paint);

    protected abstract <K extends PaintKey<P>, P extends Paint>
            Function<KeyReader, K> readerFor(Class<? extends K> type);

    protected abstract <K extends PaintKey<P>, P extends Paint> BufferedImage thumbnailFor(K key, int w, int h);

    protected abstract <T extends Paint> PaintKey<T> paintKeyForPaint(T paint);

    protected abstract <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint>
            BufferedImage imageFor(TexturedPaintWrapperKey<P, T> key);

    protected abstract <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint>
            TexturePaint paintFor(TexturedPaintWrapperKey<P, T> key);

    protected abstract Map<String, Class<?>> supportedTypes();

    protected abstract boolean isSupported(Paint p);

    protected abstract BufferedImage imageForId(String texturePaintId, ManagedTexturePaintKey key);

    protected abstract TexturePaint texturePaintFor(ManagedTexturePaintKey key);

    protected abstract ManagedTexturePaintKey toManagedKey(TexturePaintKey key);

    public static abstract class PaintKeyAccessor {

        protected abstract <K extends PaintKey<P>, P extends Paint> P toRawPaint(K key);
    }

}
