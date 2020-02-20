package org.netbeans.paint.api.components.number;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class NumberModelTest {

    @Test
    public void testSomeMethod() {
        for (StandardNumericConstraints c : StandardNumericConstraints.values()) {
            NumberHolder<?> holder;
            switch (c) {
                case DOUBLE:
                case DOUBLE_DEGREES:
                case DOUBLE_NON_NEGATIVE:
                case DOUBLE_ZERO_TO_ONE:
                    holder = new NumberHolder<Double>(Double.class, 0D);
                    break;
                case FLOAT:
                case FLOAT_NON_NEGATIVE:
                case FLOAT_ZERO_TO_ONE:
                    holder = new NumberHolder<Float>(Float.class, 0F);
                    break;
                case INTEGER:
                case INTEGER_DEGREES:
                case INTEGER_NON_NEGATIVE:
                    holder = new NumberHolder<Integer>(Integer.class, 0);
                    break;
                default:
                    throw new AssertionError(c);
            }
            testOne(c, holder);
        }
    }

    public void testSetAsString() {
        NumberHolder<Integer> h = new NumberHolder<>(Integer.class, 50);
        NumberModel<Integer> mdl = NumberModel.ofInt(StandardNumericConstraints.INTEGER_DEGREES, h::getAsInt, h::setAsInt);
        assertEquals("50", mdl.stringValue());
        mdl.setValue(51);
        assertEquals(51, h.getAsInt());
        assertTrue(mdl.setValue("52"));
        assertEquals(52, h.getAsInt());
        assertFalse(mdl.setValue("-52"));
        assertEquals(52, h.getAsInt());

        assertTrue(mdl.setValue(" 53"));
        assertEquals(53, h.getAsInt());

        assertTrue(mdl.setValue("54 "));
        assertEquals(54, h.getAsInt());

        assertFalse(mdl.setValue("361"));
        assertEquals(54, h.getAsInt());
    }

    private <T extends Number> void testOne(NumericConstraint con, NumberHolder<T> holder) {
        NumberModel<T> mdl = holder.model(con);
        mdl.set((T) con.minimum());
        assertEquals(con.minimum().intValue(), holder.getAsInt(), "Bad value for " + con);
        assertEquals(con.minimum().doubleValue(), holder.getAsDouble(), 0.000001, "Bad value for " + con);
        mdl.set((T) con.maximum());
        assertEquals(con.maximum().intValue(), holder.getAsInt(), "Bad value for " + con);
        assertEquals(con.maximum().doubleValue(), holder.getAsDouble(), 0.000001, "Bad value for " + con);

        if (!con.isMaximumLargestPossibleNumber() && !con.isMinimumSmallestPossibleNumber()) {
            for (;;) {
                T old = mdl.get();
                Number hh = holder.value;
                Number next = con.nextValue(1, holder.value);
                if (next.equals(con.maximum())) {
                    break;
                }
                mdl.set((T) next);
                assertEquals(next, holder.value);
                assertNotEquals(old, mdl.get(), "Value unchanged");
                assertNotEquals(hh, holder.value, "Holder value unchanged");
                String sv = mdl.stringValue();
                assertTrue(mdl.setValue(sv));
                assertEquals(next.doubleValue(), holder.value.doubleValue(), 0.0000001D, sv);
                if (next.equals(con.maximum()) || next.doubleValue() > con.maximum().doubleValue()) {
                    break;
                }
                if (con instanceof StandardNumericConstraints && ((StandardNumericConstraints) con).isDegrees()) {
                    if (next.doubleValue() >= 359) {
                        break;
                    }
                }
            }
        }
    }

    static class NumberHolder<T extends Number> {

        private final Class<T> type;
        Number value;

        public NumberHolder(Class<T> type, T initialValue) {
            this.type = type;
        }

        NumberModel<T> model(NumericConstraint con) {
            if (type == Integer.class) {
                return (NumberModel<T>) intModel(con);
            } else if (type == Double.class) {
                return (NumberModel<T>) doubleModel(con);
            } else if (type == Float.class) {
                return (NumberModel<T>) floatModel(con);
            } else {
                fail("Huh? " + type);
                return null;
            }
        }

        NumberModel<Double> doubleModel(NumericConstraint constraint) {
            return NumberModel.ofDouble(constraint, this::getAsDouble, this::setAsDouble);
        }

        NumberModel<Float> floatModel(NumericConstraint constraint) {
            return NumberModel.ofFloat(constraint, this::getAsFloat, this::setAsFloat);
        }

        NumberModel<Integer> intModel(NumericConstraint constraint) {
            return NumberModel.ofInt(constraint, this::getAsInt, this::setAsInt);
        }

        void set(Number val) {
            this.value = val;
        }

        Number get() {
            return value;
        }

        void setAsDouble(double val) {
            set(val);
        }

        double getAsDouble() {
            return value.doubleValue();
        }

        void setAsFloat(float val) {
            set(val);
        }

        float getAsFloat() {
            return value.floatValue();
        }

        void setAsInt(int val) {
            set(val);
        }

        int getAsInt() {
            return value.intValue();
        }
    }
}
