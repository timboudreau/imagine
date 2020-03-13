/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.Collection;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
final class MultiConsumer<T> implements Consumer<Sense<T>> {

    private final Consumer<? super Collection<? extends T>> c;

    public MultiConsumer(Consumer<? super Collection<? extends T>> c) {
        this.c = c;
    }

    @Override
    public void accept(Sense<T> t) {
        c.accept(t.all());
    }

}
