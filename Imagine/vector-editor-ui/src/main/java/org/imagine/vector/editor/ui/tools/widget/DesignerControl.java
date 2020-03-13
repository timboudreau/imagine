package org.imagine.vector.editor.ui.tools.widget;

import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
public interface DesignerControl {

    void shapeGeometryChanged(ShapeElement el);

    boolean controlPointDeleted(ShapeControlPoint pt);

    void pointCountMayBeChanged(ShapeElement el);

    boolean shapeAdded(ShapeElement el);

    boolean shapeDeleted(ShapeElement el);

    void shapeMayBeAdded();

    void shapesMayBeDeleted();

    void sync();

    void updateSelection(ShapeElement el);

    void updateSelection(ShapeControlPoint pt);
}
