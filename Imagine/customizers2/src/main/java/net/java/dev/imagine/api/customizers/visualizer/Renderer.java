package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 *
 * @author tim
 */
public interface Renderer<T> {

    Dimension getSize(T t);

    void paint(Graphics2D g, T t, int x, int y);
    
}
