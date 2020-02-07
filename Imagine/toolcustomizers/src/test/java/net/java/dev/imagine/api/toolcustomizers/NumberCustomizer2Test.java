/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.toolcustomizers;

import net.java.dev.imagine.api.toolcustomizers.NumberCustomizer2.ScalingDoubleConverter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class NumberCustomizer2Test {

    @Test
    public void testSomeMethod() {
        ScalingDoubleConverter sdc = new ScalingDoubleConverter(100);
        int v = sdc.toInt(0.5);
        assertEquals(50, v);
        assertEquals(0.5D, sdc.fromInt(50), 0.0001D);
        assertEquals("0.5", sdc.valueToString(50));
//        JSlider slider = new JSlider(sdc.createModel(0D, 1D, 0.5D));

        int v1 = sdc.toInt(0.1);
        assertEquals(10, v1);

        assertEquals(0.1D, sdc.fromInt(10), 0.0001D);
    }

}
