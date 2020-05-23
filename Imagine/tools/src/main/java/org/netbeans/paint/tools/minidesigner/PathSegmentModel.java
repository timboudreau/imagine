package org.netbeans.paint.tools.minidesigner;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.QuadConsumer;
import com.mastfrog.function.state.Int;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.swing.event.ChangeListener;
import org.imagine.geometry.Angle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.path.PathElementKind;
import org.imagine.geometry.path.PointKind;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
public class PathSegmentModel implements Iterable<SegmentEntry> {

    private final List<SegmentEntry> entries = new ArrayList<>();
    private ChangeSupport supp;
    private static final byte MAGIC = 0x3F;
    static final byte IO_REV = 1;

    public PathSegmentModel copy() {
        PathSegmentModel mdl = new PathSegmentModel();
        for (SegmentEntry e : entries) {
            mdl.entries.add(e.copy());
        }
        return mdl;
    }

    void scaleY(double d) {
        for (SegmentEntry e : entries) {
            e.scaleY(d);
        }
        fire();
    }

    public int sizeInBytes() {
        int result = 2 + Integer.BYTES;
        for (SegmentEntry entry : entries) {
            result += entry.sizeInBytes();
        }
        return result;
    }

    private double max() {
        if (entries.isEmpty()) {
            return 0;
        }
        double max = Double.MIN_VALUE;
        for (SegmentEntry se : entries) {
            max = Math.max(max, se.maximumDistance());
        }
        return max;
    }

    private double lastDistance() {
        if (entries.isEmpty()) {
            return 0;
        }
        double max = Double.MIN_VALUE;
        for (SegmentEntry se : entries) {
            max = Math.max(max, se.lastDistance());
        }
        return max;
    }


    private double min() {
        if (entries.isEmpty()) {
            return 0;
        }
        double min = Double.MAX_VALUE;
        for (SegmentEntry se : entries) {
            min = Math.min(min, se.maximumDistance());
        }
        if (min > 0) {
            return 0;
        }
        return min;
    }

    public void mirror() {
        double max = lastDistance();
        List<SegmentEntry> nue = new ArrayList<>();
        for (SegmentEntry se : entries) {
            SegmentEntry entry = se.mirror(max);
            nue.add(entry);
            max = entry.lastDistance();
        }
        entries.addAll(nue);
        fire();
    }

    public void renormalize() {
        double max = max();
        double min = min();
        double range = max - min;
        double offset = -min;
        double mul = 1D / range;
        if (mul == Double.POSITIVE_INFINITY || mul == Double.NEGATIVE_INFINITY) {
            // if we proceed, we will reliably trigger a segment violation in
            // anglesModulus in libdcpr
            mul = 1;
        }
        for (SegmentEntry se : entries) {
            se.renormalize(offset, mul);
        }
        fire();
    }

    public static PathSegmentModel read(ByteBuffer buf) throws IOException {
        byte magic = buf.get();
        if (magic != MAGIC) {
            throw new IOException("Wrong magic number " + magic + " expected " + MAGIC);
        }
        byte rev = buf.get();
        if (rev != IO_REV) {
            throw new IOException("Wrong format version " + rev + " expected " + IO_REV);
        }
        int count = buf.getInt();
        if (count == 0) {
            return new PathSegmentModel();
        } else if (count < 0) {
            throw new IOException("Negative size " + count);
        }
        PathSegmentModel result = new PathSegmentModel();
        for (int i = 0; i < count; i++) {
            result.entries.add(SegmentEntry.read(buf, rev));
        }
        return result;
    }

    public ByteBuffer write() {
        ByteBuffer buf = ByteBuffer.allocate(sizeInBytes());
        write(buf);
        return buf;
    }

    public void write(ByteBuffer buf) {
        buf.put(MAGIC);
        buf.put(IO_REV);
        buf.putInt(entries.size());
        for (SegmentEntry e : entries) {
            e.writeTo(buf);
        }
    }

    public void replaceFrom(PathSegmentModel mdl) {
        entries.clear();
        for (SegmentEntry e : mdl) {
            entries.add(e.copy());
        }
        fire();
    }

