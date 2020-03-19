/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class HemisphereTest {

    @Test
    public void testQuadrants() {
        for (double deg = 0; deg < 360; deg += 0.5) {
            Quadrant q = Quadrant.forAngle(deg);
            assertNotNull(q);
            if (deg < 90) {
                assertSame(Quadrant.NORTHEAST, q);
            } else if (deg < 180) {
                assertSame(Quadrant.SOUTHEAST, q);
            } else if (deg < 270) {
                assertSame(Quadrant.SOUTHWEST, q);
            } else if (deg < 360) {
                assertSame(Quadrant.NORTHWEST, q);
            } else if (deg == 360) {
                assertSame(Quadrant.NORTHEAST, q);
            }
            assertTrue(q.contains(deg), q + " does not contain " + deg + " but should - " + q.start() + ", " + (q.start() + q.extent()));
        }
    }

    @Test
    public void testLookup() {
        assertEquals(Axis.VERTICAL, Hemisphere.EAST.axis());
        assertEquals(Axis.VERTICAL, Hemisphere.WEST.axis());
        assertEquals(Axis.HORIZONTAL, Hemisphere.NORTH.axis());
        assertEquals(Axis.HORIZONTAL, Hemisphere.SOUTH.axis());
        for (double deg = 0; deg < 360; deg += 0.5) {
            Hemisphere vhem = Hemisphere.forAngle(deg, Axis.VERTICAL);
            assertTrue(vhem == Hemisphere.EAST || vhem == Hemisphere.WEST, "Wrong axis for vertical: " + vhem);
            assertNotNull(vhem);
            assertTrue(vhem.contains(deg), vhem + " does not contain " + deg + " but was returned by forAngle VERTICAL");
            assertSame(Axis.VERTICAL, vhem.axis(), vhem + " " + vhem.axis()
                    + " requesting for vertical angle " + deg + " should get a hemisphere on the vertical axis");

            Hemisphere hhem = Hemisphere.forAngle(deg, Axis.HORIZONTAL);
            assertNotNull(hhem);
            assertTrue(hhem == Hemisphere.NORTH || hhem == Hemisphere.SOUTH, "Wrong axis for horizontal: " + hhem);
            assertTrue(hhem.contains(deg), hhem + " does not contain " + deg + " but was returned by forAngle HORIZONTAL");
            assertSame(Axis.HORIZONTAL, hhem.axis(), " " + hhem + " " + hhem.axis());

            assertTrue(hhem.opposite().axis() == hhem.axis(), "Wrong axis for opposite of " + hhem + " " + hhem.opposite() + " " + hhem.opposite().axis());
            assertTrue(vhem.opposite().axis() == vhem.axis(), "Wrong axis for opposite of " + vhem + " " + vhem.opposite() + " " + vhem.opposite().axis());

            assertFalse(hhem.opposite().contains(deg), "Opposite hemisphere");
            assertFalse(vhem.opposite().contains(deg), "Opposite hemisphere");

            if (deg < 90 || (deg >= 270 && deg != 360)) {
                assertSame(Hemisphere.NORTH, hhem, "Horizontal hemisphere for " + deg + " was " + hhem + " should be " + Hemisphere.NORTH);
            } else {
                assertSame(Hemisphere.SOUTH, hhem, "Horizontal hemisphere for " + deg + " was " + hhem + " should be " + Hemisphere.SOUTH);
            }
            if (deg < 180) {
                assertSame(Hemisphere.EAST, vhem, "Vertical hemisphere for " + deg + " was " + vhem);
            } else {
                assertSame(Hemisphere.WEST, vhem, "Vertical hemisphere for " + deg + " was " + vhem);
            }
            if (deg == 360) {
                assertSame(Hemisphere.EAST, vhem);
                assertSame(Hemisphere.NORTH, hhem);
            }
        }
    }

}
