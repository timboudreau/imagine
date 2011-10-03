package net.java.dev.imagine.effects.impl;

import java.util.Collection;
import net.java.dev.imagine.effects.api.Effect;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class EffectAnnotationProcessorTest {

    @Test
    public void test() {
        Collection<? extends Effect> all = Effect.allEffects(); //Lookups.forPath("effects").lookupAll(Effect.class);
        System.err.println("ALL EFFECTS : " + all);
        assertEquals(2, all.size());
        Effect e = all.iterator().next();
        assertEquals("SomeEffect", e.getName());
        assertTrue(e.createInitialParam() instanceof Data);
        assertEquals(Data.class, e.parameterType());
        
        assertEquals(e, Effect.getEffectByName("SomeEffect"));
    }
}
