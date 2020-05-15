package org.imagine.editor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ZoomPrivateTest {

    @Test
    public void testSomeMethod() {

        assertEquals(1D, ZoomPrivate.nextZoom(0.937), 0.000000000001);

        assertEquals(1.125D, ZoomPrivate.nextZoom(1), 0.000000000001);

        assertEquals(0.875, ZoomPrivate.prevZoom(0.937), 0.000000000001);

        double mini = 0.0001;
        assertEquals(0.0001 / 2D, ZoomPrivate.prevZoom(mini), 0.000000000001);
        assertFalse(ZoomPrivate.hasPreviousZoom(mini));
        assertTrue(ZoomPrivate.hasNextZoom(mini));

        double[] test = {1, 1.125, 1.25, 1.5,
            1.75, 2, 2.5, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
        for (int i = 0; i < test.length - 1; i++) {
            assertEquals(test[i + 1], ZoomPrivate.nextZoom(test[i]), 0.000000000001);
            if (i > 0) {
                assertEquals(test[i - 1], ZoomPrivate.prevZoom(test[i]));
            }
            assertEquals(test[i], ZoomPrivate.nearestZoom(test[i] + 0.001));
            assertEquals(test[i], ZoomPrivate.nearestZoom(test[i] - 0.001));
        }
        assertFalse(ZoomPrivate.hasNextZoom(30));
    }

}
