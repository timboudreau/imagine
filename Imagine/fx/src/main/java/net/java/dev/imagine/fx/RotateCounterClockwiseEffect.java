package net.java.dev.imagine.fx;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImageOp;
import net.dev.java.imagine.spi.effects.Effect;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
//@ServiceProvider(service=Effect.class)
public class RotateCounterClockwiseEffect extends AbstractOpEffect {
    public RotateCounterClockwiseEffect() {
        super("CLOCKWISE");
    }

    @Override
    protected BufferedImageOp getOp() {
        return new AffineTransformFilter(AffineTransform.getQuadrantRotateInstance(1));
    }
            
}
