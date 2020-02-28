/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.dev.java.imagine.api.palette;

import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
public final class PaletteSaver<T> implements Consumer<T> {

    private final Consumer<T> saver;
    private final String displayName;

    public PaletteSaver(Consumer<T> saver, String displayName) {
        this.saver = saver;
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public void accept(T t) {
        saver.accept(t);
    }

}
