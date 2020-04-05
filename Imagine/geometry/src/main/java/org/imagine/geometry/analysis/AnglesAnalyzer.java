package org.imagine.geometry.analysis;

import com.mastfrog.function.DoubleBiPredicate;
import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntIntMap;
import com.mastfrog.util.collections.IntMap;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import static org.imagine.geometry.RotationDirection.CLOCKWISE;
import static org.imagine.geometry.RotationDirection.COUNTER_CLOCKWISE;
import org.imagine.geometry.util.DoubleList;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;
import com.mastfrog.function.state.Bool;
import com.mastfrog.function.state.Int;
import com.mastfrog.function.state.IntWithChildren;
import org.imagine.geometry.CornerAngle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.Intersectable;
import org.imagine.geometry.LineVector;
import org.imagine.geometry.Polygon2D;
import org.imagine.geometry.RotationDirection;

/**
 *
 * @author Tim Boudreau
 */
final class AnglesAnalyzer {

    private List<Collector> collectors = new ArrayList<>(5);
    private Collector currentCollector;

    AnglesAnalyzer() {
        collectors.add(currentCollector = new Collector());
    }

    public RotationDirection analyzeShape(Shape shape, AffineTransform xform) {
        return forShape(shape.getPathIterator(xform));
    }

    public RotationDirection analyze(PathIterator iter) {
        return forShape(iter);
    }

    public void visitAll(VectorVisitor v) {
        for (int i = 0; i < collectors.size(); i++) {
            Collector c = collectors.get(i);
            if (!c.isEmpty()) {
                c.visitAll(i, v);
            }
        }
    }

    private Collector nextCollector() {
        Collector last = collectors.get(collectors.size() - 1);
        if (last.isEmpty()) {
            return last;
        }
        Collector result = new Collector();
        collectors.add(result);
        return currentCollector = result;
    }

    private RotationDirection forShape(PathIterator iter) {
        double[] data = new double[6];
        Collector collector = nextCollector();
        int lastType = -1;
        double startX, startY, lastX, lastY, secondX, secondY;
        startX = lastX = lastY = startY = secondX = secondY = 0;
        // Holder for the point index which can be incremented inside leading
        // lambda
        IntWithChildren pointIndex = Int.createWithChildren();
        // Holder for the point index within the current shape, which can
        // be reset independently but will be incremented when pointIndex is
        Int pointIndexWithinShape = pointIndex.child();

        Bool hasSecondPoint = Bool.create();

        Runnable onNewSubshape = () -> {
            pointIndexWithinShape.reset();
            currentCollector.reset();
            hasSecondPoint.reset();
        };

        int shapeStart = -1;
        // XXX to do this right, we need to duplicate the
        // intersection counting code in the com.sun geom Java2D
        // package
        while (!iter.isDone()) {
            int type = iter.currentSegment(data);
            switch (type) {
                case SEG_CLOSE:
                    if (lastType != SEG_CLOSE && lastType != SEG_MOVETO) {
                        int pix = shapeStart;
                        // Handle the trailing series of lines where the
                        // last point of this sub-path is the apex
                        if (lastX != startX || lastY != startY) {
                            collector.accept(pointIndex.getLess(1), startX, startY, false);
                            // And handle the trailing series of lines where the
                            // 0th point of this sub-path is the apex - that
                            // provides leading corner we will have skipped because
                            // we only had two points when we initially iterated
                            // past it
                            if (hasSecondPoint.getAsBoolean()) {
                                collector.accept(shapeStart, secondX, secondY, false);
                            }
                        } else if (lastX == startX && lastX == startY) {
                            if (hasSecondPoint.getAsBoolean()) {
                                collector.prevX = collector.lastX;
                                collector.prevY = collector.lastY;
                                collector.lastX = startX;
                                collector.lastY = startY;
                                collector.accept(pix + 1, secondX, secondY);
                            }
                        }
                    }
                    onNewSubshape.run();
                    break;
                case SEG_MOVETO:
                    shapeStart = pointIndex.get();
                    onNewSubshape.run();
                    collector = nextCollector();
                    collector.accept(pointIndex.getLess(1), startX = lastX = data[0], startY = lastY = data[1]);
                    pointIndex.increment();
                    break;
                // fallthrough
                case SEG_LINETO:
                    collector.accept(pointIndex.getLess(1), lastX = data[0], lastY = data[1]);
                    if (pointIndexWithinShape.equals(1)) {
                        secondX = data[0];
                        secondY = data[1];
                        hasSecondPoint.set();
                    }
                    pointIndex.increment();
                    break;
                // We are only interested in straight line angles here,
                // so reset the emitter's state, but not the subshape
                // state
                case SEG_QUADTO:
                    pointIndex.increment(2);
                    collector.onQuadratic(pointIndex.getLess(1), data[0], data[1], lastX = data[2], lastY = data[3]);
                    break;
                case SEG_CUBICTO:
                    pointIndex.increment(3);
                    collector.onCubic(pointIndex.getLess(1), data[0], data[1], data[2], data[3], lastX = data[4], lastY = data[5]);
                    break;
            }
            iter.next();
            lastType = type;
        }
        return collector.direction();
    }

