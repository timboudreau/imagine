package org.imagine.vector.editor.ui.spi;

import java.util.Set;
import org.imagine.vector.editor.ui.ShapeEntry;

/**
 *
 * @author Tim Boudreau
 */
public interface ShapeCollectionListener {

    void shapesAddedOrRemoved(Set<ShapeEntry> added, Set<ShapeEntry> removed);

    void shapeReplaced(ShapeEntry entry);

    void shapesRemoved(Iterable<ShapeEntry> added);

    void controlPointSetChanged(ShapeEntry entry);

}
