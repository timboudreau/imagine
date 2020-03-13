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
final class IndividualTest<T> implements Predicate<Sense<T>> {

    private final Predicate<T> delegate;

    public IndividualTest(Predicate<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean test(Sense<T> t) {
        Collection<? extends T> all = t.all();
        if (all.isEmpty()) {
            return false;
        }
        for (T obj : all) {
            if (!delegate.test(obj)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.delegate);
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
        if (!(obj instanceof IndividualTest<?>)) {
            return false;
        }
        final IndividualTest<?> other = (IndividualTest<?>) obj;
        return this.delegate == other.delegate || this.delegate.equals(other.delegate);
    }

}
