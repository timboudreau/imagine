/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.function.BiPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class HeteroSenseIt2<T, R> implements SenseIt {

    final BiPredicate<? super Sense<T>, ? super Sense<R>> predicate;
    final Sense<T> a;
    final Sense<R> b;

    HeteroSenseIt2(Sense<T> a, Sense<R> b, BiPredicate<? super Sense<T>, ? super Sense<R>> pred) {
        this.a = a;
        this.b = b;
        this.predicate = pred;
    }

    @Override
    public void listen(Runnable r) {
        a.listen(r);
        b.listen(r);
    }

    @Override
    public void unlisten(Runnable r) {
        a.unlisten(r);
        b.unlisten(r);
    }

    @Override
    public boolean getAsBoolean() {
        return predicate.test(a, b);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + predicate.hashCode();
        hash = 43 * hash + a.type.hashCode();
        hash = 43 * hash + b.type.hashCode();
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
        final HeteroSenseIt2<?, ?> other = (HeteroSenseIt2<?, ?>) obj;
        if (this.a.type != other.a.type || this.b.type != other.b.type) {
            return false;
        }
        return this.predicate == other.predicate || this.predicate.equals(other.predicate);
    }

}
