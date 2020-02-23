/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.awt.cache;

import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
class CachingSupplier<T> implements Supplier<T> {

    private final Supplier<T> delegate;
    T object;

    public CachingSupplier(Supplier<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T get() {
        if (object == null) {
            object = delegate.get();
        }
        return object;
    }

}
