package net.java.dev.imagine.effects.impl;

import java.awt.AlphaComposite;
import java.awt.Composite;
import net.java.dev.imagine.effects.api.Preview;
import net.java.dev.imagine.effects.spi.EffectStub;
import org.openide.util.Lookup.Provider;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@net.java.dev.imagine.effects.spi.Effect(canPreview = false, parameter = Data.class, value = Composite.class, position = Integer.MIN_VALUE)
@Messages(value = "SomeEffect=Some Effect")
public class SomeEffect implements EffectStub<Data, Composite> {

    @Override
    public Data createInitialParam() {
        return new Data();
    }

    @Override
    public Composite create(Data r) {
        return AlphaComposite.getInstance(AlphaComposite.XOR, r.getValue());
    }

    @Override
    public Preview<?, Data, Composite> createPreview(Provider layer) {
        return null;
    }
}
