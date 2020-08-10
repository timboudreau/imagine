/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import com.mastfrog.function.QuadPredicate;
import com.mastfrog.function.TriConsumer;
import com.mastfrog.function.TriPredicate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
public final class FinishableActionBuilder3<T, R, S> extends ActionBuilder<FinishableActionBuilder3<T,R,S>> {

    protected final Class<T> type2;
    protected final Predicate<Sense<T>> test2;
    protected final Class<R> type1;
    protected final Predicate<Sense<R>> test1;
    protected final Class<S> type3;
    protected final Predicate<Sense<S>> test3;
    protected TriConsumer<Sense<T>, Sense<R>, Sense<S>> performer;
    TriPredicate<Sense<T>, Sense<R>, Sense<S>> multiTest;

    FinishableActionBuilder3(ActionBuilder other, Class<R> firstType, Predicate<Sense<R>> firstTest, Class<T> secondType, Predicate<Sense<T>> secondTest, Class<S> thirdType, Predicate<Sense<S>> thirdTest) {
        super(other);
        this.type2 = secondType;
        this.test2 = secondTest;
        this.type1 = firstType;
        this.test1 = firstTest;
        this.type3 = thirdType;
        this.test3 = thirdTest;
    }

    public ActionsBuilder finish(TriConsumer<? super T, ? super R, ? super S> performer) {
        this.performer = (Sense<T> a, Sense<R> b, Sense<S> c) -> {
            performer.accept(a.get(), b.get(), c.get());
        };
        return factory.add(this);
    }

    public ActionsBuilder finishMultiple(TriConsumer<? super Collection<? extends T>, ? super Collection<? extends R>, ? super Collection<? extends S>> performer) {
        this.performer = (Sense<T> a, Sense<R> b, Sense<S> c) -> {
            performer.accept(a.all(), b.all(), c.all());
        };
        return factory.add(this);
    }

    public <U> SensitivityBuilder<U, FinishableActionBuilder4<T, R, S, U>> sensitiveTo(Class<U> type) {
        return new SensitivityBuilder<>(type, (tp, pred) -> {
            FinishableActionBuilder4<T, R, S, U> result = new FinishableActionBuilder4<>(this, type1, test1, type2, test2, type3, test3, tp, pred);
            if (multiTest != null) {
                result.multiTest = new FourForThree<>(multiTest);
            }
            return result;
        });
    }

    public FinishableActionBuilder3<T, R, S> testingGroupWith(TriPredicate<? super T, ? super R, ? super S> test) {
        if (this.multiTest == null) {
            this.multiTest = new MultiTriTest<>(test);
        } else {
            this.multiTest = this.multiTest.and(new MultiTriTest(test));
        }
        return this;
    }

    public FinishableActionBuilder3<T, R, S> testingAllWith(TriPredicate<? super Collection<? extends T>, ? super Collection<? extends R>, ? super Collection<? extends S>> test) {
        if (this.multiTest == null) {
            this.multiTest = new MultiAllTest3<>(test);
        } else {
            this.multiTest = this.multiTest.and(new MultiAllTest3(test));
        }
        return this;
    }

    private static class MultiAllTest3<T, R, S> implements TriPredicate<Sense<T>, Sense<R>, Sense<S>> {

        private final TriPredicate<? super Collection<? extends T>, ? super Collection<? extends R>, ? super Collection<? extends S>> test;

        MultiAllTest3(TriPredicate<? super Collection<? extends T>, ? super Collection<? extends R>, ? super Collection<? extends S>> test) {
            this.test = test;
        }

        @Override
        public boolean test(Sense<T> t, Sense<R> r, Sense<S> s) {
            Collection<? extends T> ta = t.all();
            Collection<? extends R> ra = r.all();
            Collection<? extends S> sa = s.all();
            return !ta.isEmpty() && !ra.isEmpty() && !sa.isEmpty()
                    && test.test(ta, ra, sa);
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
            final MultiAllTest3<?, ?, ?> other = (MultiAllTest3<?, ?, ?>) obj;
            return this.test.equals(other.test);
        }

    }

    private static class MultiTriTest<T, R, S> implements TriPredicate<Sense<T>, Sense<R>, Sense<S>> {

        private final TriPredicate<? super T, ? super R, ? super S> test;

        public MultiTriTest(TriPredicate<? super T, ? super R, ? super S> test) {
            this.test = test;
        }

        @Override
        public boolean test(Sense<T> t, Sense<R> r, Sense<S> s) {
            T to = t.get();
            R ro = r.get();
            S so = s.get();
            return to != null && ro != null && test.test(to, ro, so);
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
            final MultiTriTest<?, ?, ?> other = (MultiTriTest<?, ?, ?>) obj;
            return this.test.equals(other.test);
        }
    }

    private static final class FourForThree<T, R, S, U> implements QuadPredicate<Sense<T>, Sense<R>, Sense<S>, Sense<U>> {

        private final TriPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>> delegate;

        FourForThree(TriPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean test(Sense<T> a, Sense<R> b, Sense<S> c, Sense<U> d) {
            return delegate.test(a, b, c);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.delegate);
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
            final FourForThree<?, ?, ?, ?> other = (FourForThree<?, ?, ?, ?>) obj;
            if (!Objects.equals(this.delegate, other.delegate)) {
                return false;
            }
            return true;
        }
    }
}
