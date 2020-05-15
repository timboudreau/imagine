package net.java.dev.imagine.effects.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor6;
import net.java.dev.imagine.effects.spi.Effect;
import net.java.dev.imagine.effects.spi.EffectStub;
import org.openide.filesystems.annotations.LayerBuilder;
import org.openide.filesystems.annotations.LayerGeneratingProcessor;
import org.openide.filesystems.annotations.LayerGenerationException;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes("net.java.dev.imagine.effects.spi.Effect")
@SupportedSourceVersion(SourceVersion.RELEASE_5)
@Messages("effects=Effects")
public class EffectAnnotationProcessor extends LayerGeneratingProcessor {

    public List<? extends TypeMirror> getTypeParameters(TypeElement type) {
        //Get the non-generics type name of ToolImplementation
        TypeMirror effectStubType = processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement(EffectStub.class.getName()).asType());
        final TypeMirror objectType = processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType());

        final Set<TypeMirror> seen = new HashSet<TypeMirror>();
        //Iterate all the superclasses/interfaces of our class
        while (!type.asType().equals(objectType)) {
            for (TypeMirror sup : processingEnv.getTypeUtils().directSupertypes(type.asType())) {
                if (seen.contains(sup)) {
                    continue;
                }
                seen.add(sup);
                //If it subclasses ToolImplementation
                if (processingEnv.getTypeUtils().erasure(sup).equals(effectStubType)) {
                    //Go look for the type parameters
                    TV tv = new TV();
                    List<? extends TypeMirror> v = sup.accept(tv, sup);
                    return v;
                }
            }
            type = processingEnv.getElementUtils().getTypeElement(type.getSuperclass().toString());
        }
        //Was not a subclass of ToolImplementation, just a plain old mouse listener or similar
        return null;
    }

    @Override
    protected boolean handleProcess(Set<? extends TypeElement> set, RoundEnvironment env) throws LayerGenerationException {
        TypeMirror stubType = processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement(EffectStub.class.getName()).asType());
        Set<? extends Element> elements = env.getElementsAnnotatedWith(Effect.class);
        for (Element e : elements) {
            TypeElement type = (TypeElement) e;

            if (e.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new LayerGenerationException("Cannot instantiate an abstract class", e);
            }
            if (!e.getModifiers().contains(Modifier.PUBLIC)) {
                throw new LayerGenerationException("Must be a public class", e);
            }

            boolean isStubSubclass = processingEnv.getTypeUtils().isSubtype(type.asType(), stubType);
            if (!isStubSubclass) {
                throw new LayerGenerationException("Not a subclass of " + stubType, e);
            }
            Effect effect = e.getAnnotation(Effect.class);
            DeclaredType outType = findClassValue(e, "value");
            DeclaredType paramType = findClassValue(e, "parameter");
            for (Element el : type.getEnclosedElements()) {
                if (el.getKind() == ElementKind.CONSTRUCTOR) {
                    ExecutableElement ex = (ExecutableElement) el;
                    if (!ex.getParameters().isEmpty()) {
                        //XXX will fail on multiple constructors
                        throw new LayerGenerationException("Must have a no-argument constructor", ex.getParameters().get(0));
                    }
                }
            }

//            ADD CHECK THAT TYPE PARAMETERS MATCH ANNOTATION PARAMETERS
//            List<? extends TypeMirror> params = getTypeParameters(type);
//            System.err.println("Type params " + params);
//            if (params == null || params.isEmpty()) {
//                throw new LayerGenerationException("Type parameters not specified", e);
//            } else if (params.size() != 2) {
//                throw new LayerGenerationException("Incorrect number of type parameters", e);
//            }
            if (!type.getTypeParameters().isEmpty()) {
                throw new LayerGenerationException("Class will be instantiated by reflection; type parameters are not visible at runtime and so, are illegal.");
            }

            String packageName = processingEnv.getElementUtils().getPackageOf(e).getQualifiedName().toString();

            String bundleName = packageName.replace('.', '/') + "/Bundle";

            String name = effect.name();
            if (Effect.DEFAULT_NAME.equals(name)) {
                name = type.getSimpleName().toString();
            }

            LayerBuilder b = layer(e);
            LayerBuilder.File effectsDir = b.folder(net.java.dev.imagine.effects.api.Effect.EFFECT_FOLDER);
            effectsDir.intvalue("position", 700);
            effectsDir.bundlevalue("displayName", "net.java.dev.imagine.effects.api.Bundle", "effects");
            b = effectsDir.write();
            LayerBuilder.File effectFile = b.file(net.java.dev.imagine.effects.api.Effect.EFFECT_FOLDER + '/' + name + ".instance");
            effectFile.bundlevalue("displayName", bundleName, name);
            effectFile.boolvalue(EffectDriver.CAN_PREVIEW_ATTRIBUTE, effect.canPreview());
            effectFile.stringvalue(EffectDriver.STUB_CLASS_ATTRIBUTE, type.asType().toString());
            effectFile.stringvalue(EffectDriver.OUTPUT_CLASS_ATTRIBUTE, outType.toString());
            effectFile.stringvalue(EffectDriver.PARAM_CLASS_ATTRIBUTE, paramType.toString());
            effectFile.stringvalue("instanceClass", net.java.dev.imagine.effects.api.Effect.class.getName());
            effectFile.methodvalue("instanceCreate", EffectDriver.class.getName(), "fromFileObject");
            if (effect.position() != -1) {
                effectFile.intvalue("position", effect.position());
            }
            b = effectFile.write();

            LayerBuilder.File actionsFolder = b.folder("Actions");
            b = actionsFolder.write();
            LayerBuilder.File effectActions = b.folder("Actions/Effects");
            effectActions.bundlevalue("displayName", "net.java.dev.imagine.effects.api.Bundle", "effects");
            effectActions.intvalue("position", 1150);
            b = effectActions.write();

            LayerBuilder.File action = b.file("Actions/Effects/" + name + ".instance");
            action.methodvalue("instanceCreate", EffectAction.class.getName(), "create");
            action.stringvalue("instanceClass", EffectAction.class.getName());
            action.stringvalue("instanceOf", effectActionTypes());
            action.bundlevalue("displayName", bundleName, name);

            action.write();

            LayerBuilder.File menuFolder = b.folder("Menu");
            menuFolder.position(200);
            b = menuFolder.write();
            LayerBuilder.File fxMenu = b.folder("Menu/Effects");
            fxMenu.intvalue("position", 1150);
            fxMenu.bundlevalue("displayName", "net.java.dev.imagine.effectsapi.Bundle", "effects");

            b.shadowFile("Actions/Effects/" + name + ".instance", "Menu/Effects", null).write();

            b = fxMenu.write();

            b.shadowFile(action.getPath(), "Menu/Effects", name);

        }

        return true;
    }

    private DeclaredType findClassValue(Element item, String annotationMemberName) {
        //Annotations will explode if we call methods which return a Class object,
        //so rattle through them and look up the class type in a javac-friendly way
        DeclaredType result = null;
        AnnotationMirror found = null;
        outer:
        for (AnnotationMirror a : item.getAnnotationMirrors()) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : a.getElementValues().entrySet()) {
                AnnotationValue av = e.getValue();
                if (e.getKey().getSimpleName().contentEquals(annotationMemberName) && e.getValue().getValue() instanceof DeclaredType) {
                    found = a;
                    DeclaredType dt = (DeclaredType) e.getValue().getValue();
                    if (dt != null) {
                        result = dt;
                        break outer;
                    }
                }
            }
        }
        if (result == null && found == null) {
            for (AnnotationMirror a : item.getAnnotationMirrors()) {
                for (Element el : a.getAnnotationType().asElement().getEnclosedElements()) {
                    if (el instanceof ExecutableElement) {
                        ExecutableElement eel = (ExecutableElement) el;
                        if (eel.getSimpleName().contentEquals(annotationMemberName)) {
                            if (eel.getDefaultValue() != null && eel.getDefaultValue().getValue() instanceof DeclaredType) {
                                result = (DeclaredType) eel.getDefaultValue().getValue();
                                break;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private class TV extends SimpleTypeVisitor6<List<? extends TypeMirror>, TypeMirror> {

        @Override
        public List<? extends TypeMirror> visitDeclared(DeclaredType t, TypeMirror p) {
            return t.getTypeArguments();
        }
    }

    private static String effectActionTypes() {
        Set<Class<?>> result = new HashSet<Class<?>>(Arrays.asList(EffectAction.class.getInterfaces()));
        Class<?> c = EffectAction.class;
        while (c != Object.class) {
            result.add(c);
            c = c.getSuperclass();
        }
        StringBuilder sb = new StringBuilder();
        for (Iterator<Class<?>> it = result.iterator(); it.hasNext();) {
            sb.append(it.next().getName());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    private static String types(Object o) { //debug stuff
        if (o == null) {
            return "null";
        }
        List<String> s = new ArrayList<String>();
        Class<?> x = o.getClass();
        while (x != Object.class) {
            s.add(x.getName());
            for (Class<?> c : x.getInterfaces()) {
                s.add(c.getName());
            }
            x = x.getSuperclass();
        }
        StringBuilder sb = new StringBuilder();
        for (String ss : s) {
            sb.append(ss);
            sb.append(", ");
        }
        return sb.toString();
    }
}
