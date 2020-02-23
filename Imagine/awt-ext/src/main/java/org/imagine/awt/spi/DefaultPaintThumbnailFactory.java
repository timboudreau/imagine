/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.spi;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.imagine.awt.GradientManager;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.SizedPaintKey;

/**
 *
 * @author Tim Boudreau
 */
final class DefaultPaintThumbnailFactory extends PaintThumbnailFactory {

    private final Map<PaintKey<?>, BufferedImage> cache = Collections.synchronizedMap(new WeakHashMap<>());
    private static final DefaultPaintThumbnailFactory INSTANCE = new DefaultPaintThumbnailFactory();

    static DefaultPaintThumbnailFactory getInstance() {
        return INSTANCE;
    }

    public BufferedImage thumbnailFor(PaintKey<?> key, int w, int h) {
        BufferedImage img = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(w, h);
        Graphics2D g = img.createGraphics();
        try {
            g.setPaint(GradientManager.getDefault().findPaint(key));
            if (key instanceof SizedPaintKey<?>) {
                g.transform(((SizedPaintKey<?>) key).createScalingTransform(w, h));
            }
            g.fillRect(0, 0, w, h);
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, w - 1, h - 1);
        } finally {
            g.dispose();
        }
        cache.put(key, img);
        return img;
    }

}
