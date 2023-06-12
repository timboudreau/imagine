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

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleQuadConsumer;
import com.mastfrog.function.DoubleSextaConsumer;
import com.mastfrog.util.collections.IntList;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import static net.java.dev.imagine.api.vector.design.ControlPointKind.CUBIC_CURVE_DESTINATION;
import net.java.dev.imagine.api.vector.util.Pt;
import com.mastfrog.geometry.Angle;
import com.mastfrog.geometry.Axis;
import com.mastfrog.geometry.DimensionDouble;
import com.mastfrog.geometry.EnhRectangle2D;
import com.mastfrog.geometry.EnhancedShape;
import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.util.GeometryUtils;
import java.awt.Rectangle;
import static java.lang.Math.max;
import static java.lang.Math.min;
import net.java.dev.imagine.api.vector.Vectors;

/**
 * Wrapper for a PathIterator - i.e. any shape.
 *
 * @author Tim Boudreau
 */
public final class PathIteratorWrapper implements Strokable, Fillable, Volume, Adjustable, Vectors, Mutable, Versioned {

    private static final int[] EMPTY_INTS = new int[0];
    private final boolean fill;
    private Segment[] segments;

    private int rev;

    public PathIteratorWrapper(byte[] types, double[][] data) {
        if (types.length != data.length) {
            throw new IllegalArgumentException("Types length " + types.length
                    + " data length " + data.length);
        }
        segments = new Segment[types.length];
        for (int i = 0; i < segments.length; i++) {
            double[] curr = data[i];
            checkArraySizeForType(i, types[i], curr);
            Segment seg = new Segment(curr, (byte) types[i]);
            segments[i] = seg;
        }
        fill = false;
    }

    public PathIteratorWrapper(int[] types, double[][] data) {
        if (types.length != data.length) {
            throw new IllegalArgumentException("Types length " + types.length
                    + " data length " + data.length);
        }
        segments = new Segment[types.length];
        for (int i = 0; i < segments.length; i++) {
            double[] curr = data[i];
            checkArraySizeForType(i, types[i], curr);
            Segment seg = new Segment(curr, (byte) types[i]);
            segments[i] = seg;
        }
        fill = false;
    }

    private void checkArraySizeForType(int i, int type, double[] curr) {
        switch (type) {
            case SEG_MOVETO:
            case SEG_LINETO:
                if (curr == null || curr.length != 2) {
                    throw new IllegalArgumentException("Wrong array length "
                            + "for " + i + " type " + type + " should be 0, got "
                            + (curr == null ? "null" : Arrays.toString(curr)));
                }
                break;
            case SEG_QUADTO:
                if (curr == null || curr.length != 4) {
                    throw new IllegalArgumentException("Wrong array length "
                            + "for " + i + " type " + type + " should be 0, got "
                            + (curr == null ? "null" : Arrays.toString(curr)));
                }
                break;
            case SEG_CUBICTO:
                if (curr == null || curr.length != 6) {
                    throw new IllegalArgumentException("Wrong array length "
                            + "for " + i + " type " + type + " should be 0, got "
                            + (curr == null ? "null" : Arrays.toString(curr)));
                }
                break;
            case SEG_CLOSE:
                if (curr != null && curr.length > 0) {
                    throw new IllegalArgumentException("Wrong array length "
                            + "for " + i + " type " + type + " should be 0, got "
                            + Arrays.toString(curr));
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal type at " + i
                        + ": " + type);
        }
    }

    public PathIteratorWrapper(Shape shape) {
        this(shape.getPathIterator(null));
    }

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

    @Override
    public int rev() {
        return rev;
    }

    private void change() {
        rev++;
    }

