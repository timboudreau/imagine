package org.imagine.geometry;

import com.mastfrog.function.DoubleBiPredicate;
import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntIntMap;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.collections.IntMap.IntMapConsumer;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import org.imagine.geometry.util.DoubleList;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.geometry.util.function.Bool;
import org.imagine.geometry.util.function.Int;
import org.imagine.geometry.util.function.IntWithChildren;

/**
 *
 * @author Tim Boudreau
 */
public final class AnglesAnalyzer {

    private List<Collector> collectors = new ArrayList<>(5);
    private Collector currentCollector;

    public AnglesAnalyzer() {
        collectors.add(currentCollector = new Collector());
    }

    @FunctionalInterface
    public interface V {

        void visit(int pointIndex, double x, double y, CornerAngle angles,
                int intersections, RotationDirection subpathRotationDirection,
                Polygon2D approximate);
    }

    public RotationDirection analyzeShape(Shape shape, AffineTransform xform) {
        return forShape(shape.getPathIterator(xform));
    }

    public RotationDirection analyze(PathIterator iter) {
        return forShape(iter);
    }

    public void visitAll(V v) {
        for (int i = 0; i < collectors.size(); i++) {
            Collector c = collectors.get(i);
            if (!c.isEmpty()) {
                c.visitAll(v);
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
        // Holder for the point index which can be incremented inside a
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
                    System.out.println("FOUND CLOSE AT " + pointIndex
                            + " last type is close? " + (lastType == SEG_CLOSE)
                            + " last type is move? " + (lastType == SEG_MOVETO)
                            + " last type " + lastType);
                    if (lastType != SEG_CLOSE && lastType != SEG_MOVETO) {
//                        int pix = pointIndex.getAsInt()
//                                - pointIndexWithinShape.getAsInt();
//                        if (pix < 0) {
//                            pix = pointIndex.getAsInt();
//                        }

                        int pix = shapeStart;
                        // Handle the trailing series of lines where the
                        // last point of this sub-path is the apex

                        if (lastX != startX || lastY != startY) {
                            System.out.println("  pass start " + pointIndex.getAsInt());
                            collector.accept(pointIndex.getLess(1), startX, startY, false);
                            // And handle the trailing series of lines where the
                            // 0th point of this sub-path is the apex - that
                            // provides a corner we will have skipped because
                            // we only had two points when we initially iterated
                            // past it
                            if (hasSecondPoint.getAsBoolean()) {
                                System.out.println("  pass second " + (shapeStart + 1));
                                collector.accept(shapeStart, secondX, secondY, false);
                            }
                        } else if (lastX == startX && lastX == startY) {
                            if (hasSecondPoint.getAsBoolean()) {
                                System.out.println("  pass second b");
                                collector.prevX = collector.lastX;
                                collector.prevY = collector.lastY;
                                collector.lastX = startX;
                                collector.lastY = startY;
                                collector.accept(pix + 1, secondX, secondY);
                            }
                        }
                    } else {
                        System.out.println("no pass start or second, wrong type " + lastType);

                    }
                    onNewSubshape.run();
                    break;
                case SEG_MOVETO:
                    shapeStart = pointIndex.get();
                    onNewSubshape.run();
                    collector = nextCollector();
                    collector.accept(pointIndex.getLess(1), startX = lastX = data[0], startY = lastY = data[1]);
                    System.out.println("MOVETO AT " + pointIndex.getAsInt());
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
            System.out.println("last type now " + type);
            lastType = type;
        }
        return collector.direction();
    }

    private static final class Collector {

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

        public RotationDirection visitAll(V v) {
            RotationDirection direction = direction();
            IntIntMap directionByVector = IntIntMap.create();
            IntIntMap inters = intersectionCounts();
            Polygon2D approx = approximation();
            DoubleBiPredicate contains = approx::contains;
            vectors.forEach((IntMapConsumer<LineVector>) (key, vect) -> {
                CornerAngle ang;
                // Test if the approximated shape contains
                // points at the 1/4, mid or 3/4 angle from
                // the center point - if not, then we have
                // an exterior angle and need the inverse
                int sam1 = vect.corner().sample(vect.sharedX(), vect.sharedY(), contains);
                int sam2 = vect.corner().opposite().sample(vect.sharedX(), vect.sharedY(), contains);
                System.out.println(key + ". corner " + sam1 + " / inv " + sam2);
                if (sam1 > sam2) {
                    ang = vect.corner();
                    System.out.println("\n" + key + ". INTERIOR " + vect + " -> " + ang.toShortString()
                            + " dir " + ang.direction() + " vs " + direction + " samples " + sam1);
                } else {
                    System.out.println("OPPOSITE OF " + vect.corner() + " is "
                            + vect.corner().opposite());

                    ang = vect.corner().opposite();
//                    ang = vect.toSector().opposite();
                    System.out.println("\n" + key + ". NOT INTERIOR " + vect + " -> " + ang.toShortString()
                            + " dir " + ang.direction() + " vs " + direction + " samples " + sam1);
                }
                ang = ang.normalized();
                RotationDirection dir = vect.corner().direction();
                System.out.println("DIRECTION " + key + ": " + dir + " and " + direction);
                if (direction != dir) {
                    System.out.println("FLIP for " + dir + " and " + direction);
                    ang = ang.reversed();
                }
//                if (dir != direction) {
//                    System.out.println("SWAP DIRECTION FOR " + key + " " + vect);
//                    ang = ang.opposite();
//                    dir = dir.opposite();
//                }
                directionByVector.put(key, dir.ordinal());
                int ptIntersections = inters.get(key);
                boolean oddIntersections = ptIntersections % 2 != 0;

                if (oddIntersections) {
                    System.out.println("FLIP FOR ODD ISECTS " + ang + " ext (" + ang.extent()
                            + ") to "
                            + ang.inverse() + " (" + ang.inverse().extent() + ")"
                            + " dir " + ang.direction() + " VectorCCW " + vect.ccw()
                            + " norm " + ang.isNormalized() + " inv norm " + ang.inverse().isNormalized());
                    ang = ang.inverse().normalized().opposite2();
                }

                if (dir != direction) {
//                    ang = ang.inverse();
//                    ang = new CornerAngle(ang.bDegrees(), 360 - Math.abs(ang.extent()));
//                    ang = new CornerAngle(ang.aDegrees(), 360 - Math.abs(ang.extent()));
//                    ang = ang.reversed();
                }
                v.visit(key, vect.sharedX(), vect.sharedY(), ang, ptIntersections,
                        direction, approx);

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
                System.out.println("put for " + key + " ic " + ic);
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
                    LineVector vect = new LineVectorImpl(prevX, prevY, lastX, lastY, x, y);
                    System.out.println("\n" + pointIndex + " - " + GeometryUtils.toShortString(x, y));
                    LineVector old = vectors.put(pointIndex, vect);
                    assert old == null : "Clobbering " + old + " at " + pointIndex + " with " + vect;
                    Intersectable oi = intersectors.put(pointIndex, vect);
                    assert oi == null : "Clobbering old intersector at " + pointIndex;
                    System.out.println("VECTS NOW " + vectors);
//                    CornerAngle result;
//                    if (intersections % 2 == 1) {
//                        result = new CornerAngle(x, y, lastX, lastY, prevX, prevY);
//                    } else {
//                        result = new CornerAngle(prevX, prevY, lastX, lastY, x, y);
//                    }
//                    switch (result.direction()) {
//                        case CLOCKWISE:
//                            cwCount++;
//                            break;
//                        case COUNTER_CLOCKWISE:
//                            ccwCount++;
//                            break;
//                    }
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
