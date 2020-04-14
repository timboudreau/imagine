package org.imagine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class AngleTest {

    private static final double TOL = 0.0000000001;
    private static final double AMT = 0.41225;

    @Test
    public void testForLine() {
        EqLine ln;
        Angle ang;

        ln = new EqLine(0, 0, 10, 0);
        ang = Angle.forLine(ln);
        assertEquals(90, ang.degrees(), TOL);

        ln = new EqLine(0, 0, 0, 10);
        ang = Angle.forLine(ln);
        assertEquals(180, ang.degrees(), TOL);

        ln = new EqLine(0, 10, 10, 0);
        ang = Angle.forLine(ln);
        assertEquals(45, ang.degrees(), TOL);

        ln = new EqLine(0, 0, 10, 10);
        ang = Angle.forLine(ln);
        assertEquals(135, ang.degrees(), TOL);

        ln = new EqLine(0, 0, -10, 0);
        ang = Angle.forLine(ln);
        assertEquals(270, ang.degrees(), TOL);

        assertEquals(90, Angle.canonicalize(270));

        assertEquals(270, ln.angle(), TOL);

        assertEquals(0, ln.x1, TOL);
        assertEquals(0, ln.y1, TOL);
        assertEquals(-10, ln.x2, TOL);
        assertEquals(0, ln.y2, TOL);

        System.out.println("Was " + ln);
        ln.setAngle(90);
        System.out.println("Is " + ln);
        assertEquals(90, ln.angle(), TOL);

        ln.setAngle(45);
        System.out.println("NOW " + ln);

        ln.setAngle(90);
        System.out.println("AND NOW " + ln);
    }

    @Test
    public void testLineIntersection() {
        EqLine a = new EqLine(90, 100, 110, 100);
        EqLine b = new EqLine(100, 90, 100, 110);
        EqPointDouble pt = a.intersection(b);
        System.out.println("ISECT " + pt);
        assertEquals(new EqPointDouble(100, 100), pt);

        EqLine c = new EqLine(90, 100, 57, 32);
        assertEquals(a.getP1(), a.intersection(c));
        assertEquals(a.getP1(), c.intersection(a));
        assertNull(a.intersection(a));
    }

    public void testCanonicalize() {
        for (int i = 0; i < 360; i++) {
            int exp = i % 180;
            double deg = Angle.canonicalize(i);
            assertEquals(exp, deg, TOL, i + " should canonicalize to " + exp);
        }
    }

    @Test
    public void testNormalize() {
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

    @Test
    public void testPerpendicular() {
        for (int ang = 0; ang < 361; ang++) {
            double cw = Angle.perpendicularClockwise(ang);
            int expCw = (ang + 90) % 360;

            double ccw = Angle.perpendicularCounterclockwise(ang);
            int expCcw = (ang - 90);
            if (expCcw < 0) {
                expCcw = 360 + expCcw;
            }
            expCcw %= 360;

            assertEquals((double) expCw, cw, "Clockwise of " + ang + " wrong");
            assertEquals((double) expCcw, ccw, "Counter-Clockwise of " + ang + " wrong");
        }
    }
}
