/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.Objects;
import java.util.function.Predicate;

// Better to use actual classes for these, to ensure

// identity equality if the underlying predicate is the same
final class SingleTest<T> implements Predicate<Sense<T>> {

    private final Predicate<T> realTest;

    public SingleTest(Predicate<T> realTest) {
        this.realTest = realTest;
    }

    @Override
    public boolean test(Sense<T> t) {
        T obj = t.get();
        return obj != null && realTest.test(obj);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(this.realTest);
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
        if (!(obj instanceof SingleTest<?>)) {
            return false;
        }
        final SingleTest<?> other = (SingleTest<?>) obj;
        return realTest == other.realTest || realTest.equals(other.realTest);
    }

}
