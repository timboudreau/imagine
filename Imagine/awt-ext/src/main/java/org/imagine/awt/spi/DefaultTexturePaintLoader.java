/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.spi;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.MultipleGradientPaint;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.imagine.awt.key.MultiplePaintKey;
import org.imagine.awt.key.TexturedPaintWrapperKey;

/**
 *
 * @author Tim Boudreau
 */
final class DefaultTexturePaintLoader extends TexturePaintWrapperImageLoader {

    private final Map<TexturedPaintWrapperKey<?, ?>, BufferedImage> imageForKey
            = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint> BufferedImage imageFor(TexturedPaintWrapperKey<P, T> key) {
        BufferedImage result = imageForKey.get(key);
        if (result == null) {
            P underlying = key.delegate();
            result = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration().createCompatibleImage(key.width(), key.height());
            Graphics2D g = result.createGraphics();
            try {
                g.setPaint(underlying.toPaint());
                g.fillRect(0, 0, key.width(), key.height());
            } finally {
                g.dispose();
            }
            imageForKey.put(key, result);
        }
        return result;
    }
}
