package org.netbeans.paint.tools.path;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.state.Dbl;
import com.mastfrog.function.state.Obj;
import com.mastfrog.util.collections.CollectionUtils;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.path.PathElement;
import org.imagine.geometry.path.PathElementKind;

/**
 *
 * @author Tim Boudreau
 */
class PathModel implements Iterable<Pt> {

    private Shape cachedShape;
    final List<Entry> entries = new ArrayList<>(32);
    private DoubleBiConsumer onChange;

    Pt hit(double x, double y, double radius) {
        Obj<Pt> result = Obj.create();
        Dbl bestDistance = Dbl.of(Double.MAX_VALUE);
        for (Entry e : entries) {
            for (Pt pt : e.iterable()) {
                pt.ifHit(x, y, radius, (point, dist) -> {
                    if (bestDistance.min(dist) > dist) {
                        result.set(pt);
                    }
                });
            }
        }
        return result.get();
    }

    Pt firstPoint() {
        if (entries.isEmpty()) {
            return null;
        }
        return entries.get(0).pointsIterator().next();
    }

    Pt lastPoint() {
        if (entries.isEmpty()) {
            return null;
        }
        return entries.get(entries.size() - 1).destination();
    }

    void collectBounds(EnhRectangle2D bds) {
        bds.clear();
        for (Entry e : entries) {
            e.addToBounds(bds);
        }
    }

    boolean removeLast() {
        if (!entries.isEmpty()) {
            entries.remove(entries.size() - 1);
            return !entries.isEmpty();
        }
        return false;
    }

    void visitPoints(Consumer<Pt> c) {
        entries.forEach(e -> e.iterable().forEach(c));
    }

    @Override
    public Iterator<Pt> iterator() {
        List<Iterator<Pt>> all = new ArrayList<>();
        for (Entry e : entries) {
            all.add(e.pointsIterator());
        }
        return CollectionUtils.combine(all);
    }

    public int size() {
        return entries.size();
    }

    public Shape toShape() {
        if (cachedShape != null) {
            return cachedShape;
        }
        Path2D.Double path = new Path2D.Double();
        for (Entry e : entries) {
            e.applyTo(path);
        }
        return cachedShape = path;
    }

    PathModel onChange(DoubleBiConsumer onChange) {
        this.onChange = onChange;
        return this;
    }

    public Entry add(PathElementKind kind, double x, double y) {
        if (entries.isEmpty() && kind != PathElementKind.MOVE) {
            return add(PathElementKind.MOVE, x, y);
        }
        double[] arr = new double[kind.arraySize()];
        for (int i = 0; i < arr.length; i += 2) {
            arr[i] = x;
            arr[i + 1] = y;
        }
        Entry result = new Entry(kind, arr);
        entries.add(result);
        onChange(x, y);
        return result;
    }

    void onChange(double x, double y) {
        cachedShape = null;
        if (onChange != null) {
            onChange.accept(x, y);
        }
    }

    void delete(Entry se) {
        entries.remove(se);
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    class Entry implements PathElement {

        final PathElementKind kind;
        final double[] points;

        public Entry(PathElementKind kind, double[] points) {
            switch (kind) {
                case CLOSE:
                    break;
                default:
                    if (kind.arraySize() != points.length) {
                        throw new IllegalArgumentException("Wrong size: " + points.length + " for " + kind);
                    }
            }
            this.kind = kind;
            this.points = points;
        }

        void addToBounds(EnhRectangle2D rect) {
            if (kind == PathElementKind.CLOSE) {
                return;
            }
            for (int i = 0; i < points.length; i += 2) {
                if (rect.isEmpty()) {
                    rect.x = points[i];
                    rect.y = points[i + 1];
                    rect.width = 1;
                    rect.height = 1;
                } else {
                    rect.add(points[i], points[i + 1]);
                }
            }
        }

        Iterator<Pt> secondaryPointsIterator() {
            switch (kind) {
                case CLOSE:
                case LINE:
                case MOVE:
                    return Collections.emptyIterator();
                case CUBIC:
                    return Arrays.asList(new Pt(0, this), new Pt(1, this)).iterator();
                case QUADRATIC:
                    return CollectionUtils.singletonIterator(new Pt(0, this));
                default:
                    throw new AssertionError(kind);
            }
        }

        Pt destination() {
            return new Pt(kind.pointCount() - 1, this);
        }

        Iterable<Pt> iterable() {
            return () -> {
                return pointsIterator();
            };
        }

        Iterator<Pt> pointsIterator() {
            return new Iterator<Pt>() {
                int ix = -1;

                @Override
                public boolean hasNext() {
                    return ix + 1 < kind.pointCount();
                }

                @Override
                public Pt next() {
                    return new Pt(++ix, Entry.this);
                }
            };
        }

        @Override
        public int type() {
            return kind.intValue();
        }

        @Override
        public double[] points() {
            return points;
        }

        @Override
        public PathElementKind kind() {
            return kind;
        }

        PathModel model() {
            return PathModel.this;
        }

        int index() {
            return PathModel.this.entries.indexOf(this);
        }
    }

}
