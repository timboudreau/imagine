package net.java.dev.imagine.customizers.impl;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class CustomizerAnnotationProcessorTest {

    @Test
    public void testGetSupportedAnnotationTypes() throws Exception {
        Integer in = Integer.valueOf(1);
        CustomizerFactory f = CustomizerFactory.find(in, true);
        assertNotNull(f);
        Object o = f.createCustomizer(in);
        assertNotNull(o);
        assertTrue(o instanceof Q);
        f = CustomizerFactory.find(in, false);
        assertNotNull(f);
        o = f.createCustomizer(in);
        assertTrue(o instanceof X);
    }
}
