/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.geometry.util;

import java.awt.geom.PathIterator;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
public class LazyPathIterator implements PathIterator {

    private final Supplier<PathIterator> supplier;

    public LazyPathIterator(Supplier<PathIterator> supplier) {
        this.supplier = supplier;
    }

    @Override
    public int getWindingRule() {
        return supplier.get().getWindingRule();
    }

    @Override
    public boolean isDone() {
        return supplier.get().isDone();
    }

    @Override
    public void next() {
        supplier.get().next();
    }

    @Override
    public int currentSegment(float[] coords) {
        return supplier.get().currentSegment(coords);
    }

    @Override
    public int currentSegment(double[] coords) {
        return supplier.get().currentSegment(coords);
    }
}