    public boolean hasMultiplePaths() {
        int ct = 0;
        for (Segment seg : segments) {
            if (seg.type == SEG_MOVETO) {
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
            if (seg.type == SEG_MOVETO) {
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
        if (seg.type == SEG_MOVETO || seg.type == SEG_CLOSE) {
            return false;
        }
        Segment[] nue = new Segment[segments.length - 1];
        System.arraycopy(segments, 0, nue, 0, element);
        System.arraycopy(segments, element + 1, nue, element, segments.length - (element + 1));
        segments = nue;
        change();
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
        change();
        discardCachedVirtualControlPointIndices();
    }

    @Override
    public double cumulativeLength() {
        return GeometryUtils.shapeLength(toShape());
    }

    @Override
    public void translate(double x, double y) {
        if (x != 0 || y != 0) {
            for (Segment s : segments) {
                s.translate(x, y);
            }
            change();
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

    public interface PointVisitor {

        void visit(int index, int type, double[] data);
    }

    public int visitPoints(PointVisitor v) {
        int result = segments.length;
        for (int i = 0; i < result; i++) {
            Segment seg = segments[i];
            v.visit(i, seg.type, seg.data);
        }
        return result;
    }

    @Override
    public void collectSizings(SizingCollector c) {
        if (segments.length <= 1) {
            return;
        }
        EqLine ln = new EqLine();
        int cpIx = 0;
        int prevCpIx = 0;
        for (int i = 1; i < segments.length; i++) {
            Segment prev = segments[i - 1];
            if (prev.type == SEG_CLOSE) {
                continue;
            }
            Segment curr = segments[i];
            if (curr.type == SEG_CLOSE) {
                continue;
            }
            ln.setLine(prev.physicalX(), prev.physicalY(),
                    curr.physicalX(), curr.physicalY());

            Axis axis = Axis.nearestForLine(ln);
            int pc = curr.getPointCount();
            int targetCp = cpIx + pc - 1;

            switch (axis) {
                case VERTICAL:
                    double height = Math.abs(curr.physicalY() - prev.physicalY());
                    c.dimension(height, true, prevCpIx, targetCp);
                    break;
                case HORIZONTAL:
                    double width = Math.abs(curr.physicalX() - prev.physicalX());
                    c.dimension(width, true, prevCpIx, targetCp);
                    break;
            }
            cpIx += curr.getPointCount();
            prevCpIx = targetCp;
        }
    }

    @Override
    public void collectAngles(AngleCollector c) {
        if (segments.length <= 2) {
            return;
        }
        int lastStart = 0;
        boolean reset = false;
        int emittableCount = 0;
        int cpIndex = 0;
        for (int i = 0; i < segments.length; i++) {
            Segment seg = segments[i];
            reset = false;
            int currCpIndex = cpIndex;
            switch (seg.type) {
                case SEG_CLOSE:
                    reset = true;
                    break;
                case SEG_MOVETO:
                    lastStart = i;
                    emittableCount = 1;
                    cpIndex += 1;
                    break;
                case SEG_LINETO:
                    emittableCount++;
                    cpIndex += 1;
                    break;
                case SEG_CUBICTO:
                    reset = true;
                    cpIndex += 3;
                    currCpIndex += 2;
                    break;
                case SEG_QUADTO:
                    reset = true;
                    cpIndex += 2;
                    currCpIndex += 1;
                    break;
                default:
                    throw new AssertionError("" + seg.type);
            }
            if (reset) {
                emittableCount = 0;
                reset = false;
                lastStart = 0;
            } else if (emittableCount >= 2) {
                Segment start = segments[i - 1];
                double angle = Angle.ofLine(start.physicalX(), start.physicalY(), seg.physicalX(), seg.physicalY());
                // XXX if we include bezier curves, currCpIndex-1 below will be wrong
                c.angle(angle, currCpIndex - 1, currCpIndex);
            }
        }
    }

    /*
    private static int ANGLE_STATE_NOT_STARTED = -1;
    private static int ANGLE_STATE_READY = 1;
    private static int ANGLE_STATE_USED = 0;
    private static int ANGLE_STATE_DONE = 2;

    class AngleIterator implements PrimitiveIterator.OfDouble {

        private final int revAtCreation;
        private int state = ANGLE_STATE_NOT_STARTED;
        private int cursor;
        private int lastMoveTo = -1;
        private int controlPointIndex;
        AngleIterator() {
            revAtCreation = rev();
            state = scanToNext();
        }

        private int scanToNextViable() {

        }

        private int scanToNext() {

            return 0;
        }

        @Override
        public double nextDouble() {
            if (revAtCreation != rev) {
                throw new ConcurrentModificationException("Shape is no longer "
                        + " at revision " + revAtCreation + " -> " + rev);
            }
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean hasNext() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
     */

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

        boolean isClose() {
            return type == SEG_CLOSE;
        }

        boolean isSpline() {
            switch (type) {
                case SEG_CLOSE:
                case SEG_MOVETO:
                case SEG_LINETO:
                    return false;
                default:
                    return true;
            }
        }

        void minMax(double[] xyMinMax) {
            if (data == null) {
                return;
            }
            assert xyMinMax.length == 4;
            for (int i = 0; i < data.length; i += 2) {
                xyMinMax[0] = Math.min(xyMinMax[0], data[i]);
                xyMinMax[1] = Math.min(xyMinMax[1], data[i + 1]);
                xyMinMax[2] = Math.max(xyMinMax[0], data[i]);
                xyMinMax[3] = Math.max(xyMinMax[1], data[i + 1]);
            }
        }

        void minLocation(double[] xy) {
            if (data != null) {
                for (int i = 0; i < data.length; i += 2) {
                    xy[0] = Math.min(xy[0], data[i]);
                    xy[1] = Math.min(xy[1], data[i + 1]);
                }
            }
        }

        void maxLocation(double[] xy) {
            if (data != null) {
                for (int i = 0; i < data.length; i += 2) {
                    xy[0] = Math.max(xy[0], data[i]);
                    xy[1] = Math.max(xy[1], data[i + 1]);
                }
            }
        }

        void get(float[] into) {
            if (data == null) {
                return;
            }
            if (into.length < data.length) {
                throw new IllegalArgumentException("Array size too small - " + into.length
                        + " for " + data.length + " elements");
            }
            for (int i = 0; i < data.length; i++) {
                into[i] = Math.round(data[i]);
            }
        }

        void get(double[] into) {
            if (data == null) {
                return;
            }
            if (into.length < data.length) {
                throw new IllegalArgumentException("Array size too small - " + into.length
                        + " for " + data.length + " elements");
            }
            System.arraycopy(data, 0, into, 0, data.length);
        }

        public void transform(AffineTransform xform) {
            if (data != null) {
                xform.transform(data, 0, data, 0, data.length);
            }
        }

        public Point2D physicalPoint() {
            if (type == SEG_CLOSE) {
                return null;
            }
            return new Point2D.Double(data[data.length - 2],
                    data[data.length - 1]);
        }

        public double physicalX() {
            return data == null ? 0 : data[data.length - 2];
        }

        public double physicalY() {
            return data == null ? 0 : data[data.length - 1];
        }

        public double[] primaryPoint() {
            switch (type) {
                case SEG_MOVETO:
                case SEG_LINETO:
                    return Arrays.copyOf(data, 2);
                case SEG_QUADTO:
                    return new double[]{data[2], data[3]};
                case SEG_CUBICTO:
                    return new double[]{data[4], data[5]};
                case SEG_CLOSE:
                    return null;
                default:
                    throw new AssertionError(type);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            switch (type) {
                case SEG_MOVETO:
                    sb.append("MoveTo ").append(data[0]).append(',')
                            .append(data[1]);
                    break;
                case SEG_LINETO:
                    sb.append("LineTo ").append(data[0]).append(',')
                            .append(data[1]);
                    break;
                case SEG_QUADTO:
                    sb.append("QuadTo ").append(data[0]).append(',')
                            .append(data[1]).append(',').append(data[2])
                            .append(',').append(data[3]);
                    break;
                case SEG_CUBICTO:
                    sb.append("CubicTo ").append(data[0]).append(',')
                            .append(data[1]).append(",").append(data[2])
                            .append(',').append(data[3]).append(',')
                            .append(data[4]).append(",").append(data[5]);
                    break;
                case SEG_CLOSE:
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
                case SEG_MOVETO:
                case SEG_LINETO:
                    assert data.length >= 2;
                    this.data = new double[]{
                        data[0], data[1],};
                    break;
                case SEG_QUADTO:
                    assert data.length >= 4;
                    this.data = new double[]{
                        data[0], data[1], data[2], data[3],};
                    break;
                case SEG_CUBICTO:
                    assert data.length >= 6;
                    this.data = new double[data.length];
                    System.arraycopy(data, 0, this.data, 0, data.length);
                    break;
                case SEG_CLOSE:
                    this.data = null;
                    break;
                default:
                    throw new AssertionError("PathIterator provided unknown "
                            + "segment type " + type);
            }
        }

        int primaryPointIndexInternal() {
            switch (type) {
                case SEG_MOVETO:
                case SEG_LINETO:
                    return 0;
                case SEG_QUADTO:
                    return 1;
                case SEG_CUBICTO:
                    return 2;
                default:
                    return -1;
            }
        }

        int getPointCount() {
            switch (type) {
                case SEG_MOVETO:
                    return 1;
                case SEG_LINETO:
                    return 1;
                case SEG_QUADTO:
                    return 2;
                case SEG_CUBICTO:
                    return 3;
                case SEG_CLOSE:
                    return 0;
                default:
                    throw new AssertionError("PathIterator provided unknown "
                            + "segment type " + type);
            }
        }

        public boolean hasControlPoints() {
            return getPointCount() > 1;
        }

        void addToBounds(Rectangle2D bounds) {
            if (data != null) {
                for (int i = 0; i < data.length; i += 2) {
                    if (i == 0 && bounds.isEmpty()) {
                        bounds.setFrame(data[i], data[i + 1],
                                0.0000000000001, 0.00000000001);
                    } else {
                        bounds.add(data[i], data[i + 1]);
                    }
                }
            }
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
                    if (type != SEG_CLOSE) {
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

        private void applyTransform(AffineTransform xform) {
            if (data != null) {
                xform.transform(data, 0, data, 0, data.length / 2);
            }
        }

        private void applyTo(Path2D.Double result) {
            switch (type) {
                case SEG_CLOSE:
                    result.closePath();
                    break;
                case SEG_MOVETO:
                    result.moveTo(data[0], data[1]);
                    break;
                case SEG_LINETO:
                    result.lineTo(data[0], data[1]);
                    break;
                case SEG_CUBICTO:
                    result.curveTo(data[0], data[1], data[2], data[3], data[4], data[5]);
                    break;
                case SEG_QUADTO:
                    result.quadTo(data[0], data[1], data[2], data[3]);
                    break;
                default:
                    throw new AssertionError(type);
            }
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
                case SEG_MOVETO:
                    result[cursor++] = ControlPointKind.START_POINT;
                    break;
                case SEG_LINETO:
                    result[cursor++] = ControlPointKind.LINE_TO_DESTINATION;
                    break;
                case SEG_QUADTO:
                    result[cursor++] = ControlPointKind.QUADRATIC_CONTROL_POINT;
                    result[cursor++] = ControlPointKind.QUADRATIC_CURVE_DESTINATION;
                    break;
                case SEG_CUBICTO:
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
        Path2D.Float path = new Path2D.Float(PathIterator.WIND_NON_ZERO,
                segments.length);
        for (Segment seg : segments) {
            switch (seg.type) {
                case SEG_MOVETO:
                    path.moveTo(seg.data[0], seg.data[1]);
                    break;
                case SEG_LINETO:
                    path.lineTo(seg.data[0], seg.data[1]);
                    break;
                case SEG_QUADTO:
                    path.quadTo(seg.data[0], seg.data[1], seg.data[2],
                            seg.data[3]);
                    break;
                case SEG_CUBICTO:
                    path.curveTo(seg.data[0], seg.data[1], seg.data[2],
                            seg.data[3],
                            seg.data[4], seg.data[5]);
                    break;
                case SEG_CLOSE:
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
    public <T> T as(Class<T> type) {
        if (EnhancedShape.class == type) {
            return type.cast(new Enh());
        }
        return Strokable.super.as(type);
    }

    @Override
    public boolean is(Class<?> type) {
        if (EnhancedShape.class == type) {
            return true;
        }
        return Strokable.super.is(type);
    }

    class Enh implements EnhancedShape {

        @Override
        public boolean isClosed() {
            boolean result = PathIteratorWrapper.this.isExplicitlyClosed();
            if (!result && segments.length > 1) {
                Segment last = segments[segments.length - 1];
                Segment first = segments[0];
                double fx = first.physicalX();
                double fy = first.physicalY();
                double lx = last.physicalX();
                double ly = last.physicalY();
                result = GeometryUtils.isSamePoint(fx, fy, lx, ly);
            }
            return result;
        }

        @Override
        public void visitAdjoiningLines(DoubleSextaConsumer sex) {
            EnhancedShape.super.visitAdjoiningLines(sex);
        }

        @Override
        public void visitLines(DoubleQuadConsumer consumer) {
            int shapeStart = 0;
            for (int i = 1; i < segments.length; i++) {
                Segment prev = segments[i - 1];
                Segment curr = segments[i];
                if (prev.type != SEG_CLOSE && curr.type != SEG_CLOSE) {
                    consumer.accept(prev.physicalX(), prev.physicalY(), curr.physicalX(), curr.physicalY());
                } else {
                    shapeStart = i + 1;
                }
                if (curr.type == SEG_CLOSE && shapeStart != i) {
                    Segment start = segments[shapeStart];
                    consumer.accept(curr.physicalX(), curr.physicalY(), start.physicalX(), start.physicalY());
                }
            }
        }

        @Override
        public List<? extends EqPointDouble> points() {
            List<EqPointDouble> all = new ArrayList<>();
            visitPoints((x, y) -> {
                all.add(new EqPointDouble(x, y));
            });
            return all;
        }

        @Override
        public void visitPoints(DoubleBiConsumer consumer) {
            for (int i = 0; i < segments.length; i++) {
                Segment seg = segments[i];
                if (seg.type != SEG_CLOSE) {
                    consumer.accept(seg.physicalX(), seg.physicalY());
                }
            }
        }

        @Override
        public Point2D point(int index) {
            if (!isMultipleSegments()) {
                return new EqPointDouble(segments[index].physicalX(),
                        segments[index].physicalY());
            }
            int pointCursor = 0;
            for (int i = 0; i < segments.length; i++) {
                if (pointCursor == i && segments[i].type != SEG_CLOSE) {
                    return new EqPointDouble(segments[i].physicalX(), segments[i].physicalY());
                } else if (segments[i].type != SEG_CLOSE) {
                    pointCursor++;
                }
            }
            throw new IndexOutOfBoundsException("" + index);
        }

        @Override
        public int pointCount() {
            int result = 0;
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].type != SEG_CLOSE) {
                    result++;
                }
            }
            return result;
        }

        @Override
        public DimensionDouble size() {
            DimensionDouble result = new DimensionDouble();
            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (Segment seg : segments) {
                if (seg.type != SEG_CLOSE) {
                    double x = seg.physicalX();
                    double y = seg.physicalX();
                    minX = min(minX, x);
                    maxX = max(maxX, x);
                    minY = min(minY, y);
                    maxY = max(maxY, y);
                }
            }
            if (minX < maxX) {
                result.width = maxX - minX;
            }
            if (minY < maxY) {
                result.height = maxY - minY;
            }
            return result;
        }

        @Override
        public Rectangle getBounds() {
            return getBounds2D().getBounds();
        }

        @Override
        public Rectangle2D getBounds2D() {
            EnhRectangle2D result = new EnhRectangle2D();
            for (Segment s : segments) {
                if (!s.isClose()) {
                    result.add(s.physicalPoint());
                }
            }
            return result;
        }

        @Override
        public boolean contains(double x, double y) {
            return getBounds2D().contains(x, y);
        }

        @Override
        public boolean contains(Point2D p) {
            return contains(p.getX(), p.getY());
        }

        @Override
        public boolean intersects(double x, double y, double w, double h) {
            return getBounds2D().intersects(x, y, w, h);
        }

        @Override
        public boolean intersects(Rectangle2D r) {
            return getBounds2D().intersects(r);
        }

        @Override
        public boolean contains(double x, double y, double w, double h) {
            return getBounds2D().intersects(x, y, w, h);
        }

        @Override
        public boolean contains(Rectangle2D r) {
            return getBounds2D().contains(r);
        }

        @Override
        public PathIterator getPathIterator(AffineTransform at) {
            return PathIteratorWrapper.this.getPathIterator(at);
        }

        @Override
        public PathIterator getPathIterator(AffineTransform at, double flatness) {
            return PathIteratorWrapper.this.getPathIterator(at);
        }
    }

    @Override
    public Pt getLocation() {
        //XXX optimize
        double[] min = new double[]{Double.MAX_VALUE, Double.MAX_VALUE};
        for (Segment seg : segments) {
            seg.minLocation(min);
        }
        return new Pt(min[0] == Double.MAX_VALUE ? 0 : min[0],
                min[1] == Double.MAX_VALUE ? 0 : min[1]);
    }

    @Override
    public void setLocation(double x, double y) {
        Pt old = getLocation();
        double offx = x - old.x;
        double offy = y - old.y;
        if (offx != 0 || offy != 0) {
            for (Segment seg : segments) {
                seg.translate(offx, offy);
            }
            change();
        }
    }

    @Override
    public void clearLocation() {
        Pt pt = getLocation();
        setLocation(-pt.x, -pt.y);
    }

    public PathIteratorWrapper transform(AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            return this;
        }
        for (Segment segment : segments) {
            segment.transform(xform);
        }
        change();
        return this;
    }

    public synchronized Runnable restorableSnapshot() {
        int oldRev = rev;
        Segment[] segs = new Segment[segments.length];
        for (int i = 0; i < segs.length; i++) {
            segs[i] = new Segment(segments[i]);
        }
        return () -> {
            synchronized (this) {
                rev = oldRev;
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
        if (xform == null || xform.isIdentity()) {
            return;
        }
        for (Segment seg : segments) {
            seg.applyTransform(xform);
        }
        change();
//        unpack(xform.createTransformedShape(toShape())
//                .getPathIterator(null));
    }

    @Override
    public PathIteratorWrapper copy(AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            Segment[] nue = new Segment[segments.length];
            for (int i = 0; i < segments.length; i++) {
                nue[i] = new Segment(segments[i]);
            }
            PathIteratorWrapper result = new PathIteratorWrapper(nue, fill);
            result.rev = rev;
            synchronized (this) {
                result.virtualControlPointIndices = virtualControlPointIndices;
            }
            return result;
        } else {
            Segment[] nue = new Segment[segments.length];
            for (int i = 0; i < segments.length; i++) {
                nue[i] = new Segment(segments[i]);
                nue[i].applyTransform(xform);
            }
            PathIteratorWrapper w = new PathIteratorWrapper(nue, fill);
            synchronized (this) {
                w.virtualControlPointIndices = virtualControlPointIndices;
            }
            w.rev = rev + 1;
            return w;
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
        PathIteratorWrapper result = new PathIteratorWrapper(segs, fill);
        synchronized (this) {
            result.virtualControlPointIndices = virtualControlPointIndices;
        }
        return result;
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
    public void addToBounds(Rectangle2D bds) {
        if (true) {
            for (Segment seg : segments) {
                seg.addToBounds(bds);
            }
            return;
        }

        double[] minMax = new double[]{
            Double.MAX_VALUE, Double.MAX_VALUE,
            Double.MIN_VALUE, Double.MIN_VALUE
        };
        for (Segment seg : segments) {
            seg.minMax(minMax);
        }

        if (minMax[0] != Double.MAX_VALUE && minMax[1] != Double.MAX_VALUE
                && minMax[2] != Double.MIN_VALUE && minMax[3] != Double.MIN_VALUE) {
            if (bds.isEmpty()) {
                bds.setFrameFromDiagonal(minMax[0], minMax[1], minMax[2], minMax[3]);
            } else {
                bds.add(minMax[0], minMax[1]);
                bds.add(minMax[2], minMax[3]);
            }
        } else {
            bds.setFrame(0, 0, 0, 0);
            for (Segment seg : segments) {
                seg.addToBounds(bds);
            }
        }
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        double[] minMax = new double[]{
            Double.MAX_VALUE, Double.MAX_VALUE,
            Double.MIN_VALUE, Double.MIN_VALUE
        };
        for (Segment seg : segments) {
            seg.minMax(minMax);
        }
        if (minMax[0] != Double.MAX_VALUE && minMax[1] != Double.MAX_VALUE) {
            dest.add(minMax[0], minMax[1]);
        }
        if (minMax[2] != Double.MIN_VALUE && minMax[3] != Double.MIN_VALUE) {
            dest.add(minMax[2], minMax[3]);
        }
    }

    public synchronized boolean movePathElement(int elementIndex, double offsetX, double offsetY) {
        if (elementIndex < 0 || elementIndex >= segments.length) {
            throw new IllegalArgumentException("Bad index " + elementIndex + " in " + segments.length);
        }
        Segment seg = segments[elementIndex];
        if (seg.data != null && seg.data.length > 0) {
            for (int i = 0; i < seg.data.length; i += 2) {
                seg.data[i] += offsetX;
                seg.data[i + 1] += offsetY;
            }
            change();
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
                change();
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
        if (cachedControlPointCount != -1) {
            return cachedControlPointCount;
        }
        int result = 0;
        for (Segment segment : segments) {
            int ct = segment.getPointCount();
            //            System.err.println(segments[i] + ": " + ct);
            result += ct;
        }
        return cachedControlPointCount = result;
    }

    @Override
    public synchronized void getControlPoints(double[] xy) {
        int ix = 0;
        for (Segment segment : segments) {
            ix += segment.copyData(xy, ix);
        }
    }

    public synchronized int[] getConcretePointIndices() {
        if (concreteControlPointIndices != null) {
            return concreteControlPointIndices;
        }
        IntList ints = null;
        int ct = 0;
        for (Segment seg : segments) {
            switch (seg.type) {
                case SEG_CUBICTO:
                    if (ints == null) {
                        ints = IntList.create(getControlPointCount());
                    }
                    ints.add(ct + 2);
                    break;
                case SEG_QUADTO:
                    if (ints == null) {
                        ints = IntList.create(getControlPointCount());
                    }
                    ints.add(ct + 1);
                    break;
                case SEG_LINETO:
                case SEG_MOVETO:
                    if (ints == null) {
                        ints = IntList.create(getControlPointCount());
                    }
                    ints.add(ct);
                    break;
                case SEG_CLOSE:
                    break;
                default:
                    throw new AssertionError(seg.type + " is not a segment type");
            }
            ct += seg.getPointCount();
        }
        return concreteControlPointIndices =  (ints == null ? EMPTY_INTS : ints.toIntArray());
    }

    private int[] virtualControlPointIndices = null;
    private int[] concreteControlPointIndices = null;
    private int cachedControlPointCount = -1;

    private void discardCachedVirtualControlPointIndices() {
        // We need this caching for extremely large point-count
        // paths
        synchronized (this) {
            virtualControlPointIndices = null;
            cachedControlPointCount = -1;
            concreteControlPointIndices = null;
        }
    }

    @Override
    public synchronized int[] getVirtualControlPointIndices() {
        if (virtualControlPointIndices != null) {
            return virtualControlPointIndices;
        }
        IntList ints = null;
        int ct = 0;
        for (Segment seg : segments) {
            switch (seg.type) {
                case SEG_CUBICTO:
                    if (ints == null) {
                        ints = IntList.create(segments.length - 2);
                    }
                    ints.add(ct);
                    ints.add(ct + 1);
                    break;
                case SEG_QUADTO:
                    if (ints == null) {
                        ints = IntList.create(segments.length - 2);
                    }
                    ints.add(ct);
                    break;
            }
            ct += seg.getPointCount();
        }
        return virtualControlPointIndices = (ints == null ? EMPTY_INTS : ints.toIntArray());
    }

    @Override
    public synchronized void setControlPointLocation(int pointIndex, Pt location) {
        setControlPointLocation(pointIndex, location.x, location.y);
    }

    @Override
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

    public synchronized void setControlPointLocation(int pointIndex, double x, double y) {
        if (pointIndex == 0) {
            if (segments.length > 0) {
                Segment seg = segments[0];
                int ix = seg.primaryPointIndexInternal();
                if (seg.data[ix] != x || seg.data[ix + 1] != y) {
                    seg.data[ix] = x;
                    seg.data[ix + 1] = y;
                    change();
                }
                return;
            } else {
                return;
            }
        }
        int cursor = 0;
        for (int i = 0; i < segments.length; i++) {
            Segment seg = segments[i];
            int currentPointCount = seg.getPointCount();
            if (currentPointCount == 0) {
                continue;
            }
            if (pointIndex == cursor) {
                if (seg.data[0] != x || seg.data[1] != y) {
                    seg.data[0] = x;
                    seg.data[1] = y;
                    change();
                }
                return;
            } else if (pointIndex > cursor && pointIndex < cursor + currentPointCount) {
                int offset = (pointIndex - cursor) * 2;
                if (seg.data[offset] != x || seg.data[offset + 1] != y) {
                    seg.data[offset] = x;
                    seg.data[offset + 1] = y;
                    change();
                    return;
                }
            }
            cursor += currentPointCount;
        }
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
        if (pointIndex < 0) {
            throw new IllegalArgumentException(
                    "Negative point index " + pointIndex);
        }
        int ct = getControlPointCount();
        if (pointIndex >= ct) {
            throw new IllegalArgumentException("Point index > count " + ct
                    + ": " + pointIndex);
        }
        return pointIndex;
    }

    public PathIteratorWrapper ensureClosed() {
        if (segments.length == 0) {
            return this;
        }
        if (segments[segments.length - 1].type != SEG_CLOSE) {
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

                Segment nue = new Segment(null, (byte) SEG_CLOSE);
                this.segments = Arrays.copyOf(this.segments, this.segments.length + 1);
                this.segments[this.segments.length - 1] = nue;
                change();
            }
        }
        return this;
    }

    public PathIteratorWrapper setToCurveTo(int pointIndex, double ctrlX1, double ctrlY1, double ctrlX2, double ctrlY2, double x, double y) {
        validatePointIndex(pointIndex);
        Segment nue = new Segment(new double[]{ctrlX1, ctrlY1, ctrlX2, ctrlY2, x, y}, (byte) SEG_CUBICTO);
        synchronized (this) {
            segments[pointIndex] = nue;
        }
        discardCachedVirtualControlPointIndices();
        change();
        return this;
    }

    public PathIteratorWrapper setToQuadTo(int pointIndex, double ctrlX, double ctrlY, double x, double y) {
        validatePointIndex(pointIndex);
        Segment nue = new Segment(new double[]{ctrlX, ctrlY, x, y}, (byte) SEG_QUADTO);
        synchronized (this) {
            segments[pointIndex] = nue;
        }
        discardCachedVirtualControlPointIndices();
        change();
        return this;
    }

    public PathIteratorWrapper setToLineTo(int pointIndex, double x, double y) {
        validatePointIndex(pointIndex);
        Segment nue = new Segment(new double[]{x, y}, (byte) SEG_LINETO);
        synchronized (this) {
            segments[pointIndex] = nue;
        }
        discardCachedVirtualControlPointIndices();
        change();
        return this;
    }

    public PathIteratorWrapper setToMoveTo(int pointIndex, double x, double y) {
        validatePointIndex(pointIndex);
        Segment nue = new Segment(new double[]{x, y}, (byte) SEG_MOVETO);
        synchronized (this) {
            segments[pointIndex] = nue;
        }
        discardCachedVirtualControlPointIndices();
        change();
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
            discardCachedVirtualControlPointIndices();
            change();
        }
        return result;
    }

    public synchronized boolean isExplicitlyClosed() {
        return segments.length > 0
                && segments[segments.length - 1].type == SEG_CLOSE;
    }

    public synchronized boolean isMultipleSegments() {
        int closeCount = 0;
        int lastCloseIndex = -1;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].type == SEG_CLOSE) {
                lastCloseIndex = i;
                closeCount++;
                if (closeCount > 1) {
                    return true;
                }
            }
        }
        return lastCloseIndex < segments.length - 1;
    }

    public synchronized PathIteratorWrapper addLineTo(double x, double y, boolean insertAtNearest) {
        return addLineToOrMoveTo(x, y, true, insertAtNearest);
    }

    public synchronized void resetControlPoints() {
        if (segments.length <= 2) {
            return;
        }
        int prevIndex = segments.length - 1;
        while (prevIndex > 0 && segments[prevIndex].type == SEG_CLOSE) {
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
            while (next.type == SEG_CLOSE && nextIndex != i && nextIndex != prevIndex) {
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
        discardCachedVirtualControlPointIndices();
        change();
    }

    private static double[] equidistantPoint(double x1, double y1, double x2, double y2) {
        return new double[]{(x1 + x2) / 2D, (y1 + y2) / 2D};
    }

    public synchronized PathIteratorWrapper addQuadTo(double ctrlX1, double ctrlY1, double x, double y, boolean nearest) {
        Segment seg = new Segment(new double[]{ctrlX1, ctrlY1, x, y}, (byte) SEG_QUADTO);
        appendOrInsert(seg, x, y, nearest);
        discardCachedVirtualControlPointIndices();
        return this;
    }

    public synchronized PathIteratorWrapper addCubicTo(double ctrlX1, double ctrlY1, double ctrlX2, double ctrlY2, double x, double y, boolean nearest) {
        Segment seg = new Segment(new double[]{ctrlX1, ctrlY1, ctrlX2, ctrlY2, x, y}, (byte) SEG_CUBICTO);
        appendOrInsert(seg, x, y, nearest);
        discardCachedVirtualControlPointIndices();
        return this;
    }

    private synchronized PathIteratorWrapper addLineToOrMoveTo(double x, double y, boolean line, boolean nearest) {
        Segment seg = new Segment(new double[]{x, y}, (byte) (line ? SEG_LINETO : SEG_MOVETO));
        appendOrInsert(seg, x, y, nearest);
        discardCachedVirtualControlPointIndices();
        return this;
    }

    private synchronized PathIteratorWrapper appendOrInsert(Segment seg, double nearX, double nearY, boolean nearest) {
        Segment[] segs = Arrays.copyOf(segments, segments.length + 1);
        int targetIx;
        if (nearest && segments.length > 1) {
            targetIx = getElementWithPrimaryPointNearest(nearX, nearY);
            Segment target = segments[targetIx];
            if (target.type != SEG_MOVETO) {
                int prev = targetIx - 1;
                if (prev < 0) {
                    prev = segments.length - 1;
                }
                Segment preceding = segments[prev];
                while (prev > 0 && preceding.type == SEG_CLOSE) {
                    prev--;
                    preceding = segments[prev];
                }
                int next = targetIx + 1;
                if (next == segments.length) {
                    next = 0;
                }
                Segment following = segments[next];
                if (following.type == SEG_CLOSE) {
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
            discardCachedVirtualControlPointIndices();
            change();
        }
        return this;
    }

    @Override
    public boolean insert(double x, double y, int index, int kind) {
        double[] data;
        switch (kind) {
            case SEG_MOVETO:
            case SEG_LINETO:
                data = new double[]{x, y};
                break;
            case SEG_QUADTO:
                data = new double[]{x - 10, y - 10, x, y};
                break;
            case SEG_CUBICTO:
                data = new double[]{x - 10, y, x, y + 10, x, y};
                break;
            case SEG_CLOSE:
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
        change();
        discardCachedVirtualControlPointIndices();
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
                case SEG_CUBICTO:
                case SEG_QUADTO:
                    return true;
            }
        }
        return false;
    }

    public synchronized boolean toCubicSplines() {
        boolean result = false;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].type == SEG_LINETO || segments[i].type == SEG_QUADTO) {
                double[] pt = segments[i].primaryPoint();
                double[] nue = new double[6];
                System.arraycopy(pt, 0, nue, 0, 2);
                System.arraycopy(pt, 0, nue, 2, 2);
                System.arraycopy(pt, 0, nue, 4, 2);
                segments[i] = new Segment(nue, (byte) SEG_CUBICTO);
                result = true;
            }
        }
        if (result) {
            discardCachedVirtualControlPointIndices();
            change();
        }
        return result;
    }

    public synchronized boolean toQuadSplines() {
        boolean result = false;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].type == SEG_LINETO || segments[i].type == SEG_CUBICTO) {
                double[] pt = segments[i].primaryPoint();
                double[] nue = new double[4];
                System.arraycopy(pt, 0, nue, 0, 2);
                System.arraycopy(pt, 0, nue, 2, 2);
                segments[i] = new Segment(nue, (byte) SEG_QUADTO);
                result = true;
            }
        }
        if (result) {
            discardCachedVirtualControlPointIndices();
            change();
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

    public int simplify() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        double diag = Point2D.distance(bds.x, bds.y, bds.x + bds.width, bds.y + bds.height);
        return simplify(diag * 0.05);
    }

    public int simplify(double tolerance) {
        if (segments.length < 3) {
            return 0;
        }
        Path2D.Double result = new Path2D.Double();
        segments[0].applyTo(result);
        int lastType = segments[0].type;
        double lastX = segments[0].physicalX();
        double lastY = segments[0].physicalY();
        int elided = 0;
        for (int i = 1; i < segments.length; i++) {
            Segment s = segments[i];
            if (lastType == s.type) {
                double lx = s.physicalX();
                double ly = s.physicalY();
                if (Point2D.distance(lastX, lastY, lx, ly) <= tolerance) {
                    elided++;
                } else {
                    s.applyTo(result);
                }
            } else {
                s.applyTo(result);
            }
            lastType = s.type;
        }
        if (elided > 0) {
            unpack(result.getPathIterator(null));
            discardCachedVirtualControlPointIndices();
            change();
        }
        return elided;
    }

    @Override
    public java.awt.Rectangle getBounds() {
        java.awt.Rectangle r = new java.awt.Rectangle();
        for (Segment seg : segments) {
            seg.addToBounds(r);
        }
        return r;
    }

    public PathIterator getPathIterator() {
        return new PI();
    }

    public PathIterator getPathIterator(AffineTransform x) {
        if (x == null) {
            return getPathIterator();
        }
        return new XPI(x);
    }

    class PI implements PathIterator {

        private int cursor = 0;

        @Override
        public int getWindingRule() {
            return PathIterator.WIND_NON_ZERO;
        }

        @Override
        public boolean isDone() {
            return cursor >= segments.length;
        }

        @Override
        public void next() {
            cursor++;
        }

        @Override
        public int currentSegment(float[] coords) {
            int result = segments[cursor].type;
            if (result != SEG_CLOSE) {
                segments[cursor].get(coords);
            }
            return result;
        }

        @Override
        public int currentSegment(double[] coords) {
            int result = segments[cursor].type;
            if (result != SEG_CLOSE) {
                segments[cursor].get(coords);
            }
            return result;
        }
    }

    class XPI implements PathIterator {

        private int cursor = 0;
        private final AffineTransform xform;

        public XPI(AffineTransform xform) {
            this.xform = xform;
        }

        @Override
        public int getWindingRule() {
            return PathIterator.WIND_NON_ZERO;
        }

        @Override
        public boolean isDone() {
            return cursor >= segments.length;
        }

        @Override
        public void next() {
            cursor++;
        }

        @Override
        public int currentSegment(float[] coords) {
            int result = segments[cursor].type;
            if (result != SEG_CLOSE) {
                segments[cursor].get(coords);
                xform.transform(coords, 0, coords, 0, segments[cursor].getPointCount());
            }
            return result;
        }

        @Override
        public int currentSegment(double[] coords) {
            int result = segments[cursor].type;
            if (result != SEG_CLOSE) {
                segments[cursor].get(coords);
            }
            return result;
        }
    }

}
