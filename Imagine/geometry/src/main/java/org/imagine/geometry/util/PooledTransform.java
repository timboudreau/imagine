package org.imagine.geometry.util;

import com.mastfrog.util.preconditions.Exceptions;
import com.mastfrog.util.thread.AtomicLinkedQueue;
import java.awt.geom.AffineTransform;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Profiling shows AffineTransforms to be a significant source of GC pressure;
 * use a pool to avoid having gigabytes of AffineTransform instances lying
 * around. Since this is a Swing application, cache locality is pretty well out
 * the window to begin with (you're constantly interacting with objects in
 * different generations of the heap), so this is unlikely to make things worse.
 * <p>
 * Get transforms in one of two ways:
 * <ul>
 * <li>Pass an owner object - recycling the transform when the owning object is
 * garbage collected. The owner object should be whatever has the
 * <i>shortest</i> lifecycle and could possibly use the transform, so it can be
 * recycled as soon as possible.
 * </li>
 * <li>Call one of the methods that takes a lambda, use the transform passed to
 * it, and <i>do not save it anywhere or use it outside that scope</i>.
 * </li>
 * </p>
 *
 * @author Tim Boudreau
 */
public final class PooledTransform extends AffineTransform {

    public static AffineTransform getTranslateInstance(double tx, double ty, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToTranslation(tx, ty);
        return Tx;
    }

