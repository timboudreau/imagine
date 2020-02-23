package org.imagine.awt.cache;

import java.awt.Paint;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.imagine.awt.counters.UsageCounter;

/**
 *
 * @author Tim Boudreau
 */
public class ReferenceTrackingPaintWrapperSupplierFactory implements BiFunction<Supplier<Paint>, UsageCounter, Supplier<Paint>> {

    @Override
    public Supplier<Paint> apply(Supplier<Paint> t, UsageCounter u) {
        return new ReferenceTrackingSupplier(t, u);
    }

    static final class ReferenceTrackingSupplier implements Supplier<Paint> {

        private final Supplier<Paint> delegate;
        private final UsageCounter counter;

        public ReferenceTrackingSupplier(Supplier<Paint> delegate, UsageCounter counter) {
            this.delegate = delegate;
            this.counter = counter;
        }

        @Override
        public Paint get() {
            return new ReferenceTrackingWrapperPaint(delegate.get(), counter);
        }
    }

}
