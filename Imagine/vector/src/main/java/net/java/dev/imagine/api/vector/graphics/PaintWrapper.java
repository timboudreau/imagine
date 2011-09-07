/*
 * PaintWrapper.java
 *
 * Created on October 31, 2006, 9:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.vector.graphics;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import net.java.dev.imagine.api.vector.Primitive;

/**
 * Wrapper for a Paint object.
 *
 * @author Tim Boudreau
 */
public interface PaintWrapper extends Primitive {
    public Paint toPaint();
    public Color toColor();
    public PaintWrapper createScaledInstance (AffineTransform xform);
}
