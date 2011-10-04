package net.java.dev.imagine.effects.api;

import java.awt.image.BufferedImage;
import java.util.*;
import net.java.dev.imagine.effects.impl.EffectBridge;
import net.java.dev.imagine.effects.spi.EffectDescriptor;
import net.java.dev.imagine.effects.spi.EffectImplementation;
import net.java.dev.imagine.effects.spi.ImageSource;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.Parameters;

/**
 * An effect which can be applied to an image layer of some sort.
 * An effect has a parameter type (the values which can be customized to 
 * tune its output), and an output type (typically a BufferedImageOp or
 * Composite).  Effects have visual attributes such as a name, display name,
 * help context, etc.
 * <p/>
 * The typical usage pattern is:
 * <ul>
 * <li>Get an instance of effect</li>
 * <li>Find an EffectReceiver parameterized on the effect's output type, in
 * the layer (or other Lookup.Provider) you want to apply the effect to</li>
 * <li>Call createInitialParam to get the default input parameter (for example,
 * a hue value for an effect which adjusts hue).</li>
 * <li>Possibly show a customizer which operates on the initial param type</li>
 * <li>Pass the (possibly modified) parameter and the parameter to the 
 * EffectReceiver to apply the effect (this might mean applying a BufferedImageOp
 * to an image, or storing the effect name and the parameter to apply the 
 * effect on the fly during rendering).
 *
 * @author Tim Boudreau
 */
public final class Effect<ParamType, OutputType> {

    public static final String EFFECT_FOLDER = "effects";
    private final EffectDescriptor descriptor;
    private final EffectImplementation<ParamType, OutputType> impl;

    private Effect(EffectDescriptor descriptor, EffectImplementation<ParamType, OutputType> impl) {
        Parameters.notNull("impl", impl);
        Parameters.notNull("descriptor", descriptor);
        this.descriptor = descriptor;
        this.impl = impl;
    }

    public Class<ParamType> parameterType() {
        return impl.parameterType();
    }
    
    public boolean canApply(Lookup.Provider layer) {
        for (EffectReceiver e : layer.getLookup().lookupAll(EffectReceiver.class)) {
            if (canApply(e)) {
                return true;
            }
        }
        return false;
    }

    public <T> boolean canApply(EffectReceiver<T> receiver) {
        return receiver.type() == impl.outputType();
    }

    public ParamType createInitialParam() {
        ParamType result = impl.createInitialParam();
        assert result == null || parameterType().isInstance(result);
        return result;
    }

    public String getName() {
        return descriptor.name();
    }

    public String getDisplayName() {
        return descriptor.displayName();
    }

    public String getDescription() {
        return descriptor.description();
    }

    public OutputType create(ParamType r) {
        return impl.create(r);
    }
    
    public Class<OutputType> outputType() {
        return impl.outputType();
    }

    public Preview<?, ParamType, ?> createPreview(Lookup.Provider layer) {
        if (!descriptor.canPreview()) {
            return null;
        }
        Preview<?, ParamType, ?> result = impl.createPreview(layer);
        if (result == null) {
            PreviewFactory<ImageSource> imgSourcePreview = null;
            for (PreviewFactory<?> p : Lookup.getDefault().lookupAll(PreviewFactory.class)) {
                result = check(p, layer.getLookup());
                if (result != null) {
                    break;
                }
                if (ImageSource.class == p.sourceType()) {
                    imgSourcePreview = (PreviewFactory<ImageSource>) p;
                }
            }
            if (result == null && imgSourcePreview != null) {
                BufferedImage img = layer.getLookup().lookup(BufferedImage.class);
                if (img != null) {
                    ImageSource src = EffectImplementation.createBufferedImageImageSource(layer);
                    if (src != null) {
                        result = Preview.create(imgSourcePreview, src, this);
                    }
                }
            }
        }
        return result;
    }

    private <T> Preview<T, ParamType, OutputType> check(PreviewFactory<T> preview, Lookup lookup) {
        T arg = lookup.lookup(preview.sourceType());
        if (arg != null && preview.canCreatePreview(this)) {
            return Preview.create(preview, arg, this);
        }
        return null;
    }

    static {
        EffectBridge.INSTANCE = new EffectBridge() {

            @Override
            public <T, R> Effect<T, R> createEffect(EffectDescriptor descriptor, EffectImplementation<T, R> impl) {
                return new Effect<T, R>(descriptor, impl);
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Effect && ((Effect) o).getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + " [" + descriptor + "][" + impl + ']';
    }

    public static Collection<? extends Effect<?, ?>> allEffects() {
//        return (Collection<? extends Effect<?, ?>>) Lookups.forPath("effects").lookupAll(Effect.class);
        FileObject fld = FileUtil.getConfigFile("effects");
        List<Effect<?, ?>> result = new ArrayList<Effect<?, ?>>();
        FileObject[] kids = fld.getChildren();
        Arrays.sort(kids, new PosComparator());
        for (FileObject child : kids) {
            System.err.println("TRY " + child.getPath());
            Object o = child.getAttribute("instanceCreate");
            if (o instanceof Effect) {
                result.add((Effect<?, ?>) o);
            } else {
                System.err.println("WRONG THING: " + o + " (" + (o == null ? "" : o.getClass().getName()) + ")");
            }
        }
        return result;
    }

    public static Effect<?, ?> getEffectByName(String name) {
        FileObject fld = FileUtil.getConfigFile("effects");
        FileObject child = fld.getFileObject(name + ".instance");
        if (child != null) {
            Object o = child.getAttribute("instanceCreate");
            return o instanceof Effect ? (Effect<?, ?>) o : null;
        }
        return null;
    }
    
    private static final class PosComparator implements Comparator<FileObject> {

        @Override
        public int compare(FileObject t, FileObject t1) {
            Object a = t.getAttribute("position");
            Object b = t1.getAttribute("position");
            return toInt(a).compareTo(toInt(b));
        }
        
        private Integer toInt(Object o) {
            return o instanceof Number ? ((Number) o).intValue() : 0;
        }
        
    }
}
