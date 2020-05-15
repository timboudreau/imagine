/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry.path;

/**
 *
 * @author Tim Boudreau
 */
public enum PointKind {

    QUADRATIC_FIRST_CONTROL_POINT,
    QUADRATIC_DESTINATION_POINT,
    CUBIC_FIRST_CONTROL_POINT,
    CUBIC_SECOND_CONTROL_POINT,
    CUBIC_DESTINATION_POINT,
    LINE_DESTINATION_POINT,
    MOVE_DESTINATION_POINT;

    public int pointIndex() {
        switch (this) {
            case LINE_DESTINATION_POINT:
            case MOVE_DESTINATION_POINT:
            case CUBIC_FIRST_CONTROL_POINT:
            case QUADRATIC_FIRST_CONTROL_POINT:
                return 0;
            case CUBIC_SECOND_CONTROL_POINT:
            case QUADRATIC_DESTINATION_POINT:
                return 2;
            case CUBIC_DESTINATION_POINT:
                return 3;
            default:
                throw new AssertionError(this);
        }
    }

    public int arrayPositionOffset() {
        return pointIndex() * 2;
    }

    public PathElementKind elementKind() {
        switch (this) {
            case QUADRATIC_FIRST_CONTROL_POINT:
            case QUADRATIC_DESTINATION_POINT:
                return PathElementKind.QUADRATIC;
            case CUBIC_FIRST_CONTROL_POINT:
            case CUBIC_SECOND_CONTROL_POINT:
            case CUBIC_DESTINATION_POINT:
                return PathElementKind.CUBIC;
            case LINE_DESTINATION_POINT:
                return PathElementKind.LINE;
            case MOVE_DESTINATION_POINT:
                return PathElementKind.MOVE;
            default:
                throw new AssertionError(this);
        }
    }

    public boolean isDestination() {
        switch (this) {
            case CUBIC_DESTINATION_POINT:
            case LINE_DESTINATION_POINT:
            case QUADRATIC_DESTINATION_POINT:
            case MOVE_DESTINATION_POINT:
                return true;
            default:
                return false;
        }
    }
}
