/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import org.imagine.geometry.util.DoubleList;
import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleQuadConsumer;
import com.mastfrog.function.DoubleSextaConsumer;
import com.mastfrog.function.DoubleTriConsumer;
import com.mastfrog.util.collections.DoubleSet;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleConsumer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.geometry.util.PooledTransform;

/**
 * A polygon which implements EnhancedShape to provide decoration-friendly
 * stuff.
 *
 * @author Tim Boudreau
 */
public final class Polygon2D extends AbstractShape implements EnhancedShape, Intersectable {

    private double[] points;
    private double minX, minY, maxX, maxY;

    public Polygon2D(double[] xpoints, double[] ypoints) {
        if (xpoints.length != ypoints.length) {
            throw new IllegalArgumentException("Arrays not same length: "
                    + xpoints.length + ", " + ypoints.length);
        }
        points = new double[xpoints.length * 2];
        if (points.length > 0) {
            minX = Double.MAX_VALUE;
            minY = Double.MAX_VALUE;
            maxX = Double.MIN_VALUE;
            maxY = Double.MIN_VALUE;
            for (int i = 0; i < xpoints.length; i++) {
                int arrOffset = i * 2;
                points[arrOffset] = xpoints[i];
                points[arrOffset + 1] = ypoints[i];
                minX = min(xpoints[i], minX);
                minY = min(ypoints[i], minY);
                maxX = max(xpoints[i], maxX);
                maxY = max(ypoints[i], maxY);
            }
        }
    }

    public Polygon2D(double... points) {
        if (points.length % 2 != 0) {
            throw new IllegalArgumentException("Points array has "
                    + "an odd number of coordinates: " + points.length);
        }
        this.points = points;
        initMinMax();
    }

    public Polygon2D(Polygon2D other) {
        this.points = (double[]) other.points.clone();
        this.minX = other.minX;
        this.minY = other.minY;
        this.maxX = other.maxX;
        this.maxY = other.maxY;
    }

    public Polygon2D(int[] xPoints, int[] yPoints, int nPoints) {
        assert nPoints <= xPoints.length && nPoints <= yPoints.length;
        points = new double[nPoints * 2];
        for (int i = 0; i < nPoints; i++) {
            int offset = i * 2;
            points[offset] = xPoints[i];
            points[offset + 1] = yPoints[i];
        }
        initMinMax();
    }

    public Polygon2D(java.awt.Polygon poly) {
        this(poly.xpoints, poly.ypoints, poly.npoints);
    }

    public Polygon2D(Shape shape) {
        this(shape, null);
    }

    public Polygon2D(Shape shape, AffineTransform xform) {
        PathIterator iter = shape.getPathIterator(xform);
        DoubleList pts = new DoubleList(30);
        double[] data = new double[6];
        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;
        maxY = Double.MIN_VALUE;

        while (!iter.isDone()) {
            int type = iter.currentSegment(data);
            int dataOffset = 0;
            switch (type) {
                case SEG_MOVETO:
                case SEG_LINETO:
                    break;
                case SEG_CUBICTO:
                    dataOffset = 4;
                    break;
                case SEG_QUADTO:
                    dataOffset = 2;
                    break;
                case SEG_CLOSE:
                    iter.next();
                    continue;
            }
            pts.add(data[dataOffset]);
            pts.add(data[dataOffset + 1]);
            minX = min(minX, data[dataOffset]);
            minY = min(minY, data[dataOffset + 1]);
            maxX = max(maxX, data[dataOffset]);
            maxY = max(maxY, data[dataOffset]);
            iter.next();
        }
        points = pts.toDoubleArray();
        if (points.length < 2) {
            minX = minY = maxX = maxY = 0;
        }
    }

    private void changed() {
        calc = null;
        clockwise = null;
    }

    public Polygon2D copy() {
        return new Polygon2D(this);
    }

