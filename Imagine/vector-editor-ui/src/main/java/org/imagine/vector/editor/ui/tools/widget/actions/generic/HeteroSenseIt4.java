package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import com.mastfrog.function.QuadPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class HeteroSenseIt4<T, R, S, U> implements SenseIt {

    final QuadPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>, ? super Sense<U>> predicate;
    final Sense<T> a;
    final Sense<R> b;
    final Sense<S> c;
    final Sense<U> d;

    HeteroSenseIt4(Sense<T> a, Sense<R> b, Sense<S> c, Sense<U> d,
            QuadPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>, ? super Sense<U>> pred) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.predicate = pred;
    }

    @Override
    public void listen(Runnable r) {
        a.listen(r);
        b.listen(r);
        c.listen(r);
        d.listen(r);
    }

    @Override
    public void unlisten(Runnable r) {
        a.unlisten(r);
        b.unlisten(r);
        c.unlisten(r);
        d.unlisten(r);
    }

    @Override
    public boolean getAsBoolean() {
        return predicate.test(a, b, c, d);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + predicate.hashCode();
        hash = 73 * hash + a.type.hashCode();
        hash = 73 * hash + b.type.hashCode();
        hash = 73 * hash + c.type.hashCode();
        hash = 73 * hash + d.type.hashCode();
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
        final HeteroSenseIt3<?, ?, ?> other = (HeteroSenseIt3<?, ?, ?>) obj;
        if (this.a == other.a && this.b == other.b && this.c == other.c) {
            return this.predicate == other.predicate || this.predicate.equals(other.predicate);
        }
        if (this.a.type != other.a.type || this.b.type != other.b.type || this.c.type != other.c.type) {
            return false;
        }
        return this.predicate == other.predicate || this.predicate.equals(other.predicate);
    }

}
