/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import com.mastfrog.function.QuadConsumer;
import com.mastfrog.function.QuadPredicate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
public final class FinishableActionBuilder4<T, R, S, U> extends ActionBuilder<FinishableActionBuilder4<T, R, S, U>> {

    protected final Class<T> type2;
    protected final Predicate<Sense<T>> test2;
    protected final Class<R> type1;
    protected final Predicate<Sense<R>> test1;
    protected final Class<S> type3;
    protected final Predicate<Sense<S>> test3;
    protected final Class<U> type4;
    protected final Predicate<Sense<U>> test4;
    protected QuadConsumer<Sense<T>, Sense<R>, Sense<S>, Sense<U>> performer;
    protected QuadPredicate<Sense<T>, Sense<R>, Sense<S>, Sense<U>> multiTest;

    FinishableActionBuilder4(ActionBuilder other, Class<R> firstType, Predicate<Sense<R>> firstTest, Class<T> secondType, Predicate<Sense<T>> secondTest, Class<S> thirdType, Predicate<Sense<S>> thirdTest, Class<U> fourthType, Predicate<Sense<U>> fourthTest) {
        super(other);
        this.type2 = secondType;
        this.test2 = secondTest;
        this.type1 = firstType;
        this.test1 = firstTest;
        this.type3 = thirdType;
        this.test3 = thirdTest;
        this.type4 = fourthType;
        this.test4 = fourthTest;
    }

    public ActionsBuilder finish(QuadConsumer<? super T, ? super R, ? super S, ? super U> performer) {
        assert this.performer == null;
        this.performer = new Singles4<>(performer);
        return factory.add(this);
    }

    public ActionsBuilder finishMultiple(QuadConsumer<? super Collection<? extends T>, ? super Collection<? extends R>, ? super Collection<? extends S>, ? super Collection<? extends U>> performer) {
        assert this.performer == null;
        this.performer = (Sense<T> a, Sense<R> b, Sense<S> c, Sense<U> d) -> {
            performer.accept(a.all(), b.all(), c.all(), d.all());
        };
        return factory.add(this);
    }

    private static final class Singles4<T, R, S, U> implements QuadConsumer<Sense<T>, Sense<R>, Sense<S>, Sense<U>> {

        private final QuadConsumer<? super T, ? super R, ? super S, ? super U> performer;

        public Singles4(QuadConsumer<? super T, ? super R, ? super S, ? super U> perfomer) {
            this.performer = perfomer;
        }

        @Override
        public void accept(Sense<T> a, Sense<R> b, Sense<S> c, Sense<U> d) {
            performer.accept(a.get(), b.get(), c.get(), d.get());
        }

        @Override
        public String toString() {
            return "Singles(" + performer + ")";
        }
    }

    public FinishableActionBuilder4<T, R, S, U> testingGroupWith(QuadPredicate<? super T, ? super R, ? super S, ? super U> test) {
        if (this.multiTest == null) {
            this.multiTest = new MultiQuadTest<>(test);
        } else {
            this.multiTest = this.multiTest.and(new MultiQuadTest(test));
        }
        return this;
    }

    public FinishableActionBuilder4<T, R, S, U> testingAllWith(QuadPredicate<? super Collection<? extends T>, ? super Collection<? extends R>, ? super Collection<? extends S>, ? super Collection<? extends U>> test) {
        if (this.multiTest == null) {
            this.multiTest = new MultiAllTest4<>(test);
        } else {
            this.multiTest = this.multiTest.and(new MultiAllTest4(test));
        }
        return this;
    }

    private static class MultiAllTest4<T, R, S, U> implements QuadPredicate<Sense<T>, Sense<R>, Sense<S>, Sense<U>> {

        private final QuadPredicate<? super Collection<? extends T>, ? super Collection<? extends R>, ? super Collection<? extends S>, ? super Collection<? extends U>> test;

        MultiAllTest4(QuadPredicate<? super Collection<? extends T>, ? super Collection<? extends R>, ? super Collection<? extends S>, ? super Collection<? extends U>> test) {
            this.test = test;
        }

        @Override
        public boolean test(Sense<T> t, Sense<R> r, Sense<S> s, Sense<U> u) {
            Collection<? extends T> ta = t.all();
            Collection<? extends R> ra = r.all();
            Collection<? extends S> sa = s.all();
            Collection<? extends U> ua = u.all();
            return !ta.isEmpty() && !ra.isEmpty() && !sa.isEmpty() && !ua.isEmpty()
                    && test.test(ta, ra, sa, ua);
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
            final MultiAllTest4<?, ?, ?, ?> other = (MultiAllTest4<?, ?, ?, ?>) obj;
            return this.test.equals(other.test);
        }

    }

    private static class MultiQuadTest<T, R, S, U> implements QuadPredicate<Sense<T>, Sense<R>, Sense<S>, Sense<U>> {

        private final QuadPredicate<? super T, ? super R, ? super S, ? super U> test;

        public MultiQuadTest(QuadPredicate<? super T, ? super R, ? super S, ? super U> test) {
            this.test = test;
        }

        @Override
        public boolean test(Sense<T> t, Sense<R> r, Sense<S> s, Sense<U> u) {
            T to = t.get();
            R ro = r.get();
            S so = s.get();
            U uo = u.get();
            return to != null && ro != null && test.test(to, ro, so, uo);
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
            final MultiQuadTest<?, ?, ?, ?> other = (MultiQuadTest<?, ?, ?, ?>) obj;
            return this.test.equals(other.test);
        }
    }
}
