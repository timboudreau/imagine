/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
public final class FinishableActionBuilder1<T> extends ActionBuilder<FinishableActionBuilder1<T>> {

    protected final Class<T> type;
    protected final Predicate<Sense<T>> test;
    private Consumer<? super T> performer;
    private Consumer<? super Collection<? extends T>> multiPerformer;

    FinishableActionBuilder1(ActionBuilder1 other, Class<T> type, Predicate<Sense<T>> test) {
        super(other);
        this.type = type;
        this.test = test;
    }

    public ActionsBuilder finish(Consumer<? super T> performer) {
        this.performer = performer;
        return factory.add(this);
    }

    public ActionsBuilder finishMultiple(Consumer<? super Collection<? extends T>> performer) {
        this.multiPerformer = performer;
        return factory.add(this);
    }

    Consumer<Sense<T>> consumer() {
        if (performer != null) {
            return super.<T>single(performer);
        } else if (multiPerformer != null) {
            return multi(multiPerformer);
        }
        throw new AssertionError("No performer");
    }

    public <R> SensitivityBuilder<R, FinishableActionBuilder2<T, R>> sensitiveTo(Class<R> type) {
        return new SensitivityBuilder<>(type, (tp, pred) -> {
            return new FinishableActionBuilder2<>(this, tp, pred, this.type, test);
        });
    }
}
