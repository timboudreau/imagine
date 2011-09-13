package net.java.dev.imagine.fx;

import com.jhlabs.image.EqualizeFilter;
import java.awt.image.BufferedImageOp;
import net.dev.java.imagine.spi.effects.Effect;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=Effect.class)
public final class Equalize extends AbstractOpEffect {

    public Equalize() {
        super("EQUALIZE");
    }

    @Override
    protected BufferedImageOp getOp() {
        return new EqualizeFilter();
    }
    
}
