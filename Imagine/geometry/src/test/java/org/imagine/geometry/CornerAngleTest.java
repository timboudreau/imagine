package org.imagine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class CornerAngleTest {

    private static final double TOL = 0.0000000001;

    @Test
    public void testOppositeNorm() {
        CornerAngle ca = new CornerAngle(0, 90);
        assertEquals(90, ca.extent(), TOL);
        CornerAngle exp = new CornerAngle(180, 270);
        assertEquals(90, exp.extent(), TOL);
        CornerAngle opp = ca.opposite();
        assertEquals(exp, opp);
        assertEquals(180, opp.aDegrees(), TOL);
        assertEquals(270, opp.bDegrees(), TOL);
        assertEquals(90, opp.extent(), TOL);
        assertEquals(ca, opp.opposite());
    }

    @Test
    public void testOppositeInverse() {
        CornerAngle ca = new CornerAngle(90, 0);
        assertEquals(-90, ca.extent(), TOL);
        CornerAngle exp = new CornerAngle(270, 180);
        assertEquals(-90, exp.extent(), TOL);
        CornerAngle opp = ca.opposite();
//        assertEquals(-90, opp.extent(), TOL);
//        assertEquals(ca, opp.opposite());
    }

    @Test
    public void testOppositeStraddlesZero() {
        CornerAngle ca = new CornerAngle(180-45, 180+45);
        CornerAngle exp = new CornerAngle(45, 360-45);
        CornerAngle opp = ca.opposite();
        assertEquals(-90, opp.extent(), "Wrong extent: " + opp);
        assertEquals(exp, opp);
    }

    @Test
    public void testAngle() {
        CornerAngle ca = new CornerAngle(0, 90);
        assertEquals(0D, ca.aDegrees(), TOL);
        assertEquals(90D, ca.bDegrees(), TOL);
        assertEquals(90D, ca.extent(), TOL);
        assertTrue(ca.isRightAngle());
        assertSame(RotationDirection.CLOCKWISE, ca.direction());

        ca = new CornerAngle(10, 100);
        assertEquals(10D, ca.aDegrees(), TOL);
        assertEquals(100D, ca.bDegrees(), TOL);
        assertEquals(90D, ca.extent(), TOL);
        assertTrue(ca.isRightAngle());
        assertSame(RotationDirection.CLOCKWISE, ca.direction());

        ca = new CornerAngle(100, 10);
        assertEquals(100D, ca.aDegrees(), TOL);
        assertEquals(10D, ca.bDegrees(), TOL);
//        assertEquals(-90D, ca.extent(), TOL);
        assertTrue(ca.isRightAngle());
        assertSame(RotationDirection.COUNTER_CLOCKWISE, ca.direction());

        ca = ca.inverse();
        System.out.println("INV " + ca);
    }

}
