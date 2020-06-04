package org.imagine.help.spi;

import org.imagine.help.impl.HelpComponentManagerTrampoline;
import java.awt.Component;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import org.imagine.help.api.HelpItem;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class HelpComponentManager {

    private static HelpComponentManager INSTANCE;

    public static HelpComponentManager getDefault() {
        if (INSTANCE == null) {
            INSTANCE = Lookup.getDefault().lookup(HelpComponentManager.class);
            if (INSTANCE == null) {
                System.err.println("No implementation of HelpComponentManager "
                        + "in the default lookup");
                INSTANCE = new DummyImpl();
            }
        }
        return INSTANCE;
    }

    protected abstract void popup(HelpItem item, MouseEvent evt);

    protected abstract void popup(HelpItem item, Component target);

    protected abstract void popup(JComponent target);

    protected abstract void removePopup();

    protected abstract void dismissPopupGesturePerformed(JRootPane root);

    protected abstract void enqueueNextPopup(HelpItem item, Component target);

    protected abstract void open(HelpItem item);

    static final class Impl extends HelpComponentManagerTrampoline {

        @Override
        public void activate(HelpItem item, MouseEvent evt) {
            getDefault().popup(item, evt);
        }

        @Override
        public void activate(HelpItem item, Component target) {
            getDefault().popup(item, target);
        }

        @Override
        public void activate(JComponent target) {
            getDefault().popup(target);
        }

        @Override
        public void deactivate() {
            getDefault().removePopup();
        }

        @Override
        protected void dismissGesturePerformed(JRootPane root) {
            getDefault().dismissPopupGesturePerformed(root);
        }

        @Override
        public void enqueue(HelpItem item, Component target) {
            getDefault().enqueueNextPopup(item, target);
        }

        @Override
        public void open(HelpItem item) {
            getDefault().open(item);
        }
    }

    static {
        HelpComponentManagerTrampoline.setInstance(new Impl());
    }

    static final class DummyImpl extends HelpComponentManager {

        @Override
        protected void popup(HelpItem item, MouseEvent evt) {
            // do nothing
        }

        @Override
        protected void popup(HelpItem item, Component target) {
            // do nothing
        }

        @Override
        protected void popup(JComponent target) {
            // do nothing
        }

        @Override
        protected void removePopup() {
            // do nothing
        }

        @Override
        protected void dismissPopupGesturePerformed(JRootPane root) {
            // do nothing
        }

        @Override
        protected void enqueueNextPopup(HelpItem item, Component target) {
            // do nothing
        }

        @Override
        protected void open(HelpItem item) {
            // do nothing
        }
    }
}
