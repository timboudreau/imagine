/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.spi;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Shape;
import net.java.dev.imagine.api.vector.Shaped;
import org.imagine.vector.editor.ui.ShapeEntry;

/**
 *
 * @author Tim Boudreau
 */
public interface ShapeElement {

    ShapeEntry copy();

    Rectangle getBounds();

    boolean isDraw();

    boolean isFill();

    boolean isPaths();

    Shaped item();

    Shape shape();

    BasicStroke stroke();

    void toPaths();

}
