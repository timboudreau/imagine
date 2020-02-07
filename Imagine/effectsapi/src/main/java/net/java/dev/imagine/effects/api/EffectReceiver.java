package net.java.dev.imagine.effects.api;

import java.awt.Dimension;
import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public abstract class EffectReceiver<EffectType> {

    private final Class<EffectType> type;

    protected EffectReceiver(Class<EffectType> type) {
        this.type = type;
    }

    public Class<EffectType> type() {
        return type;
    }

    public boolean canApplyEffects() {
        return true;
    }

    public <ParamType> boolean apply(Effect<ParamType, EffectType> effect, ParamType params) {
        if (!canApplyEffects()) {
            return false;
        }
        Parameters.notNull("effect", effect);
        assert params == null || effect.parameterType().isInstance(params);

        EffectType result = effect.create(params);

        return onApply(result);
    }

    protected abstract <ParamType> boolean onApply(EffectType effect);
    
    public abstract Dimension getSize();
}
