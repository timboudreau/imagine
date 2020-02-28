package net.java.dev.imagine.api.vector.elements;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ControlPointController;
import net.java.dev.imagine.api.vector.design.ControlPointFactory;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Size;
import org.imagine.geometry.Triangle;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class PathIteratorWrapperTest {

    private final ControlPointFactory f = new ControlPointFactory();

    private static final int[] pathT = new int[]{
        SEG_MOVETO,
        SEG_LINETO,
        SEG_CUBICTO,
        SEG_LINETO,
        SEG_QUADTO,
        SEG_LINETO,
        SEG_CLOSE
    };
    private static final double[] pathPoints = new double[]{
        // seg 0
        0, 5,
        // seg 1
        10, 15,
        // seg 2
        20, 25,
        18, 23,
        22, 27,
        // seg 3
        31, 36,
        // seg 4
        43, 47,
        41, 45,
        // seg 5
        50, 55
    };
    static int FIRST_POINT_CP = 0;
    static int SECOND_POINT_CP = 1;
    static int CUBIC_CP_1 = 2;
    static int CUBIC_CP_2 = 3;
    static int CUBIC_TARGET = 4;
    static int SECOND_LINE_CP = 5;
    static int QUAD_CP = 6;
    static int QUAD_TARGET = 7;
    static int THIRD_LINE_CP = 8;

    private PathIteratorWrapper path() {
        Path2D.Double p = new Path2D.Double();
        int pointCursor = 0;
        for (int i = 0; i < pathT.length; i++) {
            switch (pathT[i]) {
                case SEG_MOVETO:
                    p.moveTo(pathPoints[pointCursor], pathPoints[pointCursor + 1]);
                    pointCursor += 2;
                    break;
                case SEG_LINETO:
                    p.lineTo(pathPoints[pointCursor], pathPoints[pointCursor + 1]);
                    pointCursor += 2;
                    break;
                case SEG_CUBICTO:
                    p.curveTo(pathPoints[pointCursor], pathPoints[pointCursor + 1],
                            pathPoints[pointCursor + 2], pathPoints[pointCursor + 3],
                            pathPoints[pointCursor + 4], pathPoints[pointCursor + 5]);
                    pointCursor += 6;
                    break;
                case SEG_QUADTO:
                    p.quadTo(pathPoints[pointCursor], pathPoints[pointCursor + 1],
                            pathPoints[pointCursor + 2], pathPoints[pointCursor + 3]);
                    pointCursor += 4;
                    break;
                case SEG_CLOSE:
                    p.closePath();
                    break;
            }
        }
        return new PathIteratorWrapper(p.getPathIterator(null));
    }

    private PathIteratorWrapper triangle() {
        Triangle tri = new Triangle(0, 0, 10, 10, 20, 20);
        PathIteratorWrapper w = new PathIteratorWrapper(tri.getPathIterator(null));
        return w;
    }

    @Test
    public void testFromShape() {
        PathIteratorWrapper w = triangle();
        assertPoint(0, 0, 0, w);
        assertPoint(1, 10, 10, w);
        assertPoint(2, 20, 20, w);
    }

    @Test
    public void testComplexControlPoints() {
        PathIteratorWrapper w = path();

        int[] virt = w.getVirtualControlPointIndices();
        assertArrayEquals(new int[]{CUBIC_CP_1, CUBIC_CP_2, QUAD_CP}, virt);
        int[] cpIxs = w.getConcretePointIndices();
        assertArrayEquals(new int[]{FIRST_POINT_CP, SECOND_POINT_CP,
            CUBIC_TARGET, SECOND_LINE_CP, QUAD_TARGET, THIRD_LINE_CP}, cpIxs);

        assertEquals(pathT.length - 1, concretePoints(w).size(),
                "Shape incorrectly constructed or point count wrong");
        for (int i = 0; i < pathPoints.length; i += 2) {
            double x = pathPoints[i];
            double y = pathPoints[i + 1];
            int pix = i / 2;
            assertPoint(pix, x, y, w);
        }
        C c = new C();
        ControlPoint[] cps = f.getControlPoints(w, c);
        assertEquals(pathPoints.length / 2, cps.length);

        cps[CUBIC_CP_1].set(100, 103);
        c.assertIndexChangedTo(CUBIC_CP_1, 100, 103, w);
        assertPoint(CUBIC_CP_1, 100, 103, w);

        cps[CUBIC_TARGET].set(150, 153);
        c.assertIndexChangedTo(CUBIC_TARGET, 150, 153, w);
        assertPoint(CUBIC_TARGET, 150, 153, w);

        for (int i = 0; i < cps.length; i++) {
            double ox = cps[i].getX();
            double oy = cps[i].getY();
            double nx = ox + 7.5;
            double ny = oy - 7.5;
            cps[i].move(7.5, -7.5);
            c.assertIndexChangedTo(i, nx, ny, w);
            assertPoint(i, nx, ny, w);
        }
        int cpC = w.getControlPointCount();
        w.delete(SECOND_LINE_CP);
        int cpcNew = w.getControlPointCount();
        assertNotEquals(cpC, cpcNew);
        assertEquals(cpC - 1, cpcNew);
        ControlPoint[] newCps = f.getControlPoints(w, c);
        assertEquals(cps.length - 1, newCps.length);
        assertFalse(cps[cps.length - 1].isValid());
        for (int i = 0; i < cps.length - 1; i++) {
            assertTrue(cps[i].isValid(), "Control point " + i + " should still be valid");
        }
        assertFalse(cps[cps.length - 1].set(218, 219), w::toString);
        List<Point2D> all = allPoints(w);
        for (int i = 0; i < all.size(); i++) {
            final int ix = i;
            Point2D p = all.get(i);
            assertNotEquals(218, p.getX(), 0.0001, () -> {
                return "Changing dead control point should not update "
                        + "any X coord, but affected " + ix + " in "
                        + w;
            });
            assertNotEquals(219, p.getY(), 0.0001, () -> {
                return "Changing dead control point should not update "
                        + "any Y coord, but affected " + ix + " in "
                        + w;
            });
        }

        w.delete(CUBIC_TARGET);
        ControlPoint[] newerCps = f.getControlPoints(w, c);
        assertEquals(newCps.length - 3, newerCps.length);
    }

    @Test
    public void testSimpleControlPoints() {
        PathIteratorWrapper w = triangle();
        C c = new C();
        ControlPoint[] cps = f.getControlPoints(w, c);
        assertNotNull(cps);
        assertEquals(3, cps.length);

        assertEquals(ControlPointKind.START_POINT, cps[0].kind());
        assertEquals(ControlPointKind.LINE_TO_DESTINATION, cps[1].kind());
        assertEquals(ControlPointKind.LINE_TO_DESTINATION, cps[2].kind());
        for (int i = 0; i < cps.length; i++) {
            assertTrue(cps[i].isValid(), "Control point " + i + " invalid");
        }

        assertTrue(cps[0].set(5, 5));
        c.assertIndexChangedTo(0, 5, 5, w);
        assertPoint(0, 5, 5, w);

        assertTrue(cps[1].set(15, 15));
        c.assertIndexChangedTo(1, 15, 15, w);
        assertPoint(0, 5, 5, w);
        assertPoint(1, 15, 15, w);

        assertTrue(cps[2].set(25, 25));
        c.assertIndexChangedTo(2, 25, 25, w);
        assertPoint(0, 5, 5, w);
        assertPoint(1, 15, 15, w);
        assertPoint(2, 25, 25, w);
    }

    private void assertPoint(int ix, double expX, double expY, PathIteratorWrapper w) {
        List<Point2D> list = allPoints(w);
        Point2D p = list.get(ix);
        assertEquals(expX, p.getX(), 0.0001, "X coords do not match in " + p.getX() + ", " + p.getY() + " at " + ix + " in " + w);
        assertEquals(expY, p.getY(), 0.0001, "Y coords do not match in " + p.getX() + ", " + p.getY() + " at " + ix + " in " + w);
    }

    private List<Point2D> concretePoints(PathIteratorWrapper w) {
        List<Point2D> result = new ArrayList<>();
        PathIterator it = w.toShape().getPathIterator(null);
        double[] d = new double[6];
        while (!it.isDone()) {
            int type = it.currentSegment(d);
            int ptIx;
            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    ptIx = 0;
                    break;
                case PathIterator.SEG_CUBICTO:
                    ptIx = 4;
                    break;
                case PathIterator.SEG_QUADTO:
                    ptIx = 3;
                    break;
                case PathIterator.SEG_CLOSE:
                default:
                    it.next();
                    continue;
            }
            result.add(new Point2D.Double(d[ptIx], d[ptIx + 1]));
            it.next();
        }
        return result;
    }

    private List<Point2D> allPoints(PathIteratorWrapper w) {
        List<Point2D> result = new ArrayList<>();
        PathIterator it = w.toShape().getPathIterator(null);
        double[] d = new double[6];
        while (!it.isDone()) {
            int type = it.currentSegment(d);
            int size;
            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    size = 1;
                    break;
                case PathIterator.SEG_CUBICTO:
                    size = 3;
                    break;
                case PathIterator.SEG_QUADTO:
                    size = 2;
                    break;
                case PathIterator.SEG_CLOSE:
                default:
                    it.next();
                    continue;
            }
            for (int i = 0; i < size; i++) {
                int ix = i * 2;
                result.add(new Point2D.Double(d[ix], d[ix + 1]));
            }
            it.next();
        }
        return result;
    }

    static class C implements ControlPointController {

        private ControlPoint lastChanged;

        ControlPoint assertIndexChangedTo(int ix, double x, double y, Object msg) {
            ControlPoint cp = assertChangedIndex(ix);
            assertEquals(x, cp.getX(), 0.0001, "X coordinate does not match. " + msg);
            assertEquals(y, cp.getY(), 0.0001, "Y coordinate does not match. " + msg);
            return cp;
        }

        ControlPoint assertChangedIndex(int ix, Object msg) {
            ControlPoint last = assertChanged();
            assertEquals(ix, last.index(), "Last changed index wrong");
            return last;
        }

        ControlPoint assertChanged(Object msg) {
            ControlPoint lc = lastChanged;
            lastChanged = null;
            assertNotNull(lc, "No control point changed. " + msg);
            return lc;
        }

        ControlPoint assertIndexChangedTo(int ix, double x, double y) {
            ControlPoint cp = assertChangedIndex(ix);
            assertEquals(x, cp.getX(), 0.0001, "X coordinate does not match");
            assertEquals(y, cp.getY(), 0.0001, "Y coordinate does not match");
            return cp;
        }

        ControlPoint assertChangedIndex(int ix) {
            ControlPoint last = assertChanged();
            assertEquals(ix, last.index(), "Wrong control point " + last.index() + " changed");
            return last;
        }

        ControlPoint assertChanged() {
            ControlPoint lc = lastChanged;
            lastChanged = null;
            assertNotNull(lc, "No control point changed");
            return lc;
        }

        @Override
        public void changed(ControlPoint pt) {
            lastChanged = pt;
        }

        @Override
        public Size getControlPointSize() {
            return new Size(9, 9);
        }

    }
}
