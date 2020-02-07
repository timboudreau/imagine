/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.api.util;

import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
final class HolderImpl<T> implements Holder<T> {

    private T val;

    HolderImpl() {

    }

    HolderImpl(T val) {
        this.val = val;
    }

    @Override
    public T get() {
        return val;
    }

    @Override
    public void set(T t) {
        val = t;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.val);
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
        final HolderImpl<?> other = (HolderImpl<?>) obj;
        if (!Objects.equals(this.val, other.val)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Objects.toString(get());
    }

}
