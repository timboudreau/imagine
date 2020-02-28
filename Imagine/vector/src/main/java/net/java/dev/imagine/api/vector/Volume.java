/*
 * Volume.java
 *
 * Created on November 1, 2006, 6:33 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * A graphical element which has an interior.
 *
 * @author Tim Boudreau
 */
public interface Volume extends Primitive, Shaped {

    @Override
    public Volume copy();

    @Override
    default Shape toShape() {
        Rectangle2D.Double rect = new Rectangle2D.Double();
        getBounds(rect);
        return rect;
    }

    @Override
    default Rectangle getBounds() {
        Rectangle2D.Double rect = new Rectangle2D.Double();
        getBounds(rect);
        return rect.getBounds();
    }
}
