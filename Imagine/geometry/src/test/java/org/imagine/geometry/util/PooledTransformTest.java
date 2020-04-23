package org.imagine.geometry.util;

import com.mastfrog.util.collections.IntSet;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class PooledTransformTest {

    private int doSomething() {
        AffineTransform xf1 = PooledTransform.getQuadrantRotateInstance(32);
        return System.identityHashCode(xf1);
    }

    @Test
    public void testPool() throws InterruptedException {

        IntSet hashCodes = IntSet.arrayBased(500);

        int oldLoops = PooledTransform.loops;
        byte[] bts = new byte[1024 * 1024 * 1024];
        int loop;
        int count = 0;
        for (loop = 0; PooledTransform.loops == oldLoops; loop++) {
            bts = new byte[1024 * 1024];
            Arrays.fill(bts, (byte) loop);
            for (int i = 0; i < 50; i++) {
                hashCodes.add(doSomething());
                count++;
                System.gc();
                Thread.sleep(100);
            }
            Thread.yield();
            bts = null;
            System.out.println("Loop " + loop);
        }
        Thread.sleep(200);
        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(100);
        }
        System.out.println(bts);

        AffineTransform xf2 = PooledTransform.getRotateInstance(10);
        int idHash2 = System.identityHashCode(xf2);

        System.out.println("Have " + hashCodes.size() + " hcs for " + loop + " loops and " + count);

        assertTrue(hashCodes.contains(idHash2));
    }
}
