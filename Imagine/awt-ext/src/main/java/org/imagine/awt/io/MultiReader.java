/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.io;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

/**
 *
 * @author Tim Boudreau
 */
final class MultiReader<C extends ReadableByteChannel & SeekableByteChannel> {

    private final C channel;

    MultiReader(C channel) {
        this.channel = channel;
    }

    
}
