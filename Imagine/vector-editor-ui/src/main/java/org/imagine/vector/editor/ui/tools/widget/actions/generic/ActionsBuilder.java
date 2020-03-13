/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import com.mastfrog.function.QuadConsumer;
import com.mastfrog.function.QuadPredicate;
import com.mastfrog.function.TriConsumer;
import com.mastfrog.function.TriPredicate;
import com.mastfrog.util.collections.CollectionUtils;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;
import org.openide.awt.Mnemonics;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class ActionsBuilder {

    private final Map<SenseFactory<?>, SenseFactory<?>> internSenseFactories;
    private List<AHandler> handlers = new ArrayList<>(20);
    private LinkedList<String> currentSubmenu = new LinkedList<>();

    public ActionsBuilder() {
        internSenseFactories = new HashMap<>(20);
    }

    private ActionsBuilder(ActionsBuilder other, boolean copyHandlers) {
        this.internSenseFactories = other.internSenseFactories;
        if (copyHandlers) {
            this.handlers.addAll(other.handlers);
        }
    }

    public ActionsBuilder submenu(String name, Consumer<ActionsBuilder> c) {
        currentSubmenu.push(name);
        try {
            c.accept(this);
        } finally {
            currentSubmenu.pop();
        }
        return this;
    }

    /**
     * Creates a copy of this ActionsBuilder with all of its contents; use this
     * if you have a type of object whose actions are a superset of those of
     * another.
     *
     * @return A copy of this actions builder
     */
    public ActionsBuilder copy() {
        return new ActionsBuilder(this, true);
    }

    /**
     * Creates a copy of this ActionsBuilder which does not share its contents,
     * but under the hood will share instances of lookup/selection-listening
     * plumbing to save memory.
     *
     * @return An actions builder
     */
    public ActionsBuilder emptyCopy() {
        return new ActionsBuilder(this, false);
    }

    /**
     * Ensures that for the same predicate and type, we will only generate one
     * LookupListener per lookup. Note: For this to be useful, it is a good idea
     * for the Predicate instances used to be static method references or actual
     * instances of Predicate.
     *
     * @param <T> The type
     * @param senseFactory The factory
     * @return An interned SenseFactory which may or may not be the one passed
     */
    @SuppressWarnings(value = "unchecked")
    private <T> SenseFactory<T> intern(SenseFactory<T> senseFactory) {
        SenseFactory<T> result = (SenseFactory<T>) internSenseFactories.getOrDefault(senseFactory, senseFactory);
        return result;
    }

    /**
     * Get the set of all actions for a lookup.
     *
     * @param lookup The lookup
     * @return A list of actions
     */
    public List<? extends Action> getActions(Lookup lookup) {
        List<Action> result = new ArrayList<>(handlers.size());
        for (AHandler ah : handlers) {
            result.add(ah.prepareAction(lookup));
        }
        return result;
    }

    /**
     * Add all actions to a popup menu.
     *
     * @param menu The menu
     * @param lkp The lookup
     */
    public void applyToPopup(JPopupMenu menu, Lookup lkp) {
        boolean lastWasSeparator = true;
        for (int i = 0; i < handlers.size(); i++) {
            AHandler ah = handlers.get(i);
            if (i != 0 && ah.isSeparatorBefore() && !lastWasSeparator) {
                Component[] comps = menu.getComponents();
                if (comps.length > 0 && (!(comps[comps.length - 1] instanceof JSeparator))) {
                    menu.add(new JSeparator());
                }
            }
            ah.prepareAction(lkp).addToMenu(menu, ah.submenuPath());
            if (ah.isSeparatorAfter() && i != handlers.size() - 1) {
                lastWasSeparator = true;
                menu.add(new JSeparator());
            } else {
                lastWasSeparator = false;
            }
        }
    }

    /**
     * Add all actions to a regular menu.
     *
     * @param menu The menu
     * @param lkp The lookup
     */
    public void applyToMenu(JMenu menu, Lookup lkp) {
        for (AHandler ah : handlers) {
            ah.prepareAction(lkp).addToMenu(menu, ah.submenuPath());
        }
    }

    /**
     * Apply any actions that have key bindings to a widget.
     *
     * @param w The widget
     */
    public void applyToWidget(Widget w) {
        for (AHandler ah : handlers) {
            ah.applyToWidget(w);
        }
    }

    public ActionBuilder1 action(String name) {
        return new ActionBuilder1(name, this);
    }

    <T> ActionsBuilder add(FinishableActionBuilder1<T> f) {
        SenseFactory<T> sf = intern(new SenseFactory<>(f.type, f.test));
        Consumer<Sense<T>> consumer = f.consumer();
        handlers.add(new A1Handler<>(sf, f.test, consumer, f));
        if (!currentSubmenu.isEmpty()) {
            handlers.get(handlers.size() - 1).setSubmenuPath(currentSubmenu);
        }
        return this;
    }

    <T, R> ActionsBuilder add(FinishableActionBuilder2<T, R> f) {
        SenseFactory<T> sf1 = intern(new SenseFactory<>(f.type2, f.test2));
        SenseFactory<R> sf2 = intern(new SenseFactory<>(f.type1, f.test1));
        BiConsumer<Sense<T>, Sense<R>> consumer = f.performer;
        handlers.add(new A2Handler<>(sf1, f.test2, sf2, f.test1,
                consumer, f, f.multiTest));
        if (!currentSubmenu.isEmpty()) {
            handlers.get(handlers.size() - 1).setSubmenuPath(currentSubmenu);
        }
        return this;
    }

    <T, R, S> ActionsBuilder add(FinishableActionBuilder3<T, R, S> f) {
        SenseFactory<T> sf1 = intern(new SenseFactory<>(f.type2, f.test2));
        SenseFactory<R> sf2 = intern(new SenseFactory<>(f.type1, f.test1));
        SenseFactory<S> sf3 = intern(new SenseFactory<>(f.type3, f.test3));
        TriConsumer<Sense<T>, Sense<R>, Sense<S>> consumer = f.performer;
        handlers.add(new A3Handler<>(sf1, f.test2, sf2, f.test1, sf3, f.test3,
                consumer, f, f.multiTest));
        if (!currentSubmenu.isEmpty()) {
            handlers.get(handlers.size() - 1).setSubmenuPath(currentSubmenu);
        }
        return this;
    }

    <T, R, S, U> ActionsBuilder add(FinishableActionBuilder4<T, R, S, U> f) {
        SenseFactory<T> sf1 = intern(new SenseFactory<>(f.type2, f.test2));
        SenseFactory<R> sf2 = intern(new SenseFactory<>(f.type1, f.test1));
        SenseFactory<S> sf3 = intern(new SenseFactory<>(f.type3, f.test3));
        SenseFactory<U> sf4 = intern(new SenseFactory<>(f.type4, f.test4));
        QuadConsumer<Sense<T>, Sense<R>, Sense<S>, Sense<U>> consumer = f.performer;
        handlers.add(new A4Handler<>(sf1, f.test2, sf2, f.test1, sf3, f.test3,
                sf4, f.test4, consumer, f, f.multiTest));
        if (!currentSubmenu.isEmpty()) {
            handlers.get(handlers.size() - 1).setSubmenuPath(currentSubmenu);
        }
        return this;
    }

    interface AHandler {

        AFAction prepareAction(Lookup lookup);

        void applyToWidget(Widget widget);

        void setSubmenuPath(List<String> path);

        String[] submenuPath();

        boolean isSeparatorBefore();

        boolean isSeparatorAfter();
    }
    static String[] EMPTY_SUBMENU_PATH = new String[0];

    static abstract class AbstractAHandler implements AHandler {

        protected final String displayName;
        protected final List<KeyStroke> keyBindings;
        protected final Map<String, Object> actionValues;
        private String[] submenuPath = EMPTY_SUBMENU_PATH;
        private final boolean separatorBefore;
        private final boolean separatorAfter;

        AbstractAHandler(ActionBuilder ab) {
            this.keyBindings = new ArrayList<>(ab.keyBindings);
            this.actionValues = ab.actionValues;
            this.displayName = ab.displayName;
            this.separatorBefore = ab.separatorBefore;
            this.separatorAfter = ab.separatorAfter;
            Collections.sort(keyBindings, (a, b) -> {
                String aText = KeyEvent.getKeyText(a.getKeyCode()) + KeyEvent.getKeyModifiersText(a.getModifiers());
                String bText = KeyEvent.getKeyText(b.getKeyCode()) + KeyEvent.getKeyModifiersText(b.getModifiers());
                return Integer.compare(aText.length(), bText.length());
            });
        }

        @Override
        public String[] submenuPath() {
            return submenuPath;
        }

        @Override
        public void setSubmenuPath(List<String> path) {
            submenuPath = CollectionUtils.reversed(path).toArray(new String[path.size()]);
        }

        public boolean isSeparatorBefore() {
            return separatorBefore;
        }

        @Override
        public boolean isSeparatorAfter() {
            return separatorAfter;
        }

        abstract Runnable createRunner(Lookup lookup);

        abstract SenseIt createEnabledTester(Lookup lookup);

        @Override
        public AFAction prepareAction(Lookup lookup) {
            KeyStroke stroke = keyBindings == null ? null : keyBindings.isEmpty() ? null : keyBindings.get(0);
            AFAction result = new AFAction(displayName, stroke, createRunner(lookup), createEnabledTester(lookup), this::prepareAction, actionValues);
            return result;
        }

        private WidgetAction generalWidgetAction;

        private WidgetAction prepareWidgetAction(Widget target) {
            if (keyBindings.isEmpty()) {
                return null;
            }
            if (generalWidgetAction == null) {
                generalWidgetAction = new WidgetKeyAction(displayName,
                        keyBindings, this::prepareAction);
            }
            return generalWidgetAction;
        }

        public void applyToWidget(Widget widget) {
            WidgetAction wa = prepareWidgetAction(widget);
            if (wa != null) {
                widget.getActions().addAction(wa);
            }
        }
    }

    static class A1Handler<T> extends AbstractAHandler {

        protected final Predicate<Sense<T>> test;
        protected final Consumer<Sense<T>> performer;
        protected final SenseFactory<T> senseFactory;

        public A1Handler(SenseFactory<T> senseFactory, Predicate<Sense<T>> test, Consumer<Sense<T>> performer, ActionBuilder ab) {
            super(ab);
            this.senseFactory = senseFactory;
            this.test = test;
            this.performer = performer;
        }

        @Override
        Runnable createRunner(Lookup lookup) {
            Sense<T> sense = senseFactory.senseForLookup(lookup);
            return () -> {
                if (test.test(sense)) {
                    performer.accept(sense);
                }
            };
        }

        @Override
        SenseIt createEnabledTester(Lookup lookup) {
            return senseFactory.attachToLookup(lookup);
        }
    }

    static class A2Handler<T, R> extends AbstractAHandler {

        protected final Predicate<Sense<T>> test1;
        protected final SenseFactory<T> senseFactory1;
        protected final Predicate<Sense<R>> test2;
        protected final SenseFactory<R> senseFactory2;
        protected final BiConsumer<Sense<T>, Sense<R>> performer;
        private final BiPredicate<? super Sense<T>, ? super Sense<R>> pairTest;

        public A2Handler(SenseFactory<T> senseFactory1, Predicate<Sense<T>> test1, SenseFactory<R> senseFactory2, Predicate<Sense<R>> test2, BiConsumer<Sense<T>, Sense<R>> performer, ActionBuilder ab, BiPredicate<? super Sense<T>, ? super Sense<R>> pairTest) {
            super(ab);
            this.senseFactory1 = senseFactory1;
            this.test1 = test1;
            this.senseFactory2 = senseFactory2;
            this.test2 = test2;
            this.performer = performer;
            this.pairTest = pairTest;
        }

        @Override
        Runnable createRunner(Lookup lookup) {
            Sense<T> sense1 = senseFactory1.senseForLookup(lookup);
            Sense<R> sense2 = senseFactory2.senseForLookup(lookup);
            return () -> {
                if (test1.test(sense1)) {
                    performer.accept(sense1, sense2);
                }
            };
        }

        @Override
        SenseIt createEnabledTester(Lookup lookup) {
            SenseIt sensor1 = senseFactory1.attachToLookup(lookup);
            SenseIt sensor2 = senseFactory2.attachToLookup(lookup);
            if (pairTest != null) {
                Sense<T> sense1 = senseFactory1.senseForLookup(lookup);
                Sense<R> sense2 = senseFactory2.senseForLookup(lookup);
                SenseIt bi = new HeteroSenseIt2<>(sense1, sense2, pairTest);
                return new MultiSenseIt(sensor1, sensor2, bi);
            }
            return new MultiSenseIt(sensor1, sensor2);
        }
    }

    static class A3Handler<T, R, S> extends AbstractAHandler {

        protected final Predicate<Sense<T>> test1;
        protected final SenseFactory<T> senseFactory1;
        protected final Predicate<Sense<R>> test2;
        protected final SenseFactory<R> senseFactory2;
        protected final Predicate<Sense<S>> test3;
        protected final SenseFactory<S> senseFactory3;
        protected final TriConsumer<Sense<T>, Sense<R>, Sense<S>> performer;
        protected final TriPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>> groupTest;

        public A3Handler(SenseFactory<T> senseFactory1, Predicate<Sense<T>> test1, SenseFactory<R> senseFactory2, Predicate<Sense<R>> test2, SenseFactory<S> senseFactory3, Predicate<Sense<S>> test3, TriConsumer<Sense<T>, Sense<R>, Sense<S>> performer, ActionBuilder ab, TriPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>> groupTest) {
            super(ab);
            this.senseFactory1 = senseFactory1;
            this.test1 = test1;
            this.senseFactory2 = senseFactory2;
            this.test2 = test2;
            this.senseFactory3 = senseFactory3;
            this.test3 = test3;
            this.performer = performer;
            this.groupTest = groupTest;
        }

        @Override
        Runnable createRunner(Lookup lookup) {
            Sense<T> sense1 = senseFactory1.senseForLookup(lookup);
            Sense<R> sense2 = senseFactory2.senseForLookup(lookup);
            Sense<S> sense3 = senseFactory3.senseForLookup(lookup);
            return () -> {
                if (test1.test(sense1)) {
                    performer.apply(sense1, sense2, sense3);
                }
            };
        }

        @Override
        SenseIt createEnabledTester(Lookup lookup) {
            SenseIt sensor1 = senseFactory1.attachToLookup(lookup);
            SenseIt sensor2 = senseFactory2.attachToLookup(lookup);
            SenseIt sensor3 = senseFactory3.attachToLookup(lookup);
            if (groupTest != null) {
                Sense<T> sense1 = senseFactory1.senseForLookup(lookup);
                Sense<R> sense2 = senseFactory2.senseForLookup(lookup);
                Sense<S> sense3 = senseFactory3.senseForLookup(lookup);
                SenseIt groupSensor = new HeteroSenseIt3(sense1, sense2, sense3, groupTest);
                return new MultiSenseIt(sensor1, sensor2, sensor3,
                        groupSensor);
            }
            return new MultiSenseIt(sensor1, sensor2, sensor3);
        }
    }

    static class A4Handler<T, R, S, U> extends AbstractAHandler {

        protected final Predicate<Sense<T>> test1;
        protected final SenseFactory<T> senseFactory1;
        protected final Predicate<Sense<R>> test2;
        protected final SenseFactory<R> senseFactory2;
        protected final Predicate<Sense<S>> test3;
        protected final SenseFactory<S> senseFactory3;
        protected final Predicate<Sense<U>> test4;
        protected final SenseFactory<U> senseFactory4;
        protected final QuadConsumer<Sense<T>, Sense<R>, Sense<S>, Sense<U>> performer;
        private final QuadPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>, ? super Sense<U>> multiTest;

        public A4Handler(SenseFactory<T> senseFactory1, Predicate<Sense<T>> test1, SenseFactory<R> senseFactory2, Predicate<Sense<R>> test2, SenseFactory<S> senseFactory3, Predicate<Sense<S>> test3, SenseFactory<U> senseFactory4, Predicate<Sense<U>> test4, QuadConsumer<Sense<T>, Sense<R>, Sense<S>, Sense<U>> performer, ActionBuilder ab, QuadPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>, ? super Sense<U>> multiTest) {
            super(ab);
            this.senseFactory1 = senseFactory1;
            this.test1 = test1;
            this.senseFactory2 = senseFactory2;
            this.test2 = test2;
            this.senseFactory3 = senseFactory3;
            this.test3 = test3;
            this.senseFactory4 = senseFactory4;
            this.test4 = test4;
            this.performer = performer;
            this.multiTest = multiTest;
        }

        @Override
        Runnable createRunner(Lookup lookup) {
            Sense<T> sense1 = senseFactory1.senseForLookup(lookup);
            Sense<R> sense2 = senseFactory2.senseForLookup(lookup);
            Sense<S> sense3 = senseFactory3.senseForLookup(lookup);
            Sense<U> sense4 = senseFactory4.senseForLookup(lookup);
            return new Performer4(sense1, sense2, sense3, sense4, performer);
        }

        @Override
        SenseIt createEnabledTester(Lookup lookup) {
            SenseIt sensor1 = senseFactory1.attachToLookup(lookup);
            SenseIt sensor2 = senseFactory2.attachToLookup(lookup);
            SenseIt sensor3 = senseFactory3.attachToLookup(lookup);
            SenseIt sensor4 = senseFactory4.attachToLookup(lookup);
            if (multiTest != null) {
                Sense<T> sense1 = senseFactory1.senseForLookup(lookup);
                Sense<R> sense2 = senseFactory2.senseForLookup(lookup);
                Sense<S> sense3 = senseFactory3.senseForLookup(lookup);
                Sense<U> sense4 = senseFactory4.senseForLookup(lookup);
                SenseIt sensor5 = new HeteroSenseIt4(sense1, sense2, sense3, sense4, multiTest);
                return new MultiSenseIt(sensor1, sensor2, sensor3, sensor4, sensor5);
            }
            return new MultiSenseIt(sensor1, sensor2, sensor3, sensor4);
        }
    }

    static final class Performer4<T, R, S, U> implements Runnable {

        private final Sense<T> sense1;
        private final Sense<R> sense2;
        private final Sense<S> sense3;
        private final Sense<U> sense4;
        private final QuadConsumer<Sense<T>, Sense<R>, Sense<S>, Sense<U>> performer;

        public Performer4(Sense<T> sense1, Sense<R> sense2, Sense<S> sense3, Sense<U> sense4, QuadConsumer<Sense<T>, Sense<R>, Sense<S>, Sense<U>> performer) {
            this.sense1 = sense1;
            this.sense2 = sense2;
            this.sense3 = sense3;
            this.sense4 = sense4;
            this.performer = performer;
        }

        @Override
        public void run() {
            performer.accept(sense1, sense2, sense3, sense4);
        }

        @Override
        public String toString() {
            return "Performer4{" + "sense1=" + sense1
                    + ", sense2=" + sense2 + ", sense3=" + sense3
                    + ", sense4=" + sense4 + ", performer=" + performer + '}';
        }
    }

    static final class WidgetKeyAction extends WidgetAction.Adapter {

        private final String name;
        private final List<KeyStroke> keystrokes;
        private final Function<Lookup, AFAction> performer;

        public WidgetKeyAction(String name, List<KeyStroke> keystrokes, Function<Lookup, AFAction> performer) {
            this.name = name;
            this.keystrokes = keystrokes;
            this.performer = performer;
        }

        private State handleKey(Widget widget, WidgetKeyEvent event, boolean pressed) {
            for (KeyStroke ks : keystrokes) {
                if (ks.isOnKeyRelease() == !pressed) {
                    if (event.getKeyCode() == ks.getKeyCode()) {
//                        System.out.println("WIDGET KEY ACTION " + name
//                                + " KEY " + (pressed ? "PRESSED " : "RELEASED ")
//                                + " keyName " + KeyEvent.getKeyText(event.getKeyCode())
//                                + event.getKeyCode() + " modifiers "
//                                + event.getModifiers() + " modifiersEx " + event.getModifiersEx());

                        if (ks.getModifiers() == 0 || (event.getModifiersEx() & ks.getModifiers()) != 0) {
//                            System.out.println("run performer for key " + performer
//                                    + " on widget " + widget);
                            AFAction a = performer.apply(widget.getLookup());
                            if (a.isEnabled()) {
                                a.actionPerformed(new ActionEvent(widget, ActionEvent.ACTION_PERFORMED, name));
                                return State.CONSUMED;
                            }
                        } else {
//                            System.out.println("MODIFIER NON-MATCH " + " " + event.getModifiers() + " (" + Integer.toBinaryString(event.getModifiers()) + ") " + " vs " + ks.getModifiers() + " (" + Integer.toBinaryString(ks.getModifiers()) + ") " + " evt-modsX " + Integer.toBinaryString(event.getModifiersEx()));
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public State keyPressed(Widget widget, WidgetKeyEvent event) {
            State state = handleKey(widget, event, true);
            return state != null ? state : super.keyPressed(widget, event);
        }

        @Override
        public State keyReleased(Widget widget, WidgetKeyEvent event) {
            State state = handleKey(widget, event, false);
            return state != null ? state : super.keyReleased(widget, event);
        }
    }

    static final class AFAction extends AbstractAction implements Runnable, ContextAwareAction {

        private final Runnable runner;
        private final SenseIt enabledState;
        private final Function<Lookup, Action> ctxFactory;

        AFAction(String name, KeyStroke stroke, Runnable runner, SenseIt enabled, Function<Lookup, Action> ctxFactory, Map<String, Object> kv) {
            super(name);
            for (Map.Entry<String, Object> e : kv.entrySet()) {
                putValue(e.getKey(), e.getValue());
            }
            if (stroke != null) {
                putValue(Action.ACCELERATOR_KEY, stroke);
            }
            this.runner = runner;
            this.enabledState = enabled;
            this.ctxFactory = ctxFactory;
        }

        @Override
        public String toString() {
            return "Af(" + runner + " " + enabledState.getAsBoolean() + " " + enabledState + ")";
        }

        private void addToMenu(JMenu menu, String[] submenuPath, int submenuIndex) {
            if (submenuIndex == submenuPath.length) {
                addToMenu(menu);
            } else {
                addToMenu(findOrCreateSubmenu(menu, submenuPath, 0));
            }
        }

        private void addToMenu(JMenu menu) {
            JMenuItem item = new InvisibleWhenDisabledMenuItem(this);
            Mnemonics.setLocalizedText(item, (String) getValue(NAME));
            menu.add(item);
        }

        public void addToMenu(JMenu menu, String[] submenuPath) {
            addToMenu(menu, submenuPath, 0);
        }

        private JMenu findOrCreateSubmenu(JMenu in, String[] submenuPath, int index) {
            for (Component c : in.getMenuComponents()) {
                if (c instanceof JMenu && submenuPath[index].equals(c.getName())) {
                    return (JMenu) c;
                }
            }
            JMenu nue = new InvisibleWhenAllDisabledMenu(submenuPath[index]);
            nue.setName(submenuPath[index]);
            in.add(nue);
            if (index < submenuPath.length - 1) {
                return findOrCreateSubmenu(in, submenuPath, index + 1);
            }
            return in;
        }

        private JMenu findOrCreateSubmenu(JPopupMenu in, String[] submenuPath) {
            for (Component c : in.getComponents()) {
                if (c instanceof JMenu && submenuPath[0].equals(c.getName())) {
                    return (JMenu) c;
                }
            }
            JMenu nue = new InvisibleWhenAllDisabledMenu(submenuPath[0]);
            nue.setName(submenuPath[0]);
            in.add(nue);
            if (submenuPath.length > 1) {
                return findOrCreateSubmenu(nue, submenuPath, 1);
            }
            return nue;
        }

        public void addToMenu(JPopupMenu menu, String[] submenuPath) {
            if (submenuPath.length > 0) {
                JMenu men = findOrCreateSubmenu(menu, submenuPath);
                JMenuItem item = new InvisibleWhenDisabledMenuItem(this);
                Mnemonics.setLocalizedText(item, (String) getValue(NAME));
                men.add(item);
                return;
            }
            JMenuItem item = new InvisibleWhenDisabledMenuItem(this);
            Mnemonics.setLocalizedText(item, (String) getValue(NAME));
            menu.add(item);
        }

        @Override
        public void run() {
            setEnabled(enabledState.getAsBoolean());
        }

        @Override
        public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
            int oldCount = getPropertyChangeListeners().length;
            super.addPropertyChangeListener(listener);
            if (oldCount == 0) {
                addNotify();
            }
        }

        @Override
        public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
            super.removePropertyChangeListener(listener);
            int newCount = getPropertyChangeListeners().length;
            if (newCount == 0) {
                removeNotify();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            runner.run();
        }

        private void addNotify() {
            enabledState.listen(this);
            run();
        }

        private void removeNotify() {
            enabledState.unlisten(this);
        }

        @Override
        public Action createContextAwareInstance(Lookup actionContext) {
            return ctxFactory.apply(actionContext);
        }
    }

}
