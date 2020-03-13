/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
final class SingleConsumer<T> implements Consumer<Sense<T>> {

    private final Consumer<? super T> consumer;

    public SingleConsumer(Consumer<? super T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(Sense<T> t) {
        consumer.accept(t.get());
    }

}
