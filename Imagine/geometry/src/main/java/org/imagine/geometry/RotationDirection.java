package org.imagine.geometry;

/**
 * Direction of motion around a circle.
 *
 * @author Tim Boudreau
 */
public enum RotationDirection {

    /**
     * Clockwise rotation.
     */
    CLOCKWISE,
    /**
     * Counter clockwise rotation.
     */
    COUNTER_CLOCKWISE,
    /**
     * Rotation cannot be computed (used when, say, returning the difference
     * between two angles that are the same).
     */
    NONE;

    /**
     * Adjust an angle by the passed amount in this direction.
     *
     * @param angle The angle
     * @param byDegrees The degrees
     * @return Another angle
     */
    public double adjustAngle(double angle, double byDegrees) {
        switch (this) {
            case CLOCKWISE:
                return Angle.addAngles(angle, byDegrees);
            case COUNTER_CLOCKWISE:
                return Angle.subtractAngles(angle, byDegrees);
            case NONE:
                return angle;
            default:
                throw new AssertionError(this);
        }
    }
}
