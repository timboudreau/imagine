package net.java.dev.imagine.effects.spi;

import java.awt.Composite;
import net.java.dev.imagine.effects.api.Preview;
import org.openide.util.Lookup.Provider;

/**
 *
 * @author Tim Boudreau
 */
public abstract class CompositeEffectStub<T> implements EffectStub<T, Composite> {

    @Override
    public Preview<?, T, Composite> createPreview(Provider layer) {
        return null;
    }

    @Override
    public T createInitialParam() {
        return null;
    }
    
    
}
