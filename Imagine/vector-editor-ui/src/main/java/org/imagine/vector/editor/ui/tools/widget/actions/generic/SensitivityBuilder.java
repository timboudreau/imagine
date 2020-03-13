/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
public class SensitivityBuilder<T, R> {

    private final Class<T> type;
    private final BiFunction<Class<T>, Predicate<Sense<T>>, R> closer;

    SensitivityBuilder(Class<T> type, BiFunction<Class<T>, Predicate<Sense<T>>, R> closer) {
        this.type = type;
        this.closer = closer;
    }

    public R sensingAbsence() {
        return closer.apply(type, Sense::isEmpty);
    }

    public R sensingPresence() {
        return closer.apply(type, Sense::isNonEmpty);
    }

    public R sensingExactlyOne() {
        return closer.apply(type, Sense::isExactlyOne);
    }

    public R sensingMoreThanOne() {
        return closer.apply(type, Sense::isMoreThanOne);
    }

    public R testingAll(Predicate<Collection<? extends T>> test) {
        Predicate<Sense<T>> res = (t) -> {
            return test.test(t.all());
        };
        return closer.apply(type, res);
    }

    public R testingOne(Predicate<T> test) {
        return closer.apply(type, new SingleTest<>(test));
    }

    public R testingEach(Predicate<T> test) {
        return closer.apply(type, new IndividualTest<>(test));
    }

}