    private static final class Collector {

        private static final double TEST_DIST_1 = 1.5;
        private static final double TEST_DIST_2 = 0.5;
        private static final double TEST_DIST_3 = 3;
        private static final double TEST_DIST_4 = 6;

        private double lastX, lastY;
        private double prevX, prevY;
        private int state;
        private final IntMap<LineVector> vectors = CollectionUtils.intMap(50);
        private final IntMap<Intersectable> intersectors = CollectionUtils.intMap(50);
        private final DoubleList points = new DoubleList(50);

        Collector(Point2D p) {
            this(p.getX(), p.getY());
        }

        Collector(double lastX, double lastY) {
            this.prevX = lastX;
            this.prevY = lastY;
            state = 1;
        }

        Collector() {

        }

        public boolean isEmpty() {
            return state < 2 && vectors.isEmpty() && intersectors.isEmpty();
        }

        public Collector reset() {
            state = 0;
            return this;
        }

        public Collector fullReset() {
            reset();
            return this;
        }

        private Shape dissectApproximate() {
            // More accurate but much more expensive:
//            Path2D.Double pth = approx.toPath();
//            return new Area(approximation().toPath());
            return approximation();
        }

        public RotationDirection visitAll(int subpathIndex, VectorVisitor v) {
            RotationDirection direction = direction();
            DoubleBiPredicate contains = new Area(dissectApproximate())::contains;
            if (vectors.isEmpty()) {
                return RotationDirection.NONE;
            }
            LineVector last = vectors.valueAt(vectors.size() - 1);
            Int prevPointIndex = Int.of(vectors.greatestKey());
//            Int prevKey = Int.create(vectors.las);
            vectors.forEachIndexed((ix, key, vect) -> {
                // Test if the approximated shape contains
                // points at the 1/4, mid or 3/4 angle from
                // the center point - if not, then we have
                // an exterior angle and need the inverse

                int sam1 = vect.sample(TEST_DIST_1, contains);
                int sam2 = vect.inverse().sample(TEST_DIST_1, contains);
                // XXX for very small distances, may need to scale our
                // test distances by the min length of the preceding vector
                if (sam1 < 3 && sam2 < 3) {
                    sam1 = vect.sample(TEST_DIST_2, contains);
                    sam2 = vect.inverse().sample(TEST_DIST_2, contains);
                }
                if (sam1 < 3 && sam2 < 3) {
                    sam1 = vect.sample(TEST_DIST_3, contains);
                    sam2 = vect.inverse().sample(TEST_DIST_3, contains);
                }
                if (sam1 < 3 && sam2 < 3) {
                    sam1 = vect.sample(TEST_DIST_4, contains);
                    sam2 = vect.inverse().sample(TEST_DIST_4, contains);
                }
                if (sam1 < 3 && sam2 < 3) {
                    System.err.println(key + ". corner " + sam1 + " / opp " + sam2
                            + " ICCW " + vect.ccw() + " SUBPATH DIR " + direction
                            + " angdir " + vect.corner().direction() + " " + vect
                            + " problem sampling with " + sam1 + " / " + sam2
                            + ". Approximate: "
                            + GeometryStrings.toStringCoordinates(
                                    approx.pointsArray())
                    );
                }
                boolean invert = sam1 < sam2;
                if (invert) {
                    vect = vect.inverse();
                }

                int nextPointIndex = ix == vectors.size() - 1
                        ? vectors.leastKey() : vectors.key(ix + 1);

                v.visit(key, vect, subpathIndex, direction, approx,
                        invert ? nextPointIndex : prevPointIndex.getAsInt(),
                        invert ? prevPointIndex.getAsInt() : nextPointIndex
                );
                prevPointIndex.set(key);
            });
            return direction;
        }

        RotationDirection dir;

        public RotationDirection direction() {
            if (dir != null) {
                return dir;
            }
            Int cwCount = Int.create();
            Int ccwCount = Int.create();
            IntIntMap directionByVector = IntIntMap.create();
            IntIntMap inters = intersectionCounts();
            vectors.forEachIndexed((ix, key, vect) -> {
                CornerAngle ang = vect.corner();
                RotationDirection dir = ang.direction();
                directionByVector.put(key, dir.ordinal());
                int intersections = inters.getAsInt(key);
                boolean oddIntersections = intersections % 2 != 0;
                switch (dir) {
                    case CLOCKWISE:
                        if (oddIntersections) {
                            ccwCount.increment();
                        } else {
                            cwCount.increment();
                        }
                        break;
                    case COUNTER_CLOCKWISE:
                        if (oddIntersections) {
                            cwCount.increment();
                        } else {
                            ccwCount.increment();
                        }
                        break;
                }
            });
            return dir = (cwCount.getAsInt() > ccwCount.getAsInt()
                    ? RotationDirection.CLOCKWISE : RotationDirection.COUNTER_CLOCKWISE);
        }

