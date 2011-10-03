package net.java.dev.imagine.customizers.impl;

import java.awt.Component;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JComponent;
import net.java.dev.imagine.api.customizers.visualizer.ColumnDataScene;
import org.netbeans.api.visual.widget.ComponentWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class CustomizerFactory<T, R> {

    private final Class<T> type;
    private final Class<R> customizes;
    private final boolean widget;

    CustomizerFactory(Class<T> type, Class<R> customizes, boolean widget) {
        this.type = type;
        this.customizes = customizes;
        this.widget = widget;
        assert Component.class.isAssignableFrom(type) == !widget;
    }

    public static CustomizerFactory fromFileObject(FileObject fo) throws ClassNotFoundException {
        String className = fo.getNameExt();
        String customizesName = fo.getParent().getNameExt();
        ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
        Class<?> compType = cl.loadClass(className);
        Class<?> customizes = cl.loadClass(customizesName);
        boolean widget = Boolean.TRUE.equals(fo.getAttribute("widget"));
        return new CustomizerFactory(compType, customizes, widget);
    }

    public static CustomizerFactory find(Object o, boolean preferWidget) throws ClassNotFoundException {
        if (o == null) {
            return null;
        }
        return find(o.getClass(), preferWidget);
    }

    public static CustomizerFactory find(Class<?> clazz, boolean preferWidget) throws ClassNotFoundException {
        Class<?> type = clazz;
        while (type != Object.class) {
            FileObject fo = FileUtil.getConfigFile("customizers2/" + type.getName());
            if (fo != null) {
                FileObject first = null;
                for (FileObject ch : fo.getChildren()) {
                    if (first == null) {
                        first = ch;
                    }
                    if (Boolean.TRUE.equals(fo.getAttribute("widget")) == preferWidget) {
                        return fromFileObject(fo);
                    }
                }
                if (first != null) {
                    return fromFileObject(first);
                }
            }
            type = type.getSuperclass();
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            FileObject fo = FileUtil.getConfigFile("customizers2/" + iface.getName());
            if (fo != null) {
                FileObject first = null;
                for (FileObject ch : fo.getChildren()) {
                    if (first == null) {
                        first = ch;
                    }
                    if (Boolean.TRUE.equals(fo.getAttribute("widget")) == preferWidget) {
                        return fromFileObject(fo);
                    }
                }
                if (first != null) {
                    return fromFileObject(first);
                }
            }
        }
        return null;
    }

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
            c = type.getConstructor(Scene.class, customizes);
        } catch (NoSuchMethodException e) {
            c = type.getConstructor(ColumnDataScene.class, customizes);
        }
        Widget result = (Widget) c.newInstance(scene, toCustomize);
        if (scene instanceof ColumnDataScene) {
            result.setLayout(((ColumnDataScene) scene).getColumns().createLayout());
        }
        return result;
    }
}
