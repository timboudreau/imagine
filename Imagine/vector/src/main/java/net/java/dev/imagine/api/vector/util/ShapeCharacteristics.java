package net.java.dev.imagine.api.vector.util;

import com.mastfrog.function.TriConsumer;
import com.mastfrog.util.collections.DoubleSet;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.MinimalAggregateShapeDouble;
import com.mastfrog.geometry.Polygon2D;
import com.mastfrog.geometry.Triangle2D;

/**
 *
 * @author Tim Boudreau
 */
public enum ShapeCharacteristics {
    VOLUMNAR,
    CURVES,
    CUBIC,
    QUADRATIC,
    EMPTY,
    POLYGONAL,
    TRIANGULAR,
    RECTANGULAR,
    CLOSES,
    ALL_CLOSED,
    STRAIGHT_LINES,
    POINTS,
    UNCLOSED_SHAPES,
    MULTI_SHAPE,
    DUPLICATE_POINTS,
    ORPHAN_POINTS,
    ILLEGAL;

    public static Set<ShapeCharacteristics> get(Shape shape) {
        return get(shape, (Set<ShapeCharacteristics> cs, List<EqPointDouble> pts) -> cs);
    }

    public static <T> T get(Shape shape, BiFunction<Set<ShapeCharacteristics>, List<EqPointDouble>, T> c) {
        Set<ShapeCharacteristics> result = EnumSet.noneOf(ShapeCharacteristics.class);
        PathIterator it = shape.getPathIterator(null);
        List<EqPointDouble> pts = new ArrayList<>(50);
        List<EqPointDouble> startPoints = new ArrayList<>(16);
        List<EqPointDouble> endPoints = new ArrayList<>(16);
        double[] data = new double[6];
        int closeCount = 0;
        int index = 0;
        int lastOpen = 0;
        int openCount = 0;
        boolean effectivelyClosed = false;
        boolean explicitlyClosed = false;
        int lastType = -1;
        double[] lastData = null;
        while (!it.isDone()) {
            int type = it.currentSegment(data);
            double x = -1;
            double y = -1;
            if (index != 0) {
                switch (lastType) {
                    case SEG_CLOSE:
                        if (type != SEG_MOVETO) {
                            result.add(ILLEGAL);
                        }
                        break;
                    case SEG_MOVETO:
                        if (type == SEG_MOVETO) {
                            result.add(ORPHAN_POINTS);
                        }
                        break;
                }
            } else {
                if (type != SEG_MOVETO) {
                    result.add(ILLEGAL);
                }
            }
            switch (type) {
                case SEG_CLOSE:
                    if (index > 0) {
                        endPoints.add(startPoints.get(startPoints.size() - 1));
                    }
                    closeCount++;
                    explicitlyClosed = true;
                    effectivelyClosed = false;
                    break;
                case SEG_CUBICTO:
                    pts.add(new EqPointDouble(x = data[4], y = data[5]));
                    result.add(CURVES);
                    result.add(CUBIC);
                    break;
                case SEG_QUADTO:
                    pts.add(new EqPointDouble(x = data[2], y = data[3]));
                    result.add(CURVES);
                    result.add(QUADRATIC);
                    break;
                case SEG_MOVETO:
                    if (index > 0) {
                        endPoints.add(pts.get(pts.size() - 1));
                    }
                    if (index > 0 && pts.size() > lastOpen) {
                        EqPointDouble prev = pts.get(pts.size() - 1);
                        EqPointDouble open = pts.get(lastOpen);
                        if (!prev.equals(open)) {
                            result.add(UNCLOSED_SHAPES);
                        }
                    }
                    openCount++;
                    lastOpen = index;
                    effectivelyClosed = false;
                    if (index != 0) {
                        result.add(MULTI_SHAPE);
                    }
                    result.add(POINTS);
                    pts.add(new EqPointDouble(x = data[0], y = data[1]));
                    startPoints.add(pts.get(pts.size() - 1));
                    break;
                case SEG_LINETO:
                    if (index > 0 && type == lastType) {
                        if (pts.size() > index - 1) {
                            EqPointDouble last = pts.get(index - 1);
                            if (last.x == data[0] && last.y == data[1]) {
                                // Ignore duplicate points
                                it.next();
                                result.add(DUPLICATE_POINTS);
                                continue;
                            }
                        }
                    }
                    result.add(STRAIGHT_LINES);
                    pts.add(new EqPointDouble(x = data[0], y = data[1]));
                    break;
            }
            if (type != SEG_CLOSE && type != SEG_MOVETO) {
                EqPointDouble dbl = new EqPointDouble(x, y);
                if (pts.size() > lastOpen && dbl.equals(pts.get(lastOpen))) {
                    effectivelyClosed = true;
                } else {
                    effectivelyClosed = false;
                }
            }
            lastType = type;
            it.next();
            index++;
        }
        if (openCount != closeCount && !((openCount == closeCount - 1) && effectivelyClosed)) {
            result.add(UNCLOSED_SHAPES);
        }
        if (closeCount > 0 || effectivelyClosed) {
            result.add(CLOSES);
        }
        if (endPoints.size() == startPoints.size()) {
            if (endPoints.equals(startPoints)) {
                result.add(ALL_CLOSED);
            }
        }
        Set<EqPointDouble> pointSet = new HashSet<>(pts);
        if (pointSet.size() >= 3) {
            if (closeCount > 0 || effectivelyClosed) {
                result.add(VOLUMNAR);
            }
            if (!result.contains(CURVES)) {
                result.add(POLYGONAL);
            }
        } else if (pointSet.size() <= 1) {
            result.add(EMPTY);
        }
        if (result.contains(POLYGONAL)) {
            DoubleSet xs = DoubleSet.create(pts.size());
            DoubleSet ys = DoubleSet.create(pts.size());
            for (EqPointDouble p : pointSet) {
                xs.add(p.getX());
                ys.add(p.getY());
            }
            if (pointSet.size() == 4 && xs.size() == 2 && ys.size() == 2) {
                result.add(RECTANGULAR);
            } else if (pts.size() == 3 || pts.size() == 4 && pts.get(0).equals(pts.get(pts.size() - 1))) {
                result.add(TRIANGULAR);
            }
        }
        return c.apply(result, pts);
    }

