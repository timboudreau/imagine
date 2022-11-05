package org.imagine.nbutil;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JComponent;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

/**
 * Provides a way to open a larger editor for some customizers.
 *
 * @author Tim Boudreau
 */
public final class SingleUseWindow {

    private SingleUseWindow() {
        throw new AssertionError();
    }

    /**
     * Open a window in the window system (returns JComponent so users
     * do not inherit a dependency on the window system api).
     *
     * @param name The display name
     * @param inner The inner component
     * @return The container it is put in
     */
    public static JComponent popOut(String name, Component inner) {
        return popOut(name, inner, null);
    }

    /**
     * Open a window in the window system (returns JComponent so users
     * do not inherit a dependency on the window system api).
     *
     * @param name The display name
     * @param inner The inner component
     * @param lookup The lookup the component should expose
     * @return The container it is put in
     */
    public static JComponent popOut(String name, Component inner, Lookup lookup) {
        PopOutTopComponent result = new PopOutTopComponent(name, inner, lookup);
        result.open();
        result.requestActive();
        return result;
    }

    private static final class PopOutTopComponent extends TopComponent {

        private Lookup lookup;

        public PopOutTopComponent(String name, Component inner) {
            this(name, inner, null);
        }

        public PopOutTopComponent(String name, Component inner, Lookup lookup) {
            setLayout(new BorderLayout());
            this.lookup = lookup;
            add(inner, BorderLayout.CENTER);
            setName(name);
            setDisplayName(name);
        }

        @Override
        public Lookup getLookup() {
            return lookup == null ? Lookup.EMPTY : lookup;
        }

        @Override
        protected void componentClosed() {
            removeAll();
            lookup = null;
        }

        @Override
        public int getPersistenceType() {
            return PERSISTENCE_NEVER;
        }
    }
}
