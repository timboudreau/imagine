/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.TexturePaint;
import java.util.Objects;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.graphics.ColorWrapper;
import net.java.dev.imagine.api.vector.graphics.GradientPaintWrapper;
import net.java.dev.imagine.api.vector.graphics.LinearPaintWrapper;
import net.java.dev.imagine.api.vector.graphics.RadialPaintWrapper;

/**
 *
 * @author Tim Boudreau
 */
class PaintReference implements Hibernator {

    private Paint paint;
    Attribute<? extends Paint> attr;

    PaintReference(Paint paint) {
        attr = wrapper(paint);
    }

    PaintReference(Attribute<? extends Paint> attr) {
        this.attr = attr;
    }

    PaintReference copy() {
        return new PaintReference(attr.copy());
    }

    static Attribute<? extends Paint> wrapper(Paint paint) {
        if (paint instanceof Color) {
            return new ColorWrapper((Color) paint);
        } else if (paint instanceof TexturePaint) {
            TexturePaint tex = (TexturePaint) paint;
            return new TexturePaintReference(tex);
        } else if (paint instanceof RadialGradientPaint) {
            RadialGradientPaint rgp = (RadialGradientPaint) paint;
            return new RadialPaintWrapper(rgp);
        } else if (paint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) paint;
            return new GradientPaintWrapper(gp);
        } else if (paint instanceof LinearGradientPaint) {
            LinearGradientPaint gp = (LinearGradientPaint) paint;
            return new LinearPaintWrapper(gp);
        } else {
            throw new IllegalArgumentException("" + paint);
        }
    }

    public Paint get() {
        if (paint == null) {
            paint = attr.get();
        }
        return paint;
    }

    @Override
    public void hibernate() {
        paint = null;
        if (attr instanceof Hibernator) {
            ((Hibernator) attr).hibernate();
        }
    }

    @Override
    public void wakeup(boolean immediately, Runnable notify) {
        if (immediately) {
            paint = attr.get();
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.attr);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PaintReference other = (PaintReference) obj;
        if (other.paint == paint) {
            return true;
        }
        if (!Objects.equals(this.attr, other.attr)) {
            return false;
        }
        return true;
    }



}
