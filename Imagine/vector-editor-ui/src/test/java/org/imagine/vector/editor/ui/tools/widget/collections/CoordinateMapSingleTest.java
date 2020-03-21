package org.imagine.vector.editor.ui.tools.widget.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class CoordinateMapSingleTest {

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

    private void simpleTest(CoordinateMap<String> m) {
        m.put(110, 110, "First", this::coalesce);
        assertEquals(1, m.size());
        m.put(190, 190, "Last", this::coalesce);
        assertEquals(2, m.size());
        assertEquals("First", m.get(110, 110));
        assertEquals("Last", m.get(190, 190));

        String v = m.nearestValueTo(120, 120, 20);
        assertEquals("First", v);
        assertEquals("Last", m.nearestValueTo(180, 180, 20));

        m.put(112, 112, "Second", this::coalesce);
        assertEquals(3, m.size());
        m.visitAll((double x, double y, String val) -> {
            System.out.println(x + ", " + y + " " + val);
        });
        assertEquals("Second", m.nearestValueTo(120, 120, 20));
        assertEquals("First", m.nearestValueTo(108, 108, 20));

        m.put(110, 110, " in its class", this::coalesce);
        System.out.println("Map now " + m);

        assertEquals("First in its class", m.get(110, 110));

        v = m.nearestValueTo(0, 0, 300);
        assertEquals("First in its class", v);

        v = m.nearestValueTo(1000, 1000, 1500);
        assertEquals("Last", v);
    }

    private String coalesce(String a, String b) {
        System.out.println("COA " + a + " / " + b);
        return a + b;
    }
}
