package net.java.dev.imagine.effects.impl;

import net.java.dev.imagine.effects.api.Effect;
import net.java.dev.imagine.effects.spi.EffectDescriptor;
import net.java.dev.imagine.effects.spi.EffectImplementation;

/**
 *
 * @author Tim Boudreau
 */
public abstract class EffectBridge {

    public static EffectBridge INSTANCE;

    public abstract <T, R> Effect<T, R> createEffect(EffectDescriptor descriptor, EffectImplementation<T, R> impl);
}
