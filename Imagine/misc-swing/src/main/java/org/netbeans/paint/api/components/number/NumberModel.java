package org.netbeans.paint.api.components.number;

import com.mastfrog.function.FloatConsumer;
import com.mastfrog.function.FloatSupplier;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.swing.BoundedRangeModel;
import javax.swing.SpinnerModel;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
public interface NumberModel<N extends Number> {

    static final NumberFormat DEFAULT_FLOAT_FMT = new DecimalFormat("##########0.00#########");
    static final NumberFormat DEFAULT_DOUBLE_FMT = new DecimalFormat("##########0.00#########");
    static final NumberFormat DEFAULT_INT_FMT = new DecimalFormat("##########0");

    NumericConstraint constraints();

    N get();

    default int maxCharacters() {
        NumberFormat fmt = getFormat();
        boolean neg = constraints().canBeNegative();
        int signChars = neg ? 1 : 0;
        int result;
        if (isFloatingPoint()) {
            result = fmt.getMaximumIntegerDigits()
                    + fmt.getMaximumFractionDigits() + signChars; // sign
            result = Math.max(result, 5);
        } else {
            result = fmt.getMaximumIntegerDigits() + signChars; // sign
            result = Math.max(result, 5);
        }
        if (result == 2147483637) {
            result = Integer.toString(Integer.MAX_VALUE).length();
        }
        return result;
    }

    default int minCharacters() {
        NumberFormat fmt = getFormat();
        boolean neg = constraints().canBeNegative();
        int signChars = neg ? 1 : 0;
        if (isFloatingPoint()) {
            return fmt.getMinimumIntegerDigits()
                    + fmt.getMinimumFractionDigits() + signChars; // sign
        } else {
            return fmt.getMinimumIntegerDigits() + signChars; // sign
        }
    }

    void set(N val);

    N setValue(Number val);

    boolean isFloatingPoint();

    default boolean isValid(String val) {
        if (val.isEmpty()) {
            return false;
        }
        try {
            Number parsed = getFormat().parse(val.trim());
            return constraints().isValidValue(parsed);
        } catch (ParseException ex) {
            return false;
        }
    }

