/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
public abstract class GradientManager {

    private static GradientManager INSTANCE;

    public static GradientManager getDefault() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        ServiceLoader<GradientManager> ldr
                = ServiceLoader.load(GradientManager.class);
        Iterator<GradientManager> iter = ldr.iterator();
        if (iter.hasNext()) {
            INSTANCE = iter.next();
        }
        INSTANCE = new DefaultGradientManager();
        return INSTANCE;
    }

    int lastW = 640;
    int lastH = 480;
    public Paint paintFor(Paint orig, int w, int h) {
        lastW = w;
        lastH = h;
        if (orig == null || orig instanceof Color) {
            return orig;
        }
        return findPaint(orig, w, h);
    }

    public abstract Paint findPaint(PaintKey<?> key);

    protected abstract Paint findPaint(Paint orig, int w, int h);

    public Graphics2D wrapGraphics(Graphics2D orig) {
        return new PaintReplacingGraphics(orig);
    }

    class PaintReplacingGraphics extends AbstractDelegatingGraphics {

        public PaintReplacingGraphics(Graphics2D other) {
            super(other);
        }

        @Override
        public Graphics create() {
            return new PaintReplacingGraphics((Graphics2D) delegate.create());
        }

        @Override
        public void setPaint(Paint paint) {
            super.setPaint(findPaint(paint, lastW, lastH));
        }
    }
}
