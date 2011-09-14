/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.fx;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImageOp;
import net.dev.java.imagine.spi.effects.Effect;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=Effect.class)
public class RotateClockwiseEffect extends AbstractOpEffect {
    public RotateClockwiseEffect() {
        super("COUNTER_CLOCKWISE");
    }

    @Override
    protected BufferedImageOp getOp(Dimension size) {
        return new AffineTransformFilter(AffineTransform.getRotateInstance(Math.toRadians(270), size.width / 2, size.height / 2));
    }
            
}
