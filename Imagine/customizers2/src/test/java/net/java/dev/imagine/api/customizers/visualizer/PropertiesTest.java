package net.java.dev.imagine.api.customizers.visualizer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.customizers.ToolProperty;
import net.java.dev.imagine.api.properties.Property;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class PropertiesTest {

    @Test
    public void test() throws Exception {
        Property<Boolean> bp = ToolProperty.createBooleanProperty(Stuff.STUFF, false);
        boolean val = bp.get();
        if (val) {
            bp.set(false);
            val = false;
        }
        assertFalse(val);

        CL cl = new CL();
        bp.addChangeListener(cl);
        boolean changed = bp.set(true);
        assertTrue(changed);
        
        assertTrue(bp.get());
        
        cl.assertChanged();
        changed = bp.set(false);
        assertTrue(changed);
        
        assertFalse(bp.get());
        cl.assertChanged();
        
        changed = bp.set(false);
        assertFalse(changed);

        Property<Double> dp = ToolProperty.createDoubleProperty(Stuff.MORE_STUFF, 0.7D);
        dp.set(0.7D);
        
        Property<Integer> ip = ToolProperty.scale(dp);
        cl = new CL();
        
        int scaled = ip.get();
        assertEquals(70, scaled);
        
        ip.set(80);
        
        double d = dp.get();
        assertEquals(0.8D, d, 0.1);

    }

    static class CL implements ChangeListener {

        boolean changed;

        @Override
        public void stateChanged(ChangeEvent e) {
            changed = true;
        }

        void assertChanged() {
            boolean old = changed;
            changed = false;
            assertTrue(old);
        }
    }

    enum Stuff {

        STUFF,
        MORE_STUFF
    }
}
