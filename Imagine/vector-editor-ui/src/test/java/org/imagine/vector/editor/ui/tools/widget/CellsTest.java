/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget;

import com.mastfrog.util.collections.IntSet;
import java.awt.Rectangle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class CellsTest {

    private final Cells cells = new Cells();

    @Test
    public void testBasic() {

        cells.run(new Rectangle(0, 0, 20, 20), 5, 1, () -> {
            assertOccupy(0.5, 0.5);
            assertCells(0);
            assertNotOccupy(0.5, 0.5);
            for (int i = 0; i < 5; i++) {
                assertNotOccupy(i, i);
                assertNotOccupy(i, 0);
                assertNotOccupy(0, i);
            }
            assertOccupy(6, 6);
            for (int i = 5; i < 10; i++) {
                assertNotOccupy(i, i);
                assertNotOccupy(i, 5);
                assertNotOccupy(5, i);
            }
        });
        assertOccupied(0.25, 0.25);
        assertOccupied(5, 5);
        assertOccupied(4.5, 0);

//        assertOccupied(20, 20, new Rectangle(-20, -20, 40, 40));
//        assertOccupied(22, 22, new Rectangle(-20, -20, 40, 40));
    }

    @Test
    public void testZoomedIn() {
        cells.run(new Rectangle(0, 0, 20, 20), 5, 2, () -> {
            assertOccupy(0, 0);
            for (double d = 0; d < 2.5; d += 0.5) {
                assertNotOccupy(d, d);
                assertNotOccupy(d, 0);
                assertNotOccupy(0, d);
            }
            assertOccupy(2.6, 2.6);
//            assertCells(0, 1);
            for (double d = 2.5; d < 5; d += 0.5) {
                assertNotOccupy(d, d);
                assertNotOccupy(d, 2.5);
                assertNotOccupy(2.5, d);
            }
            assertOccupy(5, 5);
        });
    }

    @Test
    public void testZoomedOut() {
        cells.run(new Rectangle(0, 0, 40, 40), 5, 0.5, () -> {
            assertOccupy(0, 0);
            for (double d = 0; d < 10; d += 0.5) {
                assertNotOccupy(d, d);
                assertNotOccupy(d, 0);
                assertNotOccupy(0, d);
            }
            assertOccupy(10, 10);
        });

        cells.run(new Rectangle(-20, -20, 40, 40), 5, 0.5, () -> {
            assertOccupy(-20, -20);
            assertCells(0);
            for (double d = -20; d < -10; d += 0.5) {
                assertNotOccupy(d, d);
                assertNotOccupy(d, -20);
                assertNotOccupy(-20, d);
                assertNotOccupy(d, -15);
                assertNotOccupy(-15, d);
            }
            assertOccupy(-10, -10);
        });
    }

    private void assertCells(int... cls) {
        IntSet expect = IntSet.create(cls);
        assertEquals(expect, cells.cells);
    }

    private void assertOccupied(double x, double y) {
        assertTrue(cells.wasOccupied(x, y), "Should have been occupied "
                + x + ", " + y
                + " in " + cells.cells);
    }

    private void assertNotOccupied(double x, double y) {
        assertFalse(cells.wasOccupied(x, y), "Should NOT have been occupied "
                + x + ", " + y
                + " in " + cells.cells);
    }

    private void assertOccupy(double x, double y) {
        assertTrue(cells.occupy(x, y), "Should have occupied " + x + ", " + y
                + " cells " + cells.cells);
    }

    private void assertNotOccupy(double x, double y) {
        assertFalse(cells.occupy(x, y), "Should NOT have occupied " + x + ", "
                + y + " cells " + cells.cells);
    }
}
