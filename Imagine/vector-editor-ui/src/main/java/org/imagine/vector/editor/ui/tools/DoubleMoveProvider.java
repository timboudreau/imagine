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
public interface DoubleMoveProvider {

    /**
     * Called to nofity about the start of movement of a specified widget.
     * @param widget the moving widget
     */
    void movementStarted (Widget widget);

    /**
     * Called to notify about the end of movement of a specified widget.
     * @param widget the moved widget
     */
    void movementFinished (Widget widget);

    /**
     * Called to acquire a origin location against which the movement will be calculated.
     * Usually it is a value of the Widget.getLocation method.
     * @param widget the moving widget
     * @return the origin location
     */
    Point2D getOriginalLocation (Widget widget);

    /**
     * Called to set a new location of a moved widget. The new location is based on the location returned by getOriginalLocation method.
     * Usually it is implemented as the Widget.setPreferredLocation method call.
     * @param widget the moved widget
     * @param location the new location
     */
    void setNewLocation (Widget widget, Point2D location);

}