    public static Shape bestShapeFor(Shape shape) {
        return get(shape, ShapeCharacteristics::bestShapeFor);
    }

    public static Shape bestShapeFor(Shape shape, TriConsumer<Set<ShapeCharacteristics>, List<EqPointDouble>, Shape> c) {
        return get(shape, (Set<ShapeCharacteristics> ch, List<EqPointDouble> pts) -> {
            Shape res = bestShapeFor(ch, pts);
            if (c != null) {
                c.accept(ch, pts, res);
            }
            return res;
        });
    }

    public static Shape bestShapeFor(Set<ShapeCharacteristics> ch, List<EqPointDouble> pts) {
        if (!ch.contains(MULTI_SHAPE) && ch.contains(VOLUMNAR) && !ch.contains(UNCLOSED_SHAPES)) {
            if (ch.contains(TRIANGULAR)) {
                return new Triangle2D(pts.get(0), pts.get(1), pts.get(2));
            } else if (ch.contains(RECTANGULAR)) {
                Collections.sort(pts);
                Point2D.Double topLeft = pts.get(0);
                Point2D.Double bottomRight = pts.get(pts.size() - 1);
                return new Rectangle2D.Double(topLeft.x, topLeft.y,
                        bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
            } else if (ch.contains(POLYGONAL)) {
                boolean explicitlyClosed = pts.get(0).equals(pts.get(pts.size() - 1));
                int max = explicitlyClosed ? pts.size() - 1 : pts.size();
                double[] points = new double[max * 2];
                for (int i = 0; i < max; i++) {
                    int offset = i * 2;
                    EqPointDouble p = pts.get(i);
                    points[offset] = p.x;
                    points[offset + 1] = p.y;
                }
                return new Polygon2D(points);
            }
        }
        return null;
    }

    public static void main(String[] args) {
        Set<ShapeCharacteristics> cs = get(new Triangle2D(1, 1, 5, 5, 20, 20));
        System.out.println("TRIANGLE:\t\t" + cs);

        Set<ShapeCharacteristics> ln = get(new Line2D.Double(10, 10, 20, 20));
        System.out.println("LINE:\t\t\t" + ln);

        Set<ShapeCharacteristics> rect = get(new Rectangle(100, 100, 10, 10));
        System.out.println("RECT:\t\t\t" + rect);

        Set<ShapeCharacteristics> poly = get(new Polygon2D(100D, 100D, 10D, 10D, 20, 20, 30, 30, 40, 40));
        System.out.println("POLY:\t\t\t" + poly);

        Set<ShapeCharacteristics> polyRect = get(new Polygon2D(10D, 10D, 20D, 10D, 20D, 20D, 10D, 20D));
        System.out.println("POLY-RECT:\t\t" + polyRect);

        Set<ShapeCharacteristics> polyRectClosed = get(new Polygon2D(10D, 10D, 20D, 10D, 20D, 20D, 10D, 20D, 10D, 10D));
        System.out.println("POLY-RECT-CLOSED:\t" + polyRectClosed);

        Set<ShapeCharacteristics> circ = get(new Circle(100, 100, 20));
        System.out.println("CIRCLE:\t\t\t" + circ);

        Polygon2D trianglarPolyWithDuplicates = new Polygon2D(317.0, 163.0, 455.0, 87.0, 455.0, 87.0, 456.0, 225.0, 456.0, 225.0, 317.0, 163.0);
        Set<ShapeCharacteristics> triPolyCh = get(trianglarPolyWithDuplicates);
        System.out.println("TRI-POLY:\t\t\t" + triPolyCh + " for " + trianglarPolyWithDuplicates);
        Shape best = bestShapeFor(trianglarPolyWithDuplicates);
        System.out.println("   best " + best);

        MinimalAggregateShapeDouble invalidShape1 = new MinimalAggregateShapeDouble(
                new int[]{SEG_LINETO, SEG_LINETO, SEG_MOVETO, SEG_MOVETO,
                    SEG_LINETO, SEG_LINETO, SEG_LINETO},
                new double[]{
                    3, 5,
                    5, 7,
                    6, 6,
                    6, 5,
                    12, 12,
                    12, 12,
                    3, 5
                }, WIND_EVEN_ODD);
        Set<ShapeCharacteristics> invalid1 = get(invalidShape1);
        System.out.println("Invalid1: " + invalid1);
        MinimalAggregateShapeDouble invalidShape2 = new MinimalAggregateShapeDouble(
                new int[]{
                    SEG_CUBICTO,
                    SEG_CLOSE
                },
                new double[]{
                    1, 2, 3, 4, 5, 6
                },
                WIND_NON_ZERO
        );
        Set<ShapeCharacteristics> invalid2 = get(invalidShape2);

    }
}
