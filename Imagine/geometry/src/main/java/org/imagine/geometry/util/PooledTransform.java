package org.imagine.geometry.util;

import java.awt.geom.AffineTransform;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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

    public String toString() {
        return GeometryStrings.transformToString(this);
    }

    public static AffineTransform get(Object owner) {
        PooledTransform Tx = POOL.takeFromPool(owner);
        Tx.setToIdentity();
        return Tx;
    }

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

    public static void withQuadrantRotateInstance(int numquadrants, Consumer<? super AffineTransform> c) {
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

    public static void returnToPool(AffineTransform xform) {
        if (xform == null || !(xform instanceof PooledTransform)) {
            return;
        }
        POOL.returnToPool((PooledTransform) xform);
    }

    static final PhantomReferencePool<PooledTransform> POOL = new PhantomReferencePool(
            Thread.NORM_PRIORITY + 1,
            "AffineTransform", 200, PooledTransform::new);

    static {
        POOL.start();
    }

    public static TransformSupplier scopedTo(Object o) {
        return new TransformSupplier(o);
    }

    public static AffineTransform copyOf(AffineTransform xform, Object owner) {
        if (xform == null) {
            return null;
        }
        double[] mx = new double[6];
        xform.getMatrix(mx);
        return withMatrix(mx[0], mx[1], mx[2], mx[3], mx[4], mx[5], owner);
    }

    public static AffineTransform withMatrix(double m00, double m10,
            double m01, double m11,
            double m02, double m12, Object owner) {
        AffineTransform xform = POOL.takeFromPool(owner);
        xform.setTransform(m00, m10, m01, m11, m02, m12);
        return xform;
    }

    public static void withCopyOf(AffineTransform other, Consumer<AffineTransform> xf) {
        POOL.borrow(borrowed -> {
            borrowed.setTransform(other);
            xf.accept(other);
        });
    }

    public static <R> R lazyCopy(AffineTransform toCopy, BiFunction<AffineTransform, Consumer<Object>, R> c) {
        return POOL.lazyTakeFromPool((copy, ownerConsumer) -> {
            copy.setTransform(toCopy);
            return c.apply(copy, ownerConsumer);
        });
    }

    public static <R> R lazyTranslate(double x, double y, BiFunction<AffineTransform, Consumer<Object>, R> c) {
        return POOL.lazyTakeFromPool((copy, ownerConsumer) -> {
            copy.setToTranslation(x, y);
            return c.apply(copy, ownerConsumer);
        });
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
            if (xform == null) {
                return null;
            }
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
}
