package net.java.dev.imagine.effects.impl;

import java.awt.Composite;
import net.java.dev.imagine.effects.spi.CompositeEffectStub;
import net.java.dev.imagine.effects.spi.Effect;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Effect(position=Integer.MAX_VALUE)
@Messages("AnotherEffect=Another Effect")
public class AnotherEffect extends CompositeEffectStub<Void> {

    @Override
    public Composite create(Void r) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
