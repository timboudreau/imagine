/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.imagine.editor.api.Zoom;
import org.imagine.vector.editor.ui.spi.LayerRenderingWidget;
import org.imagine.vector.editor.ui.tools.widget.util.ViewL;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.SetterResult;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 * A widget sort of like ObjectScene, but can be a widget in a scene created by
 * something else, with type-safe handling of adding multiple widget types for
 * different object types. A separate LayerWidget is created for each type of
 * widget configured on the factory, so order-of-addition is important.
 * <p>
 * If a Lookup is provided to the factory, then automatic handling of setting
 * the object state to selected when the key used to create a widget can be
 * found in that lookup is set up.
 *
 * @author Tim Boudreau
 */
public class HetroObjectLayerWidget extends LayerWidget implements LayerRenderingWidget {

    // XXX this class should not be polluted with LayerRenderingWidget
    // - it is generally useful
    private final WidgetFactory factory;
    private final Lookup lkp;
    private boolean added;
    private List<WidgetAction> priorActions = new ArrayList<>();

    private HetroObjectLayerWidget(WidgetFactory factory, Scene scene, List<LayerWidget> layers, Lookup lkp) {
        super(scene);
        this.factory = factory;
        layers.forEach(this::addChild);
        this.lkp = lkp == null ? Lookup.EMPTY : lkp;
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    @Override
    public SetterResult setOpacity(double opacity) {
        return SetterResult.NOT_HANDLED;
    }

    @Override
    public SetterResult setLocation(Point location) {
        setPreferredLocation(location);
        return SetterResult.HANDLED;
    }

    @Override
    public void setZoom(Zoom zoom) {
        revalidate();
        repaint();
    }

    @Override
    public void setLookupConsumer(Consumer<Lookup[]> additionaLookupConsumer) {
        additionaLookupConsumer.accept(new Lookup[]{lkp});
    }

    /**
     * Add an action that should be included in the scene's prior actions chain
     * when this widget is present and removed when it is removed.
     *
     * @param a An action
     */
    public void addPriorAction(WidgetAction a) {
        priorActions.add(a);
        if (added) {
            getScene().getPriorActions().addAction(a);
        }
    }

    /**
     * Remove an action that should be included in the scene's prior actions
     * chain when this widget is present and removed when it is removed.
     *
     * @param a An action
     */
    public void removePriorAction(WidgetAction a) {
        priorActions.remove(a);
        if (added) {
            getScene().getPriorActions().removeAction(a);
        }
    }

    @Override
    protected void notifyAdded() {
        WidgetAction.Chain chain = getScene().getPriorActions();
        for (WidgetAction w : priorActions) {
            chain.addAction(w);
        }
        added = true;
    }

    @Override
    protected void notifyRemoved() {
        added = false;
        WidgetAction.Chain chain = getScene().getPriorActions();
        for (WidgetAction w : priorActions) {
            chain.removeAction(w);
        }
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    public <T> HetroObjectLayerWidget allWidgetsMatching(Predicate<T> pred, Class<T> type, BiConsumer<? super T, ? super Widget> bi) {
        allWidgets(type, (key, widge) -> {
            if (pred.test(key)) {
                bi.accept(key, widge);
            }
        });
        return this;
    }

    public <T> HetroObjectLayerWidget allWidgets(Class<T> type, BiConsumer<? super T, ? super Widget> bi) {
        factory.findWidgets(type, bi);
        return this;
    }

    public <T> LayerWidget parentFor(Class<T> type) {
        return factory.layerFor(type);
    }

    public <T> Widget add(T object) {
        Widget result = factory.createWidget(object);
        return result;
    }

    public <T> Widget remove(T object) {
        return factory.remove(object);
    }

    public <T> Widget find(T object) {
        return factory.find(object);
    }

    public <T, R extends Widget> R find(T object, Class<R> widgetType) {
        return widgetType.cast(find(object));
    }

    public <T> LayerWidget layerFor(Class<T> type) {
        return factory.layerFor(type);
    }

    /**
     * Create a new hetero layer widget in the passed scene, populating widget
     * factories and optionally assigning a lookup for selection in the passed
     * consumer.
     *
     * @param scene The scene
     * @param c A consumer to configure the widget factory
     * @return A widget
     */
    public static HetroObjectLayerWidget create(Scene scene, Consumer<WidgetFactory> c) {
        WidgetFactory f = new WidgetFactory();
        c.accept(f);
        return f.create(scene);
    }

    public final static class WidgetFactory {

        private final Set<Entry<?, ?>> entries = new LinkedHashSet<>();
        private final Map<Entry<?, ?>, LayerWidget> layerWidgets = new HashMap<>();
        private Scene scene;
        private Lookup selectionLookup;

        private WidgetFactory() {
        }

        /**
         * Create the widget. This method can be called exactly <i>once</i>
         * per instance, after which the configuration methods will throw
         * exceptions (as will this one).
         *
         * @param scene The scene to create the widget in
         * @return A widget
         */
        HetroObjectLayerWidget create(Scene scene) {
            if (this.scene != null) {
                throw new IllegalStateException("Used already");
            }
            this.scene = scene;
            ViewL.attach(scene);
            List<LayerWidget> layers = new ArrayList<>(entries.size());
            for (Entry<?, ?> entry : entries) {
                LayerWidget w = new LayerWidget(scene);
                layers.add(w);
                layerWidgets.put(entry, w);
                if (selectionLookup != null) {
                    entry.attachToLookup(selectionLookup);
                }
            }
            return new HetroObjectLayerWidget(this, scene, layers, selectionLookup);
        }

        <T> LayerWidget layerFor(Class<T> type) {
            for (Entry<?, ?> e : entries) {
                if (e.objectType == type) {
                    return layerWidgets.get(e);
                }
            }
            throw new IllegalArgumentException("No layer for " + type.getName());
        }

        <T> WidgetFactory findWidgets(Class<T> type, BiConsumer<? super T, ? super Widget> widgetConsumer) {
            for (Entry<?, ?> e : entries) {
                if (e.objectType == type) {
                    new HashMap<>(e.widgetMapping).entrySet().forEach(en -> {
                        widgetConsumer.accept((T) en.getKey(), en.getValue());
                    });
                }
            }
            return this;
        }

        /**
         * Configure the lookup used to automatically manage selected state on
         * the widget.
         *
         * @param lookup The lookup, whose contents should change to indicate
         * what widgets (or rather the keys used to create those widgets) are
         * selected
         * @return this
         */
        public WidgetFactory withSelectionLookup(Lookup lookup) {
            if (scene != null) {
                throw new IllegalStateException("Already in use");
            }
            this.selectionLookup = lookup;
            return this;
        }

        /**
         * Add a key type and a function which will create widgets for that key.
         *
         * @param <T> The key type
         * @param <R> The widget type
         * @param type The key type
         * @param converter The widget factory function
         * @return this
         */
        public <T, R extends Widget> WidgetFactory add(Class<T> type, BiFunction<Scene, T, R> converter) {
            if (scene != null) {
                throw new IllegalStateException("Already in use");
            }
            entries.add(new Entry<>(type, converter));
            return this;
        }

        @SuppressWarnings("unchecked")
        private <T> Entry<T, ?> entryFor(T obj) {
            for (Entry<?, ?> e : entries) {
                if (e.recognizes(obj)) {
                    return (Entry<T, ?>) e;
                }
            }
            throw new IllegalStateException("No entry for " + obj);
        }

        public <T> Widget find(T obj) {
            Entry<T, ?> e = entryFor(obj);
            return e.widgetMapping.get(obj);
        }

        public <T> Widget remove(T obj) {
            Entry<T, ?> entry = entryFor(obj);
            Widget w = entry.widgetMapping.get(obj);
            if (w != null) {
                if (w.getScene().getFocusedWidget() == w) {
                    w.getScene().setFocusedWidget(null);
                }
                entry.widgetMapping.remove(obj);
                entry.lastSelection.remove(obj);
                w.removeFromParent();
            }
            scene.validate();
            return w;
        }

        public <T> Widget createWidget(T obj) {
            if (scene == null) {
                throw new IllegalStateException("Not attached to a scene yet");
            }
            for (Entry<?, ?> e : entries) {
                Widget result = e.createWidgetIfRecognized(scene, obj);
                if (result != null) {
                    LayerWidget layer = layerWidgets.get(e);
                    assert layer != null : "No layer";
                    layer.addChild(result);
                    scene.validate();
                    return result;
                }
            }
            throw new IllegalArgumentException("No factory for " + obj);
        }

        static class Entry<T, R extends Widget> implements LookupListener {

            private final Set<T> lastSelection = new HashSet<>();
            private final Class<T> objectType;
            private final BiFunction<Scene, T, R> converter;
            private final Map<T, R> widgetMapping = new HashMap<>(64);
            private Lookup.Result<T> res;

            public Entry(Class<T> objectType, BiFunction<Scene, T, R> converter) {
                this.objectType = objectType;
                this.converter = converter;
            }

            void attachToLookup(Lookup lookup) {
                res = lookup.lookupResult(objectType);
                res.addLookupListener(this);
                res.allInstances();
            }

            public boolean recognizes(Object o) {
                return objectType.isInstance(o);
            }

            public R createWidgetIfRecognized(Scene scene, Object o) {
                if (recognizes(o)) {
                    T target = objectType.cast(o);
                    R result = converter.apply(scene, target);
                    widgetMapping.put(target, result);
                    if (res.allInstances().contains(target)) {
                        result.setState(ObjectState.createNormal()
                                .deriveSelected(true));
                        lastSelection.add(target);
                    } else {
                        result.setState(ObjectState.createNormal());
                    }
                    return result;
                }
                return null;
            }

            @Override
            public void resultChanged(LookupEvent le) {
                Collection<? extends T> selected = res.allInstances();
                Set<T> newSelection = new HashSet<>(selected);
                lastSelection.removeAll(newSelection);
                for (T obj : lastSelection) {
                    Widget w = widgetMapping.get(obj);
                    if (w != null) {
                        w.setState(w.getState().deriveSelected(false));
                    }
                }
                lastSelection.clear();
                for (T obj : newSelection) {
                    lastSelection.add(obj);
                    Widget w = widgetMapping.get(obj);
                    if (w != null) {
                        w.setState(w.getState().deriveSelected(true));
                    }
                }
                lastSelection.addAll(newSelection);
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 37 * hash + Objects.hashCode(this.objectType);
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final Entry<?, ?> other = (Entry<?, ?>) obj;
                return Objects.equals(this.objectType, other.objectType);
            }
        }
    }
}
