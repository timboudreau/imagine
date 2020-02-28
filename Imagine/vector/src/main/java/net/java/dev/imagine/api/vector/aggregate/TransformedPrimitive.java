package net.java.dev.imagine.api.vector.aggregate;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Mutable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Proxy;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.elements.Arc;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.Line;
import net.java.dev.imagine.api.vector.elements.Oval;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.Polygon;
import net.java.dev.imagine.api.vector.elements.Polyline;
import net.java.dev.imagine.api.vector.elements.Rectangle;
import net.java.dev.imagine.api.vector.elements.RoundRect;
import net.java.dev.imagine.api.vector.elements.StringWrapper;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 * Represents a primitive which is transformed using an AffineTransform. Note
 * that constructing a TransformedPrimitive over an existing one will clear its
 * location data, transferring its positioning information to the
 * AffineTransform that determines the position, scaling and rotation of the
 * TransformedPrimitive.
 *
 */
public abstract class TransformedPrimitive implements Primitive, Proxy {

    protected final AffineTransform xform;
    protected final Primitive primitive;

    private TransformedPrimitive(final Primitive primitive, final boolean xlate) {
        this(primitive, xlate ? convert(primitive)
                : AffineTransform.getTranslateInstance(0, 0));
    }

    private TransformedPrimitive(final Primitive primitive, final AffineTransform xform) {
        this.primitive = primitive;
        double[] matrix = new double[6];
        xform.getMatrix(matrix);
        this.xform = new AffineTransform(matrix);
    }

    private static AffineTransform convert(Primitive primitive) {
        AffineTransform at;
        if (primitive instanceof Vector) {
            Pt p = ((Vector) primitive).getLocation();
            ((Vector) primitive).clearLocation();
            at = AffineTransform.getTranslateInstance(p.x, p.y);
        } else {
            at = AffineTransform.getTranslateInstance(0, 0);
        }
        return at;
    }

