/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import com.mastfrog.function.TriPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class HeteroSenseIt3<T, R, S> implements SenseIt {

    final TriPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>> predicate;
    final Sense<T> a;
    final Sense<R> b;
    final Sense<S> c;

    HeteroSenseIt3(Sense<T> a, Sense<R> b, Sense<S> c, TriPredicate<? super Sense<T>, ? super Sense<R>, ? super Sense<S>> pred) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.predicate = pred;
    }

    @Override
    public void listen(Runnable r) {
        a.listen(r);
        b.listen(r);
        c.listen(r);
    }

    @Override
    public void unlisten(Runnable r) {
        a.unlisten(r);
        b.unlisten(r);
        c.unlisten(r);
    }

    @Override
    public boolean getAsBoolean() {
        return predicate.test(a, b, c);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 971 * hash + predicate.hashCode();
        hash = 971 * hash + a.type.hashCode();
        hash = 971 * hash + b.type.hashCode();
        hash = 971 * hash + c.type.hashCode();
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
        if (!(obj instanceof HeteroSenseIt3)) {
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
