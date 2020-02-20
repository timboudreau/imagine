package org.netbeans.paint.api.components.points;

/**
 *
 * @author Tim Boudreau
 */
public enum PointSelectorMode {

    POINT_ONLY,
    POINT_AND_ANGLE,
    POINT_AND_LINE;

    public boolean isShowAngle() {
        return this == POINT_AND_ANGLE
                || this == POINT_AND_LINE;
    }

    public boolean isDrawable() {
        return this == POINT_AND_LINE;
    }
}
