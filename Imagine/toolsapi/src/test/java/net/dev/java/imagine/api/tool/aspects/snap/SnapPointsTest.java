package net.dev.java.imagine.api.tool.aspects.snap;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.function.BiConsumer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class SnapPointsTest {

    @Test
    public void testSomeMethod() {
        N n = new N();
        SnapPoints pts = new SnapPoints.Builder(10)
                .add(new Point(100, 120))
                .add(new Point(200, 220))
                .add(new Point(300, 320))
                .notifying(n)
                .build();
        assertNotNull(pts);
        SnapPoint sp = pts.nearestWithinRadius(new Point(0, 0));
        assertNull(sp);
        sp = pts.nearestWithinRadius(new Point(110, 110));
        assertNotNull(sp);

        Point p = new Point(0, 0);
        Point2D p1 = pts.snap(p);
        assertSame(p, p1);
        n.assertNeither();

        p = new Point(110, 110);
        p1 = pts.snap(p);
        assertNotSame(p, p1);
        n.assertXandY();
        assertEquals(100, (int) p1.getX());
        assertEquals(120, (int) p1.getY());

        p = new Point(130, 130);
        p1 = pts.snap(p);
        assertNotSame(p, p1);
        n.assertYNotX();
        assertEquals(130, (int) p1.getX());
        assertEquals(120, (int) p1.getY());

        p = new Point(130, 131);
        p1 = pts.snap(p);
        assertSame(p, p1);
        n.assertNeither();
    }

    @Test
    public void testThorough() throws Throwable {
        N n = new N();
        SnapPoints.Builder b = new SnapPoints.Builder(10)
                .notifying(n);

        for (int x = 0; x < 1100; x += 100) {
            for (int y = 50; y < 1100; y += 50) {
                b.add(new Point(x, y));
            }
        }
        SnapPoints pts = b.build();
        String str = pts.toString();
        for (int x = 35; x < 1000; x++) {
            for (int y = 35; y < 1000; y++) {
                n.reset();
                int nextFifty = 50 * ((y / 50) + 1);
                int nextHundred = 100 * ((x / 100) + 1);
                int prevFifty = nextFifty - 50;
                int prevHundred = nextHundred - 100;

                int distToFifty = Math.min(Math.abs(prevFifty - y), Math.abs(nextFifty - y));
                int distToHundred = Math.min(Math.abs(nextHundred - x), Math.abs(prevHundred - x));
                boolean expY = distToFifty <= 10;
                boolean expX = distToHundred <= 10;
                String msg = "For " + x + ", " + y + " "
                        + expX + ", " + expY + " : "
                        + distToHundred + ", " + distToFifty
                        + " in " + str;
                Point p = new Point(x, y);
                Point2D p1 = pts.snap(p);
                if (!expX && !expY) {
                    n.assertNeither(msg);
                    assertSame(msg, p, p1);
                } else if (expX && !expY) {
                    n.assertXNotY(msg);
                    assertNotSame(msg, p, p1);
                } else if (expY && !expX) {
                    n.assertYNotX(msg);
                    assertNotSame(msg, p, p1);
                } else if (expX && expY) {
                    n.assertXandY(msg);
                    assertNotSame(msg, p, p1);
                }
            }
        }

    }

    private static class N implements BiConsumer<SnapPoint, SnapPoint> {

        SnapPoint x;
        SnapPoint y;

        N reset() {
            x = null;
            y = null;
            return this;
        }

        N assertXandY() {
            return assertX().assertY();
        }

        N assertNeither() {
            return assertNotX().assertNotY();
        }

        N assertXNotY() {
            return assertX().assertNotY();
        }

        N assertYNotX() {
            return assertNotX().assertY();
        }

        N assertX() {
            return assertX("");
        }

        N assertXandY(String msg) {
            return assertX(msg).assertY(msg);
        }

        N assertNeither(String msg) {
            return assertNotX(msg).assertNotY(msg);
        }

        N assertXNotY(String msg) {
            return assertX(msg).assertNotY(msg);
        }

        N assertYNotX(String msg) {
            return assertNotX(msg).assertY(msg);
        }

        N assertX(String msg) {
            SnapPoint s = x;
            x = null;
            assertNotNull(s + ", " + y + " " + msg, s);
            return this;
        }

        N assertY() {
            return assertY("");
        }

        N assertY(String msg) {
            SnapPoint s = y;
            y = null;
            assertNotNull(x + ", " + s + " " + msg, s);
            return this;
        }

        N assertNotY() {
            return assertNotY("");
        }

        N assertNotY(String msg) {
            SnapPoint s = y;
            y = null;
            assertNull(x + ", " + s + " " + msg, s);
            return this;
        }

        N assertNotX() {
            return assertNotX("");
        }

        N assertNotX(String msg) {
            SnapPoint s = x;
            x = null;
            assertNull(s + ", " + y + " " + msg, s);
            return this;
        }

        @Override
        public void accept(SnapPoint x, SnapPoint y) {
            this.x = x;
            this.y = y;
        }
    }
}
