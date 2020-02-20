/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.fx;

import java.awt.geom.AffineTransform;
import net.java.dev.imagine.effects.api.Preview;
import net.java.dev.imagine.effects.spi.Effect;
import net.java.dev.imagine.effects.spi.EffectStub;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
@Effect(position = 110, parameter = AffineTransform.class, value = AffineTransform.class, name = "Transform", canPreview = true)
public class TransformEffect implements EffectStub<AffineTransform, AffineTransform> {

    @Override
    public AffineTransform createInitialParam() {
        return AffineTransform.getTranslateInstance(0, 0);
    }

    @Override
    public AffineTransform create(AffineTransform r) {
        return r == null ? AffineTransform.getTranslateInstance(0, 0) : new AffineTransform(r);
    }

    @Override
    public Preview<?, AffineTransform, AffineTransform> createPreview(Lookup.Provider layer) {
        return null;
    }
}
