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

    @Override
    public String toString() {
        switch (this) {
            case CLOCKWISE:
                return "cw";
            case COUNTER_CLOCKWISE:
                return "ccw";
            case NONE:
                return "none";
            default:
                throw new AssertionError(this);
        }
    }

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

    /**
     * Get the opposite direction of rotation.
     *
     * @return The opposite
     */
    public RotationDirection opposite() {
        switch (this) {
            case CLOCKWISE:
                return COUNTER_CLOCKWISE;
            case COUNTER_CLOCKWISE:
                return CLOCKWISE;
            default:
                return this;
        }
    }
}
