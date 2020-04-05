/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AssertionFacade<T> {

    static final double TOL = 0.0000000001;

    protected T ca;

    AssertionFacade(T ca) {
        this.ca = ca;
        Assertions.assertNotNull(ca);
    }

    @Override
    public String toString() {
        return ca.toString();
    }

    protected static String msgs(String a, String b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return a + "; " + b;
        }
    }

    protected Supplier<String> msg(String message) {
        return msg(ca, message);
    }

    protected Supplier<String> msg(String msgA, String msgB) {
        return msg(msgs(msgA, msgB));
    }

    protected void assertSaneDegrees(double val, String msg) {
        Assertions.assertTrue(val >= 0);
        Assertions.assertTrue(val < 360, msg("Degrees out of range 0-360: " + val, msg));
    }

    protected void assertDouble(double expected, double val) {
        Assertions.assertEquals(expected, val, TOL);
    }

    protected void assertDouble(double expected, double val, String msg) {
        Assertions.assertEquals(expected, val, TOL, msg);
    }

    protected void assertDouble(double expected, double val, Supplier<String> msg) {
        Assertions.assertEquals(expected, val, TOL, msg);
    }

    protected void assertNotDouble(double expected, double val) {
        Assertions.assertNotEquals(expected, val, TOL);
    }

    protected void assertNotDouble(double expected, double val, String msg) {
        Assertions.assertEquals(expected, val, TOL, msg);
    }

    protected void assertNotDouble(double expected, double val, Supplier<String> msg) {
        Assertions.assertEquals(expected, val, TOL, msg);
    }

    static Supplier<String> msg(Object ca, String message) {
        return () -> {
            if (message == null) {
                return ca.toString();
            }
            return message + " in " + ca;
        };
    }
}
