/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.minidesigner;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.state.Dbl;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.path.PathElementKind;
import org.imagine.geometry.path.PointKind;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.PooledTransform;

/**
 *
 * @author Tim Boudreau
 */
public class SegmentEntry {

    private final PathElementKind kind;
    private final double[] distancesAndAngles;

    public SegmentEntry(PathElementKind kind, double[] distancesAndAngles) {
        this.kind = kind;
        this.distancesAndAngles = distancesAndAngles;
        if (distancesAndAngles.length != kind.arraySize()) {
            throw new IllegalArgumentException("Wrong array size " + distancesAndAngles.length + " for " + kind);
        }
        assert distancesAndAngles.length == kind.arraySize();
    }

    double destinationScale() {
        return distancesAndAngles[distancesAndAngles.length - 2];
    }

    public int sizeInBytes() {
        return (distancesAndAngles.length * Double.BYTES) + Integer.BYTES;
    }

    public void rotate(double by) {
        for (int i = 0; i < distancesAndAngles.length; i += 2) {
            distancesAndAngles[i + 1] = Angle.addAngles(distancesAndAngles[i], by);
        }
    }

    public void renormalize(double offset, double by) {
        for (int i = 0; i < distancesAndAngles.length; i += 2) {
            distancesAndAngles[i] += offset;
            distancesAndAngles[i] *= by;
        }
    }

    public double maximumDistance() {
        double max = Double.MIN_VALUE;
        for (int i = 0; i < distancesAndAngles.length; i += 2) {
            max = Math.max(distancesAndAngles[i], max);
        }
        return max;
    }

    public double minimumDistance() {
        double max = Double.MAX_VALUE;

        for (int i = 0; i < distancesAndAngles.length; i += 2) {
            max = Math.min(distancesAndAngles[i], max);
        }
        return max;
    }

    double lastDistance() {
        return distancesAndAngles[distancesAndAngles.length - 2];
    }

    double range() {
        return maximumDistance() - minimumDistance();
    }

    public void scaleY(double d) {
        EqPointDouble start = new EqPointDouble(0, 0);
        EqPointDouble end = new EqPointDouble(1, 0);
        Dbl minX = Dbl.of(Double.MAX_VALUE);
        Dbl maxX = Dbl.of(Double.MIN_VALUE);
        double[] points = new double[distancesAndAngles.length];
        iterate(kind.pointCount(), i -> {
            int offset = i * 2;
            apply(i, start, end, 1, (x, y) -> {
                points[offset] = x;
                points[offset + 1] = y;
                minX.min(x);
                maxX.max(x);
            });
        });
        PooledTransform.withScaleInstance(1, d, xf -> {
            xf.transform(points, 0, points, 0, kind.pointCount());
        });
        iterate(kind.pointCount(), i -> {
            int offset = i * 2;
            PathSegmentModel.relativePoint(start, end,
                    points[offset], points[offset + 1], (dist, a) -> {
                        distancesAndAngles[offset] = dist;
                        distancesAndAngles[offset + 1] = a;
                    });
        });
    }

    public SegmentEntry mirror(double newStart) {
        double[] nue = Arrays.copyOf(distancesAndAngles, distancesAndAngles.length);
        EqPointDouble start = new EqPointDouble(0, 0);
        EqPointDouble end = new EqPointDouble(1, 0);
        Dbl minX = Dbl.of(Double.MAX_VALUE);
        Dbl maxX = Dbl.of(Double.MIN_VALUE);
        double[] points = new double[nue.length];
        iterate(kind.pointCount(), i -> {
            int offset = i * 2;
            apply(i, start, end, 1, (x, y) -> {
                points[offset] = x;
                points[offset + 1] = y;
                minX.min(x);
                maxX.max(x);
            });
        });
        PooledTransform.withScaleInstance(-1, 1, xf -> {
            xf.transform(points, 0, points, 0, kind.pointCount());
        });
        double newMin = newStart - minX.getAsDouble();
        iterate(kind.pointCount(), i -> {
            int offset = i * 2;
            int revOffset = distancesAndAngles.length - ((i + 1) * 2);
            // because of the transform, points to the left are to the right,
            // so we need to subtract
            double newX = newMin - points[offset];
            PathSegmentModel.relativePoint(start, end,
                    newX, points[offset + 1], (d, a) -> {
                        nue[revOffset] = d;
                        nue[revOffset + 1] = a;
                    });
        });
        switch (kind) {
            // dest, ctrl2, ctrl1
            case CUBIC:
                swapPoints(0, 2, nue);
                // to ctrl1, ctrl2, dest
                break;
            case QUADRATIC:
                swapPoints(0, 1, nue);
                break;
        }
        return new SegmentEntry(kind, nue);
    }

    private static void swapPoints(int pt1, int pt2, double[] arr) {
        int off1 = pt1 * 2;
        int off2 = pt2 * 2;
        double holdX = arr[off1];
        double holdY = arr[off1 + 1];
        arr[off1] = arr[off2];
        arr[off1 + 1] = arr[off2 + 1];
        arr[off2] = holdX;
        arr[off2 + 1] = holdY;
    }

    public void writeTo(ByteBuffer buf) {
        assert buf.remaining() >= sizeInBytes();
        buf.putInt(kind.ordinal());
        for (int i = 0; i < distancesAndAngles.length; i++) {
            buf.putDouble(distancesAndAngles[i]);
        }
    }

    public static SegmentEntry read(ByteBuffer buf) throws IOException {
        return read(buf, PathSegmentModel.IO_REV);
    }

