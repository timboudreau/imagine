package net.java.dev.imagine.ui.actions.spi;

/**
 *
 * @author Tim Boudreau
 */
public interface Selectable {

    void selectAll();

    void clearSelection();

    boolean canInvertSelection();

    void invertSelection();
}
