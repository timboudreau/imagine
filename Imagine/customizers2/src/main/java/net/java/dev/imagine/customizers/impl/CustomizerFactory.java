package net.java.dev.imagine.customizers.impl;

import java.awt.Component;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import net.java.dev.imagine.api.customizers.visualizer.ColumnDataScene;
import net.java.dev.imagine.api.properties.Properties;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.spi.customizers.Customizable;
import net.java.dev.imagine.spi.customizers.Customizable.WidgetCustomizable;
import org.netbeans.api.visual.widget.ComponentWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class CustomizerFactory<T, R> {

    public abstract Component createCustomizer(R toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    public abstract Widget createCustomizerWidget(Scene scene, R toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    public static <T extends Property<R>, R> CustomizerFactory<?, T> fromFilePropertyObject(FileObject fo, T p) throws ClassNotFoundException {
        String className = fo.getNameExt();
        String customizesName = fo.getParent().getNameExt();
        ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
        Class<?> compType = cl.loadClass(className);
        Class<?> customizes = cl.loadClass(customizesName);
        boolean widget = Boolean.TRUE.equals(fo.getAttribute("widget"));
        return new ForProperty(compType, customizes, widget, p);
    }

    public static <T> CustomizerFactory<?, T> fromFileObject(FileObject fo) throws ClassNotFoundException {
        String className = fo.getNameExt();
        String customizesName = fo.getParent().getNameExt();
        System.err.println("FromFileObject: " + fo.getPath() + " - ClassName " + className + " customizesName " + customizesName);
        ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
        Class<?> compType = cl.loadClass(className);
        Class<?> customizes = cl.loadClass(customizesName);
        boolean widget = Boolean.TRUE.equals(fo.getAttribute("widget"));
        return new ByType(compType, customizes, widget);
    }

    public static <T> CustomizerFactory<?, T> find(final T o, final boolean preferWidget) throws ClassNotFoundException {
        if (o instanceof Customizable) {
            Customizable cc = (Customizable) o;
            return new CustomizableCF(cc);
        } else if (o instanceof Customizable.WidgetCustomizable) {
            Customizable.WidgetCustomizable wc = (Customizable.WidgetCustomizable) o;
            return new CustomizableWC(wc);
        }
        CustomizerFactory<?, T> result = null;
        if (o != null) {
            if (o instanceof Properties) {
                Properties p = (Properties) o;
                ByList b = new ByList();
                for (Property<?> prop : p) {
                    b.add(prop);
                }
                if (!b.isEmpty()) {
                    result = b;
                    return result;
                }
            } else if (o instanceof Property) {
                final Property<?> property = (Property<?>) o;
                result = (new TypeVisitor<CustomizerFactory<?, T>>() {

                    private final FileObject parent = FileUtil.getConfigFile("propertyCustomizers/");

                    @Override
                    public CustomizerFactory<?, T> visit(Class<?> type) {
                        FileObject dir = parent.getFileObject(property.type().getName());
                        if (dir != null) {
                            FileObject first = null;
                            for (FileObject ch : dir.getChildren()) {
                                if (first == null) {
                                    first = ch;
                                }
                                if (Boolean.TRUE.equals(dir.getAttribute("widget")) == preferWidget) {
                                    first = ch;
                                    break;
                                }
                            }
                            if (first != null) {
                                try {
                                    CustomizerFactory<?, ?> res = fromFilePropertyObject(first, property);
                                    return (CustomizerFactory<?, T>) res;
                                } catch (ClassNotFoundException ex) {
                                    Exceptions.printStackTrace(ex);
                                }
                            }
                        }
                        return null;
                    }
                }).visitTypesOf(o);
            } else {
                result = (CustomizerFactory<?, T>) find(o.getClass(), preferWidget);
            }
        }
        return result;
    }

    public static CustomizerFactory<?, ?> find(Class<?> clazz, final boolean preferWidget) throws ClassNotFoundException {
        Class<?> type = clazz;
        CustomizerFactory<?, ?> result = (new TypeVisitor<CustomizerFactory<?, ?>>() {

            private final FileObject parent = FileUtil.getConfigFile("customizers2/");

            @Override
            protected CustomizerFactory<?, ?> visit(Class<?> type) {
                FileObject fo = parent.getFileObject(type.getName());
                if (fo != null) {
                    FileObject first = null;
                    for (FileObject ch : fo.getChildren()) {
                        if (first == null) {
                            first = ch;
                        }
                        if (Boolean.TRUE.equals(fo.getAttribute("widget")) == preferWidget) {
                            try {
                                return fromFileObject(fo);
                            } catch (ClassNotFoundException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    }
                    if (first != null) {
                        try {
                            return fromFileObject(first);
                        } catch (ClassNotFoundException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
                return null;
            }
        }).visitTypes(type);
        return result;
    }

    private static final class ForProperty<T, R extends Property<Q>, Q> extends CustomizerFactory<T, R> {

        private final Class<T> type;
        private final Class<R> customizes;
        private final boolean widget;
        private final Property<R> property;

        ForProperty(Class<T> type, Class<R> customizes, boolean widget, Property<R> p) {
            this.type = type;
            this.customizes = customizes;
            this.widget = widget;
            assert Component.class.isAssignableFrom(type) == !widget;
            this.property = p;
        }

        @Override
        public Component createCustomizer(R toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            assert customizes.isInstance(toCustomize);
            if (widget) {
                ColumnDataScene s = new ColumnDataScene();
                Widget w = createCustomizerWidget(s, toCustomize);
                s.addChild(w);
                w.setLayout(s.getColumns().createLayout());
                return s.createView();
            } else {
                Constructor<T> c;
                c = type.getConstructor(Property.class);
                T result = c.newInstance(toCustomize);
                return (Component) result;
            }
        }

        @Override
        public Widget createCustomizerWidget(Scene scene, R toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if (!widget) {
                Component comp = createCustomizer(toCustomize);
                return new ComponentWidget(scene, comp);
            } else {
                if (scene == null) {
                    scene = new ColumnDataScene();
                }
                Constructor<T> c;
                try {
                    c = type.getConstructor(Scene.class, Property.class);
                } catch (NoSuchMethodException e) {
                    c = type.getConstructor(ColumnDataScene.class, Property.class);
                }
                Widget result = (Widget) c.newInstance(toCustomize);
                if (scene instanceof ColumnDataScene) {
                    result.setLayout(((ColumnDataScene) scene).getColumns().createLayout());
                }
                scene.addChild(result);
                return result;
            }
        }
    }

    private static final class ByType<T, R> extends CustomizerFactory<T, R> {

        private final Class<T> type;
        private final Class<R> customizes;
        private final boolean widget;

        ByType(Class<T> type, Class<R> customizes, boolean widget) {
            this.type = type;
            this.customizes = customizes;
            this.widget = widget;
            assert Component.class.isAssignableFrom(type) == !widget;
        }

        @Override
        public Component createCustomizer(R toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if (Component.class.isAssignableFrom(type)) {
                Constructor<T> c = type.getConstructor(customizes);
                return (Component) c.newInstance(toCustomize);
            } else {
                if (!Widget.class.isAssignableFrom(type)) {
                    throw new AssertionError("Cant make a widget or component from " + type);
                }
                Widget w = createCustomizerWidget(null, toCustomize);
                Scene s = w.getScene();
                JComponent view = s.getView();
                if (view == null) {
                    view = s.createView();
                }
                return view;
            }
        }

        @Override
        public Widget createCustomizerWidget(Scene scene, R toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if (scene == null) {
                scene = new ColumnDataScene();
            }
            if (!widget) {
                Component result = createCustomizer(toCustomize);
                return new ComponentWidget(scene, result);
            }
            Constructor<T> c;
            try {
                c = type.getConstructor(ColumnDataScene.class, customizes);
            } catch (NoSuchMethodException e) {
                c = type.getConstructor(Scene.class, customizes);
            }
            Widget result = (Widget) c.newInstance(scene, toCustomize);
            if (scene instanceof ColumnDataScene) {
                result.setLayout(((ColumnDataScene) scene).getColumns().createLayout());
            }
            return result;
        }
    }

    private static class ByList<T, R extends Properties> extends CustomizerFactory<T, R> {

        private final List<Entry<?, ?>> all = new ArrayList<Entry<?, ?>>();

         <T extends Property<R>, R> void add(T prop) {
            try {
                CustomizerFactory<?, T> cf = (CustomizerFactory<?, T>) find(prop, true);
                Entry<T, R> e = new Entry<T, R>(prop, cf);
                all.add(e);
            } catch (ClassNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public Component createCustomizer(R toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            ColumnDataScene scene = new ColumnDataScene();
            Widget w = createCustomizerWidget(scene, toCustomize);
            return scene.createView();
        }

        @Override
        public Widget createCustomizerWidget(Scene scene, R toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if (scene == null) {
                scene = new ColumnDataScene();
            }
            for (Entry<?, ?> e : all) {
                Widget w = e.get(scene);
                if (w != null) {
                    w.setLayout(((ColumnDataScene) scene).getColumns().createLayout());
                    scene.addChild(w);
                } else {
                    System.err.println("no customizer for " + e.property + " though factory " + e.factory + " found");
                }
            }
            return scene;
        }

        private boolean isEmpty() {
            return all.isEmpty();
        }

        static class Entry<T extends Property<R>, R> {

            final T property;
            final CustomizerFactory<?, T> factory;

            public Entry(T property, CustomizerFactory<?, T> factory) {
                this.property = property;
                this.factory = factory;
            }

            Widget get(Scene scene) {
                try {
                    return factory.createCustomizerWidget(scene, property);
                } catch (NoSuchMethodException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InstantiationException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IllegalAccessException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IllegalArgumentException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return null;
            }
        }
    }

    static final class CustomizableCF<T extends Customizable> extends CustomizerFactory<T, T> {

        private final T customizable;

        public CustomizableCF(T customizable) {
            this.customizable = customizable;
        }

        @Override
        public Component createCustomizer(T toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return customizable.getCustomizer();
        }

        @Override
        public Widget createCustomizerWidget(Scene scene, T toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            scene = scene == null ? new ColumnDataScene() : scene;
            Component c = customizable.getCustomizer();
            Widget result = new ComponentWidget(scene, c);
            scene.addChild(result);
            return result;
        }
    }

    static final class CustomizableWC<T extends Customizable.WidgetCustomizable> extends CustomizerFactory<T, T> {

        private final Customizable.WidgetCustomizable customizable;

        public CustomizableWC(WidgetCustomizable customizable) {
            this.customizable = customizable;
        }

        @Override
        public Component createCustomizer(T toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            Widget w = createCustomizerWidget(null, toCustomize);
            return w.getScene().createView();
        }

        @Override
        public Widget createCustomizerWidget(Scene scene, T toCustomize) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            ColumnDataScene s = scene == null ? new ColumnDataScene() : (ColumnDataScene) scene; //XXX
            Widget w = customizable.createWidget(s);
            s.addChild(w);
            w.setLayout(s.getColumns().createLayout());
            return w;
        }
    }
}
