/*
 * Size.java
 *
 * Created on October 31, 2006, 1:25 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.util;

import java.io.Serializable;
import static java.lang.Double.doubleToLongBits;

/**
 * Immutable equivalent of a Dimension
 *
 * @author Tim Boudreau
 */
public final class Size implements Serializable {

    public final double w;
    public final double h;

    public Size(final double w, final double h) {
        this.w = w;
        this.h = h;
    }

    public boolean isEmpty() {
        return w <= 0 || h <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        }
        boolean result = o instanceof Size;
        if (result) {
            Size s = (Size) o;
            result = s.w == w && s.h == h;
        }
        return result;
    }

    @Override
    public int hashCode() {
//        return (int) (w * h * 1_000);
        long result = (doubleToLongBits(w) * 65357)
                + (doubleToLongBits(h) + 7);
        return (int) (result ^ (result << 32));
    }
}
