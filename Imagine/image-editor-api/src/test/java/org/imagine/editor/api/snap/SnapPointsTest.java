package org.imagine.editor.api.snap;

import java.awt.Point;
import java.awt.geom.Point2D;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class SnapPointsTest {

    @Test
    public void testSomeMethod() {
        N n = new N();
        SnapPoints<String> pts = new SnapPointsBuilder<>(11)
                .add(new Point(100, 120), "A")
                .add(new Point(200, 220), "B")
                .add(new Point(300, 320), "C")
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
        if (true) {
            return;
        }

        N n = new N();
        SnapPointsBuilder<String> b = new SnapPointsBuilder<>(10)
                .notifying(n);

        for (int x = 0; x < 1100; x += 100) {
            for (int y = 50; y < 1100; y += 50) {
                b.add(new Point(x, y), " (" + x + ":" + y + ")");
            }
        }
        SnapPoints<String> pts = b.build();
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
                    assertSame(p, p1, msg);
                } else if (expX && !expY) {
                    n.assertXNotY(msg);
                    assertNotSame(p, p1, msg);
                } else if (expY && !expX) {
                    n.assertYNotX(msg);
                    assertNotSame(p, p1, msg);
                } else if (expX && expY) {
                    n.assertXandY(msg);
                    assertNotSame(p, p1, msg);
                }
            }
        }
    }

    private static class N implements OnSnap<String> {

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
            assertNotNull(s, s + ", " + y + " " + msg);
            return this;
        }

        N assertY() {
            return assertY("");
        }

        N assertY(String msg) {
            SnapPoint s = y;
            y = null;
            assertNotNull(s, x + ", " + s + " " + msg);
            return this;
        }

        N assertNotY() {
            return assertNotY("");
        }

        N assertNotY(String msg) {
            SnapPoint s = y;
            y = null;
            assertNull(s, x + ", " + s + " " + msg);
            return this;
        }

        N assertNotX() {
            return assertNotX("");
        }

        N assertNotX(String msg) {
            SnapPoint s = x;
            x = null;
            assertNull(s, s + ", " + y + " " + msg);
            return this;
        }

        @Override
        public boolean onSnap(SnapPoint<String> x, SnapPoint<String> y) {
            this.x = x;
            this.y = y;
            return true;
        }
    }
}
