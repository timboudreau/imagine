package net.java.dev.imagine.effects.spi;

import net.java.dev.imagine.effects.api.Preview;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public interface EffectStub<ParamType, OutputType> {

    ParamType createInitialParam();

    OutputType create(ParamType r);

    default Preview<?, ParamType, OutputType> createPreview(Lookup.Provider layer) {
        return null;
    }
}