    public static void withTranslateInstance(double tx, double ty, Consumer<? super AffineTransform> c) {
        PooledTransform Tx = POOL.takeFromPool(null);
        Tx.setToTranslation(tx, ty);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    public static AffineTransform getRotateInstance(double theta, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToRotation(theta);
        return Tx;
    }

    public static void withRotateInstance(double theta, Consumer<? super AffineTransform> c) {
        PooledTransform Tx = POOL.takeFromPool(null);
        Tx.setToRotation(theta);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    public static AffineTransform getRotateInstance(double theta,
            double anchorx,
            double anchory, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToRotation(theta, anchorx, anchory);
        return Tx;
    }

    public static void withRotateInstance(double theta,
            double anchorx,
            double anchory, Consumer<? super AffineTransform> c) {
        PooledTransform Tx = POOL.takeFromPool();
        Tx.setToRotation(theta, anchorx, anchory);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    public static AffineTransform getRotateInstance(double vecx, double vecy, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToRotation(vecx, vecy);
        return Tx;
    }

    public static void withRotateInstance(double vecx, double vecy, Consumer<? super AffineTransform> c) {
        PooledTransform Tx = POOL.takeFromPool();
        Tx.setToRotation(vecx, vecy);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    public static AffineTransform getRotateInstance(double vecx,
            double vecy,
            double anchorx,
            double anchory, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToRotation(vecx, vecy, anchorx, anchory);
        return Tx;
    }

    public static void withRotateInstance(double vecx,
            double vecy,
            double anchorx,
            double anchory, Consumer<? super AffineTransform> c) {
        PooledTransform Tx = POOL.takeFromPool();
        Tx.setToRotation(vecx, vecy, anchorx, anchory);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    public static AffineTransform getQuadrantRotateInstance(int numquadrants, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToQuadrantRotation(numquadrants);
        return Tx;
    }

    public static void getQuadrantRotateInstance(int numquadrants, Consumer<? super AffineTransform> c) {
        PooledTransform Tx = POOL.takeFromPool();
        Tx.setToQuadrantRotation(numquadrants);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    public static AffineTransform getQuadrantRotateInstance(int numquadrants,
            double anchorx,
            double anchory, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToQuadrantRotation(numquadrants, anchorx, anchory);
        return Tx;
    }

    public static void withQuadrantRotateInstance(int numquadrants,
            double anchorx, double anchory, Consumer<PooledTransform> c) {
        PooledTransform Tx = POOL.takeFromPool();
        Tx.setToQuadrantRotation(numquadrants, anchorx, anchory);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    public static AffineTransform getScaleInstance(double sx, double sy, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToScale(sx, sy);
        return Tx;
    }

    public static void withScaleInstance(double sx, double sy, Consumer<? super AffineTransform> c) {
        PooledTransform Tx = POOL.takeFromPool();
        Tx.setToScale(sx, sy);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    public static AffineTransform getShearInstance(double shx, double shy, Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToShear(shx, shy);
        return Tx;
    }

    public static void withShearInstance(double shx, double shy, Consumer<? super AffineTransform> c) {
        PooledTransform Tx = POOL.takeFromPool();
        Tx.setToShear(shx, shy);
        try {
            c.accept(Tx);
        } finally {
            POOL.returnToPool(Tx);
        }
    }

    private static final TransformPool POOL = new TransformPool();

    static {
        POOL.start();
    }

    static int loops;

    private static ThreadLocal<Object> SCOPE = new ThreadLocal<>();
    static Set<TransformPool.Phant> phants = ConcurrentHashMap.newKeySet(50);

    public static void inScope(Object o, Runnable r) {
        Object old = SCOPE.get();
        try {
            SCOPE.set(o);
            r.run();
        } finally {
            if (old == null) {
                SCOPE.remove();
            } else {
                SCOPE.set(old);
            }
        }
    }

    public static TransformSupplier scopedTo(Object o) {
        return new TransformSupplier(o);
    }

    /**
     * Provider for transforms which are all tied to the lifecycle of the object
     * it was created with, and will not be recycled until that object is.
     */
    public static final class TransformSupplier {

        private final Object scope;

        public TransformSupplier(Object scope) {
            this.scope = scope;
        }

        public AffineTransform get() {
            return POOL.takeFromPool(scope);
        }

        public AffineTransform copyOf(AffineTransform xform) {
            double[] mx = new double[6];
            xform.getMatrix(mx);
            return withMatrix(mx[0], mx[1], mx[2], mx[3], mx[4], mx[5]);
        }

        public AffineTransform withMatrix(double m00, double m10,
                double m01, double m11,
                double m02, double m12) {
            AffineTransform xform = POOL.takeFromPool(scope);
            xform.setTransform(m00, m10, m01, m11, m02, m12);
            return xform;
        }

        public AffineTransform getShearInstance(double shx, double shy) {
            AffineTransform xform = POOL.takeFromPool(scope);
            xform.setToShear(shx, shy);
            return xform;
        }

        public AffineTransform getScaleInstance(double sx, double sy) {
            AffineTransform xform = POOL.takeFromPool(scope);
            xform.setToScale(sx, sy);
            return xform;
        }

        public AffineTransform getQuadrantRotateInstance(int numquadrants,
                double anchorx,
                double anchory) {
            PooledTransform Tx = POOL.takeFromPool(scope);
            Tx.setToQuadrantRotation(numquadrants, anchorx, anchory);
            return Tx;
        }

        public AffineTransform getQuadrantRotateInstance(int numquadrants) {
            PooledTransform Tx = POOL.takeFromPool(scope);
            Tx.setToQuadrantRotation(numquadrants);
            return Tx;
        }

        public AffineTransform getRotateInstance(double vecx,
                double vecy,
                double anchorx,
                double anchory) {
            PooledTransform Tx = POOL.takeFromPool(scope);
            Tx.setToRotation(vecx, vecy, anchorx, anchory);
            return Tx;
        }

        public AffineTransform getTranslateInstance(double tx, double ty) {
            PooledTransform Tx = POOL.takeFromPool(scope);
            Tx.setToTranslation(tx, ty);
            return Tx;
        }

        public AffineTransform getRotateInstance(double theta) {
            PooledTransform Tx = POOL.takeFromPool(scope);
            Tx.setToRotation(theta);
            return Tx;
        }
    }

    static final class TransformPool implements UncaughtExceptionHandler {

        private final int maxSize = 100;
        private final AtomicLinkedQueue<PooledTransform> available = new AtomicLinkedQueue<>();
        private final AtomicInteger poolSize = new AtomicInteger();
        private final ReferenceQueue<Object> rq = new ReferenceQueue<>();

        void start() {
            Thread t = new Thread(this::pollingLoop);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1);
            t.setUncaughtExceptionHandler(this);
            t.start();
        }

        void pollingLoop() {
            Thread.currentThread().setName("PooledTransform recovery");
            System.out.println("Polling loop start");
            for (long loop = 0;; loop++) {
                try {
                    Reference<? extends Object> ref = rq.remove();
                    if (ref == null) {
                        System.out.println("no ref " + loop);
                        continue;
                    }
                    ref.clear();
                    loops++;
                } catch (InterruptedException ex) {
                    Logger.getLogger(PooledTransform.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        PooledTransform takeFromPool() {
            Object owner = SCOPE.get();

            return takeFromPool(owner);
        }

        PooledTransform takeFromPool(Object owner) {
            PooledTransform result = available.pop();
            if (result == null) {
                Reference<? extends Object> ref = rq.poll();
                if (ref instanceof Phant) {
                    result = ((Phant) ref).remove();
                    if (result != null) {
                        System.out.println("recycle " + System.identityHashCode(result));
                        return result;
                    }
                }
                result = new PooledTransform();
                if (owner != null && phants.size() < maxSize) {
                    Phant phant = new Phant(result, rq, result);
                    phants.add(phant);
                }
                poolSize.set(0);
            } else {
                poolSize.getAndUpdate(val -> {
                    return Math.max(val - 1, 0);
                });
            }
            System.out.println("take from pool " + System.identityHashCode(result)
                    + " size " + poolSize.get());
            return result;
        }

        boolean returnToPool(PooledTransform xform) {
            int count = poolSize.getAndUpdate(val -> {
                return Math.min(val + 1, maxSize);
            });
            if (count < maxSize) {
                System.out.println("Return to pool " + count);
                available.add(xform);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Exceptions.printStackTrace(e);
        }

        static class Phant extends PhantomReference<Object> {

            private final AtomicReference<PooledTransform> xform;

            public Phant(Object referent, ReferenceQueue<? super Object> q, PooledTransform xform) {
                super(referent, q);
                this.xform = new AtomicReference<>(xform);
            }

            PooledTransform remove() {
                PooledTransform xf = xform.getAndSet(null);
                return xf;
            }

            @Override
            public void clear() {
                PooledTransform result = remove();
                phants.remove(this);
                if (result != null) {
                    POOL.returnToPool(result);
                }
                super.clear();
            }
        }
    }
}