        public int totalIntersections() {
            Int result = Int.create();
            vectors.forEachIndexed((ix, key, vect) -> {
                EqLine first = vect.firstLine();
                intersectors.forEachIndexed((iix, ikey, ivect) -> {
                    if (key == ikey) {
                        return;
                    }
                    result.increment(ivect.intersectionCount(first, false));
                });
            });
            return result.getAsInt();
        }

        private IntIntMap intersectionCounts;

        public IntIntMap intersectionCounts() {
            if (intersectionCounts != null) {
                return intersectionCounts;
            }
            intersectionCounts = IntIntMap.create(vectors.size());
            vectors.forEachKey((key) -> {
                int ic = intersectionCount(key);
                intersectionCounts.put(key, ic);
            });
            int sum = 0;
            for (int i = 0; i < intersectionCounts.size(); i++) {
                int val = intersectionCounts.valueAt(i);
                intersectionCounts.setValueAt(i, val + sum);
                sum += val;
            }

            return intersectionCounts;
        }

        public int intersectionCount(int pointIndex) {
            LineVector lv = vectors.get(pointIndex);
            if (lv == null) {
                return 0;
            }
            Int result = Int.create();
            EqLine fl = lv.firstLine();
            intersectors.forEachIndexed((ix, key, isector) -> {
                if (key == pointIndex) {
                    return;
                }
                if (isector instanceof LineVector) {
                    LineVector o = (LineVector) isector;
                    if (fl.intersectsLine(o.secondLine())) {
                        result.increment();
                    }
                } else {
                    result.increment(lv.intersectionCount(isector, false));
                }
            });
            return result.getAsInt();
        }

        private Polygon2D approx;

        private Polygon2D approximation() {
            if (approx != null && approx.pointCount() != points.size() / 2) {
                return approx;
            }
            approx = new Polygon2D(points.toDoubleArray());
            return approx;
        }

        public void onQuadratic(int pointIndex, double x1, double y1, double x2, double y2) {
            DoubleList l = new DoubleList(2 * GeometryUtils.curveApproximationPointCount());
            GeometryUtils.approximateQuadraticCurve(lastX, lastY, x1, y1, x2, y2, (x, y) -> {
                points.add(x);
                points.add(y);
                l.add(x);
                l.add(y);
            });
            Polygon2D p = new Polygon2D(l.toDoubleArray());
            intersectors.put(pointIndex, p);
            reset();
            accept(pointIndex, x2, y2, false);
        }

        public void onCubic(int pointIndex, double x1, double y1, double x2, double y2, double x3, double y3) {
            DoubleList l = new DoubleList(2 * GeometryUtils.curveApproximationPointCount());
            GeometryUtils.approximateCubicCurve(lastX, lastY, x1, y1, x2, y2, x3, y3, (x, y) -> {
                l.add(x);
                l.add(y);
                points.add(x);
                points.add(y);
            });
            Polygon2D p = new Polygon2D(l.toDoubleArray());
            intersectors.put(pointIndex, p);
            reset();
            accept(pointIndex, x3, y3, false);
        }

        public void accept(int pointIndex, double x, double y) {
            accept(pointIndex, x, y, true);
        }

        public void accept(int pointIndex, double x, double y, boolean addToPoints) {
            if (addToPoints) {
                points.add(x);
                points.add(y);
            }
            switch (state) {
                case 0:
                    state++;
                    prevX = x;
                    prevY = y;
                    return;
                case 1:
                    state++;
                    lastX = x;
                    lastY = y;
                    return;
                case 2:
                    assert pointIndex >= 0 : "Negative point index " + pointIndex;
                    LineVector vect = LineVector.of(prevX, prevY, lastX, lastY, x, y);
                    LineVector old = vectors.put(pointIndex, vect);
                    assert old == null : "Clobbering " + old + " at " + pointIndex + " with " + vect;
                    Intersectable oi = intersectors.put(pointIndex, vect);
                    assert oi == null : "Clobbering old intersector at " + pointIndex;
                    prevX = lastX;
                    prevY = lastY;
                    lastX = x;
                    lastY = y;
                    break;
                default:
                    throw new AssertionError("Invalid state " + state);
            }
        }
    }
}
