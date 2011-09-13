package net.java.dev.imagine.fx;

import com.jhlabs.image.GaussianFilter;
import java.awt.image.BufferedImageOp;
import net.dev.java.imagine.spi.effects.Effect;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=Effect.class)
public final class GaussianBlur extends AbstractOpEffect {

    public GaussianBlur() {
        super("GAUSSIAN_BLUR");
    }

    @Override
    protected BufferedImageOp getOp() {
        return new GaussianFilter(6); //XXX customizer
    }
    
}
