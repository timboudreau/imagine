package net.dev.java.imagine.api.tool.aspects;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Tools can provide this interface in lieu of MouseListener in order to handle
 * mouse events correctly at high zoom - when editing a 16x16 icon zoomed in
 * 20x, only being able to get coordinates from 0 to 15 is useless.
 *
 * @author Tim Boudreau
 */
public interface ScalingMouseListener {

    default void mouseMoved(double x, double y, MouseEvent e) {
        // do nothing
    }

    default void mouseDragged(double x, double y, MouseEvent e) {
        // do nothing
    }

    default void mouseWheelMoved(double x, double y, MouseWheelEvent e) {
        // do nothing
    }

    default void mouseExited(double x, double y, MouseEvent e) {
        // do nothing
    }

    default void mouseEntered(double x, double y, MouseEvent e) {
        // do nothing
    }

    default void mouseReleased(double x, double y, MouseEvent e) {
        // do nothing
    }

    default void mousePressed(double x, double y, MouseEvent e) {
        // do nothing
    }

    default void mouseClicked(double x, double y, MouseEvent e) {
        // do nothing
    }
}
