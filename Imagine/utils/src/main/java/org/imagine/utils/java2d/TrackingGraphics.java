/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.utils.java2d;

import java.awt.Graphics2D;

/**
 * A Graphics object which can be notified of modified areas.
 *
 * Implementations of the object are expected not to require a call to
 * areaModified() subsequent to normal painting operations; the areaModified()
 * method exists for tools which operate directly on the backing raster of a
 * Surface object, so the Surface object cannot know on its own what has been
 * modified.
 *
 * @author Tim Boudreau
 */
public abstract class TrackingGraphics extends Graphics2D {

    private Runnable onDispose;

    public abstract void areaModified(int x, int y, int w, int h);

    final void onDispose() {
        if (onDispose != null) {
            onDispose.run();
        }
    }

    public final TrackingGraphics onDispose(Runnable r) {
        if (onDispose != null) {
            throw new IllegalStateException("onDispose already set");
        }
        this.onDispose = r;
        return this;
    }
}
