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

    void applyTransform(AffineTransform xform);

    default boolean canApplyTransform(AffineTransform xform) {
        return true;
    }

    default void applyScale(double scale) {
        applyTransform(AffineTransform.getScaleInstance(scale, scale));
    }

    default void applyScale(double scaleX, double scaleY) {
        applyTransform(AffineTransform.getScaleInstance(scaleX, scaleY));
    }

    /**
     * Create a duplicate of this object whose coordinates have been transformed
     * by the passed AffineTransform.
     */
    Transformable copy(AffineTransform transform);

    @Override
    Transformable copy();

    default void translate(double x, double y) {
        if (x == 0D && y == 0D) {
            return;
        }
        applyTransform(AffineTransform.getTranslateInstance(x, y));
    }
}
