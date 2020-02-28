/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.api.tool.aspects;

import java.util.function.Consumer;

/**
 * A customizer which can be listened on for changes.
 *
 * @author Tim Boudreau
 */
public interface ListenableCustomizer<T> extends Customizer<T> {

    /**
     * Add a consumer which shall be notified after the value
     * changes.  Note that the listener may be weakly referenced,
     * and so should be strongly referenced by the thing listening.
     *
     * @param consumer A consumer
     * @return A runnable to detach the listener
     */
    public Runnable listen(Consumer<? super T> consumer);
}
