/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.geom.Point2D;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public interface DoubleMoveStrategy {

    /**
     * Called after an user suggests a new location and before the suggested
     * location is stored to a specified widget. This allows to manipulate with
     * a suggested location to perform snap-to-grid, locked-axis on any other
     * movement strategy.
     *
     * @param widget the moved widget
     * @param originalLocation the original location specified by the
     * MoveProvider.getOriginalLocation method
     * @param suggestedLocation the location suggested by an user (usually by a
     * mouse cursor position)
     * @return the new (optional modified) location processed by the strategy
     */
    Point2D locationSuggested(Widget widget, Point2D originalLocation, Point2D suggestedLocation);

    public static DoubleMoveStrategy FREE = (w, o, s) -> {
        return s;
    };
}
