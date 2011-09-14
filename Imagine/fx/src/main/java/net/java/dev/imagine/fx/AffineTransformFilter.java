/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.java.dev.imagine.fx;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 *
 * @author tim
 */
public class AffineTransformFilter extends TransformFilter {
    private final AffineTransform at;
    public AffineTransformFilter(AffineTransform at) {
        this.at = at;
    }

    @Override
    protected AffineTransform createTransform(BufferedImage src) {
        return at;
    }
}
