/*
 * PaintedPrimitive.java
 *
 * Created on October 31, 2006, 8:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.aggregate;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Aggregate;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Mutable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Proxy;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.graphics.AffineTransformWrapper;
import net.java.dev.imagine.api.vector.graphics.Background;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import net.java.dev.imagine.api.vector.graphics.ColorWrapper;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.utils.Holder;
import net.java.dev.imagine.api.vector.Vectors;

/**
 * Wrapper for a primitive that can be painted multiple times with different
 * painting attributes. I.e. if you stroke a rectangle with blue and fill it
 * with red, then you have a PaintedPrimitive for the rectangle with two
 * AttributeSets (which represent 0 or more settings that may be performed on a
 * Graphics2D, such as setting the color).
 * <p>
 * In other words, this represents multiple series of drawing instructions, each
 * of which culminates in stroking or filling the same geometric primitive.
 * <p>
 *
 * @author Tim Boudreau
 */
public abstract class PaintedPrimitive implements Primitive, Proxy, Aggregate {

    protected Primitive p;

    //Note - this class intentionally implements nothing but Primitive, though
    //it implements the methods of all likely interfaces;  inner
    //subclasses add the necessary interfaces to the class signature;  the
    //static create() method selects the correct one for the primitive it has
    //been passed
    List<AttributeSet> attributeSets = new LinkedList<>();

    PaintedPrimitive(List<Attribute<?>> attributes, Primitive p) {
        this.p = p;
        assert p instanceof Fillable || p instanceof Strokable || p instanceof Vectors;
        boolean fill = p instanceof Fillable ? ((Fillable) p).isFill() : false;
        add(attributes, fill);
        assertClass();
    }

    PaintedPrimitive(Primitive p, List<AttributeSet> attributeSets) {
        this.p = p;
        this.attributeSets.addAll(attributeSets);
        assert p instanceof Fillable || p instanceof Strokable || p instanceof Vectors;
        assertClass();
    }

    private void assertClass() {
        Class c = getClass();
        assert c == SAV.class || c == SAVF.class || c == SAVFV.class
                || c == SAVFVM.class || c == SAVVM.class || c == Ve.class
                || c == Vo.class : "Not to be implemented outside of package"; //NOI18N
    }

    public Shape toShape() {
        Shape result = null;
        if (p instanceof Vectors) {
            result = ((Vectors) p).toShape();
        } else if (p instanceof Volume) {
            Rectangle2D.Double r = new Rectangle2D.Double();
            ((Volume) p).getBounds(r);
            result = r;
        } else {
            assert false : "Not a Vector or a Volume.  Don't call toShape() on me."; //NOI18N
        }
        return result;
    }

    public Pt getLocation() {
        return p instanceof Vectors ? ((Vectors) p).getLocation()
                : Pt.ORIGIN;
    }

    public void setLocation(double x, double y) {
        if (p instanceof Vectors) {
            ((Vectors) p).setLocation(x, y);
        }
    }

    public void clearLocation() {
        if (p instanceof Vectors) {
            ((Vectors) p).clearLocation();
        }
    }

    @Override
    public void paint(Graphics2D g) {
        for (AttributeSet set : attributeSets) {
            set.paint(g);
        }
    }

    public void fill(Graphics2D g) {
        paint(g);
        p.paint(g);
    }

    public void draw(Graphics2D g) {
        paint(g);
        if (p instanceof Strokable) {
            ((Strokable) p).draw(g);
        } else {
            p.paint(g);
        }
    }

    public boolean isFill() {
        return p instanceof Fillable ? ((Fillable) p).isFill() : false;
    }

    public void getBounds(Rectangle2D.Double r) {
        if (p instanceof Volume) {
            ((Volume) p).getBounds(r);
        }
        //Now compensate for stroke size so we really get the size right
        double strokeWidth = 0D;
        AffineTransform xform = null;
        for (AttributeSet a : attributeSets) {
            int max = a.size();
            for (int i = 0; i < max; i++) {
                Attribute at = a.get(i);
                if (at instanceof BasicStrokeWrapper) {
                    strokeWidth = Math.max(strokeWidth,
                            ((BasicStrokeWrapper) at).lineWidth);
                }
                if (at instanceof AffineTransformWrapper) {
                    AffineTransformWrapper w
                            = at.as(AffineTransformWrapper.class);
                    xform = w.getAffineTransform();
                }
            }
        }
        if (xform != null && !xform.isIdentity()) {
            Shape shape = xform.createTransformedShape(r);
            r.setFrame(shape.getBounds2D());
        }
        if (strokeWidth != 0D) {
            double halved = strokeWidth / 2;
            r.x -= halved;
            r.y -= halved;
            r.width += strokeWidth;
            r.height += strokeWidth;
        }
    }