    public static SegmentEntry read(ByteBuffer buf, byte ioRev) throws IOException {
        int kindOrdinal = buf.getInt();
        PathElementKind[] allKinds = PathElementKind.values();
        if (kindOrdinal < 0 || kindOrdinal >= allKinds.length) {
            throw new IOException("Invalid PathElement kind not between 0-" + (allKinds.length - 1)
                    + ": " + kindOrdinal);
        }
        PathElementKind kind = allKinds[kindOrdinal];
        int arraySize = kind.arraySize();
        double[] arr = new double[arraySize];
        for (int i = 0; i < arraySize; i++) {
            arr[i] = buf.getDouble();
        }
        return new SegmentEntry(kind, arr);
    }

    public SegmentEntry copy() {
        return new SegmentEntry(kind, (double[]) distancesAndAngles.clone());
    }

    private static void iterate(int end, IntConsumer c) {
        assert end > 0;
        iterate(0, end, c);
    }

    private static void iterate(int start, int end, IntConsumer c) {
        for (int i = start; i < end; i++) {
            c.accept(i);
        }
    }

    public EqPointDouble destination(EqLine startEnd) {
        return point((distancesAndAngles.length / 2) - 1, startEnd.startPoint(), startEnd.endPoint());
    }

    void set(int pointIndex, Point2D start, Point2D end, double newX, double newY) {
        PathSegmentModel.relativePoint(start, end, newX, newY, (distPct, relAngle) -> {
            int offset = pointIndex * 2;
            distancesAndAngles[offset] = distPct;
            distancesAndAngles[offset + 1] = relAngle;
        });
    }

    public Point2D apply(Path2D path, Point2D start, Point2D end) {
        return apply(path, start, end, 1);
    }

    public Point2D apply(Path2D path, Point2D start, Point2D end, double scale) {
        double[] coords = new double[distancesAndAngles.length];
        iterate(coords.length / 2, ix -> {
            int offset = ix * 2;
            apply(ix, start, end, scale, (x, y) -> {
                coords[offset] = x;
                coords[offset + 1] = y;
            });
        });
        kind.apply(coords, path);
        return new EqPointDouble(coords[coords.length - 2], coords[coords.length - 1]);
    }

    private void apply(int pointIndex, Point2D start, Point2D end, double scale, DoubleBiConsumer c) {
        int offset = pointIndex * 2;
        if (offset >= distancesAndAngles.length) {
            throw new IllegalArgumentException("Point index " + pointIndex + " for " + kind
                    + " results in an array offset of " + offset + " which is > " + distancesAndAngles.length);
        }
        double distancePercentage = distancesAndAngles[offset] * scale;
        double angle = distancesAndAngles[offset + 1];

        double startEndDistance = start.distance(end);
        double startEndAngle = Angle.ofLine(start.getX(), start.getY(), end.getX(), end.getY());

        double targetDistance = distancePercentage * startEndDistance;
        double targetAngle = startEndAngle + angle;

        Circle.positionOf(targetAngle, start.getX(), start.getY(), targetDistance, c);
    }

    private EqPointDouble point(int pointIndex, Point2D start, Point2D end) {
        EqPointDouble result = new EqPointDouble();
        apply(pointIndex, start, end, 1, result::setLocation);
        return result;
    }

    void visitPoints(Runnable onChange, Supplier<EqLine> startEnd, BiConsumer<PointKind, Point2D> c) {
        PointKind[] kinds = kind.pointKinds();
        for (int i = 0; i < distancesAndAngles.length; i += 2) {
            IP ip = new IP(startEnd, i / 2, onChange);
            c.accept(kinds[i / 2], ip);
        }
    }

    class IP extends Point2D {

        private final Supplier<EqLine> startEndSupplier;
        private final int ix;
        private Runnable onChange;

        public IP(Supplier<EqLine> startEndSupplier, int ix) {
            this(startEndSupplier, ix, null);
        }

        public IP(Supplier<EqLine> startEndSupplier, int ix, Runnable onChange) {
            this.startEndSupplier = startEndSupplier;
            this.ix = ix;
            this.onChange = onChange;
        }
        
        SegmentEntry owner() {
            return SegmentEntry.this;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof IP && ((IP) o).ix == ix
                    && ((IP) o).owner() == owner();
        }

        @Override
        public int hashCode() {
            return 73 * ix;
        }

        @Override
        public double getX() {
            EqLine line = startEndSupplier.get();
            return point(ix, line.startPoint(), line.endPoint()).x;
        }

        @Override
        public double getY() {
            EqLine line = startEndSupplier.get();
            return point(ix, line.startPoint(), line.endPoint()).y;
        }

        @Override
        public void setLocation(double x, double y) {
            EqLine line = startEndSupplier.get();
            SegmentEntry.this.set(ix, line.startPoint(), line.endPoint(), x, y);
            if (onChange != null) {
                onChange.run();
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.kind);
        hash = 29 * hash + Arrays.hashCode(this.distancesAndAngles);
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
        final SegmentEntry other = (SegmentEntry) obj;
        if (this.kind != other.kind) {
            return false;
        }
        if (!Arrays.equals(this.distancesAndAngles, other.distancesAndAngles)) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Seg(").append(kind.name()).append(' ');
        for (int i = 0; i < distancesAndAngles.length; i += 2) {
            String dist = "~" + GeometryStrings.toShortString(distancesAndAngles[i] * 100) + "% at ";
            String ang = GeometryStrings.toDegreesStringShort(distancesAndAngles[i + 1]);
            sb.append(dist).append(ang);
            if (i != distancesAndAngles.length - 2) {
                sb.append(" / ");
            }
        }
        return sb.toString();
    }
}
