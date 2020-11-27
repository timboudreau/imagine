/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector;

import java.awt.geom.AffineTransform;
import com.mastfrog.geometry.util.PooledTransform;

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
        PooledTransform.withScaleInstance(scale, scale, this::applyTransform);
//        applyTransform(AffineTransform.getScaleInstance(scale, scale));
    }

    default void applyScale(double scaleX, double scaleY) {
        PooledTransform.withScaleInstance(scaleX, scaleY, this::applyTransform);
//        applyTransform(AffineTransform.getScaleInstance(scaleX, scaleY));
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
        PooledTransform.withTranslateInstance(x, y, this::applyTransform);
//        applyTransform(AffineTransform.getTranslateInstance(x, y));
    }
}
