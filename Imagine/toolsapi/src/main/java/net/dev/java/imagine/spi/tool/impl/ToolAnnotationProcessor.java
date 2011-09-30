package net.dev.java.imagine.spi.tool.impl;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;
import net.dev.java.imagine.api.tool.Category;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.spi.tool.ToolDriver;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import org.openide.filesystems.annotations.LayerBuilder;
import org.openide.filesystems.annotations.LayerGeneratingProcessor;
import org.openide.filesystems.annotations.LayerGenerationException;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Parameters;
import org.openide.util.lookup.ServiceProvider;

/**
 * Handles Tool and ToolDef annotations, generating layer entries, etc.
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes("org.demo.action.annotation.Action")
@SupportedSourceVersion(SourceVersion.RELEASE_5)
public class ToolAnnotationProcessor extends LayerGeneratingProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> hash = new HashSet<String>();
        hash.add(Tool.class.getCanonicalName());
        hash.add(ToolDef.class.getCanonicalName());
        return hash;
    }

    /**
     * Allows us to fetch type arguments for a subclass of ToolImplementation
     */
    private class TV extends SimpleTypeVisitor6<List<? extends TypeMirror>, TypeMirror> {

        @Override
        public List<? extends TypeMirror> visitDeclared(DeclaredType t, TypeMirror p) {
            return t.getTypeArguments();
        }
    }

    /**
     * Encapsulates the list of known types a ToolImplementation or &064;Tool
     * annotated class implements at least some of.
     */
    class ImplementedTypes {

        private List<TypeMirror> allTypes = new ArrayList<TypeMirror>();
        private List<TypeMirror> types = new ArrayList<TypeMirror>();
        private final TypeElement type;

        ImplementedTypes(TypeElement el) {
            this.type = el;
            final TypeElement mouseListener = processingEnv.getElementUtils().getTypeElement(MouseListener.class.getCanonicalName());
            final TypeElement mouseMotionListener = processingEnv.getElementUtils().getTypeElement(MouseMotionListener.class.getCanonicalName());
            final TypeElement mouseWheelListener = processingEnv.getElementUtils().getTypeElement(MouseWheelListener.class.getCanonicalName());
            final TypeElement keyListener = processingEnv.getElementUtils().getTypeElement(KeyListener.class.getCanonicalName());
            final TypeElement toolImplementation = processingEnv.getElementUtils().getTypeElement(KeyListener.class.getCanonicalName());
            //this last will be null if visual library not on the classpath
            final TypeElement widgetAction = processingEnv.getElementUtils().getTypeElement("org.netbeans.api.visual.action.WidgetAction");
            for (TypeElement te : new TypeElement[]{mouseListener, mouseMotionListener, mouseWheelListener, keyListener, toolImplementation, widgetAction}) {
                if (te != null) {
                    TypeMirror tm = te.asType();
                    allTypes.add(tm);
                    if (processingEnv.getTypeUtils().isSubtype(el.asType(), te.asType())) {
                        types.add(te.asType());
                    } else {
                        for (TypeMirror s : processingEnv.getTypeUtils().directSupertypes(tm)) {
                            if (processingEnv.getTypeUtils().isSubtype(el.asType(), s)) {
                                types.add(tm);
                            }
                        }
                    }
                }
            }
        }

        public List<? extends TypeMirror> getToolImplTypeParameters() {
            //Get the non-generics type name of ToolImplementation
            TypeMirror basicToolImplType = processingEnv.getTypeUtils().erasure(processingEnv.getElementUtils().getTypeElement(ToolImplementation.class.getName()).asType());

            //Iterate all the superclasses/interfaces of our class
            for (TypeMirror sup : processingEnv.getTypeUtils().directSupertypes(type.asType())) {
                //If it subclasses ToolImplementation
                if (processingEnv.getTypeUtils().erasure(sup).equals(basicToolImplType)) {
                    //Go look for the type parameters
                    TV tv = new TV();
                    List<? extends TypeMirror> v = sup.accept(tv, sup);
                    return v;
                }
            }
            //Was not a subclass of ToolImplementation, just a plain old mouse listener or similar
            return null;
        }

        public List<TypeMirror> getTypes() {
            return types;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (TypeMirror m : types) {
                sb.append(m);
                sb.append(", ");
            }
            return sb.toString();
        }
    }
    
    @Override
    protected boolean handleProcess(Set<? extends TypeElement> set, RoundEnvironment env) throws LayerGenerationException {
        boolean result = true;
        //All of the tool definitions
        Map<String, ToolDefinitionEntry> definitions = new HashMap<String, ToolDefinitionEntry>();
        //All of the ToolImplementations, mapped by name
        Map<String, List<ToolImplEntry>> tools = new HashMap<String, List<ToolImplEntry>>();

        //Find all the ToolDefs which define tools, their icons, display names, etc.
        for (Element e : env.getElementsAnnotatedWith(ToolDef.class)) {
            TypeElement clazz = (TypeElement) e;
            ToolDef def = e.getAnnotation(ToolDef.class);
            //Don't allow duplicates, and give sane warnings
            if (definitions.containsKey(def.name())) {
                result = false;
                ToolDefinitionEntry other = definitions.get(def.name());
                processingEnv.getMessager().printMessage(Kind.ERROR, "A tool named " + def.name() + " is also defined on " + other.appearsOn().asType().toString(), e);
                processingEnv.getMessager().printMessage(Kind.ERROR, "A tool named " + def.name() + " is also defined on " + e.asType().toString(), other.appearsOn());
            }
            //Record the entry
            ToolDefinitionEntry td = new AnnotationToolDefinitionEntry(def, clazz);
            definitions.put(td.name(), td);
        }
        //Now find all the tool *implementations* - there can be multiple implementations
        //of the same tool that work against different layer lookup contents
        for (Element e : env.getElementsAnnotatedWith(net.dev.java.imagine.spi.tool.Tool.class)) {
            TypeElement clazz = (TypeElement) e;
            ToolImplEntry tl = new ToolImplEntry(e.getAnnotation(net.dev.java.imagine.spi.tool.Tool.class), clazz);
            List<ToolImplEntry> tls = tools.get(tl.name());
            if (tls == null) {
                tls = new LinkedList<ToolImplEntry>();
                tools.put(tl.name(), tls);
            }
            tls.add(tl);
        }
        //See if we have any orphan @Tool annotated classes which have no
        //corresponding definition.  For ease of development, we will generate
        //a dummy definition to make it easy to test them
        for (Map.Entry<String, List<ToolImplEntry>> e : tools.entrySet()) {
            if (!definitions.containsKey(e.getKey())) {
                //We have a @Tool annotation, but no ToolDef for it.  Invent one
                //with an ugly default icon, for development purposes
                SyntheticToolDefinitionEntry synth = new SyntheticToolDefinitionEntry(e.getValue().iterator().next());
                definitions.put(e.getKey(), synth);
            }
        }
        Set<Element> allElements = new HashSet<Element>();
        //Force full validation of all the tool definitions, generating warnings
        //and source annotations
        for (ToolDefinitionEntry td : definitions.values()) {
            allElements.add(td.appearsOn());
            result &= td.validate();
        }
        //Validate and check for duplication among all tool implementations
        for (List<ToolImplEntry> tllist : tools.values()) {
            for (ToolImplEntry tl : tllist) {
                allElements.add(tl.appearsOn());
                result &= tl.validate();
                for (List<ToolImplEntry> tllist1 : tools.values()) {
                    for (ToolImplEntry tl1 : tllist1) {
                        if (tl1 != tl) {
                            if (tl1.name().equals(tl.name())) {
                                TypeMirror a = tl.sensitiveToType();
                                TypeMirror b = tl1.sensitiveToType();
                                if (a.equals(b)) {
                                    processingEnv.getMessager().printMessage(Kind.ERROR,
                                            tl.className() + " and " + tl1.className()
                                            + " represent the tool " + tl.name() + ". Both"
                                            + " are sensitive to " + a + ". Which one would be "
                                            + "used is ambiguous.", tl.appearsOn());
                                    result = false;
                                }
                            }
                        }
                    }
                }
            }
        }

        //Now we're ready to build the layer contents
        if (result) {
            String defaultBundle = Category.class.getPackage().getName().replace('.', '/') + "/Bundle";
            
            LayerBuilder b = layer(allElements.toArray(new Element[allElements.size()]));
            LayerBuilder.File actionsFolder = b.folder("Actions");
            b = actionsFolder.write(); //make sure it exists
            //A folder for all image actions
            LayerBuilder.File imageActions = b.folder(actionsFolder.getPath() + "/ToolActions");
            imageActions.bundlevalue("displayName", defaultBundle, "ToolActions");
            b = imageActions.write();

            LayerBuilder.File menuFolder = b.folder("Menu");
            b = menuFolder.write(); //make sure it exists

            //A folder for shadow files pointing to actions in a menu
            LayerBuilder.File toolsMenu = b.folder(menuFolder.getPath() + "/ToolActions");
            toolsMenu.bundlevalue("displayName", defaultBundle, "ToolActions");
            b = toolsMenu.write();

            LayerBuilder.File toolbarsFolder = b.folder("Toolbars");
            b = toolbarsFolder.write(); //make sure it exists

            //A folder for shadow files pointing to actions in a toolbar
            LayerBuilder.File toolsToolbar = b.folder(toolbarsFolder.getPath() + "/ToolActions");
            toolsToolbar.bundlevalue("displayName", defaultBundle, "ToolActions");
            b = toolsToolbar.write();

            //Now build entries for all tool definitions and impls
            for (ToolDefinitionEntry td : definitions.values()) {
                //First, put a Tool definition in tools/ - this will be an instance
                //file that generates a ToolDriver, which reads some attributes from
                //the FileObject to find the actual implementation class
                String toolFolderPath = Tool.TOOLS_PATH + td.name();
                //Each tool is a folder containing multiple .instance files for
                //tool implementations
                LayerBuilder.File oneToolFolder = b.folder(toolFolderPath);
                //If we have a localized display name, use it
                String bundle = td.displayNameBundle();
                if (bundle != null) {
                    oneToolFolder.bundlevalue("displayName", bundle, td.name());
                }
                //There will always be an icon value, it just may be our dummy icon
                oneToolFolder.urlvalue("SystemFileSystem.icon", "nbresloc:/" + td.iconPath());
                oneToolFolder.stringvalue("category", td.category());

                //PENDING: Need to consistently handle position attributes
                if (td.position() != -1) {
                    oneToolFolder.intvalue("position", td.position());
                }
                if (!ToolDef.DEFAULT_HELP_CTX.equals(td.getHelpCtx())) {
                    oneToolFolder.stringvalue("helpID", td.getHelpCtx());
                }
                List<ToolImplEntry> impls = tools.get(td.name());
                //If no implementations, warn the user.  They could exist in some
                //other module, so it is not fatal
                if (impls == null || impls.isEmpty()) {
                    processingEnv.getMessager().printMessage(Kind.NOTE, td.name() + " defined, but no @Tool implementations found using this name.", td.appearsOn());
                }
                b = oneToolFolder.write();
                //Write out .instance files for every implementation
                for (ToolImplEntry tl : impls) {
                    String implFileName = toolFolderPath + '/' + tl.layerFileName();
                    LayerBuilder.File implFile = b.file(implFileName);
                    //standard netbeans boilerplate
                    implFile.stringvalue("instanceClass", ToolDriver.class.getName());
                    implFile.methodvalue("instanceCreate", ToolDriver.class.getName(), "create");
                    for (TypeMirror tm : tl.types.getTypes()) {
                        implFile.stringvalue("instanceOf", tm.toString());
                    }
                    implFile.stringvalue(ToolDriver.TYPE_ATTRIBUTE, tl.className());
                    implFile.stringvalue(ToolDriver.SENSITIVE_TO_ATTRIBUTE, tl.sensitiveToTypeName());
                    b = implFile.write();
                }
                boolean isDefaultCategory = ToolDef.DEFAULT_CATEGORY.equals(td.category());

                //Make an action file in the global actions pool folder
                LayerBuilder.File actionFile = b.file(imageActions.getPath() + "/" + td.name() + ".instance");
                actionFile.stringvalue("instanceClass", ToolAction.class.getName());
                StringBuilder types = new StringBuilder();
                for (Iterator<Class<?>> it=toolActionTypes().iterator(); it.hasNext();) {
                    types.append(it.next().getName());
                    if (it.hasNext()) {
                        types.append(',');
                    }
                }
                actionFile.stringvalue("instanceOf", types.toString());
                actionFile.methodvalue("instanceCreate", ToolAction.class.getName(), "create");
//                actionFile.boolvalue("noIconInMenu", true);
                actionFile.stringvalue("SystemFileSystem.icon", "nbresloc:/" + td.iconPath());
                actionFile.stringvalue(ToolAction.TOOL_NAME_ATTRIBUTE, td.name());

                if (bundle != null) {
                    actionFile.bundlevalue("displayName", bundle, td.name());
                }
                b = actionFile.write();
                //create links in toolbar and menu folders
                LayerBuilder.File menuShadow =
                        b.shadowFile(actionFile.getPath(), toolsMenu.getPath(), td.name());
                b = menuShadow.write();

                LayerBuilder.File toolbarShadow =
                        b.shadowFile(actionFile.getPath(), toolsToolbar.getPath(), td.name());
                b = toolbarShadow.write();
            }
        }

        return result;
    }
    
    private static Set<Class<?>> toolActionTypes() {
        Set<Class<?>> result = new HashSet<Class<?>>(Arrays.asList(ToolAction.class.getInterfaces()));
        Class<?> c = ToolAction.class;
        while (c != Object.class) {
            result.add(c);
            c = c.getSuperclass();
        }
        return result;
    }

    private class ToolImplEntry {

        private final net.dev.java.imagine.spi.tool.Tool anno;
        private boolean nameWarned;
        private final ImplementedTypes types;

        public ToolImplEntry(net.dev.java.imagine.spi.tool.Tool anno, TypeElement appearsOn) {
            types = new ImplementedTypes(appearsOn);
            this.anno = anno;
        }

        public TypeElement appearsOn() {
            return types.type;
        }

        public ImplementedTypes getTypes() {
            return types;
        }

        public List<? extends TypeMirror> getToolImplementationTypeParameters() {
            return types.getToolImplTypeParameters();
        }

        public String className() {
            return canonicalize(appearsOn());
        }

        public String sensitiveToTypeName() {
            return canonicalize(sensitiveTo());
        }

        /**
         * TypeElement uses . as the delimiter for inner classes;  we need $
         * @param e A type element
         * @return The name in a form suitable for Class.forName()
         */
        private String canonicalize(TypeElement e) {
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

        public String name() {
            if (ToolDef.DEFAULT_NAME.equals(anno.name())) {
                //If colocated with a non-default-named @ToolDef annotation, use the name for that
                ToolDef td = appearsOn().getAnnotation(ToolDef.class);
                if (td != null) {
                    String nm = td.name();
                    if (!ToolDef.DEFAULT_NAME.equals(nm)) {
                        return nm;
                    }
                }
                PackageElement pkg = processingEnv.getElementUtils().getPackageOf(appearsOn());
                if (pkg == null || pkg.isUnnamed()) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Tools may not be in the default package");
                }
                String result = pkg.getQualifiedName().toString().replace('.', '-');
                if (!nameWarned) {
                    processingEnv.getMessager().printMessage(Kind.NOTE, "Since no name specified, "
                            + "tool name will be package name:" + result, appearsOn());
                    nameWarned = true;
                }
                return result;
            } else {
                return anno.name();
            }
        }

        public TypeMirror sensitiveToType() {
            TypeElement te = sensitiveTo();
            return te == null ? null : te.asType();
        }

        public TypeElement sensitiveTo() {
            TypeElement result = processingEnv.getElementUtils().getTypeElement(findClassValue(appearsOn(), "value").toString());
            if ("java.lang.Object".equals(result.asType().toString())) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Type must be more specific than Object", appearsOn());
            }
            return result;
        }

        public String toString() {
            return name() + " on " + appearsOn() + " sensitive to " + sensitiveTo() + " implements " + types;
        }

        public ExecutableElement getConstructor() {
            //Find a constructor which takes the right type
            ExecutableElement foundConstructor = null;
            String sensitiveTo = findClassValue(appearsOn(), "value").toString();
            for (Element member : appearsOn().getEnclosedElements()) {
                if (member.getKind() == ElementKind.CONSTRUCTOR) {
                    ExecutableElement cn = (ExecutableElement) member;
                    Set<Modifier> mods = cn.getModifiers();
                    if (mods.contains(Modifier.PUBLIC)) {
                        List<? extends VariableElement> params = cn.getParameters();
                        if (params.size() == 1) {
                            VariableElement param = params.get(0);
                            TypeMirror paramType = param.asType();
                            if (paramType.toString().equals(sensitiveTo)) {
                                foundConstructor = cn;
                                break;
                            }
                        }
                    }
                }
            }
            return foundConstructor;
        }

        private boolean validate() {
            //check things ans issue warnings
            TypeElement sens = sensitiveTo();
            boolean result = true;
            //mangled annotation?
            if (sens == null) {
                processingEnv.getMessager().printMessage(Kind.ERROR,
                        "Cannot find sensitive to", appearsOn());
                result = false;

            }
            ExecutableElement ctor = getConstructor();
            //make sure there's a 1-arg constructor
            if (ctor == null) {
                String sensitiveTo = findClassValue(appearsOn(), "value").toString();
                processingEnv.getMessager().printMessage(Kind.ERROR,
                        "Must have a public, 1-argument "
                        + "constructor that takes a " + sensitiveTo, appearsOn());
                result = false;
            }
            sensitiveTo(); //trigger warnings
            name(); //trigger warnings
            List<? extends TypeMirror> paramTypes = getToolImplementationTypeParameters();
            //fail on abstract classes
            if (appearsOn().getModifiers().contains(Modifier.ABSTRACT)) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Cannot annotate an abstract class with @Tool", appearsOn());
                result = false;
            }

            //If a ToolImplementation subclass, make sure it's parameterized on the
            //same thing as the annotation describes
            if (paramTypes != null) { //It is a ToolImplementation subclass
                if (paramTypes.size() == 1) {
                    TypeMirror tm = paramTypes.iterator().next();
                    if (!tm.equals(sensitiveTo().asType())) {
                        processingEnv.getMessager().printMessage(Kind.ERROR,
                                "Must subclass ToolImplementation<" + sensitiveTo().asType().toString() + ">", appearsOn());
                        result = false;
                    }
                } else if (paramTypes.size() > 1) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Should have only one parameter", appearsOn());
                    result = false;
                } else if (paramTypes.isEmpty()) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Unparameterized ToolImplementation subclasses not allowed", appearsOn());
                    result = false;
                }
            }
            //Make sure we implement at least something useful
            if (types.types.isEmpty()) {
                StringBuilder sb = new StringBuilder("Must implement at least one of ");
                for (Iterator<TypeMirror> it = types.allTypes.iterator(); it.hasNext();) {
                    sb.append(it.next());
                    if (it.hasNext()) {
                        sb.append(", ");
                    }
                }
                processingEnv.getMessager().printMessage(Kind.ERROR, sb, appearsOn());
                result = false;
            }
            //block classes that can be instantiated without an enclosing scope
            switch (appearsOn().getNestingKind()) {
                case ANONYMOUS:
                case LOCAL:
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Tool annotation cannot be used on non-static inner classes", appearsOn());
                    result = false;
                    break;
                case MEMBER:
                    if (!appearsOn().getModifiers().contains(Modifier.STATIC)) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Tool annotation cannot be used on non-static inner classes", appearsOn());
                        result = false;
                    }
                    break;
                case TOP_LEVEL:
                    break;
            }
            return result;
        }

        private String layerFileName() {
            String cn = className();
            return cn.replace('.', '-') + ".instance";
        }
    }

    private interface ToolDefinitionEntry {

        TypeElement appearsOn();

        String iconPath();

        String getHelpCtx();

        String displayNameBundle();

        String category();

        String name();

        boolean validate();

        int position();
    }

    private class SyntheticToolDefinitionEntry implements ToolDefinitionEntry {

        private final ToolImplEntry name;

        SyntheticToolDefinitionEntry(ToolImplEntry name) {
            Parameters.notNull("name", name);
            this.name = name;
        }

        public int position() {
            return -1;
        }

        @Override
        public String iconPath() {
            return ToolDef.DEFAULT_ICON_PATH;
        }

        @Override
        public String getHelpCtx() {
            return ToolDef.DEFAULT_HELP_CTX;
        }

        @Override
        public String displayNameBundle() {
            return null;
        }

        @Override
        public String category() {
            return ToolDef.DEFAULT_CATEGORY;
        }

        @Override
        public String name() {
            return name.name();
        }

        public String toString() {
            return "Synthetic tool " + name();
        }

        public boolean validate() {
            return name.validate();
        }

        @Override
        public TypeElement appearsOn() {
            return name.appearsOn();
        }
    }

    private class AnnotationToolDefinitionEntry implements ToolDefinitionEntry {

        private final ToolDef def;
        private final TypeElement appearsOn;

        public AnnotationToolDefinitionEntry(ToolDef def, TypeElement appearsOn) {
            this.def = def;
            this.appearsOn = appearsOn;
        }
        private boolean iconWarned;

        public String iconPath() {
            String result = def.iconPath();
            if (!iconWarned && ToolDef.DEFAULT_ICON_PATH.equals(result)) {
                processingEnv.getMessager().printMessage(Kind.WARNING, "No icon specified, using default (ugly) icon for " + name(), appearsOn());
                iconWarned = true;
            } else if (!iconWarned) {
                try {
                    boolean found = false;
                    for (StandardLocation l : StandardLocation.values()) {
                        try {
                            processingEnv.getFiler().getResource(l, "", result);
                            found = true;
                            break;
                        } catch (IOException ex) {
                            continue;
                        } catch (IllegalArgumentException x) {
                            processingEnv.getMessager().printMessage(Kind.ERROR, "Problem with " + result + " (should be resource path with no leading slash) - " + x.getMessage(), appearsOn());
                        }
                    }
                    if (!found) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "No such icon file " + result, appearsOn());
                    }
                } finally {
                    iconWarned = true;
                }
            }
            return result;
        }

        public int position() {
            return def.position();
        }

        public String getHelpCtx() {
            return def.getHelpCtx();
        }
        private boolean bundleWarned;

        PackageElement getPackage() {
            return processingEnv.getElementUtils().getPackageOf(appearsOn);
        }

        public String displayNameBundle() {
            //XXX handle class resource bundles
            String result = def.displayNameBundle();
            if (ToolDef.DEFAULT_BUNDLE.equals(result)) {
                PackageElement pkg = getPackage();
                if (pkg == null || pkg.isUnnamed()) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Tools may not be in the default package");
                }
                result = pkg.getQualifiedName().toString().replace('.', '/') + "/Bundle";
                if (!bundleWarned) {
                    bundleWarned = true;
                    verifyBundleKey(result, name(), true);
                }
            } else {
                if (!bundleWarned) {
                    bundleWarned = true;
                    verifyBundleKey(result, name(), true);
                }
            }
            return result;
        }
        private boolean categoryWarned;
        
        private void verifyBundleKey(String bundle, String key, boolean samePackage) {
            if (processingEnv == null) {
                return;
            }
            if (samePackage) {
                for (Element e = appearsOn(); e != null; e = e.getEnclosingElement()) {
                    Messages m = e.getAnnotation(Messages.class);
                    if (m != null) {
                        for (String kv : m.value()) {
                            if (kv.startsWith(key + "=")) {
                                return;
                            }
                        }
                    }
                }
            }
            String resource = bundle.replace('.', '/') + ".properties";
            try {
                InputStream is;
                try {
                    is = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, "", resource).openInputStream();
                } catch (FileNotFoundException x) { // #181355
                    try {
                        is = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", resource).openInputStream();
                    } catch (IOException x2) {
                        throw x;
                    }
                }
                try {
                    Properties p = new Properties();
                    p.load(is);
                    if (p.getProperty(key) == null) {
                        processingEnv.getMessager().printMessage(Kind.WARNING, "No key '" + key + "' found in " + resource, appearsOn());
                    }
                } finally {
                    is.close();
                }
            } catch (IOException x) {
                processingEnv.getMessager().printMessage(Kind.WARNING, "Could not open " + resource + ": " + x, appearsOn());
            }
        }        

        public String category() {
            if (!categoryWarned) {
                try {
                    String bundle = displayNameBundle();
                    if (bundle != null) {
                        verifyBundleKey(bundle, def.category(), true);
                    }
                } finally {
                    categoryWarned = true;
                }
            }
            return def.category();
        }
        private boolean nameWarned;

        public String name() {
            //use the munged package name if unspecified
            if (ToolDef.DEFAULT_NAME.equals(def.name())) {
                //If colocated with a non-default-named @Tool annotation, use the name for that
                net.dev.java.imagine.spi.tool.Tool tl = appearsOn.getAnnotation(net.dev.java.imagine.spi.tool.Tool.class);
                if (tl != null) {
                    String nm = tl.name();
                    if (!ToolDef.DEFAULT_NAME.equals(nm)) {
                        return nm;
                    }
                }
                PackageElement pkg = processingEnv.getElementUtils().getPackageOf(appearsOn);
                if (pkg == null || pkg.isUnnamed()) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Tools may not be in the default package");
                }
                String result = pkg.getQualifiedName().toString().replace('.', '_');
                if (!nameWarned) {
                    processingEnv.getMessager().printMessage(Kind.NOTE, "Since no name specified, " + result + " will be used", appearsOn);
                    nameWarned = true;
                }
                return result;
            } else {
                return def.name();
            }
        }

        public boolean equals(Object o) {
            return o instanceof ToolDefinitionEntry && ((ToolDefinitionEntry) o).name().equals(name());
        }

        public int hashCode() {
            return name().hashCode();
        }

        public String toString() {
            return name() + " appearing on " + appearsOn.getQualifiedName();
        }

        @Override
        public boolean validate() {
            name();
            iconPath();
            category();
            return true;
        }

        @Override
        public TypeElement appearsOn() {
            return appearsOn;
        }
    }

    DeclaredType findClassValue(Element item, String annotationMemberName) {
        //Annotations will explode if we call methods which return a Class object,
        //so rattle through them and look up the class type in a javac-friendly way
        DeclaredType result = null;
        outer:
        for (AnnotationMirror a : item.getAnnotationMirrors()) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : a.getElementValues().entrySet()) {
                if (e.getKey().getSimpleName().contentEquals(annotationMemberName) && e.getValue().getValue() instanceof DeclaredType) {
                    DeclaredType dt = (DeclaredType) e.getValue().getValue();
                    result = dt;
                    break outer;
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
}
