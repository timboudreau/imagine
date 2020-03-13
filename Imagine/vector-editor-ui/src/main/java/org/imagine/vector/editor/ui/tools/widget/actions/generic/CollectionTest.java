/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class CollectionTest<T> implements Predicate<Sense<T>> {

    private final Predicate<? super Collection<? extends T>> delegate;

    public CollectionTest(Predicate<? super Collection<? extends T>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean test(Sense<T> t) {
        return delegate.test(t.all());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.delegate);
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
        if (!(obj instanceof CollectionTest<?>)) {
            return false;
        }
        final CollectionTest<?> other = (CollectionTest<?>) obj;
        return delegate.equals(other.delegate);
    }

}
