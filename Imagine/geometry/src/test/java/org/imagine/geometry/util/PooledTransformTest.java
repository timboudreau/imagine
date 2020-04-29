package org.imagine.geometry.util;

import java.awt.geom.AffineTransform;
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

        Thing t1 = new Thing();

        AffineTransform xf1 = PooledTransform.getQuadrantRotateInstance(2, t1);
        AffineTransform xf2 = PooledTransform.getRotateInstance(0.5, t1);

        int hash1 = System.identityHashCode(xf1);
        int hash2 = System.identityHashCode(xf2);

        int oldReclaimed = PooledTransform.POOL.recycled();
        int phantSize = PooledTransform.POOL.outstanding();
        System.out.println("reclaimed was " + oldReclaimed);
        System.out.println("old phants " + phantSize);
        t1 = null;
        for (int i = 0; i < 150; i++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(10);
//            System.out.println("  phants sz now " + PooledTransform.phants.size());
        }
        int newReclaimed = PooledTransform.POOL.recycled();
        int newPhantSize = PooledTransform.POOL.outstanding();
        System.out.println("reclaimed was " + newReclaimed);
        System.out.println("old phants " + newPhantSize);

        Thing t2 = new Thing();
        AffineTransform xf3 = PooledTransform.getTranslateInstance(3, 3, t2);
        AffineTransform xf4 = PooledTransform.getTranslateInstance(5, 5, t2);

        System.out.println("phants " + PooledTransform.POOL);

        int hash3 = System.identityHashCode(xf3);
        int hash4 = System.identityHashCode(xf4);

        assertTrue(hash1 == hash3 || hash1 == hash4, "First item should have been recycled");
        assertTrue(hash2 == hash3 || hash2 == hash4, "First item should have been recycled");

        System.out.println("Hashes " + hash1 + " / " + hash2 + " / " + hash3 + " / " + hash4);
    }

    static class Thing {

        private static int ids;
        private final int id = ids++;

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + this.id;
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
            if (this.id != other.id) {
                return false;
            }
            return true;
        }

        public String toString() {
            return Integer.toString(id);
        }
    }
}
