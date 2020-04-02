/*
 * Adjustable.java
 *
 * Created on November 1, 2006, 6:56 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 * Represents a graphical element whose size, rotation or position may be
 * adjusted.
 *
 * @author Tim Boudreau
 */
public interface Adjustable extends Primitive {

    /**
     * Get the number of user-manipulable control points for this object
     */
    int getControlPointCount();

    /**
     * Get the control points of this object in an array of coordinates
     * x,y,x,y...
     *
     * @param xy An array of doubles to write into, which must be at least as
     * large as the value returned by getControlPointCount()
     */
    void getControlPoints(double[] xy);

    /**
     * Get the set of control point indices which are virtual (control points
     * for quadratic or cubic curves).
     *
     * @return An array
     */
    int[] getVirtualControlPointIndices();

    /**
     * Change the control point's location.
     *
     * @param pointIndex An index
     * @param location The location
     */
    void setControlPointLocation(int pointIndex, Pt location);

    /**
     * Get the type of each point.
     *
     * @return An array of kinds
     */
    ControlPointKind[] getControlPointKinds();


    default boolean isVirtualControlPoint(int index) {
        int[] virts = getVirtualControlPointIndices();
        if (virts.length == 0) {
            return false;
        }
        return Arrays.binarySearch(virts, index) >= 0;
    }

    /**
     * Get the set of control point kinds this point could be changed to.
     *
     * @param point The point index
     * @return A set of kinds - generally empty except in paths objects
     */
    default Set<ControlPointKind> availablePointKinds(int point) {
        return Collections.emptySet();
    }

    default boolean hasReadOnlyControlPoints() {
        return false;
    }

    default boolean isControlPointReadOnly(int index) {
        return hasReadOnlyControlPoints();
    }

}
