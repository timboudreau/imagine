/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.utils.painting;

import java.awt.Rectangle;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
public final class CollectingRepaintHandle implements RepaintHandle, Supplier<Rectangle> {

    private final Rectangle dirty = new Rectangle();

    @Override
    public void repaintArea(int x, int y, int w, int h) {
        dirty.add(x, y);
        dirty.add(x + w, y + h);
    }

    @Override
    public Rectangle get() {
        return dirty;
    }
}
