package net.java.dev.imagine.effects.impl;

import net.java.dev.imagine.effects.api.Effect;
import net.java.dev.imagine.effects.api.Preview;
import net.java.dev.imagine.effects.spi.EffectDescriptor;
import net.java.dev.imagine.effects.spi.EffectImplementation;
import net.java.dev.imagine.effects.spi.EffectStub;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;
import org.openide.util.Parameters;

/**
 * Takes annotation parameters and creates an effect
 *
 * @author Tim Boudreau
 */
public class EffectDriver<StubClass extends EffectStub<ParamType, OutputType>, ParamType, OutputType> {

    public static final String STUB_CLASS_ATTRIBUTE = "type";
    public static final String PARAM_CLASS_ATTRIBUTE = "paramType";
    public static final String OUTPUT_CLASS_ATTRIBUTE = "outputType";
    public static final String CAN_PREVIEW_ATTRIBUTE = "canPreview";
    private final Class<StubClass> stubClass;
    private final Class<ParamType> paramClass;
    private final Class<OutputType> outputClass;

    private EffectDriver(Class<StubClass> stubClass, Class<ParamType> paramClass, Class<OutputType> outputClass) {
        Parameters.notNull("outputClass", outputClass);
        Parameters.notNull("stubClass", stubClass);
        Parameters.notNull("paramClass", paramClass);
        this.stubClass = stubClass;
        this.paramClass = paramClass;
        this.outputClass = outputClass;
    }

    public static Effect<?, ?> fromFileObject(FileObject fo) throws DataObjectNotFoundException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String stubClass = (String) fo.getAttribute(STUB_CLASS_ATTRIBUTE);
        String paramClass = (String) fo.getAttribute(PARAM_CLASS_ATTRIBUTE);
        String outputClass = (String) fo.getAttribute(OUTPUT_CLASS_ATTRIBUTE);
        DescriptorImpl desc = new DescriptorImpl(DataObject.find(fo));
        return create(stubClass, paramClass, outputClass).createEffectImpl(desc);
    }

//    public static /*<StubClass extends EffectStub<ParamType, OutputType>, ParamType, OutputType>*/ EffectDriver /*<StubClass, ParamType, OutputType>*/ create (String stubClassName, String paramClassName, String outputClassName) throws InstantiationException, IllegalAccessException {
    public static EffectDriver create(String stubClassName, String paramClassName, String outputClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
        Class<?> paramClass = cl.loadClass(paramClassName);
        Class<?> outClass = cl.loadClass(outputClassName);
        Class<?> stubClass = cl.loadClass(stubClassName);
        return create(stubClass, paramClass, outClass);
    }

    public Effect<ParamType, OutputType> createEffectImpl(EffectDescriptor descriptor) throws InstantiationException, IllegalAccessException {
        StubClass stub = stubClass.newInstance();
        return new EI<StubClass, ParamType, OutputType>(stub, paramClass, outputClass).toEffect(descriptor);
    }

    //private static <StubClass extends EffectStub<ParamType, OutputType>, ParamType, OutputType> EffectDriver<StubClass, ParamType, OutputType> create(Class<?> stubClass, Class<ParamType> paramClass, Class<OutputType> outputClass) throws InstantiationException, IllegalAccessException {
    private static EffectDriver create(Class<?> stubClass, Class<?> paramClass, Class<?> outputClass) throws InstantiationException, IllegalAccessException {
//        Class<StubClass> c = (Class<StubClass>) stubClass;
//        return new EffectDriver<StubClass, ParamType, OutputType>(stubClass, paramClass, outputClass);
        return new EffectDriver (stubClass, paramClass, outputClass);
    }

    static class EI<StubClass extends EffectStub<ParamType, OutputType>, ParamType, OutputType> extends EffectImplementation<ParamType, OutputType> {

        private final StubClass stub;

        EI(StubClass stub, Class<ParamType> paramType, Class<OutputType> outType) {
            super(paramType, outType);
            this.stub = stub;
        }

        @Override
        public Preview<?, ParamType, OutputType> createPreview(Provider layer) {
            return stub.createPreview(layer);
        }

        @Override
        public ParamType createInitialParam() {
            return stub.createInitialParam();
        }

        @Override
        public OutputType create(ParamType r) {
            return stub.create(r);
        }

        public Effect<ParamType, OutputType> toEffect(EffectDescriptor descriptor) {
            Parameters.notNull("descriptor", descriptor);
            return EffectBridge.INSTANCE.createEffect(descriptor, this);
        }

        public String toString() {
            return stub + "; " + parameterType().getName() + ";" + outputType().getName();
        }
    }

    static class DescriptorImpl implements EffectDescriptor {

        private final DataObject dob;

        public DescriptorImpl(DataObject dob) {
            this.dob = dob;
        }

        @Override
        public String name() {
            return dob.getPrimaryFile().getName();
        }

        @Override
        public String displayName() {
            return dob.getNodeDelegate().getDisplayName();
        }

        @Override
        public String description() {
            return dob.getNodeDelegate().getShortDescription();
        }

        @Override
        public HelpCtx helpCtx() {
            return dob.getNodeDelegate().getHelpCtx();
        }

        @Override
        public boolean canPreview() {
            return Boolean.TRUE.equals(dob.getPrimaryFile().getAttribute(CAN_PREVIEW_ATTRIBUTE));
        }

        public String toString() {
            return "Descriptor: " + name() + " " + displayName();
        }
    }
}