    public void addChangeListener(ChangeListener cl) {
        if (supp == null) {
            supp = new ChangeSupport(this);
        }
        supp.addChangeListener(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        if (supp == null) {
            return;
        }
        supp.removeChangeListener(cl);
        if (!supp.hasListeners()) {
            supp = null;
        }
    }

    private void fire() {
        if (supp != null) {
            supp.fireChange();
        }
    }

    public void visitPoints(SegmentEntry of, Supplier<EqLine> supp, BiConsumer<PointKind, Point2D> c) {
        of.visitPoints(this::fire, supp, c);
    }

    public void visitPoints(Supplier<EqLine> supp, QuadConsumer<SegmentEntry, Integer, PointKind, Point2D> c) {
        Int index = Int.create();
        for (SegmentEntry se : this) {
            se.visitPoints(this::fire, supp, (pk, pt) -> {
                c.accept(se, index.get(), pk, pt);
            });
            index.increment();
        }
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public SegmentEntry insertCubicBefore(Point2D start, Point2D end, SegmentEntry other, double x, double y) {
        if (isEmpty() || other == null) {
            return addCubicTo(start, end, x, y, x, y, x, y);
        }
        int ix = entries.indexOf(other);
        if (ix < 0) {
            return addCubicTo(start, end, x, y, x, y, x, y);
        }
        SegmentEntry entry = cubicEntry(start, end, x, y, x, y, x, y);
        entries.add(ix, entry);
        return entry;
    }

    public SegmentEntry insertCubicAfter(Point2D start, Point2D end, SegmentEntry other, double x, double y) {
        if (isEmpty() || other == null) {
            return addCubicTo(start, end, x, y, x, y, x, y);
        }
        int ix = entries.indexOf(other);
        if (ix < 0 || ix == entries.size() - 1) {
            return addCubicTo(start, end, x, y, x, y, x, y);
        }
        SegmentEntry entry = cubicEntry(start, end, x, y, x, y, x, y);
        entries.add(ix + 1, entry);
        return entry;
    }

    private SegmentEntry cubicEntry(Point2D start, Point2D end,
            double ctrlX1, double ctrlY1, double ctrlX2, double ctrlY2,
            double destX, double destY) {
        double[] das = new double[6];
        relativePoint(start, end, ctrlX1, ctrlY1, (distPct, relAng) -> {
            das[0] = distPct;
            das[1] = relAng;
        });
        relativePoint(start, end, ctrlX2, ctrlY2, (distPct, relAng) -> {
            das[2] = distPct;
            das[3] = relAng;
        });
        relativePoint(start, end, destX, destY, (distPct, relAng) -> {
            das[4] = distPct;
            das[5] = relAng;
        });
        SegmentEntry entry = new SegmentEntry(PathElementKind.CUBIC, das);
        return entry;
    }

    public SegmentEntry addCubicTo(Point2D start, Point2D end,
            double ctrlX1, double ctrlY1, double ctrlX2, double ctrlY2,
            double destX, double destY) {
        SegmentEntry entry = cubicEntry(start, end, ctrlX1, ctrlY1, ctrlX2, ctrlY2, destX, destY);
        entries.add(entry);
        return entry;
    }

    public SegmentEntry addQuadTo(Point2D start, Point2D end,
            double ctrlX, double ctrlY,
            double destX, double destY) {
        double[] das = new double[4];
        relativePoint(start, end, ctrlX, ctrlY, (distPct, relAng) -> {
            das[0] = distPct;
            das[1] = relAng;
        });
        relativePoint(start, end, destX, destY, (distPct, relAng) -> {
            das[2] = distPct;
            das[3] = relAng;
        });
        SegmentEntry entry = new SegmentEntry(PathElementKind.QUADRATIC, das);
        entries.add(entry);
        return entry;
    }

    public SegmentEntry addLineTo(Point2D start, Point2D end,
            double destX, double destY) {
        double[] das = new double[2];
        relativePoint(start, end, destX, destY, (distPct, relAng) -> {
            das[0] = distPct;
            das[1] = relAng;
        });
        SegmentEntry entry = new SegmentEntry(PathElementKind.LINE, das);
        entries.add(entry);
        return entry;
    }

    public SegmentEntry addMoveTo(Point2D start, Point2D end,
            double destX, double destY) {
        double[] das = new double[2];
        relativePoint(start, end, destX, destY, (distPct, relAng) -> {
            das[0] = distPct;
            das[1] = relAng;
        });
        SegmentEntry entry = new SegmentEntry(PathElementKind.MOVE, das);
        entries.add(entry);
        return entry;
    }

    public static void relativePoint(Point2D start, Point2D end, double x, double y, DoubleBiConsumer distanceAndAngleConsumer) {
        EqLine ln = new EqLine(start, end);
        double length = ln.length();
        double ang = ln.angle();
        double percentageDistance = start.distance(x, y) / length;
//        System.out.println("Dist pct " + percentageDistance + " for " + new EqLine(start, new EqPointDouble(x, y))
//            + " over " + length + " for baseline " + ln);
        EqLine ln2 = new EqLine(start, new EqPointDouble(x, y));
        double angleOfPoint = ln2.angle();
        double relativeAngle = Angle.subtractAngles(angleOfPoint, ang);
        distanceAndAngleConsumer.accept(percentageDistance, relativeAngle);
    }

    public void clear() {
        entries.clear();
        fire();
    }

    public int size() {
        return entries.size();
    }

    public double maxDestinationScale() {
        double max = Double.MIN_VALUE;
        for (SegmentEntry e : entries) {
            max = Math.max(max, e.destinationScale());
        }
        if (max == Double.MIN_VALUE) {
            return 1;
        }
        return max;
    }

    public Point2D apply(Path2D.Double path, Point2D start, Point2D end, double scale) {
        return apply(path, start, end, true, scale);
    }

    public Point2D apply(Path2D.Double path, Point2D start, Point2D end) {
        return apply(path, start, end, true, 1D);
    }

    public Point2D apply(Path2D.Double path, Point2D start, Point2D end, boolean empty) {
        return apply(path, start, end, empty, 1D);
    }

    public Point2D apply(Path2D.Double path, Point2D start, Point2D end, boolean empty, double scale) {
        if (empty) {
            path.moveTo(start.getX(), start.getY());
        }
        if (entries.isEmpty()) {
            path.lineTo(end.getX(), end.getY());
            return end;
        }
        Point2D result = start;
        for (SegmentEntry e : entries) {
            result = e.apply(path, start, end, scale);
        }
        return result;
    }

    @Override
    public Iterator<SegmentEntry> iterator() {
        return entries.iterator();
    }

    public void delete(SegmentEntry entry) {
        entries.remove(entry);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.entries);
        hash = 83 * hash + Objects.hashCode(this.supp);
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
        final PathSegmentModel other = (PathSegmentModel) obj;
        if (!Objects.equals(this.entries, other.entries)) {
            return false;
        }
        if (!Objects.equals(this.supp, other.supp)) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("PSM(").append(entries.size()).append(':');
        for (SegmentEntry entry : entries) {
            sb.append("\n\t").append(entry);
        }
        return sb.append(')').toString();
    }
}
