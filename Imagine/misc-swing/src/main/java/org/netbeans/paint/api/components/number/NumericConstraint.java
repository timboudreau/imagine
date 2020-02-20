package org.netbeans.paint.api.components.number;

/**
 * Models the constraints placed on numbers in various roles.
 *
 * @author Tim Boudreau
 */
public interface NumericConstraint {

    Number minimum();

    Number maximum();

    default boolean isMaximumLargestPossibleNumber() {
        return false;
    }

    default boolean isMinimumSmallestPossibleNumber() {
        return false;
    }

    default boolean isEntireNumberTypeRange() {
        return isMinimumSmallestPossibleNumber() && isMaximumLargestPossibleNumber();
    }

    default boolean canBeNegative() {
        return minimum().doubleValue() < 0;
    }

    default boolean isValidValue(Number num) {
        if (isFloatingPoint()) {
            if (isFloat()) {
                Number max = maximum();
                if (Float.compare(max.floatValue(), num.floatValue()) < 0) {
                    return false;
                }
                Number min = minimum();
                if (Float.compare(num.floatValue(), min.floatValue()) < 0) {
                    return false;
                }
            } else {
                Number max = maximum();
                if (Double.compare(max.doubleValue(), num.doubleValue()) < 0) {
                    return false;
                }
                Number min = minimum();
                if (Double.compare(num.doubleValue(), min.doubleValue()) < 0) {
                    return false;
                }
            }
        } else {
            Number max = maximum();
            if (Integer.compare(max.intValue(), num.intValue()) < 0) {
                return false;
            }
            Number min = minimum();
            if (Integer.compare(num.intValue(), min.intValue()) < 0) {
                return false;
            }
            if (Double.compare(max.doubleValue(), num.doubleValue()) < 0) {
                return false;
            }
            if (Double.compare(num.doubleValue(), min.doubleValue()) < 0) {
                return false;
            }
        }
        return true;
    }

    default Number constrain(Number num) {
        if (isFloatingPoint()) {
            Number max = maximum();
            if (Double.compare(max.doubleValue(), num.doubleValue()) < 0) {
                return max;
            }
            Number min = minimum();
            if (Double.compare(num.doubleValue(), min.doubleValue()) < 0) {
                return min;
            }
        } else {
            Number max = maximum();
            if (Integer.compare(max.intValue(), num.intValue()) < 0) {
                return max;
            }
            Number min = minimum();
            if (Integer.compare(num.intValue(), min.intValue()) < 0) {
                return min;
            }
            if (Double.compare(max.doubleValue(), num.doubleValue()) < 0) {
                return max;
            }
            if (Double.compare(num.doubleValue(), min.doubleValue()) < 0) {
                return min;
            }

        }
        return num;
    }

    default Number step() {
        return isFloatingPoint() ? 0.1D : 1;
    }

    default boolean isFloat() {
        return minimum() instanceof Float || maximum() instanceof Float;
    }

    default Number nextValue(int steps, Number last) {
        last = constrain(last);
        if (isFloatingPoint()) {
            if (isFloat()) {
                Number step = step();
                return constrain((steps * step.floatValue()) + last.floatValue()).floatValue();
            }
            Number step = step();
            return constrain((steps * step.doubleValue()) + last.doubleValue()).doubleValue();
        } else {
            Number step = step();
            return constrain((steps * step.intValue()) + last.intValue()).intValue();
        }
    }

    default Number prevValue(int steps, Number last) {
        last = constrain(last);
        if (isFloatingPoint()) {
            if (isFloat()) {
                Number step = step();
                return constrain(last.floatValue() - (steps * step.floatValue())).floatValue();
            }
            Number step = step();
            return constrain(last.doubleValue() - (steps * step.doubleValue())).doubleValue();
        } else {
            Number step = step();
            return constrain(last.intValue() - (steps * step.intValue())).intValue();
        }
    }

    default Number nextValue(double fractionalSteps, Number last) {
        last = constrain(last);
        if (isFloatingPoint()) {
            if (isFloat()) {
                Number step = step();
                return constrain((fractionalSteps * step.floatValue()) + last.floatValue()).floatValue();
            }
            Number step = step();
            return constrain((fractionalSteps * step.doubleValue()) + last.doubleValue()).doubleValue();
        } else {
            Number step = step();
            return constrain((fractionalSteps * step.intValue()) + last.intValue()).intValue();
        }
    }

    default Number prevValue(double fractionalSteps, Number last) {
        last = constrain(last);
        if (isFloatingPoint()) {
            if (isFloat()) {
                Number step = step();
                return constrain(last.floatValue() - (fractionalSteps * step.floatValue())).floatValue();
            }
            Number step = step();
            return constrain(last.doubleValue() - (fractionalSteps * step.doubleValue())).doubleValue();
        } else {
            Number step = step();
            return constrain(last.intValue() - (fractionalSteps * step.intValue())).intValue();
        }
    }

    default boolean isFloatingPoint() {
        Number a = minimum();
        Number b = minimum();
        return a instanceof Float || b instanceof Float
                || a instanceof Double || b instanceof Double;
    }

    default NumericConstraint withMinimum(Number minimum) {
        return new NumericConstraint() {
            @Override
            public Number minimum() {
                Number origMax = NumericConstraint.this.minimum();
                if (origMax instanceof Float) {
                    return minimum.floatValue();
                } else if (origMax instanceof Double) {
                    return minimum.doubleValue();
                } else if (origMax instanceof Integer) {
                    return minimum.intValue();
                }
                return minimum;
            }

            @Override
            public Number maximum() {
                return NumericConstraint.this.maximum();
            }
        };
    }

    default NumericConstraint withMaximum(Number maximum) {
        return new NumericConstraint() {
            @Override
            public Number minimum() {
                return NumericConstraint.this.minimum();
            }

            @Override
            public Number maximum() {
                Number origMax = NumericConstraint.this.maximum();
                if (origMax instanceof Float) {
                    return maximum.floatValue();
                } else if (origMax instanceof Double) {
                    return maximum.doubleValue();
                } else if (origMax instanceof Integer) {
                    return maximum.intValue();
                }
                return maximum;
            }
        };
    }

    default NumericConstraint withStep(Number step) {
        return new NumericConstraint() {
            @Override
            public Number step() {
                return isFloatingPoint() ? 0.1D : 1;
            }

            @Override
            public Number minimum() {
                return NumericConstraint.this.minimum();
            }

            @Override
            public Number maximum() {
                return NumericConstraint.this.maximum();
            }
        };
    }
}
