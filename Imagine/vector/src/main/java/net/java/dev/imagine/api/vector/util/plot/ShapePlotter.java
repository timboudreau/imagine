/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.java.dev.imagine.api.vector.util.plot;

import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.Triangle2D;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;
import static java.lang.Math.pow;
import java.util.LinkedList;
import java.util.function.Consumer;
import com.mastfrog.geometry.Angle;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.util.DoubleList;
import com.mastfrog.geometry.util.GeometryUtils;
import com.mastfrog.geometry.util.PooledTransform;

/**
 * Basically, a recapitulation of the shape rasterizers in the JDK, so we can
 * iterate the points of a shape in order, with a tangent value at each step, in
 * order to find the tangent at any point in a shape. Note that for cubic and
 * quadratic curves, the plotter may be called repeatedly for very small
 * distances.
 *
 * @author Tim Boudreau
 */
public class ShapePlotter {

    private final Plotter plotter;
    private double interval = 1;
    static int ix;
    private static final double TAN_HALF_LENGTH = 0.5;
    private static final double TAN_FULL_LENGTH = (TAN_HALF_LENGTH * 2D);

    public static final double TRANSLATE = -0.5;

    private double tangentLength;
    private double tangentHalfLength;

    public ShapePlotter(Plotter plotter) {
        this.plotter = plotter;
        tangentLength = TAN_FULL_LENGTH;
        tangentHalfLength = TAN_HALF_LENGTH;
    }

    /**
     * Set the length of the tangent line, which will be such that its midpoint
     * passes through the x/y coordinates of the current point.
     *
     * @param val The tangent length
     * @return this
     */
    public ShapePlotter setTangentLength(double val) {
        this.tangentLength = val;
        this.tangentHalfLength = val * .5;
        return this;
    }

    /**
     * Set the interval at which plot points should be emitted for straight
     * lines - by default, this is infrequent, since for many purposes only
     * the start and end points are needed.
     *
     * @param ival The interval
     * @return this
     */
    public ShapePlotter setInterval(double ival) {
        this.interval = ival;
        return this;
    }

    public static EqLine tangentNear(double x, double y, Shape shape, double tolerance) {
        TangentFinder f = new TangentFinder(x, y, tolerance);
        ShapePlotter plot = new ShapePlotter(f);
        plot.plot(shape);
        return f.result;
    }

    public static void position(Shape shape, Positioner pos) {
        position(shape, pos, 1);
    }

    public static void position(Shape shape, Positioner pos, double straightLineInterval) {
        position(shape, pos, plotter -> {
            plotter.setInterval(straightLineInterval);
        });
    }

    public static void position(Shape shape, Positioner pos, Consumer<ShapePlotter> configurer) {
        ShapePositioner sp = new ShapePositioner(pos);
        ShapePlotter plot = new ShapePlotter(sp);
        if (configurer != null) {
            configurer.accept(plot);
        }
        plot.plot(shape);
    }

    private static final class ShapePositioner implements Plotter {

        private final Positioner pos;
        private double requiredDistance;
        private boolean first;
        private double prevX, prevY;
        private double currentDistance;
        private boolean done;
        private double lastEmittedAngle = Double.MIN_VALUE;

        public ShapePositioner(Positioner pos) {
            this.pos = pos;
        }

        private double height = 10;

        public void height(double height) {
            this.height = height;
            circ.setRadius(height);
        }

        private Circle circ = new Circle(0, 0, 1);

        private double lastEmittedX, lastEmittedY;

        private double lastEmittedTanX1, lastEmittedTanY1;

