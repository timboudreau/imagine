/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package net.java.dev.imagine.customizers.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.java.dev.imagine.api.customizers.visualizer.ColumnDataScene;
import net.java.dev.imagine.api.customizers.visualizer.ExpandableWidget.Expandable;
import net.java.dev.imagine.api.properties.Property;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Exceptions;

/**
 *
 * @author tim
 */
class PropertiesExpandable implements Expandable {
    private final ColumnDataScene scene;
    private final Property<?> prop;
    private final Map<Property<?>, Widget> widgetForProperty = new LinkedHashMap<Property<?>, Widget>();
    PropertiesExpandable (ColumnDataScene scene, Property<?> prop) {
        this.scene = scene;
        this.prop = prop;
    }

    @Override
    public String getTitle() {
        return prop.getDisplayName();
    }

    @Override
    public List<Widget> getWidgets(boolean expanded) {
        List<Widget> result = new ArrayList<Widget>();
        for (Property<?> p : prop.getSubProperties()) {
            Widget w = widgetForProperty.get(p);
            if (w == null) {
                w = createWidget(p);
                widgetForProperty.put(p, w);
            }
            if (w.getParentWidget() == null) {
                scene.addChild(w);
            }
            result.add(w);
        }
        return result;
    }
    
    private <T> Widget createWidget(Property<T> p) {
        try {
            CustomizerFactory<?,Property<T>> cf = CustomizerFactory.find(p, true);
            if (cf != null) {
                try {
                    return cf.createCustomizerWidget(scene, p);
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
            }
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
}
