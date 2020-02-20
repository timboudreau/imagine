/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package net.java.dev.imagine.api.vector.elements;

import com.mastfrog.util.collections.IntList;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.io.Serializable;
import static java.lang.Double.doubleToLongBits;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Mutable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import static net.java.dev.imagine.api.vector.design.ControlPointKind.CUBIC_CURVE_DESTINATION;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 * Wrapper for a PathIterator - i.e. any shape.
 *
 * @author Tim Boudreau
 */
public final class PathIteratorWrapper implements Strokable, Fillable, Volume, Adjustable, Vector, Mutable {

    private static final int[] EMPTY_INTS = new int[0];
    private final boolean fill;

    private Segment[] segments;

    /**
     * Creates a new instance of PathIteratorWrapper2
     */
    public PathIteratorWrapper(PathIterator it) {
        this(it, false);
    }

    public PathIteratorWrapper(PathIterator it, boolean fill) {
        unpack(it);
        this.fill = fill;
    }

    public PathIteratorWrapper(GlyphVector gv, float x, float y) {
        this(gv.getOutline(x, y).getPathIterator(null));
    }

    PathIteratorWrapper(Segment[] segments, boolean fill) {
        this.segments = segments;
        this.fill = fill;
    }

    public boolean hasMultiplePaths() {
        int ct = 0;
        for (Segment seg : segments) {
            if (seg.type == PathIterator.SEG_MOVETO) {
                ct++;
                if (ct > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public void split(Consumer<PathIteratorWrapper> wrapper) {
        List<Segment> curr = new ArrayList<>(segments.length);
        for (Segment seg : segments) {
            if (seg.type == PathIterator.SEG_MOVETO) {
                if (!curr.isEmpty()) {
                    wrapper.accept(new PathIteratorWrapper(curr.toArray(new Segment[curr.size()]), fill));
                    curr.clear();
                }
            }
            curr.add(seg);
        }
        if (!curr.isEmpty()) {
            wrapper.accept(new PathIteratorWrapper(curr.toArray(new Segment[curr.size()]), fill));
            curr.clear();
        }
    }

    public synchronized boolean deleteElement(int element) {
        if (element < 0 || element >= segments.length) {
            return false;
        }
        Segment seg = segments[element];
        if (seg.type == PathIterator.SEG_MOVETO || seg.type == PathIterator.SEG_CLOSE) {
            return false;
        }
        Segment[] nue = new Segment[segments.length - 1];
        System.arraycopy(segments, 0, nue, 0, element);
        System.arraycopy(segments, element + 1, nue, element, segments.length - (element + 1));
        segments = nue;
        return true;
    }

    private void unpack(PathIterator it) {
        double[] d = new double[6];
        List<Segment> segments = new ArrayList<>();
        while (!it.isDone()) {
            Arrays.fill(d, 0D);
            byte type = (byte) it.currentSegment(d);
            segments.add(new Segment(d, type));
            it.next();
        }
        this.segments = segments.toArray(new Segment[segments.size()]);
    }

    @Override
    public void translate(double x, double y) {
        for (Segment s : segments) {
            s.translate(x, y);
        }
    }

    public Point2D nearestPhysicalPointTo(Point2D loc) {
        double x = loc.getX();
        double y = loc.getY();
        double minDist = java.lang.Double.MAX_VALUE;
        Point2D best = null;
        for (Segment seg : segments) {
            Point2D phys = seg.physicalPoint();
            if (phys == null) {
                continue;
            }
            double d = Point2D.distance(x, y, phys.getX(), phys.getY());
            if (d < minDist) {
                best = phys;
            }
        }
        return best;
    }

    private static final class Segment implements Serializable {

        private static final int[] MORE_PRIMES = new int[]{
            2_311, 4_877, 5_237, 8_089, 10_079, 13_219, 16_547, 17_519, 19_211, 7, 149
        };

        private static int[] PRIMES = new int[]{
            149, 3_253, 9_431, 18_457, 33_641};

        final double[] data;
        final byte type;

        Segment(Segment other) {
            data = other.data == null ? null
                    : Arrays.copyOf(other.data, other.data.length);
            this.type = other.type;
        }

        private Segment copy() {
            return new Segment(this);
        }

        public void transform(AffineTransform xform) {
            if (data != null) {
                xform.transform(data, 0, data, 0, data.length);
            }
        }

        public Point2D physicalPoint() {
            if (type == PathIterator.SEG_CLOSE) {
                return null;
            }
            return new Point2D.Double(data[data.length - 2],
                    data[data.length - 1]);
        }

        public double[] primaryPoint() {
            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    return Arrays.copyOf(data, 2);
                case PathIterator.SEG_QUADTO:
                    return new double[]{data[2], data[3]};
                case PathIterator.SEG_CUBICTO:
                    return new double[]{data[4], data[5]};
                case PathIterator.SEG_CLOSE:
                    return null;
                default:
                    throw new AssertionError(type);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    sb.append("MoveTo ").append(data[0]).append(',')
                            .append(data[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    sb.append("LineTo ").append(data[0]).append(',')
                            .append(data[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    sb.append("QuadTo ").append(data[0]).append(',')
                            .append(data[1]).append(',').append(data[2])
                            .append(',').append(data[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    sb.append("CubicTo ").append(data[0]).append(',')
                            .append(data[1]).append(",").append(data[2])
                            .append(',').append(data[3]).append(',')
                            .append(data[4]).append(",").append(data[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    sb.append("Close");
                    break;
                default:
                    sb.append("Unknown type ").append(type);
            }
            return sb.toString();
        }

        Segment(double[] data, byte type) {
            this.type = type;
            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    assert data.length >= 2;
                    this.data = new double[]{
                        data[0], data[1],};
                    break;
                case PathIterator.SEG_QUADTO:
                    assert data.length >= 4;
                    this.data = new double[]{
                        data[0], data[1], data[2], data[3],};
                    break;
                case PathIterator.SEG_CUBICTO:
                    assert data.length >= 6;
                    this.data = new double[data.length];
                    System.arraycopy(data, 0, this.data, 0, data.length);
                    break;
                case PathIterator.SEG_CLOSE:
                    this.data = null;
                    break;
                default:
                    throw new AssertionError("PathIterator provided unknown "
                            + "segment type " + type);
            }
        }

        int primaryPointIndexInternal() {
            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    return 0;
                case PathIterator.SEG_QUADTO:
                    return 1;
                case PathIterator.SEG_CUBICTO:
                    return 2;
                default:
                    return -1;
            }
        }

        int getPointCount() {
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    return 1;
                case PathIterator.SEG_LINETO:
                    return 1;
                case PathIterator.SEG_QUADTO:
                    return 2;
                case PathIterator.SEG_CUBICTO:
                    return 3;
                case PathIterator.SEG_CLOSE:
                    return 0;
                default:
                    throw new AssertionError("PathIterator provided unknown "
                            + "segment type " + type);
            }
        }

        public boolean hasControlPoints() {
            return getPointCount() > 1;
        }

        void translate(double dx, double dy) {
            if (data != null) {
                for (int i = 0; i < data.length; i += 2) {
                    data[i] += dx;
                    data[i + 1] += dy;
                }
            }
        }

        int copyData(double[] dest, int offset) {
            int count = (getPointCount() * 2);
            if (data != null) {
                System.arraycopy(data, 0, dest, offset, count);
            }
            return count;
        }

        double distanceTo(double x, double y) {
            double[] pts = primaryPoint();
            if (pts == null) {
                return java.lang.Double.MAX_VALUE;
            }
            if (pts[0] == x && pts[1] == y) {
                return 0;
            }
            double rx = x - pts[0];
            double ry = y - pts[1];
            return Math.sqrt((rx * rx) + (ry * ry));
        }

        int ixNearest(double x, double y, double[] dist) {
            int result = -1;
            double shortestLength = Integer.MAX_VALUE;
            if (data != null) {
                for (int i = 0; i < data.length; i += 2) {
                    double rx = x - data[i];
                    double ry = y - data[i + 1];
                    dist[0] = Math.sqrt((rx * rx) + (ry * ry));
                    if (dist[0] < shortestLength) {
                        result = i / 2;
                    }
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            }
            boolean result = o instanceof Segment;
            if (result) {
                Segment s = (Segment) o;
                result = s.type == type;
                if (result) {
                    if (type != PathIterator.SEG_CLOSE) {
                        result = Arrays.equals(data, s.data);
                    }
                }
            }
            return result;
        }

        @Override
        public int hashCode() {
            if (data == null || data.length == 0) {
                return 1;
            }
            long bits = PRIMES[type];
            for (int i = 0; i < data.length; i++) {
                bits += doubleToLongBits(data[i])
                        * (MORE_PRIMES[i % MORE_PRIMES.length]);
            }
            return (((int) bits) ^ ((int) (bits >> 32)));
        }
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        int count = 0;
        for (Segment s : segments) {
            count += s.getPointCount();
        }
        ControlPointKind[] result = new ControlPointKind[count];
        int cursor = 0;
        for (Segment segment : segments) {
            switch (segment.type) {
                case PathIterator.SEG_MOVETO:
                    result[cursor++] = ControlPointKind.START_POINT;
                    break;
                case PathIterator.SEG_LINETO:
                    result[cursor++] = ControlPointKind.LINE_TO_DESTINATION;
                    break;
                case PathIterator.SEG_QUADTO:
                    result[cursor++] = ControlPointKind.QUADRATIC_CONTROL_POINT;
                    result[cursor++] = ControlPointKind.QUADRATIC_CURVE_DESTINATION;
                    break;
                case PathIterator.SEG_CUBICTO:
                    result[cursor++] = ControlPointKind.CUBIC_CONTROL_POINT;
                    result[cursor++] = ControlPointKind.CUBIC_CONTROL_POINT;
                    result[cursor++] = ControlPointKind.CUBIC_CURVE_DESTINATION;
                    break;
            }
        }
        return result;
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    public synchronized int size() {
        return segments.length;
    }

    @Override
    public synchronized Shape toShape() {
        Path2D.Double path = new Path2D.Double();
        for (Segment seg : segments) {
            switch (seg.type) {
                case PathIterator.SEG_MOVETO:
                    path.moveTo(seg.data[0], seg.data[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    path.lineTo(seg.data[0], seg.data[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    path.quadTo(seg.data[0], seg.data[1], seg.data[2],
                            seg.data[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    path.curveTo(seg.data[0], seg.data[1], seg.data[2],
                            seg.data[3],
                            seg.data[4], seg.data[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    path.closePath();
                    break;
                default:
                    throw new AssertionError("PathIterator provided unknown "
                            + "segment type " + seg.type);

            }
        }
        return path;
    }

    @Override
    public Pt getLocation() {
        //XXX optimize
        java.awt.Rectangle r = toShape().getBounds();
        return new Pt(r.getLocation());
    }

    @Override
    public void setLocation(double x, double y) {
        Pt old = getLocation();
        double offx = x - old.x;
        double offy = y - old.y;
        for (Segment seg : segments) {
            seg.translate(offx, offy);
        }
    }

    @Override
    public void clearLocation() {
        Pt pt = getLocation();
        setLocation(0 - pt.x, 0 - pt.y);
    }

    public PathIteratorWrapper transform(AffineTransform xform) {
        if (xform == null || AffineTransform.getTranslateInstance(0, 0).equals(xform)) {
            return this;
        }
        for (Segment segment : segments) {
            segment.transform(xform);
        }
        return this;
    }

    public synchronized Runnable restorableSnapshot() {
        Segment[] segs = new Segment[segments.length];
        for (int i = 0; i < segs.length; i++) {
            segs[i] = new Segment(segments[i]);
        }
        return () -> {
            synchronized (this) {
                this.segments = segs;
            }
        };
    }

    public void setShape(Shape shape) {
        synchronized (this) {
            this.unpack(shape.getPathIterator(null));
        }
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform.isIdentity()) {
            return;
        }
        unpack(xform.createTransformedShape(toShape())
                .getPathIterator(null));
    }

    @Override
    public PathIteratorWrapper copy(AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            Segment[] nue = new Segment[segments.length];
            for (int i = 0; i < segments.length; i++) {
                Segment seg = segments[i];
                Segment s = new Segment(seg);
                nue[i] = s;
            }
            return new PathIteratorWrapper(nue, fill);
        } else {
            return new PathIteratorWrapper(toShape().getPathIterator(null), fill);
        }
    }

    @Override
    public void paint(Graphics2D g) {
        if (isFill()) {
            fill(g);
        } else {
            draw(g);
        }
    }

    @Override
    public PathIteratorWrapper copy() {
        Segment[] segs = new Segment[segments.length];
        for (int i = 0; i < segs.length; i++) {
            segs[i] = segments[i].copy();
        }
        return new PathIteratorWrapper(segs, fill);
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(toShape());
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public void getBounds(Double dest) {
        //XXX optimize
        dest.setRect(toShape().getBounds2D());
    }

    public synchronized boolean movePathElement(int elementIndex, double offsetX, double offsetY) {
        if (elementIndex < 0 || elementIndex >= segments.length) {
            throw new IllegalArgumentException("Bad index " + elementIndex + " in " + segments.length);
        }
        Segment seg = segments[elementIndex];
        if (seg.data != null && seg.data.length > 0) {
            for (int i = 0; i < seg.data.length; i += 2) {
                seg.data[i] += offsetX;
                seg.data[i + 1] = seg.data[i] + offsetY;
            }
            return true;
        }
        return false;
    }

    public synchronized boolean movePointAndControlPoints(int pointIndex, double offsetX, double offsetY) {
        int targetSegment = -1;
        int ct = 0;
        for (int i = 0; i < segments.length; i++) {
            int pc = segments[i].getPointCount();
            if (pointIndex >= ct && pointIndex < ct + pc) {
                targetSegment = i;
                break;
            }
            ct += pc;
        }
        if (targetSegment >= 0) {
            Segment seg = segments[targetSegment];
            if (seg.data != null && seg.data.length > 0) {
                for (int i = 0; i < seg.data.length; i += 2) {
                    seg.data[i] += offsetX;
                    seg.data[i + 1] = seg.data[i] + offsetY;
                }
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface PointConsumer {

        void accept(double x, double y, boolean isControl);
    }

    public synchronized void visitPoints(PointConsumer c) {
        for (Segment seg : segments) {
            double[] data = seg.data;
            if (data != null) {
                for (int j = 0; j < data.length; j += 2) {
                    c.accept(data[j], data[j + 1], j != data.length - 2);
                }
            }
        }
    }

    @Override
    public synchronized int getControlPointCount() {
        int result = 0;
        for (Segment segment : segments) {
            int ct = segment.getPointCount();
            //            System.err.println(segments[i] + ": " + ct);
            result += ct;
        }
        return result;
    }

    @Override
    public synchronized void getControlPoints(double[] xy) {
        int ix = 0;
        for (Segment segment : segments) {
            ix += segment.copyData(xy, ix);
        }
    }

    public synchronized int[] getConcretePointIndices() {
        IntList ints = null;
        int ct = 0;
        for (Segment seg : segments) {
            switch (seg.type) {
                case PathIterator.SEG_CUBICTO:
                    if (ints == null) {
                        ints = IntList.create(getControlPointCount());
                    }
                    ints.add(ct + 2);
                    break;
                case PathIterator.SEG_QUADTO:
                    if (ints == null) {
                        ints = IntList.create(getControlPointCount());
                    }
                    ints.add(ct + 1);
                    break;
                case PathIterator.SEG_LINETO:
                case PathIterator.SEG_MOVETO:
                    if (ints == null) {
                        ints = IntList.create(getControlPointCount());
                    }
                    ints.add(ct);
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
                default:
                    throw new AssertionError(seg.type + " is not a segment type");
            }
            ct += seg.getPointCount();
        }
        return ints == null ? EMPTY_INTS : ints.toIntArray();
    }

    @Override
    public synchronized int[] getVirtualControlPointIndices() {
        IntList ints = null;
        int ct = 0;
        for (Segment seg : segments) {
            switch (seg.type) {
                case PathIterator.SEG_CUBICTO:
                    if (ints == null) {
                        ints = IntList.create(8);
                    }
                    ints.add(ct);
                    ints.add(ct + 1);
                    break;
                case PathIterator.SEG_QUADTO:
                    if (ints == null) {
                        ints = IntList.create(8);
                    }
                    ints.add(ct);
                    break;
            }
            ct += seg.getPointCount();
        }
        return ints == null ? EMPTY_INTS : ints.toIntArray();
    }

    @Override
    public synchronized void setControlPointLocation(int pointIndex, Pt location) {
        setControlPointLocation(pointIndex, location.x, location.y);
    }

    public Set<ControlPointKind> availablePointKinds(int point) {
        ControlPointKind[] kinds = getControlPointKinds();
        if (point >= kinds.length) {
            return Collections.emptySet();
        }
        switch (kinds[point]) {
            case CUBIC_CURVE_DESTINATION:
                return EnumSet.of(ControlPointKind.LINE_TO_DESTINATION, ControlPointKind.QUADRATIC_CURVE_DESTINATION);
            case LINE_TO_DESTINATION:
                return EnumSet.of(ControlPointKind.CUBIC_CURVE_DESTINATION, ControlPointKind.QUADRATIC_CURVE_DESTINATION);
            case QUADRATIC_CURVE_DESTINATION:
                return EnumSet.of(ControlPointKind.LINE_TO_DESTINATION, ControlPointKind.CUBIC_CURVE_DESTINATION);
            case START_POINT:
            default:
                return Collections.emptySet();
        }
    }

    private int leastPossibleSegmentForPointIndex(int pointIndex) {
        if (pointIndex == 0) {
            return 0;
        }
        // first segement is always a single point;
        // assuming every point after it is 3-point cubic,
        // divide and we have our starting point, and add
        // the one back in for the 0th point we would have
        // caught above
        return ((pointIndex - 1) / 3) + 1;
    }

    public synchronized void setControlPointLocation(int pointIndex, double x, double y) {
        int ct = 0;
        for (int i = leastPossibleSegmentForPointIndex(pointIndex); i < segments.length; i++) {
            Segment seg = segments[i];
            int pc = seg.getPointCount();
            if (pc != 0 && pointIndex >= ct && pointIndex < ct + pc) {
                int offset = pointIndex - ct;
                seg.data[offset * 2] = x;
                seg.data[(offset * 2) + 1] = y;
                break;
            }
            ct += seg.getPointCount();
        }
        /*
        int cursor = 0;
        for (Segment seg : segments) {
            int ct = seg.getPointCount();
            if (ct == 0) {
                continue;
            }
            if (pointIndex == cursor) {
                seg.data[0] = x;
                seg.data[1] = y;
                return;
            } else if (pointIndex < cursor + ct) {
                int offset = (pointIndex - ct) * 2;
                seg.data[offset] = x;
                seg.data[offset + 1] = y;
            }
            cursor += ct;
        }

         */
    }

    private int entryIndexFor(int pointIndex) {
        int ct = 0;
        for (int i = 0; i < segments.length; i++) {
            Segment seg = segments[i];
            int pc = seg.getPointCount();
            if (pointIndex >= ct && pointIndex < pc + ct) {
                return i;
            }
            ct += pc;
        }
        return -1;
    }

    public byte type(int pointIndex) {
        return segments[validatePointIndex(pointIndex)].type;
    }

    private int validatePointIndex(int pointIndex) {
        if (pointIndex < 0 || pointIndex >= segments.length) {
            throw new IllegalArgumentException(
                    "No point at index " + pointIndex + " - " + segments.length
                    + " present in " + this);
        }
        return pointIndex;
    }

    public PathIteratorWrapper ensureClosed() {
        if (segments.length == 0) {
            return this;
        }
        if (segments[segments.length - 1].type != PathIterator.SEG_CLOSE) {
            synchronized (this) {
                if (segments.length > 1) {
                    Segment first = segments[0];
                    Segment last = segments[segments.length - 1];
                    double[] fpp = first.primaryPoint();
                    double[] lpp = last.primaryPoint();
                    if (fpp != null && lpp != null && Arrays.equals(fpp, lpp)) {
                        // It is directly closed by returning to the origin
                        // point - no SEG_CLOSE needed
                        return this;
                    }
                }

                Segment nue = new Segment(null, (byte) PathIterator.SEG_CLOSE);
                this.segments = Arrays.copyOf(this.segments, this.segments.length + 1);
                this.segments[this.segments.length - 1] = nue;
            }
        }
        return this;
    }

    public PathIteratorWrapper setToCurveTo(int pointIndex, double ctrlX1, double ctrlY1, double ctrlX2, double ctrlY2, double x, double y) {
        validatePointIndex(pointIndex);
        Segment nue = new Segment(new double[]{ctrlX1, ctrlY1, ctrlX2, ctrlY2, x, y}, (byte) PathIterator.SEG_CUBICTO);
        synchronized (this) {
            segments[pointIndex] = nue;
        }
        return this;
    }

    public PathIteratorWrapper setToQuadTo(int pointIndex, double ctrlX, double ctrlY, double x, double y) {
        validatePointIndex(pointIndex);
        Segment nue = new Segment(new double[]{ctrlX, ctrlY, x, y}, (byte) PathIterator.SEG_QUADTO);
        synchronized (this) {
            segments[pointIndex] = nue;
        }
        return this;
    }

    public PathIteratorWrapper setToLineTo(int pointIndex, double x, double y) {
        validatePointIndex(pointIndex);
        Segment nue = new Segment(new double[]{x, y}, (byte) PathIterator.SEG_LINETO);
        synchronized (this) {
            segments[pointIndex] = nue;
        }
        return this;
    }

    public PathIteratorWrapper setToMoveTo(int pointIndex, double x, double y) {
        validatePointIndex(pointIndex);
        Segment nue = new Segment(new double[]{x, y}, (byte) PathIterator.SEG_MOVETO);
        synchronized (this) {
            segments[pointIndex] = nue;
        }
        return this;
    }

    @Override
    public boolean delete(int pointIndex) {
        validatePointIndex(pointIndex);
        int ix = entryIndexFor(pointIndex);
        boolean result = ix >= 0;
        if (result) {
            Segment[] nue = new Segment[segments.length - 1];
            for (int i = 0; i < nue.length; i++) {
                if (i < ix) {
                    nue[i] = segments[i];
                } else if (i >= ix) {
                    nue[i] = segments[i + 1];
                }
            }
            segments = nue;
        }
        return result;
    }

    public synchronized boolean isExplicitlyClosed() {
        return segments.length > 0 && segments[segments.length - 1].type == PathIterator.SEG_CLOSE;
    }

    public synchronized PathIteratorWrapper addLineTo(double x, double y, boolean insertAtNearest) {
        return addLineToOrMoveTo(x, y, true, insertAtNearest);
    }

    public synchronized void resetControlPoints() {
        if (segments.length <= 2) {
            return;
        }
        int prevIndex = segments.length - 1;
        while (prevIndex > 0 && segments[prevIndex].type == PathIterator.SEG_CLOSE) {
            prevIndex--;
        }
        Segment prev = segments[prevIndex];
        for (int i = 0; i < segments.length; i++) {
            Segment curr = segments[i];
            if (!curr.hasControlPoints()) {
                prev = curr;
                prevIndex = i;
                continue;
            }
            int nextIndex = i + 1;
            Segment next = segments[nextIndex];
            while (next.type == PathIterator.SEG_CLOSE && nextIndex != i && nextIndex != prevIndex) {
                nextIndex++;
                if (nextIndex == segments.length) {
                    nextIndex = 0;
                }
                next = segments[nextIndex];
            }
            double[] nextPoint = next.primaryPoint();
            double pc = curr.getPointCount();
            double[] px = curr.primaryPoint();
            double[] prevPoint = prev.primaryPoint();
            double[] newPoint = equidistantPoint(px[0], px[1], nextPoint[0], nextPoint[1]);
            curr.data[0] = newPoint[0];
            curr.data[1] = newPoint[1];
            if (pc == 2) {
                continue;
            }

            newPoint = equidistantPoint(px[0], px[1], prevPoint[0], prevPoint[1]);
            curr.data[2] = newPoint[0];
            curr.data[3] = newPoint[1];
        }
    }

    private static double[] equidistantPoint(double x1, double y1, double x2, double y2) {
        return new double[]{(x1 + x2) / 2D, (y1 + y2) / 2D};
    }

    public synchronized PathIteratorWrapper addQuadTo(double ctrlX1, double ctrlY1, double x, double y, boolean nearest) {
        Segment seg = new Segment(new double[]{ctrlX1, ctrlY1, x, y}, (byte) PathIterator.SEG_QUADTO);
        appendOrInsert(seg, x, y, nearest);
        return this;
    }

    public synchronized PathIteratorWrapper addCubicTo(double ctrlX1, double ctrlY1, double ctrlX2, double ctrlY2, double x, double y, boolean nearest) {
        Segment seg = new Segment(new double[]{ctrlX1, ctrlY1, ctrlX2, ctrlY2, x, y}, (byte) PathIterator.SEG_CUBICTO);
        appendOrInsert(seg, x, y, nearest);
        return this;
    }

    private synchronized PathIteratorWrapper addLineToOrMoveTo(double x, double y, boolean line, boolean nearest) {
        Segment seg = new Segment(new double[]{x, y}, (byte) (line ? PathIterator.SEG_LINETO : PathIterator.SEG_MOVETO));
        appendOrInsert(seg, x, y, nearest);
        return this;
    }

    private synchronized PathIteratorWrapper appendOrInsert(Segment seg, double nearX, double nearY, boolean nearest) {
        Segment[] segs = Arrays.copyOf(segments, segments.length + 1);
        int targetIx;
        if (nearest && segments.length > 1) {
            targetIx = getElementWithPrimaryPointNearest(nearX, nearY);
            Segment target = segments[targetIx];
            if (target.type != PathIterator.SEG_MOVETO) {
                int prev = targetIx - 1;
                if (prev < 0) {
                    prev = segments.length - 1;
                }
                Segment preceding = segments[prev];
                while (prev > 0 && preceding.type == PathIterator.SEG_CLOSE) {
                    prev--;
                    preceding = segments[prev];
                }
                int next = targetIx + 1;
                if (next == segments.length) {
                    next = 0;
                }
                Segment following = segments[next];
                if (following.type == PathIterator.SEG_CLOSE) {
                    next += 1;
                    if (next == segments.length) {
                        next = 0;
                    }
                    following = segments[next];
                }

                if (prev != targetIx && next != targetIx) {
                    double targetDistance = target.distanceTo(nearX, nearY);
                    double prevDistance = preceding.distanceTo(nearX, nearY);
                    double nextDistance = following.distanceTo(nearX, nearY);
                    if (nextDistance < prevDistance) {
                        targetIx = next;
                    }
                }
            } else if (isExplicitlyClosed()) {
                targetIx = segments.length - 1;
            } else {
                targetIx = segments.length;
            }
            if (targetIx == 0 && segments.length > 0) {
                targetIx++;
            }
            Segment[] nue = Arrays.copyOf(segments, segments.length + 1);
            if (targetIx != nue.length - 1) {
                System.arraycopy(segments, targetIx, nue, targetIx + 1, segments.length - targetIx);
            }
            nue[targetIx] = seg;
            this.segments = nue;
        }
        return this;
    }

    @Override
    public boolean insert(double x, double y, int index, int kind) {
        double[] data;
        switch (kind) {
            case PathIterator.SEG_MOVETO:
            case PathIterator.SEG_LINETO:
                data = new double[]{x, y};
                break;
            case PathIterator.SEG_QUADTO:
                data = new double[]{x - 10, y - 10, x, y};
                break;
            case PathIterator.SEG_CUBICTO:
                data = new double[]{x - 10, y, x, y + 10, x, y};
                break;
            case PathIterator.SEG_CLOSE:
                data = new double[0];
                break;
            default:
                throw new AssertionError(Integer.toString(kind));
        }
        int eix = entryIndexFor(index);
        Segment s = new Segment(data, (byte) kind);
        Segment[] nue = new Segment[segments.length + 1];
        for (int i = 0; i < nue.length; i++) {
            Segment seg = i < eix ? segments[i] : i > eix ? segments[i - 1] : s;
            nue[i] = seg;
        }
        segments = nue;
        return true;
    }

    @Override
    public int getPointIndexNearest(double x, double y) {
        double[] dist = new double[1];
        double bestDist = java.lang.Double.MAX_VALUE;
        int ct = 0;
        int result = -1;
        synchronized (this) {
            for (Segment seg : segments) {
                int ix = seg.ixNearest(x, y, dist);
                int realIndex = ct + ix;
                if (dist[0] < bestDist) {
                    bestDist = dist[0];
                    result = realIndex;
                }
                ct += seg.getPointCount();
            }
        }
        return result;
    }

    public synchronized boolean containsSplines() {
        for (Segment s : segments) {
            switch (s.type) {
                case PathIterator.SEG_CUBICTO:
                case PathIterator.SEG_QUADTO:
                    return true;
            }
        }
        return false;
    }

    public synchronized boolean toCubicSplines() {
        boolean result = false;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].type == PathIterator.SEG_LINETO) {
                double[] pt = segments[i].primaryPoint();
                double[] nue = new double[6];
                System.arraycopy(pt, 0, nue, 0, 2);
                System.arraycopy(pt, 0, nue, 2, 2);
                System.arraycopy(pt, 0, nue, 4, 2);
                segments[i] = new Segment(nue, (byte) PathIterator.SEG_CUBICTO);
                result = true;
            }
        }
        return result;
    }

    public synchronized boolean toQuadSplines() {
        boolean result = false;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].type == PathIterator.SEG_LINETO) {
                double[] pt = segments[i].primaryPoint();
                double[] nue = new double[4];
                System.arraycopy(pt, 0, nue, 0, 2);
                System.arraycopy(pt, 0, nue, 2, 2);
                segments[i] = new Segment(nue, (byte) PathIterator.SEG_QUADTO);
                result = true;
            }
        }
        return result;
    }

    public int getPrimaryPointIndexNearest(double x, double y) {
        double bestDist = java.lang.Double.MAX_VALUE;
        int ct = 0;
        int result = -1;
        synchronized (this) {
            for (Segment segment : segments) {
                double dist = segment.distanceTo(x, y);
                if (dist < bestDist) {
                    bestDist = dist;
                    result = ct + segment.primaryPointIndexInternal();
                }
                ct += segment.getPointCount();
            }
        }
        return result;
    }

    public synchronized int getElementWithPrimaryPointNearest(double x, double y) {
        double bestDist = java.lang.Double.MAX_VALUE;
        int result = -1;
        synchronized (this) {
            for (int i = 0; i < segments.length; i++) {
                double dist = segments[i].distanceTo(x, y);
                if (dist < bestDist) {
                    bestDist = dist;
                    result = i;
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else {
            boolean result = o instanceof PathIteratorWrapper;
            if (result) {
                result = Arrays.equals(segments, ((PathIteratorWrapper) o).segments);
            }
            return result;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(segments);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            Segment seg = segments[i];
            sb.append(seg);
            if (i != segments.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }
}
