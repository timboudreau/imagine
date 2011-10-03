package net.java.dev.imagine.effects.spi;

import java.awt.image.BufferedImageOp;
import net.java.dev.imagine.effects.api.Preview;
import org.openide.util.Lookup.Provider;

/**
 *
 * @author Tim Boudreau
 */
public abstract class BufferedImageOpEffectStub<T> implements EffectStub<T, BufferedImageOp> {

    @Override
    public Preview<?, T, BufferedImageOp> createPreview(Provider layer) {
        return null;
    }

    @Override
    public T createInitialParam() {
        return null;
    }
    
    
}