    public void applyTransform(AffineTransform xform) {
        this.xform.concatenate(xform);
    }

    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }

    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform currXform = g2.getTransform();
        currXform.concatenate(xform);
        g2.setTransform(currXform);
        ((Strokable) primitive).draw(g2);
        g2.dispose();
    }

    public Shape toShape() {
        return xform.createTransformedShape(((Vector) primitive).toShape());
    }

    public Pt getLocation() {
        Pt pt = ((Vector) primitive).getLocation();
        double[] d = new double[]{pt.x, pt.y};
        xform.transform(d, 0, d, 0, 1);
        return new Pt((int) d[0], (int) d[1]);
    }

    @Override
    public void paint(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform currXform = g2.getTransform();
        currXform.concatenate(xform);
        g2.setTransform(currXform);
        primitive.paint(g2);
        g2.dispose();
    }

    @Override
    public Primitive getProxiedPrimitive() {
        return primitive;
    }

    public void fill(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        AffineTransform currXform = g2.getTransform();
        currXform.concatenate(xform);
        g2.setTransform(currXform);
        ((Fillable) primitive).fill(g2);
        g2.dispose();
    }

    public boolean isFill() {
        return ((Fillable) primitive).isFill();
    }

    public void getBounds(Rectangle2D.Double dest) {
        //Pending:  optimize
        dest.setRect(toShape().getBounds2D());
    }

    public int getControlPointCount() {
        return ((Adjustable) primitive).getControlPointCount();
    }

    public void getControlPoints(double[] xy) {
        int ct = ((Adjustable) primitive).getControlPointCount();
        ((Adjustable) primitive).getControlPoints(xy);
        xform.transform(xy, 0, xy, 0, ct);
    }

    @Override
    public boolean equals(Object o) {
        boolean result = o instanceof TransformedPrimitive;
        if (result) {
            TransformedPrimitive tp = (TransformedPrimitive) o;
            result = tp.xform.equals(xform)
                    && tp.primitive.equals(primitive);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return xform.hashCode() * primitive.hashCode() * 3;
    }

    public void setLocation(double x, double y) {
        Pt p = getLocation();
        double offX = x - p.x;
        double offY = y - p.y;
        xform.concatenate(
                AffineTransform.getTranslateInstance(offX, offY));
    }

    public void clearLocation() {
        xform.setToIdentity();
    }

    @Override
    public String toString() {
        return super.toString() + " for " + primitive + " XFORM " + xform;
    }

    public int[] getVirtualControlPointIndices() {
        return ((Adjustable) primitive).getVirtualControlPointIndices();
    }

    public ControlPointKind[] getControlPointKinds() {
        if (primitive instanceof Adjustable) {
            return ((Adjustable) primitive).getControlPointKinds();
        }
        return new ControlPointKind[0];
    }

    public Primitive toIdentityPrimitive() {
        if (primitive instanceof Arc) {
            Arc arc = (Arc) primitive;
            return arc.copy(xform);
        } else if (primitive instanceof Line) {
            Line line = (Line) primitive;
            return line.copy(xform);
        } else if (primitive instanceof Oval) {
            Oval oval = (Oval) primitive;
            return oval.copy(xform);
        } else if (primitive instanceof PathIteratorWrapper) {
            PathIteratorWrapper piw = (PathIteratorWrapper) primitive;
            return piw.copy(xform);
        } else if (primitive instanceof Polygon) {
            Polygon polygon = (Polygon) primitive;
            return polygon.copy(xform);
        } else if (primitive instanceof Polyline) {
            Polyline polyline = (Polyline) primitive;
            return polyline.copy(xform);
        } else if (primitive instanceof Rectangle) {
            Rectangle rectangle = (Rectangle) primitive;
            return rectangle.copy(xform);
        } else if (primitive instanceof RoundRect) {
            RoundRect roundRect = (RoundRect) primitive;
            return roundRect.copy(xform);
        } else if (primitive instanceof StringWrapper) {
            StringWrapper stringWrapper = (StringWrapper) primitive;
            return stringWrapper.copy(xform);
        } else if (primitive instanceof ImageWrapper) {
            ImageWrapper imageWrapper = (ImageWrapper) primitive;
            return imageWrapper.copy(xform);
        } else if (primitive instanceof TransformedPrimitive) {
            TransformedPrimitive paintedPrimitive = (TransformedPrimitive) primitive;
            Primitive x = paintedPrimitive;
            while (x instanceof TransformedPrimitive) {
                x = ((TransformedPrimitive) x).copy(xform);
            }
            return x;
        } else {
            throw new IllegalArgumentException("What is this?"
                    + primitive);
        }
    }

    public Vector copy(AffineTransform trans) {
        double[] matrix = new double[6];
        xform.getMatrix(matrix);
        AffineTransform nue = new AffineTransform(matrix);
        nue.concatenate(trans);
        if (this instanceof Ve) {
            return new Ve(primitive, nue);
        } else if (this instanceof SAV) {
            return new SAV(primitive, nue);
        } else if (this instanceof SAVF) {
            return new SAVF(primitive, nue);
        } else if (this instanceof SAVVM) {
            return new SAVVM(primitive, nue);
        } else if (this instanceof SAVFV) {
            return new SAVFV(primitive, nue);
        } else if (this instanceof SAVFVM) {
            return new SAVFVM(primitive, nue);
        } else {
            throw new IllegalStateException("Copy called on "
                    + //NOI18N
                    "non-vector instance " + this); //NOI18N
        }
    }

    public boolean delete(int pointIndex) {
        return ((Mutable) primitive).delete(pointIndex);
    }

    public boolean insert(double x, double y, int index, int kind) {
        return ((Mutable) primitive).insert(x, y, index, kind);
    }

    public int getPointIndexNearest(double x, double y) {
        return ((Mutable) primitive).getPointIndexNearest(x, y);
    }

    public void setControlPointLocation(int pointIndex, Pt location) {
        ((Adjustable) primitive).setControlPointLocation(pointIndex, location);
    }

    //XXX create Mutable variants
    //Below are logic-free subclasses of TransformedPrimitive which
    //implement the various combinations of interfaces needed.  All
    //methods of all interfaces are implemented by the parent class,
    //so the subclass simply marks which interfaces it provides so
    //that it will be equivalent to the object it wraps.
    private static final class Vo extends TransformedPrimitive implements Volume {

        Vo(Primitive primitive) {
            super(primitive, true);
            assert primitive instanceof Volume;
        }

        Vo(Primitive primitive, AffineTransform xform) {
            super(primitive, xform);
            assert primitive instanceof Volume;
        }

        @Override
        public Vo copy() {
            return new Vo(this.primitive, (AffineTransform) xform.clone());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (primitive instanceof Shaped) {
                return ((Shaped) primitive).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class Ve extends TransformedPrimitive implements Vector {

        Ve(Primitive primitive) {
            super(primitive, true);
            assert primitive instanceof Vector;
        }

        Ve(Primitive primitive, AffineTransform xform) {
            super(primitive, xform);
            assert primitive instanceof Vector;
        }

        @Override
        public Ve copy() {
            return new Ve(this.primitive, (AffineTransform) xform.clone());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (primitive instanceof Shaped) {
                return ((Shaped) primitive).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    static class SAV extends TransformedPrimitive implements Strokable, Adjustable, Vector {

        SAV(Primitive primitive) {
            super(primitive, true);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Vector;
        }

        SAV(Primitive primitive, AffineTransform xform) {
            super(primitive, xform);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Vector;
        }

        @Override
        public SAV copy() {
            return new SAV(this.primitive, (AffineTransform) xform.clone());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (primitive instanceof Shaped) {
                return ((Shaped) primitive).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class SAVVM extends TransformedPrimitive implements Strokable, Adjustable, Vector, Volume, Mutable {

        SAVVM(Primitive primitive) {
            super(primitive, true);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Vector;
            assert primitive instanceof Volume;
        }

        SAVVM(Primitive primitive, AffineTransform xform) {
            super(primitive, xform);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Vector;
            assert primitive instanceof Volume;
        }

        @Override
        public SAVVM copy() {
            return new SAVVM(this.primitive, (AffineTransform) xform.clone());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (primitive instanceof Shaped) {
                return ((Shaped) primitive).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class SAVF extends TransformedPrimitive implements Strokable, Adjustable, Volume, Fillable {

        SAVF(Primitive primitive) {
            super(primitive, true);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Volume;
            assert primitive instanceof Fillable;
        }

        SAVF(Primitive primitive, AffineTransform xform) {
            super(primitive, xform);
        }

        @Override
        public SAVF copy() {
            return new SAVF(this.primitive, (AffineTransform) xform.clone());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (primitive instanceof Shaped) {
                return ((Shaped) primitive).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class SAVFV extends TransformedPrimitive implements Strokable, Adjustable, Volume, Fillable, Vector {

        SAVFV(Primitive primitive) {
            super(primitive, true);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Volume;
            assert primitive instanceof Fillable;
            assert primitive instanceof Vector;
        }

        SAVFV(Primitive primitive, AffineTransform xform) {
            super(primitive, xform);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Volume;
            assert primitive instanceof Fillable;
            assert primitive instanceof Vector;
        }

        @Override
        public SAVFV copy() {
            return new SAVFV(this.primitive, (AffineTransform) xform.clone());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (primitive instanceof Shaped) {
                return ((Shaped) primitive).restorableSnapshot();
            }
            return () -> {
            };
        }

    }

    private static final class SAVFVM extends TransformedPrimitive implements Strokable, Adjustable, Volume, Fillable, Vector, Mutable {

        SAVFVM(Primitive primitive) {
            super(primitive, true);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Volume;
            assert primitive instanceof Fillable;
            assert primitive instanceof Vector;
        }

        SAVFVM(Primitive primitive, AffineTransform xform) {
            super(primitive, xform);
            assert primitive instanceof Strokable;
            assert primitive instanceof Adjustable;
            assert primitive instanceof Volume;
            assert primitive instanceof Fillable;
            assert primitive instanceof Vector;
        }

        @Override
        public SAVFVM copy() {
            return new SAVFVM(this.primitive, (AffineTransform) xform.clone());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (primitive instanceof Shaped) {
                return ((Shaped) primitive).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    public static TransformedPrimitive create(Primitive p) {
        if (p == null) {
            throw new NullPointerException("Null primitive");
        } else if (p instanceof Strokable && p instanceof Fillable && p instanceof Volume && p instanceof Adjustable && p instanceof Vector && p instanceof Mutable) {
            return new SAVFVM(p);
        } else if (p instanceof Strokable && p instanceof Fillable && p instanceof Volume && p instanceof Adjustable && p instanceof Vector) {
            return new SAVFV(p);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Volume && p instanceof Fillable) {
            return new SAVF(p);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Volume && p instanceof Vector) {
            return new SAVVM(p);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Vector) {
            return new SAV(p);
        } else if (p instanceof Volume) {
            return new Vo(p);
        } else if (p instanceof Vector) {
            return new Ve(p);
        } else {
            throw new IllegalArgumentException("Unknown type combination:" + p); //NOI18N
        }
    }

    public static TransformedPrimitive create(Primitive p, AffineTransform xform) {
        if (p == null) {
            throw new NullPointerException("Null primitive");
        } else if (p instanceof Strokable && p instanceof Fillable && p instanceof Volume && p instanceof Adjustable && p instanceof Vector && p instanceof Mutable) {
            return new SAVFVM(p, xform);
        } else if (p instanceof Strokable && p instanceof Fillable && p instanceof Volume && p instanceof Adjustable && p instanceof Vector) {
            return new SAVFV(p, xform);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Volume && p instanceof Fillable) {
            return new SAVF(p, xform);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Volume && p instanceof Vector) {
            return new SAVVM(p, xform);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Vector) {
            return new SAV(p, xform);
        } else if (p instanceof Volume) {
            return new Vo(p, xform);
        } else if (p instanceof Vector) {
            return new Ve(p, xform);
        } else {
            throw new IllegalArgumentException("Unknown type combination:" + p); //NOI18N
        }
    }
}