    public int getControlPointCount() {
        return p instanceof Adjustable
                ? ((Adjustable) p).getControlPointCount() : 0;
    }

    public void getControlPoints(double[] xy) {
        ((Adjustable) p).getControlPoints(xy);
    }

    public int[] getVirtualControlPointIndices() {
        return ((Adjustable) p).getVirtualControlPointIndices();
    }

    public boolean delete(int pointIndex) {
        if (!(p instanceof Mutable)) {
            return false;
        }
        return ((Mutable) p).delete(pointIndex);
    }

    public boolean insert(double x, double y, int index, int kind) {
        if (!(p instanceof Mutable)) {
            return false;
        }
        return ((Mutable) p).insert(x, y, index, kind);
    }

    public int getPointIndexNearest(double x, double y) {
        if (!(p instanceof Mutable)) {
            return -1;
        }
        return ((Mutable) p).getPointIndexNearest(x, y);
    }

    public PaintedPrimitive copy(AffineTransform transform) {
        Vectors v = ((Vectors) p).copy(transform);
        List<AttributeSet> attrs = copyAttrs();
        if (this instanceof Ve) {
            return new Ve(v, attrs);
        } else if (this instanceof SAV) {
            return new SAV(v, attrs);
        } else if (this instanceof SAVF) {
            return new SAVF(v, attrs);
        } else if (this instanceof SAVFV) {
            return new SAVFV(v, attrs);
        } else if (this instanceof SAVFVM) {
            return new SAVFVM(v, attrs);
        } else if (this instanceof SAVVM) {
            return new SAVVM(v, attrs);
        } else {
            throw new IllegalArgumentException("What is this? " + this); //NOI18N
        }
    }

    List<AttributeSet> copyAttrs(AffineTransform xform) {
        List<AttributeSet> result = new ArrayList<>(attributeSets.size());
        for (AttributeSet set : attributeSets) {
            result.add(set.clone());
        }
        return result;
    }

    List<AttributeSet> copyAttrs() {
        List<AttributeSet> result = new ArrayList<>(attributeSets.size());
        for (AttributeSet set : attributeSets) {
            result.add(set.clone());
        }
        return result;
    }

    public Primitive getDrawnObject() {
        return p;
    }

    public boolean matchesDrawnObject(Primitive p) {
        return p.equals(this.p);
    }

    public void add(List<Attribute<?>> attributes, boolean fill) {
        attributeSets.add(new AttributeSet(p, fill, attributes));
    }

    @Override
    public int getPrimitiveCount() {
        int result = 0;
        for (AttributeSet a : attributeSets) {
            result += a.size();
        }
        return result + 1; //one for the only visual primitive we represent
    }

    @Override
    public Primitive getPrimitive(int i) {
        int ct = 0;
        for (AttributeSet a : attributeSets) {
            int sz = a.size();
            if (i >= ct && i < ct + sz) {
                return a.get(i - ct);
            }
            ct += sz;
        }
        if (i == ct) {
            return p;
        }
        throw new IndexOutOfBoundsException("Tried to fetch " + i + " out of " //NOI18N
                + getPrimitiveCount());
    }

    @Override
    public int getVisualPrimitiveCount() {
        return 1;
    }

    @Override
    public Primitive getVisualPrimitive(int i) {
        if (i == 0) {
            return getProxiedPrimitive();
        } else {
            throw new IndexOutOfBoundsException("Tried to fetch primitive " + i //NOI18N
                    + ".  There is only one."); //NOI18N
        }
    }

    @Override
    public Primitive getProxiedPrimitive() {
        return p;
    }

