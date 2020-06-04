package org.imagine.geometry;

import java.awt.geom.Rectangle2D;
import org.imagine.geometry.util.GeometryStrings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class EqLineTest {

    private static final double TOLERANCE = 0.0000000000001;
    EqLine ln = new EqLine();

    @Test
    public void testMidPoint() {
        ln.setLine(-10, 0, 10, 0);
        assertMidpoint(0, 0);
        ln.setLine(0, -10, 0, 10);
        assertMidpoint(0, 0);
        ln.setLine(10, 10, -10, -10);
        assertMidpoint(0, 0);
        ln.setLine(-10, 0, 10, 0);
        assertMidpoint(0, 0);
        ln.setLine(0, 0, 10, 10);
        assertMidpoint(5, 5);
    }

    @Test
    public void testAngle() {
        ln.setLine(-10, -10, 10, 10);
        assertAngle(135);

        ln.setLine(10, 10, -10, -10);
        assertAngle(315);

        ln.setLine(-10, 10, 10, -10);
        assertAngle(45);

        ln.setLine(10, -10, -10, 10);
        assertAngle(225);
    }

    @Test
    public void testSetAngle() {
        ln.setLine(0, 0, 0, 10);
        ln.setAngle(90);
        assertAngle(90);
        assertLength(10);

        ln.setAngleAndLength(180, 20);
        assertLength(20);
        assertLine(0, 0, 0, 20);
    }

    @Test
    public void testSetLengthHorizontal() {
        ln.setLine(0, 0, 10, 0);
        ln.setLength(9, false);
        assertLength(9);
        assertLine(0, 0, 9, 0);

        ln.setLine(0, 0, 10, 0);
        ln.setLength(9, true);
        assertLength(9);
        assertLine(1, 0, 10, 0);

        ln.setLine(0, 0, -10, 0);
        ln.setLength(9, false);
        assertLength(9);
        assertLine(0, 0, -9, 0);

        ln.setLine(0, 0, -10, 0);
        ln.setLength(9, true);
        assertLength(9);
        assertLine(-1, 0, -10, 0);
    }

    @Test
    public void testSetLengthVertical() {
        ln.setLine(0, 0, 0, 10);
        ln.setLength(9, false);
        assertLength(9);
        assertLine(0, 0, 0, 9);

        ln.setLine(0, 0, 0, 10);
        ln.setLength(9, true);
        assertLength(9);
        assertLine(0, 1, 0, 10);

        ln.setLine(0, 0, 0, -10);
        ln.setLength(9, false);
        assertLength(9);
        assertLine(0, 0, 0, -9);

        ln.setLine(0, 0, 0, -10);
        ln.setLength(9, true);
        assertLength(9);
        assertLine(0, -1, 0, -10);
    }

    @Test
    public void testSetLengthZero() {
        ln.setLine(0, 0, 9, 9);
        ln.setLength(0, true);
        assertLine(9, 9, 9, 9);
        assertLength(0);
        assertTrue(ln.isEmpty());

        ln.setLine(0, 0, 9, 9);
        ln.setLength(0, false);
        assertLine(0, 0, 0, 0);
        assertLength(0);
        assertTrue(ln.isEmpty());

        ln.setLine(100, 100, 111, 111);
        ln.setLength(0, false);
        assertLength(0);
        assertLine(100, 100, 100, 100);
        assertTrue(ln.isEmpty());

        ln.setLine(111, 111, 99, 99);
        ln.setLength(0, true);
        assertLine(99, 99, 99, 99);
        assertTrue(ln.isEmpty());
    }

    @Test
    public void testSetLengthDiagonal() {
        double baseLength = 12.727922061357855;
        ln.setLine(0, 0, 9, 9);
        assertLength(baseLength);
        ln.setLength(baseLength - 2, true);
        assertLength(baseLength - 2);
        assertLine(1.4142135623730931, 1.4142135623730958, 9.0, 9.0);

        ln.setLine(0, 0, 9, 9);
        ln.setLength(baseLength - 1, false);
        assertLine(0.0, 0.0, 8.292893218813452, 8.292893218813452);
        assertLength(baseLength - 1);

        ln.setLine(9, 9, 0, 0);
        ln.setLength(baseLength - 1, true);
        assertLine(8.292893218813452, 8.292893218813452, 0, 0);
        assertLength(baseLength - 1);

        ln.setLine(9, 9, 0, 0);
        ln.setLength(baseLength - 2, false);
        assertLine(9.0, 9.0, 1.4142135623730931, 1.4142135623730958);
        assertLength(baseLength - 2);

        ln.setLine(0, 9, 9, 0);
        ln.setLength(baseLength - 2, true);
        assertLine(1.4142135623730958, 7.585786437626905, 9.0, 0.0);
        assertLength(baseLength - 2);

        ln.setLine(0, 9, 9, 0);
        ln.setLength(baseLength - 2, false);
        assertLength(baseLength - 2);
        assertLine(0.0, 9.0, 7.585786437626905, 1.4142135623730958);

        ln.setLine(100, 0, 0, 50);
        ln.setLength(100, false);
        assertLength(100);
        assertLine(100.0, 0.0, 10.557280900008408, 44.72135954999577);

        ln.setLine(100, 0, 0, 50);
        ln.setLength(100, true);
        assertLength(100);
        assertLine(89.44271909999159, 5.278640450004232, 0.0, 50.0);
    }

    @Test
    public void testIntersections() {
        ln.setLine(5, 5, 10, 10);
        assertDoesNotIntersect(0, 0, 4, 4);
        assertIntersects(0, 0, 5, 5);

        ln.setLine(2.5, 5, 10, 10);
        assertIntersects(0, 0, 5, 5);

        ln.setLine(2.5, 2.5, 5, 5);
        assertIntersects(0, 0, 10, 10);

        assertDoesNotIntersect(10, 10, 10, 10);

        ln.setLine(0, 0, 5, 5);
        assertIntersects(5, 5, 5, 5);
        assertDoesNotIntersect(5.1, 5.1, 5, 5);

        ln.setLine(2.5, 2.5, 2.5, 2.5);
        assertLength(0);
        assertDoesNotIntersect(0, 0, 5, 5);
        ln.setLength(0.001, true);
        assertIntersects(0, 0, 5, 5);

        ln.setLine(0, 0, 90, 90);
        EqLine ln2 = ln.copy();
        ln2.setLength(ln2.length() * 1.25, true);
        ln2.setLength(ln2.length() * 1.25, false);
        Rectangle2D rr = ln2.getBounds2D();
        assertIntersects(rr.getX(), rr.getY(), rr.getWidth(), rr.getHeight());
    }

    private void assertIntersects(double x, double y, double w, double h) {
        EnhRectangle2D r = new EnhRectangle2D(x, y, w, h);
        assertTrue(ln.intersects(r), ln + " should intersect " + r);
    }

    private void assertDoesNotIntersect(double x, double y, double w, double h) {
        EnhRectangle2D r = new EnhRectangle2D(x, y, w, h);
        assertFalse(ln.intersects(r), ln + " should not intersect " + r);
    }

    private void assertMidpoint(double x, double y) {
        EqPointDouble pt = ln.midPoint();
        assertDoubleEquals(x, pt.x, "Midpoint X incorrect - expected " + x + "," + y + " got " + pt);
        assertDoubleEquals(y, pt.y, "Midpoint Y incorrect - expected " + x + "," + y + " got " + pt);
        assertEquals(pt, new EqPointDouble(x, y), "EqPointDouble equality test failed for " + pt + " and "
                + new EqPointDouble(x, y));
    }

    private void assertAngle(double ang) {
        assertDoubleEquals(ang, ln.angle(), "Angle incorrect");
    }

    private void assertLength(double exp) {
        assertDoubleEquals(exp, ln.length(), "Length incorrect");
    }

    private void assertLine(double x1, double y1, double x2, double y2) {
        String msg = "Expected " + GeometryStrings.lineToString(x1, y1, x2, y2)
                + " (length " + GeometryStrings.toString(new EqLine(x1, y1, x2, y2).length()) + ")"
                + " got " + ln + " (length " + GeometryStrings.toString(ln.length()) + ")";
        assertLine(x1, y1, x2, y2, msg);
        assertEquals(new EqLine(x1, y1, x2, y2), ln, "EqLineDouble equality test failed for " + ln
                + " and " + new EqLine(x1, y1, x2, y2));
    }

    private static void assertDoubleEquals(double exp, double got) {
        assertEquals(exp, got, TOLERANCE);
    }

    private void assertLine(double x1, double y1, double x2, double y2, String msg) {
        assertDoubleEquals(x1, ln.x1, msg + " - x1 differs");
        assertDoubleEquals(y1, ln.y1, msg + " - y1 differs");
        assertDoubleEquals(x2, ln.x2, msg + " - x2 differs");
        assertDoubleEquals(y2, ln.y2, msg + " - y2 differs");
    }

    private static void assertDoubleEquals(double exp, double got, String msg) {
        assertEquals(exp, got, TOLERANCE, msg);
    }
}
