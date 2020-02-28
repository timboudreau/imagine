/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.spi.palette;

import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
public interface PaletteHandler {

    public String displayName();

    public <T> Consumer<T> saver(T obj);
}
