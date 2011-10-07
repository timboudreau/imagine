package net.java.dev.imagine.customizers.impl;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;
import net.java.dev.imagine.api.customizers.visualizer.ColumnDataScene;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.spi.customizers.Customizes;
import net.java.dev.imagine.spi.customizers.CustomizesProperty;
import org.openide.filesystems.annotations.LayerBuilder;
import org.openide.filesystems.annotations.LayerGeneratingProcessor;
import org.openide.filesystems.annotations.LayerGenerationException;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes({"net.java.dev.imagine.spi.customizers.Customizes",
    "net.java.dev.imagine.spi.customizers.CustomizesProperty"
})
@SupportedSourceVersion(SourceVersion.RELEASE_5)
public class CustomizerAnnotationProcessor extends LayerGeneratingProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>(Arrays.asList(Customizes.class.getCanonicalName(),
                CustomizesProperty.class.getCanonicalName()));
    }

    @Override
    protected boolean handleProcess(Set<? extends TypeElement> set, RoundEnvironment env) throws LayerGenerationException {
        Types types = processingEnv.getTypeUtils();
        Elements elements = processingEnv.getElementUtils();

        TypeElement propertyType = elements.getTypeElement(Property.class.getName());
        TypeElement componentType = elements.getTypeElement(Component.class.getName());
        TypeElement widgetType = elements.getTypeElement("org.netbeans.api.visual.widget.Widget");
        TypeElement sceneType = elements.getTypeElement("org.netbeans.api.visual.widget.Scene");
        TypeElement columnSceneType = elements.getTypeElement(ColumnDataScene.class.getName());
        TypeElement annotationType = elements.getTypeElement(Customizes.class.getName());

        System.err.println("Handle process " + set + " - " + env.getElementsAnnotatedWith(Customizes.class));

        Set<Element> all = new HashSet<Element>();
        all.addAll(env.getElementsAnnotatedWith(Customizes.class));
        all.addAll(env.getElementsAnnotatedWith(CustomizesProperty.class));

        for (Element el : all) {
            System.err.println("Process " + el);

            Customizes customizesAnn = el.getAnnotation(Customizes.class);
            CustomizesProperty customizesPropAnn = el.getAnnotation(CustomizesProperty.class);

            if (customizesPropAnn != null && customizesAnn != null) {
                throw new LayerGenerationException("Cannot have both Customizes and CustomizesProperty annotations on one class");
            }

            boolean isProp = customizesPropAnn != null;

            DeclaredType customizes = findClassValue(el, "value");
            if (customizes == null) {
                System.err.println("Could not find on " + el.getAnnotation(Customizes.class));
            }

            if (!customizes.asElement().getModifiers().contains(Modifier.PUBLIC)) {
                throw new LayerGenerationException("Must be a public class to use @Customizes");
            }
            if (customizes.asElement().getModifiers().contains(Modifier.ABSTRACT)) {
                throw new LayerGenerationException("Must be a non-abstract class to use @Customizes");
            }

            TypeElement te = (TypeElement) el;
            TypeMirror asType = te.asType();

            boolean valid = types.isSubtype(te.asType(), componentType.asType());
            boolean isWidget = false;
            if (!valid && widgetType != null) {
                valid = types.isSubtype(te.asType(), widgetType.asType());
                isWidget = valid;
            }
            if (!valid) {
                throw new LayerGenerationException("Must be a subclass of " + componentType + " or " + widgetType);
            }

            List<ExecutableElement> constructors = new ArrayList<ExecutableElement>();
            for (Element en : te.getEnclosedElements()) {
                if (en.getKind() == ElementKind.CONSTRUCTOR) {
                    constructors.add((ExecutableElement) en);
                }
            }
            boolean hasUsableConstructor = false;
            for (ExecutableElement con : constructors) {
                List<? extends VariableElement> params = con.getParameters();
                if (!isProp) {
                    if (isWidget) {
                        if (params.size() == 2) {
                            VariableElement param1 = params.get(0);
                            VariableElement param2 = params.get(1);
                            System.err.println("Param 1 " + param1 + " tp " + param1.asType());
                            System.err.println("Param 2 " + param1 + " tp " + param2.asType());
                            hasUsableConstructor |= isSceneSubtype(param1) &&
                                    customizes.equals(param2.asType());
                        }
                    } else {
                        if (params.size() == 1) {
                            if (customizes.equals(params.get(0).asType())) {
                                hasUsableConstructor |= true;
                                break;
                            }
                        }
                    }
                } else { //isProp
                    if (isWidget) {
                        if (params.size() == 2) {
                            VariableElement param1 = params.get(0);
                            VariableElement param2 = params.get(1);
                            System.err.println("Param 1 " + param1 + " tp " + param1.asType());
                            System.err.println("Param 2 " + param1 + " tp " + param2.asType());
                            hasUsableConstructor |= isSceneSubtype(param1) &&
                                    isPropertySubtype(param2, customizes);
                        }
                    } else {
                        System.err.println("Not a widget: " + con + " on " + el);
                        if (params.size() == 1) {
                            VariableElement param = params.get(0);
                            System.err.println("Param is " + param);
                            hasUsableConstructor |= isPropertySubtype(param, customizes);
                        }
                    }
                }
                if (hasUsableConstructor) {
                    break;
                }
            }
            if (!hasUsableConstructor) {
                if (!isProp) {
                    if (isWidget) {
                        throw new LayerGenerationException("Must have a 2-argument public constructor which takes " + sceneType + ", " + customizes, el);
                    } else {
                        throw new LayerGenerationException("Must have a public constructor which takes a single argument of " + customizes, el);
                    }
                } else {
                    if (isWidget) {
                        throw new LayerGenerationException("Must have a 2-argument public constructor which takes " + sceneType + ", Property<" + customizes + ", ?>", el);
                    } else {
                        throw new LayerGenerationException("Must have a public constructor which takes a single argument of Property<" + customizes + ", ?>", el);
                    }
                }
            }
            LayerBuilder b = layer(el);
            LayerBuilder.File customizersDir = b.folder(isProp ? "propertyCustomizers" : "customizers2");
            b = customizersDir.write();
            LayerBuilder.File typeFolder = b.folder(customizersDir.getPath() + "/" + customizes.toString());
            b = typeFolder.write();
            LayerBuilder.File customizerFile = b.file(typeFolder.getPath() + "/" + asType);
            customizerFile.boolvalue("widget", isWidget);
            b = customizerFile.write();
            System.err.println("Wrote " + customizerFile.getPath());
        }

        return true;
    }

    private boolean isSceneSubtype(VariableElement e) {
        boolean result = false;
        Types types = processingEnv.getTypeUtils();
        Elements elements = processingEnv.getElementUtils();
        TypeElement sceneType = elements.getTypeElement("org.netbeans.api.visual.widget.Scene");
        TypeElement columnSceneType = elements.getTypeElement(ColumnDataScene.class.getName());
        if (types.isSubtype(e.asType(), sceneType.asType())) {
            result = true;
        } else if (types.isSubtype(e.asType(), columnSceneType.asType())) {
            result = true;
        }
        return result;
    }

    private boolean isPropertySubtype(VariableElement e, DeclaredType customizes) {
        boolean result = false;
        Types types = processingEnv.getTypeUtils();
        Elements elements = processingEnv.getElementUtils();

        TypeElement propertyType = elements.getTypeElement(Property.class.getName());
        VariableElement param = e;
        TypeMirror type = param.asType();
        System.err.println("As type " + types.erasure(type) + " equals  " + types.erasure(propertyType.asType()) + " " + types.erasure(type).equals(types.erasure(propertyType.asType())));
        if (types.erasure(type).equals(types.erasure(propertyType.asType()))) {
            List<? extends TypeMirror> gtypes = type.accept(new TV(), type);
            System.err.println("  gtypes " + gtypes);
            if (gtypes != null) {
                if (gtypes.size() == 2) {
                    TypeMirror firstParam = gtypes.get(0);
                    if (types.erasure(customizes).equals(types.erasure(firstParam))) {
                        result = true;
                        result = true;
                    } else {
                        System.err.println("Type mismatch " + firstParam + " is not " + customizes);
                    }
                }
            }
        }
        return result;
    }

    private DeclaredType findClassValue(Element item, String annotationMemberName) {
        //Annotations will explode if we call methods which return a Class object,
        //so rattle through them and look up the class type in a javac-friendly way
        DeclaredType result = null;
        AnnotationMirror found = null;
        outer:
        for (AnnotationMirror a : item.getAnnotationMirrors()) {
            System.err.println("check AM " + a + " - " + a.getAnnotationType().asElement().getEnclosedElements());
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : a.getElementValues().entrySet()) {
                AnnotationValue av = e.getValue();
                if (e.getKey().getSimpleName().contentEquals(annotationMemberName) && av.getValue() instanceof DeclaredType) {
                    found = a;
                    DeclaredType dt = (DeclaredType) e.getValue().getValue();
                    if (dt != null) {
                        result = dt;
                        break outer;
                    }
                } else {
                    System.err.println("XXX " + av.getValue() + " " + av + " " + types(av.getValue()));
                }
            }
        }
        if (result == null && found == null) {
            System.err.println("Huh? " + item + " - " + annotationMemberName);
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

    private static String types(Object o) { //debug stuff
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

    private class TV extends SimpleTypeVisitor6<List<? extends TypeMirror>, TypeMirror> {

        @Override
        public List<? extends TypeMirror> visitDeclared(DeclaredType t, TypeMirror p) {
            return t.getTypeArguments();
        }
    }
}
