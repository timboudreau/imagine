package net.java.dev.imagine.effects.spi;

import net.java.dev.imagine.effects.api.Preview;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public interface EffectStub<ParamType, OutputType> {

    public abstract ParamType createInitialParam();

    public abstract OutputType create(ParamType r);

    public Preview<?, ParamType, OutputType> createPreview(Lookup.Provider layer);
}