        @Override
        public void plot(double x, double y, double c, double tanX1, double tanY1, double tanX2, double tanY2) {
            if (!first) {
                currentDistance += Point2D.distance(prevX, prevY, x, y);
            }
            if (currentDistance >= requiredDistance) {
                double d2 = Point2D.distance(tanX1, tanY1, lastEmittedTanX1, lastEmittedTanY1);
                if (d2 < requiredDistance) {
//                    System.out.println("skip for " + d2 + " < " + requiredDistance + " though " + currentDistance);
                    return;
                }
                circ.setCenter(lastEmittedTanX1, lastEmittedTanY1);

                EqLine tangent = new EqLine(tanX2, tanY2, tanX1, tanY1);
                lastEmittedAngle = tangent.angle();
                lastEmittedTanX1 = tanX1;
                lastEmittedTanY1 = tanY1;
                circ.setRotation(lastEmittedAngle);
                circ.setCenter(x, y);
                requiredDistance = pos.position(x, y, tangent);
                currentDistance = 0;
                if (requiredDistance < 0) {
                    done = true;
                }
                lastEmittedX = x;
                lastEmittedY = y;
            }
            first = false;
            prevX = x;
            prevY = y;
        }

        @Override
        public boolean isDone() {
            return done;
        }
    }

    private static final class TangentFinder implements Plotter {

        private final double targetX;
        private final double targetY;
        private final double toleranceX;
        private final double toleranceY;
        private EqLine result;

        public TangentFinder(double targetX, double targetY, double tolerance) {
            this(targetX, targetY, tolerance, tolerance);
        }

        public TangentFinder(double targetX, double targetY, double toleranceX, double toleranceY) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.toleranceX = toleranceX;
            this.toleranceY = toleranceY;
        }

        private boolean isNoTangent(double x, double y, double tanX1, double tanY1, double tanX2, double tanY2) {
            return x == tanX1 && x == tanX2 && y == tanY1 && y == tanY2;
        }

        @Override
        public void plot(double x, double y, double c, double tanX1, double tanY1, double tanX2, double tanY2) {
            if (GeometryUtils.isSameCoordinate(targetX, x, toleranceX) && GeometryUtils.isSameCoordinate(targetY, y, toleranceY)) {
                if (isNoTangent(x, y, tanX1, tanY1, tanX2, tanY2)) {
                    result = new EqLine(tanX1, tanY1, tanX2, tanY2);
                }
            }
        }

