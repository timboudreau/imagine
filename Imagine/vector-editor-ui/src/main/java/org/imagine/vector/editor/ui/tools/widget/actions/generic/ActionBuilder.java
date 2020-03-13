/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.KeyStroke;

/**
 *
 * @author Tim Boudreau
 */
public abstract class ActionBuilder<B extends ActionBuilder> {

    protected final String displayName;
    protected Set<KeyStroke> keyBindings = new HashSet<>(2);
    protected final Map<String, Object> actionValues = new HashMap<>(3);
    protected final ActionsBuilder factory;
    protected boolean separatorBefore;
    protected boolean separatorAfter;

    static final String KEY_HIDE_WHEN_DISABLED = "hideWhenDisabled";

    ActionBuilder(ActionBuilder other) {
        this.displayName = other.displayName;
        keyBindings.addAll(other.keyBindings);
        actionValues.putAll(other.actionValues);
        this.separatorBefore = other.separatorBefore;
        this.separatorAfter = other.separatorAfter;
        this.factory = other.factory;
    }

    public ActionBuilder(String displayName, ActionsBuilder factory) {
        this.displayName = displayName;
        this.factory = factory;
        actionValues.put(KEY_HIDE_WHEN_DISABLED, true);
    }

    public B separatorBefore() {
        this.separatorBefore = true;
        return cast();
    }

    public B separatorAfter() {
        this.separatorAfter = true;
        return cast();
    }

    B cast() {
        return (B) this;
    }

    public B withKeyBinding(KeyStroke ks) {
        keyBindings.add(ks);
        return cast();
    }

    public B dontHideWhenDisabled() {
        actionValues.remove(KEY_HIDE_WHEN_DISABLED);
        return cast();
    }

    public B addActionValue(String key, Object val) {
        actionValues.put(key, val);
        return cast();
    }

    static <T> Consumer<Sense<T>> multi(Consumer<? super Collection<? extends T>> c) {
        return new MultiConsumer<>(c);
    }

    static <T> Consumer<Sense<T>> single(Consumer<? super T> c) {
        return new SingleConsumer<>(c);
    }

}
