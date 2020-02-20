package org.imagine.vector.editor.ui;

import java.util.function.Consumer;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
class DelegatingLookupConsumer implements Consumer<Lookup[]> {

    private static final Lookup[] NO_LOOKUPS = new Lookup[0];
    private Lookup[] lastValue = NO_LOOKUPS;
    private Consumer<Lookup[]> delegate;

    void setDelegate(Consumer<Lookup[]> c) {
        if (c != delegate) {
            if (delegate != null) {
                delegate.accept(NO_LOOKUPS);
            }
        }
        delegate = c;
        c.accept(lastValue);
    }

    @Override
    public void accept(Lookup[] t) {
        lastValue = t;
        if (delegate != null) {
            delegate.accept(t);
        }
    }

}
