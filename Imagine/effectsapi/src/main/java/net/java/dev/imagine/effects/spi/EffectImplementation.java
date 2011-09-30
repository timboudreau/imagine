package net.java.dev.imagine.effects.spi;

import net.java.dev.imagine.effects.api.Preview;
import net.java.dev.imagine.effects.impl.EffectBridge;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;

/**
 *
 * @author Tim Boudreau
 */
public abstract class EffectImplementation<ParamType, OutputType> {

    private final Class<OutputType> outputType;
    private final Class<ParamType> parameterType;

    public EffectImplementation(Class<ParamType> paramType, Class<OutputType> outputType) {
        this.parameterType = paramType;
        this.outputType = outputType;
    }

    protected final net.java.dev.imagine.effects.api.Effect<ParamType, OutputType> createEffect(EffectDescriptor des) {
        return EffectBridge.INSTANCE.createEffect(des, this);
    }

    public final Class<ParamType> parameterType() {
        return parameterType;
    }

    public final Class<OutputType> outputType() {
        return outputType;
    }

    static {
        try {
            //force initialization of EffectBridge.INSTANCE
            Class.forName(Effect.class.getName());
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public abstract ParamType createInitialParam();

    public abstract OutputType create(ParamType r);

    public Preview<?, ParamType, OutputType> createPreview(Provider layer) {
        return null;
    }

    /**
     * If a BufferedImage is present in the lookup of the argument, creates an
     * ImageSource which uses that image to create scaled previews.
     * @param layer A Lookup.Provider such as an instance of Layer
     * @return An image source if a BufferedImage is available, otherwise null
     */
    public static ImageSource createBufferedImageImageSource(Lookup.Provider layer) {
        return BufferedImageImageSource.create(layer);
    }
}
