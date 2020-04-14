package org.imagine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class EqLineTest {

    @Test
    public void testMidPoint() {
        EqLine ln = new EqLine(-10, 0, 10, 0);
        assertEquals(new EqPointDouble(0, 0), ln.midPoint());
        ln = new EqLine(0, -10, 0, 10);
        assertEquals(new EqPointDouble(0, 0), ln.midPoint());
        ln = new EqLine(10, 10, -10, -10);
        assertEquals(new EqPointDouble(0, 0), ln.midPoint());
        ln = new EqLine(-10, 0, 10, 0);
        assertEquals(new EqPointDouble(0, 0), ln.midPoint());
    }

    @Test
    public void testAngle() {
        EqLine ln = new EqLine(-10, -10, 10, 10);
        assertEquals(135D, ln.angle(), 0.0000000001);

        ln = new EqLine(10, 10, -10, -10);
        assertEquals(360 - 45D, ln.angle(), 0.0000000001);

        ln = new EqLine(-10, 10, 10, -10);
        assertEquals(45D, ln.angle(), 0.0000000001);

        ln = new EqLine(10, -10, -10, 10);
        assertEquals(225D, ln.angle(), 0.0000000001);
    }

    @Test
    public void testSetAngle() {
        EqLine ln = new EqLine(0, 0, 0, 10);
        ln.setAngle(90);
        assertEquals(new EqLine(0, 0, 10, 0), ln);
        assertEquals(90D, ln.angle(), 0.000000000001);
        assertEquals(10, ln.length(), 0.000000000001);

        ln.setAngleAndLength(180, 20);
        assertEquals(20, ln.length());
        assertEquals(new EqLine(0, 0, 0, 20), ln);
    }
}
