/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.spi;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.imagine.awt.impl.Accessor;
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
import org.openide.util.Lookup;
import org.imagine.io.KeyReader;

/**
 * Allows for pluggable paint key factories, which allows the set of paints
 * supported to be pluggable - in particular, facilitating using batik-awt's
 * alternate paint implementations, or to implement caching and hashing of
 * texture paints without requiring this library to do it.
 *
 * @author Tim Boudreau
 */
public abstract class PaintKeyFactory {

    static PaintKeyFactory INSTANCE;
    private static List<PaintKeyFactory> all;

    private static List<PaintKeyFactory> factories() {
        if (all == null) {
            all = Collections.unmodifiableList(new ArrayList<>(
                    Lookup.getDefault().lookupAll(PaintKeyFactory.class)));
        }
        return all;
    }

    static <K extends PaintKey<P>, P extends Paint>
            Function<KeyReader, K> readerFor(Class<? extends K> type) {
        Set<PaintKeyFactory> compatibleFactories = new LinkedHashSet<>();
        for (PaintKeyFactory f : factories()) {
            f.supportedTypes((name, keyType) -> {
                if (type == keyType) {
                    compatibleFactories.add(f);
                }
            });
        }
        if (compatibleFactories.isEmpty()) {
            return null;
        }
        for (PaintKeyFactory p : compatibleFactories) {
            Function<KeyReader, ? extends K> result = p.createReader(type);
            if (result != null) {
                return (Function<KeyReader, K>) result;
            }
        }
        return null;
    }

    static <T extends Paint> PaintKey<T> keyFor(T paint) {
        List<PaintKeyFactory> allLocal = factories();
        if (!allLocal.isEmpty()) {
            for (PaintKeyFactory f : allLocal) {
                if (f.recognizes(paint)) {
                    PaintKey<T> result = f.create(paint);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return createDefaultKey(paint);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Paint> PaintKey<T> createDefaultKey(T paint) {
        if (paint instanceof Color) {
            return (PaintKey<T>) new ColorKey((Color) paint);
        } else if (paint instanceof GradientPaint) {
            return (PaintKey<T>) new GradientPaintKey((GradientPaint) paint);
        } else if (paint instanceof LinearGradientPaint) {
            return (PaintKey<T>) new LinearPaintKey((LinearGradientPaint) paint);
        } else if (paint instanceof RadialGradientPaint) {
            return (PaintKey<T>) new RadialPaintKey((RadialGradientPaint) paint);
        } else if (paint instanceof Color) {
            return (PaintKey<T>) new ColorKey((Color) paint);
        } else if (paint instanceof TexturePaint) {
            return (PaintKey<T>) TexturePaintManager.getDefault().keyFor((TexturePaint) paint);
        } else {
            // XXX texture paint
            return new UnknownPaintKey<>(paint);
        }
    }

    static <T extends Paint> boolean isSupportedType(T paint) {
        if (paint instanceof Color
                || paint instanceof GradientPaint
                || paint instanceof LinearGradientPaint
                || paint instanceof RadialGradientPaint) {
            return true;
        }
        for (PaintKeyFactory f : factories()) {
            if (f.recognizes(paint)) {
                return true;
            }
        }
        return false;
    }

    static Map<String, Class<?>> allTypes() {
        Map<String, Class<?>> all = new LinkedHashMap<>();
        all.put(ColorKey.ID_BASE, ColorKey.class);
        all.put(GradientPaintKey.ID_BASE, GradientPaintKey.class);
        all.put(LinearPaintKey.ID_BASE, LinearPaintKey.class);
        all.put(RadialPaintKey.ID_BASE, RadialPaintKey.class);
        all.put(TexturedPaintWrapperKey.ID_BASE, TexturedPaintWrapperKey.class);
        all.put(TexturePaintKey.ID_BASE, TexturePaintKey.class);
        all.put(ManagedTexturePaintKey.ID_BASE, ManagedTexturePaintKey.class);
        factories().forEach(f -> f.supportedTypes(all::put));
        return all;
    }

    protected abstract <T extends Paint> boolean recognizes(T paint);

    protected abstract <T extends Paint> PaintKey<T> create(T paint);

    protected abstract void supportedTypes(BiConsumer<String, Class<? extends PaintKey<?>>> c);

    public <K extends PaintKey<P>, P extends Paint>
            Function<KeyReader, K> createReader(Class<? extends K> type) {
        return null;
    }

    static {
        Accessor.DEFAULT = new AccessorImpl();
    }

    static class AccessorImpl extends Accessor {

        @Override
        protected Map<String, Class<?>> supportedTypes() {
            return allTypes();
        }

        @Override
        protected boolean isSupported(Paint p) {
            return isSupportedType(p);
        }

        @Override
        protected <T extends Paint> PaintKey<T> paintKeyForPaint(T paint) {
            return PaintKeyFactory.<T>keyFor(paint);
        }

        @Override
        protected <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint> BufferedImage imageFor(TexturedPaintWrapperKey<P, T> key) {
            return TexturePaintWrapperImageLoader.getDefault().imageFor(key);
        }

        @Override
        protected <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint> TexturePaint paintFor(TexturedPaintWrapperKey<P, T> key) {
            return TexturePaintWrapperImageLoader.getDefault().paintFor(key);
        }

        @Override
        protected <K extends PaintKey<P>, P extends Paint> BufferedImage thumbnailFor(K key, int w, int h) {
            return PaintThumbnailFactory.getDefault().thumbnailFor(key, w, h);
        }

        protected <K extends PaintKey<P>, P extends Paint>
                Function<KeyReader, K> readerFor(Class<? extends K> type) {
            return PaintKeyFactory.readerFor(type);
        }

        @Override
        protected boolean isManaged(TexturePaint paint) {
            return TexturePaintManager.getDefault().isManaged(paint);
        }

        @Override
        protected BufferedImage imageForId(String texturePaintId, ManagedTexturePaintKey key) {
            if (!texturePaintId.startsWith(ManagedTexturePaintKey.PREFIX)) {
                texturePaintId = ManagedTexturePaintKey.PREFIX + texturePaintId;
            }
            return TexturePaintManager.getDefault().imageFor(texturePaintId, key);
        }

        @Override
        protected TexturePaint texturePaintFor(ManagedTexturePaintKey key) {
            return TexturePaintManager.getDefault().paintFor(key);
        }

        @Override
        protected ManagedTexturePaintKey toManagedKey(TexturePaintKey key) {
            return TexturePaintManager.getDefault().toManagedKey(key);
        }
    }
}
