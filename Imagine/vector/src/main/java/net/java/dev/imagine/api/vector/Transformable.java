/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector;

import java.awt.geom.AffineTransform;

/**
 *
 * @author Tim Boudreau
 */
public interface Transformable extends Primitive {

    void applyScale(AffineTransform xform);

    default void applyScale(double scale) {
        applyScale(AffineTransform.getScaleInstance(scale, scale));
    }

    default void applyScale(double scaleX, double scaleY) {
        applyScale(AffineTransform.getScaleInstance(scaleX, scaleY));
    }

    /**
     * Create a duplicate of this object whose coordinates have
     * been transformed by the passed AffineTransform.
     */
    Transformable copy (AffineTransform transform);

    @Override
    Transformable copy();
}
