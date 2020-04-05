/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class AngleTest {

    private static final double TOL = 0.0000000001;
    private static final double AMT = 0.41225;

    @Test
    public void testSomeMethod() {
        assertEquals(0D, Angle.normalize(-360), TOL);
        assertEquals(0D, Angle.normalize(360), TOL);
        assertEquals(1D, Angle.normalize(361), TOL);
        assertEquals(0D, Angle.normalize(-720), TOL);
        for (int i = 360; i <= 720 * 3; i++) {
            double d = i;
            double n = Angle.normalize(d);
            if (i % 360 == 0) {
                assertEquals(n, 0D, TOL);
            } else {
                assertEquals(n, (double) (i % 360), TOL);
            }
            d += AMT;
            double exp = (i % 360) + AMT;
            assertEquals(exp, Angle.normalize(d), TOL);
        }
        for (int i = -720 * 3; i < 0; i++) {
            int exp = 360 - ((-i) % 360);
            if (exp % 360 == 0) {
                exp = 0;
            }
            double n = Angle.normalize(i);
            assertEquals((double) exp, n, TOL, i
                    + " exp should normalize to " + exp);
            double x = exp + 0.4112;
            double t = i + 0.4112;
            n = Angle.normalize(t);
            assertEquals(x, n, TOL, t + " should normalize to " + x);
        }
    }

}
