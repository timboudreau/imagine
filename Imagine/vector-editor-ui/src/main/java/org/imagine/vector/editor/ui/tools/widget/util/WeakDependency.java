package org.imagine.vector.editor.ui.tools.widget.util;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.widget.Widget.Dependency;

/**
 * A dependency that doesn't have to explicitly be removed.
 *
 * @author Tim Boudreau
 */
public final class WeakDependency implements Dependency, Runnable {

    private final Reference<Widget> owner;
    private final Reference<Widget> revalidate;
    private final RevalidatingState state;

    WeakDependency(Widget owner, Widget toRevalidate, RevalidatingState state) {
        this.owner = new WeakReference<>(owner);
        this.revalidate = new WeakReference<>(toRevalidate);
        this.state = state;
    }

    public static Dependency create(Widget owner, Widget toRevalidate) {
        return new WeakDependency(owner, toRevalidate, NoOpState.INSTANCE);
    }

    public static void attachBiDirectional(Widget owner, Widget toRevalidate) {
        RevalidatingState state = new RevalidatingStateImpl();
        Dependency a = new WeakDependency(owner, toRevalidate, state);
        owner.addDependency(a);
        Dependency b = new WeakDependency(toRevalidate, owner, state);
        toRevalidate.addDependency(b);
    }

    public static void attach(Widget owner, Widget toRevalidate) {
        owner.addDependency(create(owner, toRevalidate));
    }

    @Override
    public void revalidateDependency() {
        state.run(this);
    }

    @Override
    public void run() {
        Widget reval = revalidate.get();
        if (reval != null) {
            reval.revalidate();
            reval.repaint();
        } else {
            Widget own = owner.get();
            if (own != null) {
                own.removeDependency(this);
            }
        }
    }

    interface RevalidatingState {

        void run(Runnable r);
    }

    private static class RevalidatingStateImpl implements RevalidatingState {

        private boolean running;

        public void run(Runnable r) {
            if (running) {
                return;
            }
            running = true;
            try {
                r.run();
            } finally {
                running = false;
            }
        }
    }

    private static class NoOpState implements RevalidatingState {

        private static final NoOpState INSTANCE = new NoOpState();

        @Override
        public void run(Runnable r) {
            r.run();
        }

    }

}
