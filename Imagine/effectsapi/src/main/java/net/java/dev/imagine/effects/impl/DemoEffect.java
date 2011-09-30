package net.java.dev.imagine.effects.impl;

import java.awt.AlphaComposite;
import java.awt.Composite;
import net.java.dev.imagine.effects.api.Preview;
import net.java.dev.imagine.effects.impl.DemoEffect.Data;
import net.java.dev.imagine.effects.spi.Effect;
import net.java.dev.imagine.effects.spi.EffectStub;
import org.openide.util.Lookup.Provider;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Effect(canPreview = true, parameter = Data.class, value = Composite.class)
@Messages("DemoEffect=Demo Effect")
public class DemoEffect implements EffectStub<Data, Composite> {

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
    
    public static class Data {
        private float value = 1.0F;

        public float getValue() {
            return value;
        }

        public void setValue(float value) {
            this.value = value;
        }
    }
}