    default boolean setValue(String val) {
        NumberFormat fmt = getFormat();
        try {
            Number num = fmt.parse(val.trim());
            setValue(num);
            return true;
        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }

    default NumberFormat getFormat() {
        N obj = get();
        if (obj instanceof Integer) {
            return DEFAULT_INT_FMT;
        } else if (obj instanceof Double) {
            return DEFAULT_DOUBLE_FMT;
        } else if (obj instanceof Float) {
            return DEFAULT_FLOAT_FMT;
        } else {
            return DEFAULT_DOUBLE_FMT;
        }
    }

    default String stringValue() {
        return getFormat().format(get());
    }

    default NumberModel<N> withFormat(String fmt) {
        return withFormat(new DecimalFormat(fmt));
    }

    N nextValue();

    N prevValue();

    N increment(int bySteps);

    N increment(double fractionalSteps);

    default N decrement(int bySteps) {
        return increment(-bySteps);
    }
    default N decrement(double fractionalSteps) {
        return increment(-fractionalSteps);
    }

    default N increment() {
        return setValue(nextValue());
    }

    default N decrement() {
        return setValue(prevValue());
    }

    default NumberModel<N> withFormat(NumberFormat fmt) {
        return new NumberModel<N>() {
            @Override
            public NumericConstraint constraints() {
                return NumberModel.this.constraints();
            }

            public NumberFormat getFormat() {
                return fmt;
            }

            @Override
            public N get() {
                return NumberModel.this.get();
            }

            @Override
            public void set(N val) {
                NumberModel.this.set(val);
            }

            @Override
            public N setValue(Number val) {
                return NumberModel.this.setValue(val);
            }

            @Override
            public boolean isFloatingPoint() {
                return NumberModel.this.isFloatingPoint();
            }

            @Override
            public String stringValue() {
                return fmt.format(get());
            }

            @Override
            public N increment(double fractionalSteps) {
                return NumberModel.this.increment(fractionalSteps);
            }

            @Override
            public N nextValue() {
                N result = NumberModel.this.nextValue();
                if (!NumberModel.this.constraints().isValidValue(result)) {
                    return get();
                }
                return NumberModel.this.nextValue();
            }

            @Override
            public N prevValue() {
                N result = NumberModel.this.prevValue();
                if (!NumberModel.this.constraints().isValidValue(result)) {
                    return get();
                }
                return NumberModel.this.prevValue();
            }

            @Override
            public N increment(int bySteps) {
                return NumberModel.this.increment(bySteps);
            }
        };
    }

    abstract class Abstract<T extends Number> implements NumberModel<T> {

        private final NumericConstraint constraints;
        private final Function<Number, T> converter;

        public Abstract(NumericConstraint constraints, Function<Number, T> converter) {
            this.constraints = constraints;
            this.converter = converter;
        }

        @Override
        public T setValue(Number val) {
            T result = converter.apply(constraints().constrain(val));
            set(result);
            return result;
        }

        @Override
        public NumericConstraint constraints() {
            return constraints;
        }

        @Override
        public T get() {
            return doGet();
        }

        @Override
        public T increment(int bySteps) {
            T result = setValue(converter.apply(constraints().nextValue(bySteps, get())));
            return result;
        }

        @Override
        public T increment(double fractionalSteps) {
            return setValue(converter.apply(constraints().nextValue(fractionalSteps, get())));
        }

        @Override
        public T nextValue() {
            Number n = constraints().nextValue(1, get());
            return converter.apply(n);
        }

        @Override
        public T prevValue() {
            Number n = constraints().prevValue(1, get());
            return converter.apply(n);
        }

        @Override
        public void set(T val) {
            Number constrained = constraints().constrain(val);
            if (constrained == val) {
                doSet((T) val);
            } else if (constrained.getClass() == val.getClass()) {
                doSet((T) constrained);
            } else {
                doSet(converter.apply(constrained));
            }
        }

        protected abstract T doGet();

        protected abstract void doSet(T val);
    }

    public static NumberModel<Double> ofDouble(NumericConstraint constraints, DoubleSupplier getter, DoubleConsumer setter) {
        assert constraints.isFloatingPoint();
        return new Abstract<Double>(constraints, Number::doubleValue) {
            @Override
            protected Double doGet() {
                return getter.getAsDouble();
            }

            @Override
            protected void doSet(Double val) {
                assert val != null;
                setter.accept(val);
            }

            @Override
            public boolean isFloatingPoint() {
                return true;
            }
        };
    }

    public static NumberModel<Float> ofFloat(NumericConstraint constraints, FloatSupplier getter, FloatConsumer setter) {
        assert constraints.isFloatingPoint() && constraints.isFloat();
        return new Abstract<Float>(constraints, Number::floatValue) {
            @Override
            protected Float doGet() {
                return getter.getAsFloat();
            }

            @Override
            protected void doSet(Float val) {
                assert val != null;
                setter.accept(val);
            }

            @Override
            public boolean isFloatingPoint() {
                return true;
            }
        };
    }

    public static NumberModel<Integer> ofInt(NumericConstraint constraints, IntSupplier getter, IntConsumer setter) {
        assert !constraints.isFloatingPoint();
        return new Abstract<Integer>(constraints, Number::intValue) {
            @Override
            protected Integer doGet() {
                return getter.getAsInt();
            }

            @Override
            protected void doSet(Integer val) {
                assert val != null;
                setter.accept(val);
            }

            @Override
            public boolean isFloatingPoint() {
                return false;
            }
        };
    }

    default SpinnerModel toSpinnerModel() {
        return new NumberModelSpinnerModel<>(this);
    }

    default BoundedRangeModel toBoundedRangeModel() {
        return new NumberModelBoundedRangeModel<>(this);
    }
}
