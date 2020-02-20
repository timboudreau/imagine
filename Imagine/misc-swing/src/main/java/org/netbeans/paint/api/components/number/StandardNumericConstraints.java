/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.number;

import com.mastfrog.function.FloatConsumer;
import com.mastfrog.function.FloatSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 *
 * @author Tim Boudreau
 */
public enum StandardNumericConstraints implements NumericConstraint {

    FLOAT,
    FLOAT_NON_NEGATIVE,
    DOUBLE,
    DOUBLE_NON_NEGATIVE,
    INTEGER,
    INTEGER_NON_NEGATIVE,
    INTEGER_DEGREES,
    DOUBLE_DEGREES,
    DOUBLE_ZERO_TO_ONE,
    FLOAT_ZERO_TO_ONE;

    public boolean isDegrees() {
        return this == INTEGER_DEGREES || this == DOUBLE_DEGREES;
    }

    @Override
    public boolean isMinimumSmallestPossibleNumber() {
        return this == FLOAT || this == DOUBLE || this == INTEGER;
    }

    @Override
    public boolean isMaximumLargestPossibleNumber() {
        return this == FLOAT || this == DOUBLE || this == INTEGER
                || this == FLOAT_NON_NEGATIVE || this == INTEGER_NON_NEGATIVE
                || this == DOUBLE_NON_NEGATIVE;
    }

    @Override
    public boolean isEntireNumberTypeRange() {
        return this == FLOAT || this == DOUBLE || this == INTEGER;
    }

    @Override
    public Number step() {
        switch (this) {
            case DOUBLE_ZERO_TO_ONE:
                return 0.001D;
            case FLOAT_ZERO_TO_ONE:
                return 0.001F;
            default:
                return NumericConstraint.super.step();
        }
    }

    @Override
    public Number nextValue(int steps, Number last) {
        switch (this) {
            case DOUBLE_DEGREES:
                double lv = last.doubleValue();
                if (lv < 0) {
                    return 0D;
                }
                double step = step().doubleValue();
                double next = Math.max(0, lv) + step;
                if (next > 360D) {
                    next -= 360D;
                }
                return next;
            case INTEGER_DEGREES:
                int iv = last.intValue();
                if (iv < 0) {
                    return 0;
                }
                int st = step().intValue();
                return (st + iv) % 360;
            default:
                return NumericConstraint.super.nextValue(steps, last);
        }
    }

    @Override
    public Number prevValue(int steps, Number last) {
        switch (this) {
            default:
                return NumericConstraint.super.prevValue(steps, last);
        }
    }

    @Override
    public boolean isValidValue(Number num) {
        switch (this) {
            case DOUBLE:
                if (num instanceof Double || num instanceof Float) {
                    return true;
                }
                break;
            case FLOAT:
                if (num instanceof Float) {
                    return true;
                }
                break;
            case INTEGER:
                if (num instanceof Integer) {
                    return true;
                }
                break;
        }
        return NumericConstraint.super.isValidValue(num);
    }

    @Override
    public Number minimum() {
        switch (this) {
            case FLOAT:
                return Float.MIN_VALUE;
            case FLOAT_NON_NEGATIVE:
                return 0F;
            case DOUBLE:
                return Double.MIN_VALUE;
            case DOUBLE_NON_NEGATIVE:
                return 0D;
            case INTEGER:
                return Integer.MIN_VALUE;
            case INTEGER_NON_NEGATIVE:
                return 0;
            case INTEGER_DEGREES:
                return 0;
            case DOUBLE_DEGREES:
                return 0D;
            case DOUBLE_ZERO_TO_ONE:
                return 0D;
            case FLOAT_ZERO_TO_ONE:
                return 0F;
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public Number maximum() {
        switch (this) {
            case FLOAT:
            case FLOAT_NON_NEGATIVE:
                return Float.MAX_VALUE;
            case DOUBLE:
            case DOUBLE_NON_NEGATIVE:
                return Double.MAX_VALUE;
            case INTEGER:
            case INTEGER_NON_NEGATIVE:
                return Integer.MAX_VALUE;
            case DOUBLE_DEGREES:
                return 360D;
            case INTEGER_DEGREES:
                return 360;
            case DOUBLE_ZERO_TO_ONE:
                return 1D;
            case FLOAT_ZERO_TO_ONE:
                return 1F;
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public boolean isFloatingPoint() {
        switch (this) {
            case DOUBLE:
            case DOUBLE_NON_NEGATIVE:
            case DOUBLE_DEGREES:
            case DOUBLE_ZERO_TO_ONE:
            case FLOAT:
            case FLOAT_NON_NEGATIVE:
            case FLOAT_ZERO_TO_ONE:
                return true;
            case INTEGER:
            case INTEGER_DEGREES:
            case INTEGER_NON_NEGATIVE:
                return false;
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public boolean canBeNegative() {
        switch (this) {
            case DOUBLE_NON_NEGATIVE:
            case DOUBLE_DEGREES:
            case DOUBLE_ZERO_TO_ONE:
            case FLOAT_ZERO_TO_ONE:
            case FLOAT_NON_NEGATIVE:
            case INTEGER_DEGREES:
            case INTEGER_NON_NEGATIVE:
                return false;
            case DOUBLE:
            case FLOAT:
            case INTEGER:
                return true;
            default:
                throw new AssertionError(this);
        }
    }

    public NumberModel<Double> doubleModel(DoubleSupplier getter, DoubleConsumer setter) {
        return NumberModel.ofDouble(this, getter, setter);
    }

    public NumberModel<Float> floatModel(FloatSupplier getter, FloatConsumer setter) {
        return NumberModel.ofFloat(this, getter, setter);
    }

    public NumberModel<Integer> intModel(IntSupplier getter, IntConsumer setter) {
        return NumberModel.ofInt(this, getter, setter);
    }
}
