package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import com.mastfrog.function.TriPredicate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
public final class FinishableActionBuilder2<T, R> extends ActionBuilder<FinishableActionBuilder2<T,R>> {

    protected final Class<T> type2;
    protected final Predicate<Sense<T>> test2;
    protected final Class<R> type1;
    protected final Predicate<Sense<R>> test1;
    BiConsumer<Sense<T>, Sense<R>> performer;
    BiPredicate<Sense<T>, Sense<R>> multiTest;

    FinishableActionBuilder2(ActionBuilder other, Class<R> type1, Predicate<Sense<R>> test1, Class<T> type2, Predicate<Sense<T>> test2) {
        super(other);
        this.type2 = type2;
        this.test2 = test2;
        this.type1 = type1;
        this.test1 = test1;
    }

    public ActionsBuilder finish(BiConsumer<? super T, ? super R> performer) {
        this.performer = (Sense<T> a, Sense<R> b) -> {
            if (test2.test(a) && test1.test(b)) {
                performer.accept(a.get(), b.get());
            }
        };
        return factory.add(this);
    }

    public ActionsBuilder finishMultiple(BiConsumer<? super Collection<? extends T>, ? super Collection<? extends R>> performer) {
        this.performer = (Sense<T> a, Sense<R> b) -> {
            if (test2.test(a) && test1.test(b)) {
                performer.accept(a.all(), b.all());
            }
        };
        return factory.add(this);
    }

    public <S> SensitivityBuilder<S, FinishableActionBuilder3<T, R, S>> sensitiveTo(Class<S> type) {
        return new SensitivityBuilder<>(type, (tp, pred) -> {
            FinishableActionBuilder3<T, R, S> result = new FinishableActionBuilder3<>(this, type1, test1, type2, test2, tp, pred);
            if (multiTest != null) {
                result.multiTest = new ThreeForTwo<>(multiTest);
            }
            return result;
        });
    }

    private static final class ThreeForTwo<T, R, S> implements TriPredicate<Sense<T>, Sense<R>, Sense<S>> {

        private final BiPredicate<? super Sense<T>, ? super Sense<R>> delegate;

        public ThreeForTwo(BiPredicate<? super Sense<T>, ? super Sense<R>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean test(Sense<T> a, Sense<R> b, Sense<S> c) {
            return delegate.test(a, b);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + Objects.hashCode(this.delegate);
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
            final ThreeForTwo<?, ?, ?> other = (ThreeForTwo<?, ?, ?>) obj;
            return Objects.equals(this.delegate, other.delegate);
        }
    }

    public FinishableActionBuilder2<T, R> testingBothWith(BiPredicate<? super T, ? super R> test) {
        if (this.multiTest == null) {
            this.multiTest = new MultiBiTest<>(test);
        } else {
            this.multiTest = this.multiTest.and(new MultiBiTest(test));
        }
        return this;
    }

    public FinishableActionBuilder2<T, R> testingAllWith(BiPredicate<? super Collection<? extends T>, ? super Collection<? extends R>> test) {
        if (this.multiTest == null) {
            this.multiTest = new MultiAllTest2<>(test);
        } else {
            this.multiTest = this.multiTest.and(new MultiAllTest2(test));
        }
        return this;
    }

    private static class MultiAllTest2<T, R> implements BiPredicate<Sense<T>, Sense<R>> {

        private final BiPredicate<? super Collection<? extends T>, ? super Collection<? extends R>> test;

        public MultiAllTest2(BiPredicate<? super Collection<? extends T>, ? super Collection<? extends R>> test) {
            this.test = test;
        }

        @Override
        public boolean test(Sense<T> t, Sense<R> r) {
            Collection<? extends T> ta = t.all();
            Collection<? extends R> ra = r.all();
            return !ta.isEmpty() && !ra.isEmpty() && test.test(ta, ra);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.test);
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
            final MultiAllTest2<?, ?> other = (MultiAllTest2<?, ?>) obj;
            return this.test.equals(other.test);
        }

    }

    private static class MultiBiTest<T, R> implements BiPredicate<Sense<T>, Sense<R>> {

        private final BiPredicate<? super T, ? super R> test;

        public MultiBiTest(BiPredicate<? super T, ? super R> test) {
            this.test = test;
        }

        @Override
        public boolean test(Sense<T> t, Sense<R> r) {
            T to = t.get();
            R ro = r.get();
            return to != null && ro != null && test.test(to, ro);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.test);
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
            final MultiBiTest<?, ?> other = (MultiBiTest<?, ?>) obj;
            return this.test.equals(other.test);
        }
    }
}
