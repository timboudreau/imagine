/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import net.java.dev.imagine.spi.image.support.AbstractLayerImplementation;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
final class VectorLayer extends AbstractLayerImplementation {

    private RepaintHandle target;
    private final Shapes shapes = new Shapes();
    private final VectorSurface surface;

    VectorLayer(String name, RepaintHandle handle, Dimension size, VectorLayerFactory factory) {
        super(factory, true, name);
        target = handle;
        addRepaintHandle(handle);
        surface = new VectorSurface(shapes, this, getMasterRepaintHandle());
    }

    VectorLayer(VectorLayer layer, boolean deepCopy) {
        super(layer.getFactory(), layer.isResolutionIndependent(), layer.getName());
        target = layer.target;
        addRepaintHandle(layer.target);
        surface = new VectorSurface(shapes, this, getMasterRepaintHandle());
    }

    @Override
    protected Lookup createLookup() {
        return Lookups.fixed(this, getLayer(), shapes, surface, surface.getSurface());
    }

    @Override
    public Rectangle getBounds() {
        return shapes.getBounds();
    }

    @Override
    public void resize(int width, int height) {
        
    }

    @Override
    public LayerImplementation clone(boolean isUserCopy, boolean deepCopy) {
        return new VectorLayer(this, deepCopy);
    }

    @Override
    public void commitLastPropertyChangeToUndoHistory() {
    }

    @Override
    protected boolean paint(Graphics2D g, Rectangle bounds, boolean showSelection) {
        return shapes.paint(g, bounds);
    }

}