        @Override
        public boolean isDone() {
            return result != null;
        }
    }

    public void plot(Shape shape) {
        // XXX: since we test from centered coordinates (x or y + 0.5 gets
        // you the cell value with no contribution from neightbors), we
        // need to shift the shape leftward to wind up with plotted coordinates
        // that match
        PooledTransform.withTranslateInstance(TRANSLATE, TRANSLATE, xf -> {
//            plot(xf.createTransformedShape(shape).getPathIterator(null));
            plot(shape.getPathIterator(xf));
        });
//        AffineTransform tx = AffineTransform.getTranslateInstance(TRANSLATE, TRANSLATE);
//        shape = tx.createTransformedShape(shape);
//        plot(shape.getPathIterator(null));
    }

    public void plot(PathIterator iter) {
        // XXX we are synthesizing tangents at all segment boundaries
        // except the first one;  to do that last, we would need to pre-run
        // the iterator and precompute the tangent at the last plotted
        // point
        double[] coords = new double[8];
        LinkedList<PlotState> states = new LinkedList<>();
        PlotState state = new PlotState();
        states.push(state);
        while (!iter.isDone()) {
            int type = iter.currentSegment(coords);
            switch (type) {
                case SEG_MOVETO:
                    state.last(coords[0], coords[1]);
                    break;
                case SEG_LINETO:
                    double tanAngle1 = plotLine(state.x(), state.y(), coords[0], coords[1], state.hasTangentAngle(), state.lastTangentAngle());
                    state.last(coords[0], coords[1], tanAngle1);
                    break;
                case SEG_CLOSE:
                    double fx = state.firstX();
                    double fy = state.firstY();
                    if (state.x() != fx || state.y() != fy) {
                        plotLine(state.x(), state.y(), fx, fy, state.hasTangentAngle(), state.lastTangentAngle());
                        state = new PlotState();
                        states.push(state);
                    }
                    break;
                case SEG_CUBICTO:
                    double tanAngle2 = plotCubic(state.x(), state.y(), coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], state.hasTangentAngle(), state.lastTangentAngle());
                    state.last(coords[4], coords[5], tanAngle2);
                    break;
                case SEG_QUADTO:
                    double tanAngle3 = plotQuadratic(state.x(), state.y(), coords[0], coords[1], coords[2], coords[3], state.hasTangentAngle(), state.lastTangentAngle());
                    state.last(coords[2], coords[3], tanAngle3);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown type " + type);
            }
            iter.next();
            if (plotter.isDone()) {
                return;
            }
        }
    }

    private double curveStep(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double minX = Math.min(x1, Math.min(x2, Math.min(x3, x4)));
        double maxX = Math.max(x1, Math.max(x2, Math.max(x3, x4)));

        double minY = Math.min(y1, Math.min(y2, Math.min(y3, y4)));
        double maxY = Math.max(y1, Math.max(y2, Math.max(y3, y4)));

        return 1D / Point2D.distance(minX, minY, maxX, maxY);
    }

    private double curveStep(double x1, double y1, double x2, double y2, double x3, double y3) {
        Triangle2D t = new Triangle2D(x1, y1, x2, y2, x3, y3);
        EqLine f = t.side(t.longestSide());
        return 1D / f.length();
    }

    private double plotQuadratic(double x1, double y1, double x2, double y2, double x3, double y3, boolean useAngle, double previousTangentAngle) {
        double step = curveStep(x1, y1, x2, y2, x3, y3) * 0.1;
//        System.out.println("PLOT QUAD " + step);
        double lastX = 0;
        double lastY = 0;
        boolean first = true;
        double lastTangentAngle = 0;
        for (double t = 0; t <= 1; t += step) {
            double a = pow((1.0 - t), 2.0);
            double b = 2.0 * t * (1.0 - t);
            double c = pow(t, 2.0);
            double x = a * x1 + b * x2 + c * x3;
            double y = a * y1 + b * y2 + c * y3;

//            if (first || (Math.abs(x - lastX) > 0.9625 || Math.abs(y - lastY) > 0.9625)) {
            double tx = quadTangent(x1, x2, x3, t);
            double ty = quadTangent(y1, y2, y3, t);
            cir.setCenterAndRadius(x, y, tangentHalfLength * 2);
            double ang = cir.angleOf(x + tx, y + ty);
            if (first && useAngle) {
                ang = Angle.angleBetween(ang, previousTangentAngle);
            }
            double invAng = ang - 90D;
            if (invAng > 360) {
                invAng -= 360D;
            }
            lastTangentAngle = invAng;

            EqLine ln = cir.line(invAng);

            plotter.plot(x, y, 1, ln.x1, ln.y1, ln.x2, ln.y2);
            lastX = x;
            lastY = y;
            first = false;
            if (plotter.isDone()) {
                return lastTangentAngle;
            }
//            }
        }
        return lastTangentAngle;
    }

    private double quadTangent(double x1, double x2, double x3, double t) {
        return 2D * (x1 * (t - 1D) - x2 * (2D * t - 1D) + x3 * t);
    }

    private final Circle cir = new Circle(0, 0, 1);

    private double plotCubic(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, boolean useAngle, double previousTangentAngle) {

        int ct = 1;

        double step = curveStep(x0, y0, x1, y1, x2, y2, x3, y3) * 0.1;
        double lastX = 0;
        double lastY = 0;
        boolean first = true;
//        System.out.println("plot cubic " + step);
        double lastTangentAngle = 0;
        for (double t = 0.0; t <= 1.0; t += step) {
            double x = pow(1D - t, 3D) * x0 + 3D * t * pow(1D - t, 2D) * x1 + 3D * pow(t, 2D) * (1D - t) * x2
                    + pow(t, 3D) * x3;
            double y = pow(1D - t, 3D) * y0 + 3D * t * pow(1D - t, 2D) * y1 + 3D * pow(t, 2D) * (1D - t) * y2
                    + pow(t, 3D) * y3;

//            if (first || (Math.abs(x - lastX) > 0.9625 || Math.abs(y - lastY) > 0.9625)) {
            double tanX = bezierTangent(t, x0, x1, x2, x3);
            double tanY = bezierTangent(t, y0, y1, y2, y3);
            cir.setCenterAndRadius(x, y, tangentHalfLength * 2);
            double ang = cir.angleOf(x + tanX, y + tanY);
            if (first && useAngle) {
                ang = Angle.angleBetween(ang, previousTangentAngle);
            }
            double invAng = ang - 90D;
            if (invAng > 360) {
                invAng -= 360D;
            }
            lastTangentAngle = invAng;
            EqLine ln = cir.line(invAng);

            plotter.plot(x, y, 1, ln.x1, ln.y1, ln.x2, ln.y2);
            lastX = x;
            lastY = y;
            first = false;
            if (plotter.isDone()) {
                return lastTangentAngle;
            }
//            }
        }
        return lastTangentAngle;
    }

    private double bezierTangent(double t, double a, double b, double c, double d) {
        double oneMinusT = 1D - t;
        double result = 3D * oneMinusT * oneMinusT * (b - a)
                + 6D * t * oneMinusT * (c - b)
                + 3D * t * t * (d - c);
        return result;
    }

    private double plotLine(double x1, double y1, double x2, double y2, boolean useAngle, double lastTanAngle) {
        return plotLine(x1, y1, x2, y2, interval, plotter, useAngle, lastTanAngle);
    }

    static double plotLine(double x1, double y1, double x2, double y2, double interval, Plotter plotter, boolean useAngle, double previousTangentAngle) {
        return plotLine(x1, y1, x2, y2, interval, plotter, useAngle, previousTangentAngle, TAN_FULL_LENGTH);
    }

    static double plotLine(double x1, double y1, double x2, double y2, double interval, Plotter plotter, boolean useAngle, double previousTangentAngle, double tangentLineLength) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lastTangentAngle = 0;
        if (x1 == x2 && y1 == y2) {
            plotter.plot(x1, y1, 1, x1, y1, x1, y1);
        } else if (x1 == x2) {
            if (y1 < y2) {
                lastTangentAngle = 90;
                for (double y = y1; y < y2; y += interval) {
                    double tx1 = x1 + tangentLineLength;
                    double tx2 = x1 - tangentLineLength;
                    double ty1 = y;
                    double ty2 = y;
                    if (y == y1 && useAngle && previousTangentAngle != lastTangentAngle) {
                        Circle cir = new Circle(x1, y, tangentLineLength);
                        double ang = Angle.angleBetween(lastTangentAngle, previousTangentAngle);
                        double[] pos = cir.positionOf(ang);
                        tx1 = pos[0];
                        ty1 = pos[1];
                        pos = cir.positionOf(ang - 180D);
                        tx2 = pos[0];
                        ty2 = pos[1];
//                        System.out.println("90 corner a " + ang + " prev " + previousTangentAngle
//                                + " @ " + tx1 + "," + ty1 + " / " + tx2 + ", " + ty2
//                                + " starting line from " + x1 + "," + y1 + " to " + x2 + "," + y2);

                    }
                    // FIXME: We should ignore the tangent on the last point,
                    // IF and only if we are the last segment in an unclosed
                    // path

//                    if (y + interval < y2) {
                    plotter.plot(x1, y, 1, tx1, ty1, tx2, ty2);
                    if (plotter.isDone()) {
                        break;
                    }
//                    } else {
//                        plotter.plot(x1, y, 1, x1, y, x1, y);
//                    }
                }
            } else {
                lastTangentAngle = 90;
                for (double y = y1; y > y2; y -= interval) {
                    double tx1 = x1 - tangentLineLength;
                    double tx2 = x1 + tangentLineLength;
                    double ty1 = y;
                    double ty2 = y;
                    if (y == y1 && useAngle && previousTangentAngle != lastTangentAngle) {
                        Circle cir = new Circle(x1, y, tangentLineLength);
                        double ang = Angle.angleBetween(lastTangentAngle, previousTangentAngle);
                        double[] pos = cir.positionOf(ang);
                        tx2 = pos[0];
                        ty2 = pos[1];
                        pos = cir.positionOf(ang - 180D);
                        tx1 = pos[0];
                        ty1 = pos[1];
//                        System.out.println("90 corner b " + ang + " @ " + tx1 + ","
//                                + ty1 + " / " + tx2 + ", " + ty2
//                                + " starting line from " + x1 + "," + y1 + " to " + x2 + "," + y2);
                    }
//                    if (y - interval >= y2) {
                    plotter.plot(x1, y, 1, tx1, ty1, tx2, ty2);
//                    } else {
//                        plotter.plot(x1, y, 1, x1, y, x1, y);
//                    }
                    if (plotter.isDone()) {
                        break;
                    }
                }
            }
        } else if (y1 == y2) {
            if (x1 < x2) {
                lastTangentAngle = 0;
                for (double x = x1; x < x2; x += interval) {
                    double ty1 = y1 - tangentLineLength;
                    double ty2 = y1 + tangentLineLength;
                    double tx1 = x;
                    double tx2 = x;
                    if (x == x1 && useAngle && previousTangentAngle != lastTangentAngle) {
                        Circle cir = new Circle(x1, y1, tangentLineLength);
                        double ang = Angle.angleBetween(lastTangentAngle, previousTangentAngle);
                        double[] pos = cir.positionOf(ang);
                        tx1 = pos[0];
                        ty1 = pos[1];
                        pos = cir.positionOf(ang - 180D);
                        tx2 = pos[0];
                        ty2 = pos[1];
//                        System.out.println("90 corner c " + ang + " @ "
//                                + tx1 + "," + ty1 + " / " + tx2 + ", " + ty2
//                                + " starting line from " + x1 + "," + y1
//                                + " to " + x2 + "," + y2
//                        );
                    }
//                    if (x + interval < x2) {
                    plotter.plot(x, y1, 1, tx1, ty1, tx2, ty2);
//                    } else {
//                        plotter.plot(x, y1, 1, x, y1, x, y1);
//                    }
                    if (plotter.isDone()) {
                        break;
                    }
                }
            } else {
                lastTangentAngle = 0;
                for (double x = x1; x > x2; x -= interval) {
                    double ty1 = y1 + tangentLineLength;
                    double ty2 = y1 - tangentLineLength;
                    double tx1 = x;
                    double tx2 = x;
                    if (x == x1 && useAngle && previousTangentAngle != lastTangentAngle) {
                        Circle cir = new Circle(x1, y1, tangentLineLength);
                        double ang = Angle.angleBetween(lastTangentAngle, previousTangentAngle) + 90;

                        double[] pos = cir.positionOf(ang);
                        tx1 = pos[0];
                        ty1 = pos[1];
                        pos = cir.positionOf(ang - 180D);
                        tx2 = pos[0];
                        ty2 = pos[1];
//                        System.out.println("90 corner d " + ang + " @ " + tx1 + "," + ty1
//                                + " / " + tx2 + ", " + ty2
//                                + " starting line from " + x1 + "," + y1 + " to " + x2 + "," + y2);
                    }
//                    if (x - interval >= x2) {
                    plotter.plot(x, y1, 1, tx1, ty1, tx2, ty2);
//                    } else {
//                        plotter.plot(x, y1, 1, x, y1, x, y1);
//                    }
                    if (plotter.isDone()) {
                        break;
                    }
                }
            }
        } else {
            double lastX = 0;
            double lastY = 0;
            if (x1 < x2) {
                double ival;
                if (x2 - x1 > Math.abs(y2 - y1)) {
                    ival = 1D / (Math.ceil(x2 - x1) * 2D);
                } else {
                    ival = 1D / (Math.ceil(Math.abs(y2 - y1)) * 2D);
                }
                double angle = Angle.ofLine(x1, y1, x2, y2) + 90;
                double realAngle = angle;
                if (useAngle && previousTangentAngle != realAngle) {
                    angle = Angle.angleBetween(previousTangentAngle, angle) + 180;
                }
                for (double x = x1; x < x2; x += ival) {
                    double y = y1 + dy * (x - x1) / dx;
                    if (x != x1 && Math.abs(lastX - x) > IP_THRESH && Math.abs(lastY - y) > IP_THRESH) {
                        double interX = lastX + ((x - lastX) / 2D);
                        double interY = lastY + ((y - lastY) / 2D);
                        plotter.plot(interX, interY, 1, interX, interY, interX, interY);
                    }
                    Circle c = new Circle(x, y, tangentLineLength);
                    EqLine perp = c.line(angle);
                    plotter.plot(x, y, 1, perp.x1, perp.y1, perp.x2, perp.y2);
                    lastX = x;
                    lastY = y;
                    angle = realAngle;
                    if (plotter.isDone()) {
                        break;
                    }
                }
                lastTangentAngle = angle;
            } else {
                double ival;
                if (x1 - x2 > Math.abs(y2 - y1)) {
                    ival = 1D / (Math.ceil(x1 - x2) * 2D);
                } else {
                    ival = 1D / (Math.ceil(Math.abs(y2 - y1)) * 2D);
                }
                double angle = Angle.ofLine(x1, y1, x2, y2) + 90;
                double realAngle = angle;
                if (useAngle && previousTangentAngle != realAngle) {
                    angle = Angle.angleBetween(previousTangentAngle, angle);
                }
                for (double x = x1; x > x2; x -= ival) {
                    double y = y1 + dy * (x - x1) / dx;
                    if (x != x1 && Math.abs(lastX - x) > IP_THRESH && Math.abs(lastY - y) > IP_THRESH) {
                        double interX = lastX + ((x - lastX) / 2D);
                        double interY = lastY + ((y - lastY) / 2D);
                        plotter.plot(interX, interY, 1, interX, interY, interX, interY);
                    }
                    Circle c = new Circle(x, y, tangentLineLength);
                    EqLine perp = c.line(angle);
                    plotter.plot(x, y, 1, perp.x1, perp.y1, perp.x2, perp.y2);
                    lastX = x;
                    lastY = y;
                    angle = realAngle;
                    if (plotter.isDone()) {
                        break;
                    }
                }
                lastTangentAngle = angle;
            }
        }
        return lastTangentAngle;
    }

    static double IP_THRESH = 0.625;

    static final class PlotState {

        private final DoubleList xpoints = new DoubleList(100);
        private final DoubleList ypoints = new DoubleList(100);
        private double lastX;
        private double lastY;
        private double lastTangentAngle;
        private boolean hasAngle;

        PlotState last(double x, double y) {
            last(x, y, -1);
            hasAngle = false;
            return this;
        }

        PlotState last(double x, double y, double tanAngle) {
            lastX = x;
            lastY = y;
            xpoints.add(x);
            ypoints.add(y);
            lastTangentAngle = tanAngle;
            hasAngle = true;
            return this;
        }

        double lastTangentAngle() {
            return lastTangentAngle;
        }

        boolean hasTangentAngle() {
            return hasAngle;
        }

        double firstX() {
            if (xpoints.isEmpty()) {
                return 0;
            }
            return xpoints.getDouble(0);
        }

        double firstY() {
            if (ypoints.isEmpty()) {
                return 0;
            }
            return ypoints.getDouble(0);

        }

        double x() {
            return lastX;
        }

        double y() {
            return lastY;
        }
    }
}
