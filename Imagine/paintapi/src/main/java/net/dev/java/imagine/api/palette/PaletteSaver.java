/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.api.palette;

import com.mastfrog.function.TriConsumer;
import java.util.function.BiConsumer;

/**
 *
 * @author Tim Boudreau
 */
public final class PaletteSaver<T> implements BiConsumer<T, BiConsumer<Throwable, String>>, TriConsumer<String, T, BiConsumer<Throwable, String>> {

    private final TriConsumer<String, T, BiConsumer<Throwable, String>> saver;
    private final String displayName;

    PaletteSaver(TriConsumer<String, T, BiConsumer<Throwable, String>> saver, String displayName) {
        this.saver = saver;
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public void accept(T t, BiConsumer<Throwable, String> c) {
        saver.apply(null, t, c);
    }

    @Override
    public void apply(String name, T t, BiConsumer<Throwable, String> c) {
        saver.apply(name, t, c);
    }

}
