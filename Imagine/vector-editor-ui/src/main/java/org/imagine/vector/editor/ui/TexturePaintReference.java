/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui;

import java.awt.Graphics2D;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import net.java.dev.imagine.api.vector.graphics.TexturePaintWrapper;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.netbeans.paint.misc.image.ByteNIOBufferedImage;

/**
 *
 * @author Tim Boudreau
 */
class TexturePaintReference implements HibernatablePaintReference<TexturePaint> {

    private TexturePaint cached;
    TexturePaintWrapper wrapper;
    ImgRef ref;

    TexturePaintReference(TexturePaint paint) {
        cached = paint;
        BufferedImage orig = paint.getImage();
        ByteNIOBufferedImage img = new ByteNIOBufferedImage(orig.getWidth(), orig.getHeight());
        Graphics2D g = img.createGraphics();
        try {
            g.drawRenderedImage(orig, null);
        } finally {
            g.dispose();
        }
        wrapper = new TexturePaintWrapper(paint, img);
        ref = new ImgRef(img.disposer, img);
    }

    TexturePaintReference(TexturePaintWrapper wrapper, ImgRef ref) {
        this.ref = ref;
        this.wrapper = wrapper;
    }

    public TexturePaintReference copy() {
        return new TexturePaintReference(wrapper, ref);
    }

    @Override
    public TexturePaint get() {
        if (cached == null) {
            synchronized (this) {
                if (cached == null) {
                    return cached = wrapper.get((slow) -> {
                        if (slow instanceof ByteNIOBufferedImage) {
                            return GraphicsUtils.newBufferedImage(slow.getWidth(), slow.getHeight(), (g) -> {
                                g.drawRenderedImage(slow, null);
                            });
                        }
                        return slow;
                    });
                }
            }
        }
        return cached;
    }

    @Override
    public void hibernate() {
        cached = null;
    }

    @Override
    public void wakeup(boolean immediately, Runnable notify) {
        get();
        if (notify != null) {
            notify.run();
        }
    }

    @Override
    public void paint(Graphics2D g) {
        g.setPaint(get());
    }

}
