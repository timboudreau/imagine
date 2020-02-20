package org.netbeans.paint.api.components.number;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class StandardNumericConstraintsTest {

    @Test
    public void testSomeMethod() {
        for (StandardNumericConstraints c : StandardNumericConstraints.values()) {
            switch (c) {
                case FLOAT:
                case FLOAT_NON_NEGATIVE:
                case FLOAT_ZERO_TO_ONE:
                    assertTrue(c.isFloat(), "Not float? " + c);
                    assertTrue(c.minimum() instanceof Float, " wrong type " + c.minimum().getClass());
                    assertTrue(c.maximum() instanceof Float, " wrong type " + c.maximum().getClass());
                    assertTrue(c.nextValue(0.75F, c.minimum()) instanceof Float);
                    assertTrue(c.prevValue(0.75F, c.maximum()) instanceof Float);
                    assertTrue(c.nextValue(0.75D, c.minimum()) instanceof Float);
                    assertTrue(c.prevValue(0.75D, c.maximum()) instanceof Float);
                case DOUBLE:
                case DOUBLE_DEGREES:
                case DOUBLE_NON_NEGATIVE:
                case DOUBLE_ZERO_TO_ONE:
                    assertTrue(c.isFloatingPoint(), "Not floating point? " + c);
                    break;
            }
            switch (c) {
                case DOUBLE_DEGREES:
                case DOUBLE_NON_NEGATIVE:
                case DOUBLE_ZERO_TO_ONE:
                case DOUBLE:
                    assertTrue(c.minimum() instanceof Double, " wrong type " + c.minimum().getClass());
                    assertTrue(c.maximum() instanceof Double, " wrong type " + c.maximum().getClass());
                    assertTrue(c.nextValue(0.75D, c.minimum()) instanceof Double);
                    assertTrue(c.prevValue(0.75D, c.maximum()) instanceof Double);
                    assertTrue(c.nextValue(0.75F, c.minimum()) instanceof Double);
                    assertTrue(c.prevValue(0.75F, c.maximum()) instanceof Double);
                    break;
                case INTEGER:
                case INTEGER_DEGREES:
                case INTEGER_NON_NEGATIVE:
                    assertTrue(c.minimum() instanceof Integer);
                    assertTrue(c.maximum() instanceof Integer);
                    assertTrue(c.nextValue(1.75D, c.minimum()) instanceof Integer);
                    assertTrue(c.prevValue(1.75D, c.maximum()) instanceof Integer);
                    assertTrue(c.nextValue(1.75F, c.minimum()) instanceof Integer);
                    assertTrue(c.prevValue(1.75F, c.maximum()) instanceof Integer);
                    assertTrue(c.nextValue(1, c.minimum()) instanceof Integer);
                    assertTrue(c.prevValue(1, c.maximum()) instanceof Integer);
                    break;
            }
            switch (c) {
                case DOUBLE:
                case FLOAT:
                case INTEGER:
                    assertTrue(c.isEntireNumberTypeRange(), "Entire range? " + c);
                    assertTrue(c.isMaximumLargestPossibleNumber(), "Max not largest? " + c);
                    assertTrue(c.isMinimumSmallestPossibleNumber(), "Min not smallest? " + c);
                    break;
                case DOUBLE_NON_NEGATIVE:
                case INTEGER_NON_NEGATIVE:
                case FLOAT_NON_NEGATIVE:
                    assertFalse(c.isValidValue(-1));
                    assertFalse(c.isEntireNumberTypeRange(), "Entire range? " + c);
                    assertTrue(c.isMaximumLargestPossibleNumber(), "Max not largest? " + c);
                    assertFalse(c.isMinimumSmallestPossibleNumber(), "Min smallest? " + c);
                    break;
                default:
                    assertFalse(c.isEntireNumberTypeRange(), "Entire range? " + c);
                    assertFalse(c.isMaximumLargestPossibleNumber(), "Max largest? " + c);
                    assertFalse(c.isMinimumSmallestPossibleNumber(), "Min smallest? " + c);

            }
            assertTrue(c.maximum().doubleValue() > c.minimum().doubleValue(), "Max < min: " + c.maximum() + " and " + c.minimum());
            switch (c) {
                case DOUBLE:
                    assertEquals(Double.MAX_VALUE, c.maximum().doubleValue());
                    assertEquals(Double.MIN_VALUE, c.minimum().doubleValue());
                    break;
                case FLOAT:
                    assertEquals(Float.MAX_VALUE, c.maximum().floatValue());
                    assertEquals(Float.MIN_VALUE, c.minimum().floatValue());
                    break;
                case INTEGER:
                    assertEquals(Integer.MAX_VALUE, c.maximum().floatValue());
                    assertEquals(Integer.MIN_VALUE, c.minimum().floatValue());
                    break;
                case DOUBLE_DEGREES:
                    assertEquals(360D, c.maximum().doubleValue(), 0.0001);
                    assertEquals(0D, c.minimum().doubleValue(), 0.0001);
                    assertTrue(c.isDegrees());
                    break;
                case DOUBLE_NON_NEGATIVE:
                    assertEquals(0D, c.minimum().doubleValue(), 0.00001);
                    assertEquals(Double.MAX_VALUE, c.maximum().doubleValue(), 0.0001);
                    break;
                case FLOAT_NON_NEGATIVE:
                    assertEquals(0F, c.minimum().floatValue(), 0.00001F);
                    assertEquals(Float.MAX_VALUE, c.maximum().floatValue(), 0.00001F);
                    break;
                case INTEGER_NON_NEGATIVE:
                    assertEquals(0, c.minimum().intValue());
                    assertEquals(Integer.MAX_VALUE, c.maximum().intValue());
                    break;
                case DOUBLE_ZERO_TO_ONE:
                    assertEquals(0D, c.minimum().doubleValue(), 0.00001);
                    assertEquals(1D, c.maximum().doubleValue(), 0.00001);
                    break;
                case FLOAT_ZERO_TO_ONE:
                    assertEquals(0F, c.minimum().floatValue(), 0.00001);
                    assertEquals(1F, c.maximum().floatValue(), 0.00001);
                    break;
                case INTEGER_DEGREES:
                    assertEquals(0, c.minimum().intValue());
                    assertEquals(360, c.maximum().intValue());
                    assertTrue(c.isDegrees());
                    break;

            }
            testOne(c);
        }
    }

    private void testOne(NumericConstraint c) {
        Number min = c.minimum();
        assertTrue(c.isValidValue(min), "Min " + min + " not valid for " + c + " " + min.getClass().getName());
        Number max = c.maximum();
        assertTrue(c.isValidValue(max), "Max " + max + " not valid for " + c + " type " + max.getClass().getName());

        if (!c.isMaximumLargestPossibleNumber()) {
            assertFalse(c.isValidValue(max.intValue() + 1));
            assertFalse(c.isValidValue(max.doubleValue() + 1));
            assertFalse(c.isValidValue(max.doubleValue() + 0.01));
            assertEquals(max, c.constrain(max.intValue() + 1));
            assertEquals(max, c.constrain(max.doubleValue() + 1));
            assertEquals(max, c.constrain(max.floatValue() + 1));
            assertEquals(max, c.constrain(max.doubleValue() + 0.001));
            assertEquals(max, c.constrain(max.floatValue() + 0.01));
        }
        if (!c.isMinimumSmallestPossibleNumber()) {
            assertFalse(c.isValidValue(min.intValue() - 1));
            assertFalse(c.isValidValue(min.doubleValue() - 1));
            assertEquals(min, c.constrain(min.intValue() - 1));
            assertEquals(min, c.constrain(min.doubleValue() - 1));
            assertEquals(min, c.constrain(min.floatValue() - 1));
            assertEquals(min, c.constrain(min.doubleValue() - 0.01),
                    c + " constraining double " + (min.doubleValue() - 0.01)
                    + " should return min " + min);
            assertEquals(min, c.constrain(min.floatValue() - 0.01));
        }
        if (!c.isMinimumSmallestPossibleNumber() && !c.isMaximumLargestPossibleNumber()) {
            Number curr = c.nextValue(1, min);
            while (!curr.equals(max)) {
                assertTrue(c.isValidValue(curr));
                Number nue = c.nextValue(1, curr);
                assertNotEquals(nue, curr,
                        "Got same value for next from " + curr
                        + " by " + 1);
                if (!nue.equals(max)) {
                    assertNotEquals(curr, nue);
                }
                curr = nue;
                if (c instanceof StandardNumericConstraints && ((StandardNumericConstraints) c).isDegrees()) {
                    if (nue.intValue() >= 359) {
                        break;
                    }
                }
            }
        }
    }
}
