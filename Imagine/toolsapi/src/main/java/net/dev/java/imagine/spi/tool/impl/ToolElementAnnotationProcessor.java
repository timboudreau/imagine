package net.dev.java.imagine.spi.tool.impl;

import com.mastfrog.util.strings.Escaper;
import com.mastfrog.util.strings.Strings;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import net.dev.java.imagine.spi.tool.ToolElement;
import static net.dev.java.imagine.spi.tool.impl.ToolElementAnnotationProcessor.ANNOTATION_TYPE_NAME;
import org.openide.filesystems.annotations.LayerBuilder;
import org.openide.filesystems.annotations.LayerGeneratingProcessor;
import org.openide.filesystems.annotations.LayerGenerationException;
import org.openide.util.EditableProperties;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(ANNOTATION_TYPE_NAME)
public class ToolElementAnnotationProcessor extends LayerGeneratingProcessor {

    public static final String ANNOTATION_TYPE_NAME = "net.dev.java.imagine.spi.tool.ToolElement";

    @Override
    protected boolean handleProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) throws LayerGenerationException {
        try {
            Set<ToolElementEntry> entries = new HashSet<>();
            for (Element el : roundEnv.getElementsAnnotatedWith(ToolElement.class)) {
                switch (el.getKind()) {
                    case CLASS:
                        processOneElement((TypeElement) el, entries);
                        continue;
                    default:
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@ToolElement must appear on a class", el);
                        break;
                }
            }
            if (!entries.isEmpty()) {
                finish(entries);
            }
        } catch (Exception | Error e) {
            e.printStackTrace(System.err);
            com.mastfrog.util.preconditions.Exceptions.chuck(e);
        }
        return true;
    }

    private void visitTypeAndSupertypes(TypeMirror type, Consumer<String> nc) {
        nc.accept(canonicalize(type));
        for (TypeMirror tm : processingEnv.getTypeUtils().directSupertypes(type)) {
            String name = canonicalize(tm);
            nc.accept(name);
        }
    }

    private void finish(Set<ToolElementEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        List<ToolElementEntry> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, (a, b) -> {
            return Integer.compare(a.position, b.position);
        });
        Map<String, EditableProperties> propertiesForPackage = new HashMap<>();
        Set<String> usedNames = new HashSet<>();
        Map<String, Set<Element>> elementsForPackage = new HashMap<>();
        for (ToolElementEntry e : sorted) {
            LayerBuilder lb = super.layer(e.theAnnotatedType);
            String pkg = packageName(e.theAnnotatedType);
            String bundleName = pkg + ".ToolElements";
            String propertiesPath = bundleName.replace('.', '/') + ".properties";
            EditableProperties props = propertiesForPackage.get(propertiesPath);
            if (props == null) {
                props = new EditableProperties(true);
                propertiesForPackage.put(propertiesPath, props);
            }
            Set<Element> els = elementsForPackage.get(propertiesPath);
            if (els == null) {
                els = new HashSet<>();
                elementsForPackage.put(propertiesPath, els);
            }
            els.add(e.theAnnotatedType);
            String instanceFileName = e.instanceFileName(usedNames);
            String bundleKey = instanceFileName;
            props.put(bundleKey, e.name());
            props.setComment(bundleKey, new String[]{e.theAnnotatedType.getQualifiedName().toString()}, true);
            try {
                LayerBuilder.File file = lb.instanceFile(e.folder, instanceFileName, e.proxy, "folder");
                file.stringvalue("iconBase", e.icon());
                if (e.position != Integer.MAX_VALUE) {
                    file.intvalue("position", e.position);
                }
                file.bundlevalue("displayName", bundleName, bundleKey);
                file.stringvalue("instanceClass", e.typeName);
                file.stringvalue("instanceOf", e.typeName);
//                file.stringvalue("registeredBy", e.theAnnotatedType.getQualifiedName().toString());
//                visitTypeAndSupertypes(e.theType, name -> {
//                    if (!"java.lang.Object".equals(name)) {
//                        file.stringvalue("instanceOf", name);
//                    }
//                });
                file.write();
            } catch (Exception | Error ex) {
                ex.printStackTrace(System.err);
                onError("Exception thrown generating layer entries for " + e.theType + " on "
                        + e.theAnnotatedType.getQualifiedName() + ": " + ex);
            }
        }
        for (Map.Entry<String, EditableProperties> e : propertiesForPackage.entrySet()) {
            try {
                FileObject fo = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", e.getKey(), elementsForPackage.get(e.getKey()).toArray(new Element[0]));
                try (OutputStream out = fo.openOutputStream()) {
                    e.getValue().store(out);
                }
            } catch (Exception | Error ex) {
                ex.printStackTrace(System.err);
                onError("Properties generation failed for " + e.getKey());
            }
        }
    }

    private void processOneElement(TypeElement typeElement, Set<ToolElementEntry> entries) {
        ToolElement proxy = typeElement.getAnnotation(ToolElement.class);
        AnnotationMirror mirror = findMirror(typeElement, ToolElement.class.getName());
        if (proxy == null || mirror == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not resolve a @ToolElement annotation", typeElement);
            return;
        }
        withTypeElementAndMirror(typeElement, mirror, () -> {
            withRegisteredTypes(typeElement, mirror, (theType, typeName) -> {
                int layerPosition = proxy.position();
                if (layerPosition == Integer.MAX_VALUE) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Annotation should "
                            + " specify position() to have a deterministic sort order in the user interface", typeElement, mirror);;
                }
                String name = proxy.name();
                String icon = proxy.icon();
                if (proxy.folder() == null || proxy.folder().trim().isEmpty()) {
                    onError("No folder name specified - will not register into the root of the default filesystem");
                }
                if (isDefaultName(name)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "No name specified - munging the class name to create one - will not be localizable.",
                            typeElement, mirror);
                }
                if (isDefaultIcon(icon)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "No icon specified - using the (ugly) default", typeElement, mirror);
                }
                ToolElementEntry en = new ToolElementEntry(proxy, typeElement, theType, typeName, icon, proxy.folder(), name, proxy.position());
                entries.add(en);
            });
        });
    }

    private void withRegisteredTypes(TypeElement typeElement, AnnotationMirror mirror, BiConsumer<TypeMirror, String> c) {
        String typeToRegister = typeName(mirror, "value");
        if (typeToRegister == null) {
            typeToRegister = ANNOTATION_TYPE_NAME;
        }
        if (typeToRegister == null) {
            onError("Found null type name for 'value' in " + mirror);
            return;
        }
        TypeMirror theType;
        if (ANNOTATION_TYPE_NAME.equals(typeToRegister)) {
            typeToRegister = typeElement.getQualifiedName().toString();
            theType = typeElement.asType();
            if (!testTypeElement(typeElement)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Aborting registration of " + typeToRegister);
                return;
            }
        } else {
            theType = type(typeToRegister);
            if (theType == null) {
                onError("Could not resolve " + typeToRegister + " - classpath broken?");
                return;
            }
            TypeElement el = processingEnv.getElementUtils().getTypeElement(theType.toString());
            if (!testTypeElement(el)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Aborting registration of " + typeToRegister);
                return;
            }
        }
        c.accept(theType, typeToRegister);
    }

    private boolean testTypeElement(TypeElement typeElement) {
        boolean failed = false;
        switch (typeElement.getNestingKind()) {
            case ANONYMOUS:
            case LOCAL:
                failed = true;
                onError("Cannot register anonymous or local classes with @ToolElement - "
                        + "nesting kind of " + typeElement.getSimpleName() + " is "
                        + typeElement.getNestingKind());
                break;
            default:
                boolean publicFound = false;
                for (Modifier mod : typeElement.getModifiers()) {
                    switch (mod) {
                        case ABSTRACT:
                            failed = true;
                            onError("Cannot register an abstract class with @ToolElement");
                            break;
                        case PROTECTED:
                            failed = true;
                            onError("Cannot register a protected class with @ToolElement - must be public with a public no-argument constructor");
                            break;
                        case PUBLIC:
                            publicFound = true;
                            break;
                    }
                }
                if (!publicFound) {
                    failed = true;
                    onError("Classes provided via ");
                }
        }
        List<ExecutableElement> constructors = new ArrayList<>();
        for (Element enc : typeElement.getEnclosedElements()) {
            switch (enc.getKind()) {
                case CONSTRUCTOR:
                    if (enc instanceof ExecutableElement) {
                        constructors.add((ExecutableElement) enc);
                    } else {
                        onError("Broken source? Found a constructor that is not an ExecutableElement: " + enc);
                    }
            }
        }
        if (!constructors.isEmpty()) {
            ExecutableElement noarg = null;
            for (ExecutableElement con : constructors) {
                if (con.getParameters().isEmpty()) {
                    noarg = con;
                    break;
                }
            }
            if (noarg == null) {
                failed = true;
                onError(constructors.size() + " constructor(s) found, but no public no-argument constructor on "
                        + typeElement.getQualifiedName()
                        + " which is needed for @ToolElement");
            }
        }
        return !failed;

    }

    private TypeElement currentTypeElement;
    private AnnotationMirror currentMirror;

    private void withTypeElementAndMirror(TypeElement el, AnnotationMirror mirror, Runnable r) {
        TypeElement old = currentTypeElement;
        AnnotationMirror oldMirror = currentMirror;
        currentTypeElement = el;
        currentMirror = mirror;
        try {
            r.run();
        } finally {
            currentTypeElement = old;
            currentMirror = oldMirror;
        }
    }

    private void onError(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, currentTypeElement, currentMirror);
    }

    static class ToolElementEntry {

        private final TypeElement theAnnotatedType;
        private final TypeMirror theType;
        private final String typeName;
        private final String icon;
        private final String folder;
        private final String name;
        private String instanceFileName;
        private final int position;
        private final ToolElement proxy;

        public ToolElementEntry(ToolElement proxy, TypeElement on, TypeMirror theType, String typeName, String icon, String folder, String displayName, int position) {
            this.theAnnotatedType = on;
            this.theType = theType;
            this.typeName = typeName;
            this.icon = icon;
            this.folder = folder;
            this.name = displayName;
            this.position = position;
            this.proxy = proxy;
        }

        String instanceFileName(Set<String> used) {
            if (instanceFileName != null) {
                return instanceFileName;
            }
            String name;
            int ix = 0;
            String suffix = "";
            do {
                name = typeName.replace('.', '-') + suffix;
                ix++;
                suffix = "-" + Integer.toString(ix);
            } while (used.contains(name));
            return instanceFileName = name;
        }

        String icon() {
            return isDefaultIcon(icon) ? "net/dev/java/imagine/api/tool/unknown.png" : icon;
        }

        String name() {
            if (isDefaultName(name)) {
                String nm = Strings.escape(theAnnotatedType.getSimpleName().toString(), new Escaper() {
                    boolean lastUpperCase = false;
                    int count;

                    @Override
                    public CharSequence escape(char c) {
                        boolean up = Character.isUpperCase(c);
                        if (count++ != 0 || lastUpperCase && !up) {
                            return " " + c;
                        }
                        return "" + c;
                    }
                });
                return nm;
            }
            return name;
        }
    }

    static boolean isDefaultName(String name) {
        return name == null || "_unnamed".equals(name) || name.isEmpty();
    }

    static boolean isDefaultIcon(String icon) {
        return icon == null || "net/dev/java/imagine/api/tool/unknown.png".equals(icon);
    }

    // XXX everything below here is borrowed from com.mastfrog.annotation.AnnotationUtils, which
    // was developed years after this project and based on code originally developed for this
    // project.  At some point, all of the annotion processors in Imagine should be rewritten
    // to use that, with the result that most of their code could be deleted.  At present, we
    // do not have a dependency on that project.
    /**
     * Find the AnnotationMirror for a specific annotation type.
     *
     * @param el The annotated element
     * @param annotationTypeFqn The fully qualified name of the annotation class
     * @return An annotation mirror
     */
    public AnnotationMirror findMirror(Element el, String annotationTypeFqn) {
        for (AnnotationMirror mir : el.getAnnotationMirrors()) {
            TypeMirror type = mir.getAnnotationType().asElement().asType();
            String typeName = canonicalize(type);
            if (annotationTypeFqn.equals(typeName)) {
                return mir;
            }
        }
        return null;
    }

    public String canonicalize(TypeMirror tm) {
        Types types = processingEnv.getTypeUtils();
        Element maybeType = types.asElement(tm);
        if (maybeType == null) {
            if (tm.getKind().isPrimitive()) {
                return types.getPrimitiveType(tm.getKind()).toString();
            } else {
                return stripGenericsFromStringRepresentation(tm);
            }
        }
        if (maybeType instanceof TypeParameterElement) {
            maybeType = ((TypeParameterElement) maybeType).getGenericElement();
        }
        if (maybeType instanceof TypeElement) {
            TypeElement e = (TypeElement) maybeType;
            StringBuilder nm = new StringBuilder(e.getQualifiedName().toString());
            Element enc = e.getEnclosingElement();
            while (enc != null && enc.getKind() != ElementKind.PACKAGE) {
                int ix = nm.lastIndexOf(".");
                if (ix > 0) {
                    nm.setCharAt(ix, '$');
                }
                enc = enc.getEnclosingElement();
            }
            return nm.toString();
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Cannot canonicalize " + tm);
        return null;
    }

    /**
     * Failover stringification.
     *
     * @param m A type mirror
     * @return A string representation
     */
    private String stripGenericsFromStringRepresentation(TypeMirror m) {
        String result = m.toString();
        result = result.replaceAll("\\<.*\\>", "");
        return result;
    }

    public String typeName(AnnotationMirror mirror, String param) {
        return typeName(mirror, param, this::onError);
    }

    public String typeName(AnnotationMirror mirror, String param, Consumer<String> errMessages) {
        List<String> results = typeList(mirror, param, errMessages);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get a list of fully qualified, canonicalized Java class names for a
     * member of an annotation. Typically such classes cannnot actually be
     * loaded by the compiler, so referencing the Class objects for them results
     * in an exception at compilation time; this method produces string
     * representations of such names without relying on being able to load the
     * classes in question. Handles the case that the annotation member is not
     * an array by returning a one-element list.
     * <p>
     * <b>Note</b> - even if you are expecting a single Class object and the
     * annotation does not return an array, if the user happened to write an
     * array declaration, you will find more than one type here.
     * </p>
     * <p>
     * This overload will not fail the build if a type cannot be loaded, but
     * will pass such messages to the string consumer for deferred error
     * handling.
     * </p>
     *
     * @param mirror The annotation mirror
     * @param param The name of the annotation method you expect to return a
     * <code>Class</code> or <code>Class[]</code>
     * @param errMessages A consumer for error messages - either-or predicates
     * will not want to immediately fail the build in case the alternative
     * branch succeeds (AbstractPredicateBuilder has support for holding these
     * in a ThreadLocal and dumping them out in the case of failure)
     *
     * @param failIfNotSubclassesOf If non-empty, fail if the resulting type is
     * not a subtype of one of the passed type names.
     *
     * @return The list of type names for an annotation mirror parameter which
     * returns <code>Class</code> or <code>Class[]</code>
     */
    public List<String> typeList(AnnotationMirror mirror, String param, Consumer<String> errMessages) {
        List<String> result = new ArrayList<>();
        if (mirror != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> x : mirror.getElementValues()
                    .entrySet()) {
                String annoParam = x.getKey().getSimpleName().toString();
                if (param.equals(annoParam)) {
                    if (x.getValue().getValue() instanceof List<?>) {
                        List<?> list = (List<?>) x.getValue().getValue();
                        for (Object o : list) {
                            if (o instanceof AnnotationValue) {
                                AnnotationValue av = (AnnotationValue) o;
                                if (av.getValue() instanceof DeclaredType) {
                                    DeclaredType dt = (DeclaredType) av.getValue();
                                    // Convert e.g. mypackage.Foo.Bar.Baz to mypackage.Foo$Bar$Baz
                                    String canonical = canonicalize(dt.asElement().asType());
                                    if (canonical == null) {
                                        // Unresolvable generic or broken source
                                        errMessages.accept("Could not canonicalize " + dt.asElement());
                                    } else {
                                        result.add(canonical);
                                    }
                                } else {
                                    // Unresolvable type?
                                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Not a declared type: " + av);
                                }
                            } else {
                                // Probable invalid source
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Annotation value for value() is not an AnnotationValue " + o);
                            }
                        }
                    } else if (x.getValue().getValue() instanceof DeclaredType) {
                        DeclaredType dt = (DeclaredType) x.getValue().getValue();
                        // Convert e.g. mypackage.Foo.Bar.Baz to mypackage.Foo$Bar$Baz
                        String canonical = canonicalize(dt.asElement().asType());
                        if (canonical == null) {
                            // Unresolvable generic or broken source
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not canonicalize " + dt, x.getKey());
                        } else {
                            result.add(canonical);
                        }
                    } else {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation value for is not a List of types or a DeclaredType on " + mirror + " - "
                                + x.getValue().getValue() + " - invalid source?");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get a TypeMirror for a fully qualified class name.
     *
     * @param what A class name
     * @return A TypeMirror or null if no such type can be resolved on the
     * classpath
     */
    public TypeMirror type(String what) {
        if (what == null) {
            throw new IllegalArgumentException("Requesting null type");
        }
        TypeElement te = processingEnv.getElementUtils().getTypeElement(what);
        if (te == null) {
            return null;
        }
        return te.asType();
    }

    public String packageName(Element el) {
        PackageElement pkg = processingEnv.getElementUtils().getPackageOf(el);
        return pkg == null ? null : pkg.getQualifiedName().toString();
    }
}
