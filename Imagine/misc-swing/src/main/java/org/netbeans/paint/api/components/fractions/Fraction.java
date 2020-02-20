package org.netbeans.paint.api.components.fractions;

import java.text.DecimalFormat;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
public final class Fraction implements Comparable<Fraction> {

    float value;
    private final FractionValidator min;
    private final FractionValidator max;
    private final Consumer<Fraction> onChange;
    private static final DecimalFormat FMT = new DecimalFormat("0.######");

    Fraction(float value, FractionValidator min, FractionValidator max, Consumer<Fraction> onChange) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
    }

    @Override
    public String toString() {
        return FMT.format(value);
    }

    public boolean isZero() {
        return 0F == value;
    }

    public boolean isOne() {
        return 1f == value;
    }

    public boolean isEnd() {
        return value == 0.0F || value == 1.0F;
    }

    public boolean isValid() {
        return min.validate(this, value) && max.validate(this, value);
    }

    public float getValue() {
        return value;
    }

    public boolean canSet(float value) {
        if (value >= 1.0F || value <= 0.0F) {
            return false;
        }
        if (value == this.value) {
            return false;
        }
        return min.validate(this, value) && max.validate(this, value);
    }

    public boolean setValue(float value) {
        if (value >= 1.0F || value <= 0.0F) {
            return false;
        }
        if (value != this.value) {
            if (min.validate(this, value) && max.validate(this, value)) {
                this.value = value;
                onChange.accept(this);
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Fraction o) {
        return Float.compare(value, o.value);
    }

}
