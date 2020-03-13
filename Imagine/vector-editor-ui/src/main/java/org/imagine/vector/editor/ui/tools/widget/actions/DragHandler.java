package org.imagine.vector.editor.ui.tools.widget.actions;

import java.awt.geom.Point2D;
import org.netbeans.api.visual.widget.Widget;

/**
 * Callback interface for processing drag updates from a
 * MoveInSceneCoordinateSpaceAction.
 */
public interface DragHandler {

    /**
     * Probably useless. Called on the initial mouse press.
     *
     * @param w The widget
     * @param original The point, <i>in scene coordinates</i>
     */
    default boolean onPotentialDrag(Widget w, Point2D original) {
        return true;
    }

    /**
     * Called on the first drag event where the event point is not the same as
     * the press-point - the mouse has moved.
     *
     * @param w The widget
     * @param original The mouse-pressed point, <i>in absolute scene
     * coordinates</i>
     * @param current The current drag point, <i>in absolute scene
     * coordinates</i>
     */
    void onBeginDrag(Widget w, Point2D original, Point2D current);

    /**
     * Equivalent to MoveStrategy from Visual Library - allows the point to
     * really use to be snapped to a position.
     *
     * @param w The widget
     * @param original The mouse-pressed point, <i>in absolute scene
     * coordinates</i>
     * @param suggested The <i>suggested</i> drag point, <i>in absolute scene
     * coordinates</i>
     * @return The drag point to use instead of the suggested one
     */
    default Point2D snap(Widget w, Point2D original, Point2D suggested) {
        return suggested;
    }

    /**
     * Called when a drag event occurs which is not the <i>initial</i>
     * drag event off the press-point.
     *
     * @param w The widget
     * @param original The mouse-pressed point, <i>in absolute scene
     * coordinates</i>
     * @param current The current drag point, <i>in absolute scene
     * coordinates</i>
     */
    void onDrag(Widget w, Point2D original, Point2D current);

    /**
     * Called when the mouse is released.
     *
     * @param w The widget
     * @param original The mouse-pressed point, <i>in absolute scene
     * coordinates</i>
     * @param current The current drag point, <i>in absolute scene
     * coordinates</i>
     */
    void onEndDrag(Widget w, Point2D original, Point2D current);

    /**
     * Called when dragging is cancelled for some reason (focus loss, release
     * out-of-bounds or similar).
     *
     * @param w The widget
     * @param original The mouse-pressed point, <i>in absolute scene
     * coordinates</i>
     */
    void onCancelDrag(Widget w, Point2D original);

}
