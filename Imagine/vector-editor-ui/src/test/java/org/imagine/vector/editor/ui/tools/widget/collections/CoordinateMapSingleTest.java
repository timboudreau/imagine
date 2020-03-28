package org.imagine.vector.editor.ui.tools.widget.collections;

import com.mastfrog.util.collections.IntSet;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.util.GeometryUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class CoordinateMapSingleTest {

    @Test
    public void testPartitionCoordinateMapping() {
        for (int i = 10; i < 100; i++) {
            Map<Integer, CoordinatesKey> duplicateIdCheck = new HashMap<>();
            CoordinateMapPartitioned<String> p = new CoordinateMapPartitioned<>(i);
            Map<CoordinatesKey, CoordinateMap<String>> cache = new HashMap<>();
            for (double d = -220; d < 220; d++) {
                testPartitionMapping(i, d, duplicateIdCheck, p, cache);
            }
        }
    }

    private void testPartitionMapping(int size, double d, Map<Integer, CoordinatesKey> duplicateIdCheck, CoordinateMapPartitioned<String> p, Map<CoordinatesKey, CoordinateMap<String>> cache) {
        testPartitionMapping(size, d, d, duplicateIdCheck, p, cache);
        testPartitionMapping(size, d, -d, duplicateIdCheck, p, cache);
        testPartitionMapping(size, -d, d, duplicateIdCheck, p, cache);
        testPartitionMapping(size, -d, -d, duplicateIdCheck, p, cache);
        testPartitionMapping(size, 0, d, duplicateIdCheck, p, cache);
        testPartitionMapping(size, d, 0, duplicateIdCheck, p, cache);
        testPartitionMapping(size, 0, -d, duplicateIdCheck, p, cache);
        testPartitionMapping(size, -d, 0, duplicateIdCheck, p, cache);
    }

    private void testPartitionMapping(int size, double xCoord, double yCoord, Map<Integer, CoordinatesKey> duplicateIdCheck, CoordinateMapPartitioned<String> p, Map<CoordinatesKey, CoordinateMap<String>> cache) {
        int addr = CoordinateMapPartitioned.addressOf(xCoord, yCoord, size);
        CoordinatesKey existing = duplicateIdCheck.get(addr);
        CoordinateMapPartitioned.coordsOf(size, xCoord, yCoord,
                (xStart, yStart) -> {
                    if (existing == null) {
                        duplicateIdCheck.put(addr, CoordinatesKey.of(xStart, yStart));
                    } else {
                        assertEquals(CoordinatesKey.of(xStart, yStart), existing,
                                "Same address " + addr
                                + " used for two different rectangles: "
                                + existing.x + ", " + existing.y + " and "
                                + xStart + "," + yStart + " when fetching for "
                                + xCoord + ", " + yCoord
                                + " with size " + size);
                    }
                    Rectangle bds = new Rectangle(xStart, yStart, size, size);
                    if (!(bds.contains(xCoord, yCoord))) {
                        fail("Partition bounds " + GeometryUtils.toString(bds) + " "
                                + "does not contain requested point " + xCoord + "," + yCoord);
                    }
                    CoordinateMap<String> partm = p.partitionFor(xCoord, yCoord, true);
                    assertNotNull(partm);
                    assertTrue(partm.containsCoordinate(xCoord, yCoord));
                    assertTrue(p.partitions.containsKey(addr), "partitionFor broken? No value for address " + addr + " for " + xStart + "," + yStart + " for " + xCoord + "," + yCoord
                            + " in " + p);
                    assertSame(partm, p.partitions.get(addr), "Wrong partition in address map");
                    assertTrue(partm.bounds().contains(xCoord, yCoord));
                    CoordinatesKey k = CoordinatesKey.of(xStart, yStart);
                    CoordinateMap<String> m = cache.get(k);
                    if (m != null) {
                        assertSame(m, partm, "Got different instance for "
                                + xStart + "," + yStart + " for " + xCoord + "," + yCoord);
                    } else {
                        cache.put(k, partm);
                    }
                    return null;
                });
    }

    @Test
    public void testDirtSimple() {
        DirtSimpleCoordinateMap<String> m = new DirtSimpleCoordinateMap<>(100, 100, 200);
        System.out.println("ds:");
        simpleTest(m);
    }

    @Test
    public void testDirtSimpleComplex() {
        DirtSimpleCoordinateMap<Set<Thing>> m = new DirtSimpleCoordinateMap<>(-40, -40, 80);
        System.out.println("ds-cpl:");
        complexTest(m, -20, 20);
    }

    /*
    @Test
    public void testBasic() {
        CoordinateMapSingle<String> m = new CoordinateMapSingle<>(100, 100, 200, 200);
        System.out.println("single:");
        simpleTest(m);
    }


    @Test
    public void testPartitioned() {
        CoordinateMapPartitioned<String> m = new CoordinateMapPartitioned<>(10);
        System.out.println("partitioned: ");
        simpleTest(m);
    }

    @Test
    public void testSingleComplex() {
        CoordinateMapSingle<Set<Thing>> m = new CoordinateMapSingle<>(0, 0, 100, 100);
        System.out.println("single complex:");
        complexTest(m, 0, 99);
    }

    @Test
    public void testPartitionedComplex() {
        CoordinateMapPartitioned<Set<Thing>> m = new CoordinateMapPartitioned<>(24);
        System.out.println("partitioned complex:");
        complexTest(m, -250, 250);
    }

    @Test
    public void testDoubleToIntAddressing() {
        testAddressing(100, 0, 0);
        testAddressing(100, 100, 100);
        testAddressing(100, -100, -100);
        testAddressing(100, -100, 100);
    }
     */
    private void testAddressing(int w, int minX, int minY) {
        double[] scratch = new double[2];
        IntSet dups = IntSet.arrayBased(100);
        for (double x = minX; x < minX + 20; x += 0.5) {
            for (double y = minY; y < minY + 20; y += 0.5) {
                int addr = CoordinateMapSingle.toAddress(x, y, w, minX, minY);
                assertFalse(dups.contains(addr), "Duplicate address " + addr);
                dups.add(addr);
                CoordinateMapSingle.fromAddress(addr, scratch, w, minX, minY);
//                System.out.println("p " + x + ", " + y + " -> " + addr
//                        + " -> " + scratch[0] + ", " + scratch[1]);
                assertEquals(x, scratch[0], "Wrong X coord for " + addr + " from " + x + ", " + y
                        + " - got " + scratch[0] + ", " + scratch[1] + " for width " + w
                        + " minX " + minX + " minY " + minY);
                assertEquals(y, scratch[1], "Wrong Y coord for " + addr + " from " + x + ", " + y
                        + " - got " + scratch[0] + ", " + scratch[1] + " for width " + w
                        + " minX " + minX + " minY " + minY);
            }
        }
    }

    private void simpleTest(CoordinateMap<String> m) {
        m.put(110, 110, "First", this::coalesce);
        assertEquals(1, m.size());
        m.put(190, 190, "Last", this::coalesce);
        assertEquals(2, m.size());
        assertEquals("First", m.get(110, 110), m::toString);
        assertEquals("Last", m.get(190, 190), m::toString);

        String v = m.nearestValueTo(120, 120, 20);
        assertEquals("First", v);
        assertEquals("Last", m.nearestValueTo(180, 180, 20));

        m.put(112, 112, "Second", this::coalesce);
        assertEquals(m instanceof DirtSimpleCoordinateMap<?> ? 2 : 3, m.size());
        m.visitAll((double x, double y, String val) -> {
//            System.out.println(x + ", " + y + " " + val);
        });
        assertEquals("Second", m.nearestValueTo(120, 120, 20));
        assertEquals("First", m.nearestValueTo(108, 108, 20));

        m.put(110, 110, " in its class", this::coalesce);
//        System.out.println("Map now " + m);

        assertEquals("First in its class", m.get(110, 110));

        v = m.nearestValueTo(0, 0, 300);
        assertEquals("First in its class", v);

        v = m.nearestValueTo(1000, 1000, 1500);
        assertEquals("Last", v);

        m.moveData(112, 112, 125, 125, (String oldValue, String intoValue, BiConsumer<String, String> oldNewConsumer) -> {
            assertNull(intoValue);
            assertEquals("Second", oldValue);
            oldNewConsumer.accept("Replaced", oldValue);
            return oldValue;
        });

        assertTrue(m.contains(125, 125), "New value not updated");
        assertTrue(m.contains(112, 112), "Old value should not have been deleted");

        v = m.nearestValueTo(130, 130, 40);
        assertEquals("Second", v, "Should find moved second value near 130 in " + m);
        v = m.nearestValueTo(113, 113, 20);
        assertEquals("Replaced", v, "Should find replacement value near 119 in " + m);
    }

    private String coalesce(String a, String b) {
//        System.out.println("COA " + a + " / " + b);
        return a + b;
    }

    private void complexTest(CoordinateMap<Set<Thing>> m, int start, int end) {
//        assert end > m.minX() : "Base start/end values passed "
//                + start + " / " + end + " in " + m.bounds();
//        assert start >= m.minX() : "Base start/end values passed "
//                + start + " / " + end + " in " + m.bounds();
//        assert end < m.maxX() : "Base start/end values passed "
//                + start + " / " + end + " in " + m.bounds();
//        assert start < m.maxX() : "Base start/end values passed "
//                + start + " / " + end + " in " + m.bounds();
//
//        assert end > m.minY() : "Base start/end values passed "
//                + start + " / " + end + " in " + m.bounds();
//        assert start >= m.minY() : "Base start/end values passed "
//                + start + " / " + end + " in " + m.bounds();
//        assert end < m.maxY() : "Base start/end values passed "
//                + start + " / " + end + " in " + m.bounds();
//        assert start < m.maxY() : "Base start/end values passed "
//                + start + " / " + end + " in " + m.bounds();

        Map<EqPointDouble, Thing> contents = new TreeMap<>();
        Map<Thing, EqPointDouble> origMapping = new HashMap<>();
        Set<Thing> exp = new HashSet<>();
        int range = (end - start);
        int subStartX = start + (range / 4);
        int subStartY = start + (range / 3);
        int subEndX = end - (range / 3);
        int subEndY = end - (range / 2);
        for (double x = start; x < end; x += 0.5) {
            for (double y = start; y < end; y += 0.5) {
//                System.out.println("p " + x + ", " + y);
                EqPointDouble pt = new EqPointDouble(x, y);
                Thing t = new Thing();
                contents.put(pt, t);
                origMapping.put(t, pt);
                Set<Thing> s = new HashSet<>(2);
                s.add(t);
                double xx = x;
                double yy = y;
                m.put(x, y, s, (old, nue) -> {
                    nue.addAll(old);
                    old.clear();
                    return nue;
//                    throw new AssertionError("Empty map should not try to "
//                            + "coalesce anything but got " + old + " and " + nue
//                            + " for " + xx + "," + yy);
                });
                if (x >= subStartX && x < subEndX && y >= subStartY && y < subEndY) {
                    exp.add(t);
                }
                if (m instanceof CoordinateMapPartitioned<?>) {
                    CoordinateMap<?> part = ((CoordinateMapPartitioned<?>) m).partitionFor(x, y, false);
                    assertNotNull(part);
                    assertTrue(part.minX() <= x, "Wrong bounds for " + x + "," + y + ": " + part.bounds() + " - minX " + part.minX());
                    assertTrue(part.minY() <= y, "Wrong bounds for " + x + "," + y + ": " + part.bounds() + " - minY " + part.minY());
                    assertTrue(part.maxX() >= x, "Wrong bounds for " + x + "," + y + ": " + part.bounds() + " - maxX " + part.maxX());
                    assertTrue(part.maxY() >= y, "Wrong bounds for " + x + "," + y + ": " + part.bounds() + " - maxY " + part.maxY());
                }
                assertTrue(m.containsCoordinate(xx, yy));
                assertTrue(m.contains(xx, yy));
                if (!(m instanceof DirtSimpleCoordinateMap<?>)) {
                    assertSame(s, m.get(xx, yy));
                }
            }
        }
//        for (double d = start + range / 4; d < start + range / 2; d += 0.5) {
//            assertFalse(m.contains(d, start + 1), "address computation is broken in " + m.getClass().getName()
//                    + " getting " + d + "," + (start + 1) + " in " + m.bounds());
//
//            assertFalse(m.contains(d, start + 2), "address computation is broken");
//            assertFalse(m.contains(d, start + 3), "address computation is broken");
//        }

        if (!(m instanceof DirtSimpleCoordinateMap<?>)) {
            assertEquals(m.size(), contents.size());
        }

        Set<Thing> got = new HashSet<>();
        Set<EqPointDouble> gotPoints = new HashSet<>();

        m.valuesWithin(subStartX, subStartY, subEndX, subEndY, (double x, double y, Set<Thing> val) -> {
            if (m instanceof DirtSimpleCoordinateMap<?>) {
                // this impl is much coarser grained
                for (Thing t : val) {
                    EqPointDouble d = origMapping.get(t);
                    if (d.x >= subStartX && d.x < subEndX && d.y >= subStartY && d.y < subEndY) {
                        gotPoints.add(d);
                        got.add(t);
                    }
                }
            } else {
                gotPoints.add(new EqPointDouble(x, y));
                got.addAll(val);
            }
        });
        assertSets(exp, got);
        assertFalse(gotPoints.isEmpty(), "Found no points within "
                + subStartX + "," + subStartY + " to " + subEndX + ", " + subEndY
                + " in bounds " + GeometryUtils.toString(m.bounds()));

        Refiner<Set<Thing>> ref = m instanceof DirtSimpleCoordinateMap<?>
                ? new R(origMapping) : new NoR();

        if (!(m instanceof DirtSimpleCoordinateMap<?>)) {
            for (Map.Entry<EqPointDouble, Thing> e : contents.entrySet()) {
                EqPointDouble pt = e.getKey();
                Thing t = e.getValue();
                Set<Thing> s = setOf(t);
                assertEquals(s, m.get(pt.x, pt.y));
                assertEquals(s, m.nearestValueTo(pt.x, pt.y, 5, ref));
                assertEquals(s, m.nearestValueTo(pt.x + 0.1, pt.y + 0.1, 5, ref));
                assertEquals(s, m.nearestValueTo(pt.x + 0.1, pt.y - 0.1, 5, ref));
                assertEquals(s, m.nearestValueTo(pt.x - 0.1, pt.y + 0.1, 5, ref));
                assertEquals(s, m.nearestValueTo(pt.x - 0.1, pt.y - 0.1, 5, ref));
            }
        }
    }

    static final class NoR implements Refiner<Set<Thing>> {

        @Override
        public Set<Thing> refine(double tMinX, double tMaxX, double tMinY, double tMaxY, Set<Thing> obj) {
            return obj;
        }

    }

    static final class R implements Refiner<Set<Thing>> {

        private final Map<Thing, EqPointDouble> omap;

        public R(Map<Thing, EqPointDouble> omap) {
            this.omap = omap;
        }

        @Override
        public Set<Thing> refine(double subStartX, double subEndX, double subStartY, double subEndY, Set<Thing> val) {
            Set<Thing> ref = new HashSet<>();
            for (Thing t : val) {
                EqPointDouble d = omap.get(t);
                if (d.x >= subStartX && d.x < subEndX && d.y >= subStartY && d.y < subEndY) {
                    ref.add(t);
                }
            }
            return ref;
        }

    }

    private <T> void assertSets(Set<T> a, Set<T> b) {
        if (a.equals(b)) {
            return;
        }
        Set<T> shared = new HashSet<T>(a);
        shared.retainAll(b);
        Set<T> missingFromB = new HashSet<>(a);
        missingFromB.removeAll(b);
        Set<T> unexpectedInB = new HashSet<>(b);
        unexpectedInB.removeAll(a);

        StringBuilder sb = new StringBuilder("Common items: ")
                .append(shared.size())
                .append(" missing items ").append(missingFromB.size())
                .append(" extra items ").append(unexpectedInB)
                .append("\nMissing: ").append(missingFromB)
                .append("\nUnexpected: ").append(unexpectedInB);
        fail(sb.toString());

    }

    private Set<Thing> setOf(Thing... t) {
        return new HashSet<>(Arrays.asList(t));
    }

    private Set<Thing> coalesce(Set<Thing> a, Set<Thing> b) {
        Set<Thing> result = new HashSet<>();
        result.addAll(a);
        result.addAll(b);
        return result;
    }

    private static final class Thing implements Comparable<Thing> {

        private static int ids = 0;
        private final int id = ids++;

        public String toString() {
            return Integer.toString(id);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Thing other = (Thing) obj;
            return this.id == other.id;
        }

        @Override
        public int compareTo(Thing o) {
            return Integer.compare(id, o.id);
        }

        public int id() {
            return id;
        }
    }

    private static final class CoordinatesKey implements Comparable<CoordinatesKey> {

        private final int x;
        private final int y;
        private static final CoordinatesKey ZERO = new CoordinatesKey(0, 0);

        public CoordinatesKey(int x, int y) {
            this.x = x;
            this.y = y;
        }

        static CoordinatesKey of(int x, int y) {
            if (x == 0 && y == 0) {
                return ZERO;
            }
            return new CoordinatesKey(x, y);
        }

        @Override
        public String toString() {
            return x + "," + y;
        }

        @Override
        public int compareTo(CoordinatesKey o) {
            int result = Integer.compare(y, o.y);
            if (result == 0) {
                result = Integer.compare(x, o.x);
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof CoordinatesKey) {
                CoordinatesKey co = (CoordinatesKey) o;
                return co.x == x && co.y == y;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (y * 547957) + x;
        }
    }
}