    public void setControlPointLocation(int pointIndex, Pt location) {
        ((Adjustable) p).setControlPointLocation(pointIndex, location);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.p);
        hash = 53 * hash + Objects.hashCode(this.attributeSets);
        hash = 53 * hash + Objects.hashCode(this.font);
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
        final PaintedPrimitive other = (PaintedPrimitive) obj;
        if (!Objects.equals(this.p, other.p)) {
            return false;
        }
        if (!Objects.equals(this.attributeSets, other.attributeSets)) {
            return false;
        }
        return Objects.equals(this.font, other.font);
    }

    public interface AttributeConsumer {

        void accept(Vectors vector, Shape shape, boolean fill, boolean draw,
                Paint fillWith, Paint drawWith, Color background, BasicStroke stroke,
                AffineTransform xform);

    }

    public int visit(Holder<Paint> currentFill, Holder<Paint> currentDraw,
            Holder<Color> currentBackground, Holder<BasicStroke> currentStroke,
            Holder<AffineTransform> currentTransform, AttributeConsumer c) {

        int result = 0;
        int max = attributeSets.size() - 1;
        for (int i = max; i >= 0; i--) {
            AttributeSet set = attributeSets.get(i);
            result += set.visit(currentFill, currentDraw, currentBackground,
                    currentStroke, currentTransform, c);
        }
        return result;
    }

    public Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }

    private static final class AttributeSet {

        private final List<Attribute<?>> attributes = new LinkedList<>();
        private final boolean fill;
        private final Primitive shape;

        AttributeSet(Primitive shape, boolean fill, List<Attribute<?>> attributes) {
            this.attributes.addAll(attributes);
            this.fill = fill;
            this.shape = shape;
        }

        int visit(Holder<Paint> currentFill, Holder<Paint> currentDraw,
                Holder<Color> currentBackground, Holder<BasicStroke> currentStroke,
                Holder<AffineTransform> currentTransform, AttributeConsumer c) {
            boolean expectingFill = false;
            if (shape instanceof Fillable) {
                Fillable v = (Fillable) shape;
                expectingFill = v.isFill();
            }
            for (Attribute<?> attr : attributes) {
//                System.out.println("ATTR " + attr + " " + attr.getClass().getSimpleName());
                attr.as(Background.class, bg -> {
//                    System.out.println("set background");
                    currentBackground.set(bg.toColor());
                });
                if (attr instanceof ColorWrapper) {
                    if (expectingFill) {
//                        System.out.println("set fill " + attr.as(ColorWrapper.class).get());
                        currentFill.set(attr.as(ColorWrapper.class).get());
                    } else {
//                        System.out.println("set draw " + attr.as(ColorWrapper.class).get());
                        currentDraw.set(attr.as(ColorWrapper.class).get());
                    }
                } else if (attr instanceof AffineTransformWrapper) {
//                    System.out.println("set transform");
                    currentTransform.set(attr.as(AffineTransformWrapper.class).getAffineTransform());
                } else if (attr instanceof BasicStrokeWrapper) {
//                    System.out.println("set stroke " + attr.as(BasicStrokeWrapper.class).get());
                    currentStroke.set(attr.as(BasicStrokeWrapper.class).get());
                }
            }
            if (shape instanceof Vectors) {
                Vectors v = (Vectors) shape;
                c.accept(v, v.toShape(), expectingFill, !expectingFill, currentFill.get(), currentDraw.get(), currentBackground.get(), currentStroke.get(), currentTransform.get());
                return 1;
            } else {
                return 0;
            }
        }

        public List<Attribute<?>> getAttributes() {
            return Collections.unmodifiableList(attributes);
        }

        public int size() {
            return attributes.size();
        }

        public Attribute get(int i) {
            return attributes.get(i);
        }

        @Override
        public AttributeSet clone() {
            List<Attribute<?>> nue = new ArrayList<>(attributes.size());
            for (Attribute a : attributes) {
                nue.add((Attribute) a.copy());
            }
            return new AttributeSet(shape.copy(), fill, nue);
        }

        public void paint(Graphics2D g) {
            for (Attribute a : attributes) {
                a.paint(g);
            }
            if (fill && shape instanceof Fillable) {
                ((Fillable) shape).fill(g);
            } else if (!fill && shape instanceof Strokable) {
                ((Strokable) shape).draw(g);
            } else {
                shape.paint(g);
            }
        }

        private void toString(StringBuilder sb) {
            sb.append("  attrSet(").append(attributes.size())
                    .append(") for ").append(shape).append('\n');
            int ix = 1;
            for (Attribute a : attributes) {
                sb.append(ix++).append(". ")
                        .append(a);
                if (a instanceof Fillable) {
                    sb.append(" fill ").append(((Fillable) a).isFill());
                }
                sb.append('\n');
            }
        }

        @Override
        public String toString() {
            return "AttributeSet for one paint of " + shape + " with " + attributes; //NOI18N
        }
    }

    private FontWrapper font;

    public void setFont(FontWrapper f) {
        this.font = f;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PaintedPrimitive (").append(attributeSets.size()).append(") for ").append(p).append('\n');
        for (AttributeSet a : attributeSets) {
            a.toString(sb);
        }
        return sb.toString();
//        return "PaintedPrimitive " + p + " : " + attributeSets; //NOI18N
    }

    public List<Attribute<?>> allAttributes() {
        List<Attribute<?>> result = new ArrayList<>();
        for (AttributeSet set : attributeSets) {
            result.addAll(set.getAttributes());
        }
        return result;
    }

    public void applyTransform(AffineTransform xform) {
        Set<Attribute> seen = new HashSet<>();
        for (AttributeSet a : attributeSets) {
            for (Attribute aa : a.attributes) {
                if (aa instanceof Transformable) {
                    if (seen.contains(aa)) {
                        continue;
                    }
                    ((Transformable) aa).applyTransform(xform);
                    seen.add(aa);
                }
            }
        }
    }

    public ControlPointKind[] getControlPointKinds() {
        if (p instanceof Adjustable) {
            return ((Adjustable) p).getControlPointKinds();
        }
        return new ControlPointKind[0];
    }

    //Below are logic-free subclasses of PaintedPrimitive which
    //implement the various combinations of interfaces needed.  All
    //methods of all interfaces are implemented by the parent class,
    //so the subclass simply marks which interfaces it provides so
    //that it will be equivalent to the object it wraps.
    private static final class Vo extends PaintedPrimitive implements Volume {

        Vo(Primitive p, List<AttributeSet> attrs) {
            super(p, attrs);
            assert p instanceof Volume;
        }

        Vo(List<Attribute<?>> attrs, Primitive p) {
            super(attrs, p);
            assert p instanceof Volume;
        }

        @Override
        public Vo copy() {
            return new Vo(p, super.copyAttrs());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (p instanceof Shaped) {
                return ((Shaped) p).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class Ve extends PaintedPrimitive implements Vectors {

        Ve(Primitive p, List<AttributeSet> attrs) {
            super(p, attrs);
            assert p instanceof Vectors;
        }

        Ve(List<Attribute<?>> attrs, Primitive p) {
            super(attrs, p);
            assert p instanceof Vectors;
        }

        @Override
        public Ve copy() {
            return new Ve(p, super.copyAttrs());
        }

        @Override
        public Ve copy(AffineTransform transform) {
            Vectors v = ((Vectors) p).copy(transform);
            return new Ve(v, copyAttrs());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (p instanceof Shaped) {
                return ((Shaped) p).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class SAV extends PaintedPrimitive implements Strokable, Adjustable, Vectors {

        SAV(Primitive p, List<AttributeSet> attrs) {
            super(p, attrs);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Vectors;
        }

        SAV(List<Attribute<?>> attrs, Primitive p) {
            super(attrs, p);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Vectors;
        }

        @Override
        public SAV copy() {
            return new SAV(p, super.copyAttrs());
        }

        @Override
        public SAV copy(AffineTransform transform) {
            Vectors v = ((Vectors) p).copy(transform);
            return new SAV(v, copyAttrs());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (p instanceof Shaped) {
                return ((Shaped) p).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class SAVVM extends PaintedPrimitive implements Strokable, Adjustable, Vectors, Volume, Mutable {

        SAVVM(Primitive p, List<AttributeSet> attrs) {
            super(p, attrs);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Vectors;
            assert p instanceof Volume;
            assert p instanceof Mutable;
        }

        SAVVM(List<Attribute<?>> attrs, Primitive p) {
            super(attrs, p);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Vectors;
            assert p instanceof Volume;
            assert p instanceof Mutable;
        }

        @Override
        public SAVVM copy() {
            return new SAVVM(p, super.copyAttrs());
        }

        @Override
        public SAVVM copy(AffineTransform transform) {
            Vectors v = ((Vectors) p).copy(transform);
            return new SAVVM(v, copyAttrs());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (p instanceof Shaped) {
                return ((Shaped) p).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class SAVF extends PaintedPrimitive implements Strokable, Adjustable, Volume, Fillable {

        SAVF(Primitive p, List<AttributeSet> attrs) {
            super(p, attrs);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Volume;
            assert p instanceof Fillable;
        }

        SAVF(List<Attribute<?>> attrs, Primitive p) {
            super(attrs, p);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Volume;
            assert p instanceof Fillable;
        }

        @Override
        public SAVF copy() {
            return new SAVF(p, super.copyAttrs());
        }

        @Override
        public SAVF copy(AffineTransform transform) {
            Vectors v = ((Vectors) p).copy(transform);
            return new SAVF(v, copyAttrs());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (p instanceof Shaped) {
                return ((Shaped) p).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    private static final class SAVFV extends PaintedPrimitive implements Strokable, Adjustable, Volume, Fillable, Vectors {

        SAVFV(Primitive p, List<AttributeSet> attrs) {
            super(p, attrs);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Volume;
            assert p instanceof Fillable;
            assert p instanceof Vectors;
        }

        SAVFV(List<Attribute<?>> attrs, Primitive p) {
            super(attrs, p);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Volume;
            assert p instanceof Fillable;
            assert p instanceof Vectors;
        }

        @Override
        public SAVFV copy() {
            return new SAVFV(p, super.copyAttrs());
        }

        @Override
        public SAVFV copy(AffineTransform transform) {
            Vectors v = ((Vectors) p).copy(transform);
            return new SAVFV(v, copyAttrs());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (p instanceof Shaped) {
                return ((Shaped) p).restorableSnapshot();
            }
            return () -> {
            };
        }

    }

    private static final class SAVFVM extends PaintedPrimitive implements Strokable, Adjustable, Volume, Fillable, Vectors, Mutable {

        SAVFVM(Primitive p, List<AttributeSet> attrs) {
            super(p, attrs);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Volume;
            assert p instanceof Fillable;
            assert p instanceof Vectors;
            assert p instanceof Mutable;
        }

        SAVFVM(List<Attribute<?>> attrs, Primitive p) {
            super(attrs, p);
            assert p instanceof Strokable;
            assert p instanceof Adjustable;
            assert p instanceof Volume;
            assert p instanceof Fillable;
            assert p instanceof Vectors;
            assert p instanceof Mutable;
        }

        @Override
        public SAVFVM copy() {
            return new SAVFVM(p, super.copyAttrs());
        }

        @Override
        public SAVFVM copy(AffineTransform transform) {
            Vectors v = ((Vectors) p).copy(transform);
            return new SAVFVM(v, copyAttrs());
        }

        @Override
        public Runnable restorableSnapshot() {
            if (p instanceof Shaped) {
                return ((Shaped) p).restorableSnapshot();
            }
            return () -> {
            };
        }
    }

    public static PaintedPrimitive create(Primitive p, Attribute<?>... attrs) {
        return create(p, Arrays.asList(attrs));
    }

    public static PaintedPrimitive create(Primitive p, List<Attribute<?>> attributes) {
        if (p instanceof Strokable && p instanceof Fillable && p instanceof Volume && p instanceof Adjustable && p instanceof Vectors && p instanceof Mutable) {
            return new SAVFVM(attributes, p);
        } else if (p instanceof Strokable && p instanceof Fillable && p instanceof Volume && p instanceof Adjustable && p instanceof Vectors) {
            return new SAVFV(attributes, p);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Volume && p instanceof Fillable) {
            return new SAVF(attributes, p);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Volume && p instanceof Vectors) {
            return new SAVVM(attributes, p);
        } else if (p instanceof Strokable && p instanceof Adjustable && p instanceof Vectors) {
            return new SAV(attributes, p);
        } else if (p instanceof Volume) {
            return new Vo(attributes, p);
        } else if (p instanceof Vectors) {
            return new Ve(attributes, p);
        } else {
            throw new IllegalArgumentException("Unknown type combination:" + p); //NOI18N
        }
    }
}