    public void applyTransform(AffineTransform xform) {
        if (!xform.isIdentity()) {
            xform.transform(points, 0, points, 0, points.length / 2);
            initMinMax();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(points.length * 6)
                .append("Polygon2D(");
        return GeometryStrings.toStringCoordinates(sb, points)
                .append(')').toString();
    }

    public void deletePoint(int at) {
        int currPoints = pointCount();
        if (at < 0 || at >= currPoints) {
            throw new IndexOutOfBoundsException("Attempt to insert point "
                    + at + " of " + currPoints);
        }
        int arrayOffset = at * 2;
        double currX = points[arrayOffset];
        double currY = points[arrayOffset + 1];
        if (at == currPoints - 1) {
            points = Arrays.copyOf(points, points.length - 2);
        } else {
            // XXX something is broken here
            double[] nue = new double[points.length - 1];
            System.arraycopy(points, 0, nue, 0, arrayOffset);
            System.arraycopy(points, arrayOffset + 2, nue, arrayOffset,
                    points.length - (arrayOffset + 2));
            points = nue;
        }
        if (currX == minX || currX == maxX || currY == minY || currY == maxY) {
            initMinMax();
        }
        changed();
    }

    public void insertPoint(int at, double x, double y) {
        int currPoints = pointCount();
        if (at < 0 || at > currPoints) {
            throw new IndexOutOfBoundsException("Attempt to insert point "
                    + at + " of " + currPoints);
        }
        int arrayOffset = at * 2;
        double[] nue;
        if (at == pointCount()) {
            nue = Arrays.copyOf(points, points.length + 2);
            nue[nue.length - 2] = x;
            nue[nue.length - 1] = y;
        } else {
            nue = new double[points.length + 1];
            if (arrayOffset > 0) {
                System.arraycopy(points, 0, nue, 0, arrayOffset);
                System.arraycopy(points, arrayOffset, nue, arrayOffset + 2, points.length - arrayOffset);
                nue[arrayOffset] = x;
                nue[arrayOffset + 1] = y;
            }
        }
        points = nue;
        minX = Math.min(x, minX);
        minY = Math.min(y, minY);
        maxX = max(x, maxX);
        maxY = max(y, maxY);
        changed();
    }

    public boolean setPoint(int point, double x, double y) {
        if (point < 0 || point >= pointCount()) {
            throw new IndexOutOfBoundsException("Attempt to set point " + point
                    + " of " + pointCount());
        }
        int arrayOffset = point * 2;
        double currX = points[arrayOffset];
        double currY = points[arrayOffset + 1];
        if (x != currX || y != currY) {
            points[arrayOffset] = x;
            points[arrayOffset + 1] = y;
            minX = min(minX, x);
            minY = min(minY, y);
            maxX = max(maxX, x);
            maxY = max(maxY, y);
            changed();
            return true;
        }
        return false;
    }

    /**
     * Get the array of coordinates; note that for performance, this returns the
     * raw internal points array - do not modify it if anything may be caching
     * the bounds or other information about this shape.
     *
     * @return The points array
     */
    public double[] pointsArray() {
        return points;
    }

    /**
     * Tests if this polygon is homomorphic with respect to another.
     *
     * @param other
     * @return
     */
    public boolean isHomomorphic(Polygon2D other) {
        Polygon2D a = this.normalized();
        Polygon2D b = other.normalized();
        double[] op = b.pointsArray();
        if (a.points.length != op.length) {
            return false;
        }
        if (!GeometryUtils.isSameCoordinate(other.maxX - other.minX, maxX - minX)
                || !GeometryUtils.isSameCoordinate(other.maxY - other.minY, maxX - minY)) {
            return false;
        }
        DoubleSet dists = DoubleSet.create(points.length);
        for (int i = 0; i < a.points.length; i++) {
            dists.add(op[i] - a.points[i]);
        }
        if (dists.size() == 1) {
            return true;
        }
        for (int i = 1; i < dists.size(); i++) {
            double prev = dists.getAsDouble(i - 1);
            double curr = dists.getAsDouble(i);
            if (!GeometryUtils.isSameCoordinate(prev, curr)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isNormalized() {
        return indexOfTopLeftmostPoint() == 0 && isClockwise();
    }

    public Polygon2D normalized() {
        if (!isNormalized()) {
            Polygon2D result = new Polygon2D(this);
            result.normalize();
            return result;
        }
        return this;
    }

    public void add(Line2D line) {
        points = Arrays.copyOf(points, points.length + 4);
        points[points.length - 4] = line.getX1();
        points[points.length - 3] = line.getY1();
        points[points.length - 2] = line.getX2();
        points[points.length - 1] = line.getY2();
        minX = Math.min(Math.min(line.getX1(), line.getX2()), minX);
        minY = Math.min(Math.min(line.getY1(), line.getY2()), minY);
        maxX = Math.max(Math.max(line.getX1(), line.getX2()), maxX);
        maxY = Math.max(Math.max(line.getY1(), line.getY2()), maxY);
        changed();
    }

    public void add(Point2D point) {
        points = Arrays.copyOf(points, points.length + 2);
        points[points.length - 2] = point.getX();
        points[points.length - 1] = point.getY();
        minX = Math.min(minX, point.getX());
        minY = Math.min(minY, point.getY());
        maxX = Math.min(maxX, point.getX());
        maxY = Math.min(maxY, point.getY());
        clockwise = null;
        changed();
    }

    public void add(Polygon2D other) {
        double[] nue = Arrays.copyOf(points, points.length + other.points.length);
        System.arraycopy(other.points, 0, nue, points.length, other.points.length);
        points = nue;
        clockwise = null;
        changed();
        for (int i = 0; i < other.points.length; i += 2) {
            minX = min(other.points[i], minX);
            maxX = max(other.points[i], maxX);
            minY = min(other.points[i + 1], minY);
            maxY = max(other.points[i + 1], maxY);
        }
    }

    private Boolean clockwise;

    private static RotationDirection directionOf(double ax, double ay, double sx, double sy, double bx, double by) {
        return new CornerAngle(ax, ay, sx, sy, bx, by).direction();
    }

    private boolean isClockwise() {
        if (clockwise != null) {
            return clockwise;
        }
        int pc = pointCount();
        int max = pc * 2;
        int cwCount = 0;
        int ccwCount = 0;
        for (int i = 4; i < max; i += 2) {
            RotationDirection dir = directionOf(points[i - 4], points[i - 3],
                    points[i - 2], points[i - 1], points[i], points[i + 1]);
            switch (dir) {
                case CLOCKWISE:
                    cwCount++;
                    break;
                case COUNTER_CLOCKWISE:
                    ccwCount++;
                    break;
            }
            if (i == max - 2) {
                dir = directionOf(points[i - 2], points[i - 1], points[i],
                        points[i + 1], points[0], points[1]);
                switch (dir) {
                    case CLOCKWISE:
                        cwCount++;
                        break;
                    case COUNTER_CLOCKWISE:
                        ccwCount++;
                        break;
                }
            }
        }
        boolean result = cwCount > ccwCount;
        clockwise = result;
        return result;
    }

    // Since we are using these to approximate quadratic and cubic
    // curves, provide a way to synthesize an angle that approximates
    // that of the curve represented
    CornerAngle initialAngle(double precedingX, double precedingY) {
        if (points.length < 4) {
            return new CornerAngle(0, 0);
        }
        return new CornerAngle(precedingX, precedingY, points[0],
                points[1], points[2], points[3]);
    }

    CornerAngle endAngle(double followingX, double followingY) {
        if (points.length < 4) {
            return new CornerAngle(0, 0);
        }
        int last = points.length;
        return new CornerAngle(points[last - 4], points[last - 3], points[last - 2],
                points[last - 1], followingX, followingY);
    }

    static void reversePointsInPlace(double[] points) {
        double[] temp = (double[]) points.clone();
        for (int i = 0, i2 = points.length - 2; i < points.length; i += 2, i2 -= 2) {
            points[i] = temp[i2];
            points[i + 1] = temp[i2 + 1];
        }
    }

    @Override
    public boolean normalize() {
        int ptix = indexOfTopLeftmostPoint();
        boolean isClockwise = isClockwise();
        if (ptix > 0 || !isClockwise) {
            if (ptix > 0) {
                int coordOffset = ptix * 2;
                double[] nue = new double[points.length];
                System.arraycopy(points, coordOffset, nue, 0, points.length - coordOffset);
                System.arraycopy(points, 0, nue, points.length - coordOffset, coordOffset);
                if (!isClockwise()) {
                    reversePointsInPlace(nue);
                    isClockwise = true;
                }
                points = nue;
            } else {
                reversePointsInPlace(points);
                isClockwise = true;
            }
            changed();
            return true;
        }
        return false;
    }

    void initMinMax() {
        switch (points.length) {
            case 0:
                minX = minY = maxX = maxY = 0;
                break;
            case 1:
                throw new AssertionError("Odd number of coordinates: 1");
            case 2:
                minX = maxX = points[0];
                maxX = maxY = points[1];
                break;
            default:
                minX = minY = Double.MAX_VALUE;
                maxX = maxY = Double.MIN_VALUE;
                for (int i = 0; i < points.length; i += 2) {
                    double px = points[i];
                    double py = points[i + 1];
                    minX = min(px, minX);
                    maxX = max(px, maxX);
                    minY = min(py, minY);
                    maxY = max(py, maxY);
                }
                break;
        }
        changed();
    }

    @Override
    public int pointCount() {
        return points.length / 2;
    }

    @Override
    public Point2D point(int index) {
        int arrOffset = index * 2;
        return new Point2D.Double(points[arrOffset], points[arrOffset + 1]);
    }

    @Override
    public <T extends Rectangle2D> T addToBounds(T into) {
        if (into.isEmpty()) {
            into.setFrameFromDiagonal(minX, minY, maxX, maxY);
        } else {
            into.add(minX, minY);
            into.add(maxX, maxY);
        }
        return into;
    }

    @Override
    public void visitPoints(DoubleBiConsumer consumer) {
        for (int i = 0; i < points.length; i += 2) {
            consumer.accept(points[i], points[i + 1]);
        }
    }

    @Override
    public void visitAngles(DoubleConsumer angleConsumer) {
        Circle circ = new Circle(0, 0, 1);
        for (int i = 2; i < points.length; i += 2) {
            int prev = i - 2;
            int next = i == points.length - 2 ? 0 : i + 2;
            double x1 = points[prev];
            double y1 = points[prev + 1];
            double xShared = points[i];
            double yShared = points[i + 1];
            double x3 = points[next];
            double y3 = points[next + 1];

            circ.setCenter(xShared, yShared);
            double angle1 = circ.angleOf(x3, y3);
            double otherAngle = circ.angleOf(x1, y1);
            double realAngle = Angle.normalize(otherAngle - angle1);
            angleConsumer.accept(realAngle);
        }
    }

    @Override
    public void visitAnglesAndPoints(DoubleTriConsumer angleConsumer) {
        Circle circ = new Circle(0, 0, 1);
        for (int i = 2; i < points.length; i += 2) {
            int prev = i - 2;
            int next = i == points.length - 2 ? 0 : i + 2;
            double x1 = points[prev];
            double y1 = points[prev + 1];
            double xShared = points[i];
            double yShared = points[i + 1];
            double x3 = points[next];
            double y3 = points[next + 1];
            circ.setCenter(xShared, yShared);
            double angle1 = circ.angleOf(x3, y3);
            double otherAngle = circ.angleOf(x1, y1);
            double realAngle = Angle.normalize(otherAngle - angle1);
            angleConsumer.accept(realAngle, xShared, yShared);
        }
    }

    @Override
    public void visitAdjoiningLines(DoubleSextaConsumer sex) {
        for (int i = 2; i < points.length + 4; i += 4) {
            int prev = (i - 2) % points.length;
            int ix = i % points.length;
            int next = (i + 2) % points.length;
            double x1 = points[prev];
            double y1 = points[prev + 1];
            double xShared = points[ix];
            double yShared = points[ix + 1];
            double x3 = points[next];
            double y3 = points[next + 1];
            sex.accept(x1, y1, xShared, yShared, x3, y3);
        }
    }

    /**
     * Polygon's containment test in the case of intersecting lines may be a
     * little different than that of Path2D -&gt; Area, so provide a simple way
     * to convert.
     *
     * @return A path
     */
    public Path2D.Double toPath() {
        Path2D.Double result = new Path2D.Double(PathIterator.WIND_EVEN_ODD);
        for (int i = 0; i < points.length; i += 2) {
            if (i == 0) {
                result.moveTo(points[0], points[1]);
            } else {
                result.lineTo(points[i], points[i + 1]);
            }
        }
        return result;
    }

    private PolyCalc calc;

    @Override
    public boolean contains(double tx, double ty) {
        if ((tx < minX && ty < minY) || (tx > maxX && ty > maxY)) {
            return false;
        }
        if (true) {
            if (calc == null) {
                calc = new PolyCalc();
            }
            return calc.pointInPolygon(tx, ty);
        }
        double testY = ty < minY ? maxY + 1 : minY - 1;
        int count = 0;

        // Line2D.linesIntersect will give leading false positive for
        // some points where the y coordinate EXACTLY MATCHES
        // one of the points on the polygon; so we tilt the tested
        // line VERY fractionally to avoid having leading few stripes across
        // the polygon which test as not being part of it, and
        // leading few out to the bounding box that do
        double workingTxFirst = tx + GeometryUtils.INTERSECTION_FUDGE_FACTOR;
        double workingTxSecond = tx - GeometryUtils.INTERSECTION_FUDGE_FACTOR;

        for (int i = 0; i < points.length; i += 2) {
            int next = i == points.length - 2 ? 0 : i + 2;
            double x = points[i];
            double y = points[i + 1];
            // compensate for -0.0 by adding 0.0
            if (tx + 0.0 == x + 0.0 && ty + 0.0 == y + 0.0) {
                return true;
            }
            double nx = points[next];
            double ny = points[next + 1];
            if (nx + 0.0 == tx + 0.0 && ny + 0.0 == tx + 0.0) {
                return true;
            }
            boolean isect = Line2D.linesIntersect(workingTxFirst, ty,
                    workingTxSecond, testY, x, y, nx, ny);
            if (isect) {
                count++;
            }
        }
        return count % 2 == 1;
    }

    public Polygon2D reverse() {
        reversePointsInPlace(points);
        return this;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        byte[] types = new byte[(points.length / 2) + 1];
        Arrays.fill(types, (byte) SEG_LINETO);
        types[0] = (byte) SEG_MOVETO;
        types[types.length - 1] = (byte) SEG_CLOSE;
        return new ArrayPathIteratorDouble(PathIterator.WIND_EVEN_ODD,
                types, points, at);
    }

    public static void main(String[] args) {

        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(EXIT_ON_CLOSE);
        jf.setLayout(new BorderLayout());
        PolyComp comp = new PolyComp(poly(new Random(10291029)));
        jf.add(comp, BorderLayout.CENTER);
        jf.pack();
        jf.setVisible(true);
    }

    static Polygon2D poly(Random rnd) {
        double[] pts = new double[]{
            200, 20.5,
            180, 50,
            200.232, 200,
            150, 150.77773,
            12, 200,
            50, 50,
            60, 45,
            50, 40,
            3, 80,
            10, 60,
            10, 10,
            100, 10,
            100, 100,};
        Polygon2D p = new Polygon2D(pts);
        return p;
    }

    @Override
    public void visitLines(DoubleQuadConsumer consumer, boolean includeClose) {
        for (int i = 0; i < points.length; i += 2) {
            int next = i == points.length - 2 ? 0 : i + 2;
            consumer.accept(points[i], points[i + 1], points[next], points[next + 1]);
        }
        if (includeClose && points.length > 6) {
            consumer.accept(points[points.length - 2], points[points.length - 1], points[0], points[1]);
        }
    }

    private static final class PolyComp extends JComponent {

        private Shape shape;
        private int margin = 40;

        private AffineTransform xform = AffineTransform.getScaleInstance(2, 2);

        PolyComp(Polygon2D shape) {
//            this.shape = new MinimalAggregateShapeDouble(shape);
            this.shape = shape;
//            this.shape = new Triangle2(shape.minX, shape.minY, shape.maxX, shape.maxY, shape.points[6], shape.points[7]);
//            this.shape = new Rhombus(90, 100, 30, 90, 0);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    double px = e.getX() - (margin * xform.getScaleX());
                    double py = e.getY() - (margin * xform.getScaleY());
                    double x = px * (1D / xform.getScaleX());
                    double y = py * (1D / xform.getScaleY());
                    System.out.println("CONTAINS " + x + ", " + y + " : " + shape.contains(x, y));
                }
            });
        }

        public Dimension getPreferredSize() {
            Rectangle r = shape.getBounds();
            r = xform.createTransformedShape(shape).getBounds();
            return new Dimension(r.width + (margin * 2), r.height + (margin * 2));
        }

        private static final DecimalFormat DEC = new DecimalFormat("0.00");

        @Override
        protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr;
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.transform(xform);
            g.translate(margin, margin);
            Rectangle bds = shape.getBounds();
//            bds.x += margin;
//            bds.y += margin;
            g.setColor(Color.BLUE);
            g.fill(bds);

            g.setColor(Color.ORANGE);
            g.fill(shape);
            g.setColor(new Color(128, 128, 255));
            g.setStroke(new BasicStroke(2));
            g.draw(shape);

            g.setFont(g.getFont().deriveFont(PooledTransform.getScaleInstance(0.6, 0.6, gr)));

            Circle circ = new Circle(0, 0, 3);
            g.setColor(Color.WHITE);

            g.setStroke(new BasicStroke(0.35F));

            int[] lc = new int[1];
//            g.setStroke(new BasicStroke(1.5F));
            ((EnhancedShape) shape).visitLines((x1, y1, x2, y2) -> {
                lc[0]++;
                scratchLine.setLine(x1, y1, x2, y2);
                g.setColor(Color.WHITE);
                g.draw(scratchLine);
            });

            int[] ptt = new int[1];
            ((EnhancedShape) shape).visitPoints((x, y) -> {
//                System.out.println("P-" + ptt[0]++ + ". " + x + ", " + y);
            });

            g.setStroke(new BasicStroke(0.35F));
            int[] adjl = new int[1];
            ((EnhancedShape) shape).visitAdjoiningLines((x1, y1, xs, ys, x2, y2) -> {
//                System.out.println("ADJ " + adjl[0] + ". " + x1 + ", " + y1 + ", " + xs + ", " + ys + ", " + x2 + ", " + y2);
                adjl[0]++;
                g.setStroke(new BasicStroke(3));
                scratchLine.setLine(x1, y1, xs, ys);
                g.setColor(Color.RED);
                g.draw(scratchLine);
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.MAGENTA);
                scratchLine.setLine(xs, ys, x2, y2);
            });

            g.setStroke(new BasicStroke(0.35F));

            int[] ac = new int[1];
            double[] asum = new double[1];
//            System.out.println("ANGLES: ");
            ((EnhancedShape) shape).visitAnglesWithOffsets((angle, absX, absY, px, py) -> {
//                System.out.println(ac[0] + ". " + angle + " @ " + absX + ", " + absY);

                ac[0]++;
                double norm = 180D - angle;
                asum[0] += norm;
                g.setColor(Color.BLACK);
                scratchLine.setLine(absX, absY, px, py);
                g.draw(scratchLine);
                g.setColor(Color.WHITE);
                circ.setCenter(absX, absY);
                g.draw(circ);
                String ang = DEC.format(angle) + "\u00B0";
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(ang);
                float tpx = (float) (px - (w / 2));
                float tpy = (float) py + ((fm.getMaxAscent()) / 2);
                g.drawString(ang, tpx, tpy);

            }, -35);

            int[] lns = new int[1];
            ((EnhancedShape) shape).visitLines((x1, y1, x2, y2) -> {
//                System.out.println(lns[0] + ". " + x1 + ", " + y1 + ", " + x2 + ", " + y2);
                lns[0]++;
                g.setColor(Color.WHITE);
                scratchLine.setLine(x1, y1, x2, y2);
                g.draw(scratchLine);
            });

//            System.out.println("POINTS " + ((EnhancedShape) shape).pointCount());
//            System.out.println("ANGLES " + ac[0]);
//            System.out.println("LINES " + lc[0]);
//            System.out.println("ADJ-LINES " + adjl[0]);
//            System.out.println("ANGLE SUM " + asum[0]);
//            bds.x += margin;
//            bds.y += margin;
//            bds = shape.getBounds();
//            System.out.println("iter bounds " + bds);
            for (int x = 0; x < bds.width; x++) {
                for (int y = 0; y < bds.height; y++) {
                    try {
                        if (shape.contains(x + bds.x, y + bds.y)) {
//                        g.setColor(Color.GREEN);
                            g.setColor(new Color(128, 128, 255, 100));
                            g.fillRect(x, y, 1, 1);
                        } else {
//                            g.setColor(new Color(240, 240, 240, 80));
////                        g.setColor(Color.WHITE);
//                            g.fillRect(x + margin, y+margin, 1, 1);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
            ((EnhancedShape) shape).visitAnglesWithArcs((int angleIndex, double angle1, double x1, double y1,
                    double angle2, double x2, double y2,
                    Rectangle2D bounds, double apexX, double apexY,
                    double offsetX, double offsetY, double midAngle) -> {
//                g.setColor(new Color(200, 200, 150, 180));
//                g.fill(bounds);
                g.setColor(Color.BLACK);

                g.setStroke(new BasicStroke(1F / (float) xform.getScaleX()));
                ang.setCenter(apexX, apexY);
                ang.setRadius(12 * (1D / xform.getScaleX()));
                ang.setAngle(angle1);
                ang.setExtent(angle2 - angle1);
//                System.out.println(ang);
                g.draw(ang);
            }, 4 * (1D / xform.getScaleX()));
        }
        private static final Line2D.Double scratchLine = new Line2D.Double();
        private static final PieWedge ang = new PieWedge(0, 0, 1, 1, 1);
    }

    class PolyCalc {

        // http://alienryderflex.com/polygon/
        private final double[] constant;
        private final double[] multiple;

        PolyCalc() {
            int half = points.length / 2;
            constant = new double[half];
            multiple = new double[half];
            int j = half - 1;
            for (int i = 0; i < half; i++) {
                int iOff = i * 2;
                int jOff = j * 2;
                double polyXi = points[iOff];
                double polyYi = points[iOff + 1];
                double polyXj = points[jOff];
                double polyYj = points[jOff + 1];
                if (polyYj == polyYi) {
                    constant[i] = polyXi;
                    multiple[i] = 0;
                } else {
                    constant[i] = polyXi - (polyYi * polyXj)
                            / (polyYj - polyYi) + (polyYi * polyXi)
                            / (polyYj - polyYi);
                    multiple[i] = (polyXj - polyXi) / (polyYj - polyYi);
                }
                j = i;
            }
        }

        boolean pointInPolygon(double x, double y) {

            int i, j = (points.length / 2) - 1;
            boolean oddNodes = false;

            for (i = 0; i < points.length / 2; i++) {
                double polyYi = points[(i * 2) + 1];
                double polyYj = points[(j * 2) + 1];
                if ((polyYi < y && polyYj >= y
                        || polyYj < y && polyYi >= y)) {
                    oddNodes ^= (y * multiple[i] + constant[i] < x);
                }
                j = i;
            }

            return oddNodes;
        }
    }
}
