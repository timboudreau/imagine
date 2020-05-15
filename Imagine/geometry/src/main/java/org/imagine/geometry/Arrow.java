package org.imagine.geometry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import org.imagine.geometry.util.GeometryStrings;

/**
 * A shape for drawing arrows and rules without recapitulating the code to do it
 * all over the place.
 *
 * @author Tim Boudreau
 */
public class Arrow implements Shape {

    public static final double DEFAULT_HEAD_ANGLE = 22.5;
    public double headLength;
    public double headAngleA;
    public double headAngleB;
    public boolean aHead;
    public boolean bHead;
    public double x1;
    public double y1;
    public double x2;
    public double y2;

    public Arrow() {
        this(0D, 0D, 1D, 1D);
    }

    public Arrow(double x1, double y1, double x2, double y2) {
        this(5D, x1, y1, x2, y2);
    }

    public Arrow(double headLength, double x1, double y1, double x2, double y2) {
        this(headLength, true, true, x1, y1, x2, y2);
    }

    public Arrow(double headLength, boolean aHead, boolean bHead, double x1, double y1, double x2, double y2) {
        this(headLength, DEFAULT_HEAD_ANGLE, aHead, bHead, x1, y1, x2, y2);
    }

    public Arrow(double headLength, double headAngle, boolean aHead, boolean bHead, double x1, double y1, double x2, double y2) {
        this(headLength, headAngle, aHead, headAngle, bHead, x1, y1, x2, y2);
    }

    public Arrow(double headLength, double headAngleA, boolean aHead, double headAngleB, boolean bHead, double x1, double y1, double x2, double y2) {
        this.headLength = headLength;
        this.headAngleA = headAngleA;
        this.headAngleB = headAngleB;
        this.aHead = aHead;
        this.bHead = bHead;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Get the length of this arrow.
     *
     * @return the length
     */
    public double length() {
        return Point2D.distance(x1, y1, x2, y2);
    }

    /**
     * Get this arrow's line as a line.
     *
     * @return A line
     */
    public EqLine toLine() {
        return new EqLine(x1, y1, x2, y2);
    }

    /**
     * Set the length of this arrow, retaining its current angle and potentially
     * changing the coordinates of the second point.
     *
     * @param newLength The new length
     * @return this
     */
    public Arrow setLengthFromPoint1(double newLength) {
        return setLength(newLength, false);
    }

    /**
     * Set the length of this arrow, retaining its current angle and potentially
     * changing the coordinates of the
     * <i>first</i> point.
     *
     * @param newLength The new length
     * @return this
     */
    public Arrow setLengthFromPoint2(double newLength) {
        return setLength(newLength, true);
    }

    /**
     * Get the angle of this line in degrees, from first point to the second
     * point, in a coordinate space where 0\u00B0 equals 12:00.
     *
     * @return The angle
     */
    public double angle() {
        return Circle.angleOf(x1, y1, x2, y2);
    }

    private Arrow setLength(double length, boolean fromPoint2) {
        EqLine ln = toLine();
        ln.setLength(length, !fromPoint2);
        x1 = ln.x1;
        y1 = ln.y1;
        x2 = ln.x2;
        y2 = ln.y2;
        return this;
    }

    /**
     * Set the starting point, angle and length.
     *
     * @param start The starting point
     * @param angle The angle in degrees (0\u00B0 degrees equals 12:00)
     * @param distance The length along the angle from the starting point
     * @return this
     */
    public Arrow setPointAndAngle(Point2D start, double angle, double distance) {
        return setPointAndAngle(start.getX(), start.getY(), angle, distance);
    }

    /**
     * Set the starting point, angle and length.
     *
     * @param x The starting x coordinate
     * @param y The starting y coordinate
     * @param angle The angle in degrees (0\u00B0 degrees equals 12:00)
     * @param distance The length along the angle from the starting point
     * @return this
     */
    public Arrow setPointAndAngle(double x, double y, double angle, double distance) {
        Circle circ = new Circle(x, y, distance);
        x1 = x;
        y1 = y;
        circ.positionOf(angle, (xb, yb) -> {
            x2 = xb;
            y2 = yb;
        });
        return this;
    }

    private double headOffset = 2;

    public double headOffset() {
        return headOffset;
    }

    /**
     * When painting at low resolution (for example, when drawing images for
     * system cursors), it may be needed to shift the position of the arrow head
     * away from the exact position of the line endpoints in order to have
     * stroking them with a wide and then narrow stroke not result in bumps that
     * correspond to the end of the line, depending on the stroke cap style.
     *
     * The default value is 1.5.
     *
     * @param val The head offset
     * @return The head offset
     */
    public Arrow setHeadOffset(double val) {
        this.headOffset = val;
        return this;
    }

    double[] headPoints(boolean a) {
        if (a) {
            if (!aHead) {
                return new double[0];
            }
            double ang = Circle.angleOf(x1, y1, x2, y2);
            double[] result = new double[6];
            Circle.positionOf(ang + headAngleA, x1, y1, headLength, result, 0);
            Circle.positionOf(ang - headAngleA, x1, y1, headLength, result, 4);

            Circle.positionOf(Angle.opposite(ang), x1, y1, headOffset, (nx, ny) -> {
                result[2] = nx;
                result[3] = ny;
            });
            return result;
        } else {
            if (!bHead) {
                return new double[0];
            }
            double ang = Circle.angleOf(x2, y2, x1, y1);
            double[] result = new double[6];
            Circle.positionOf(ang + headAngleB, x2, y2, headLength, result, 0);
            Circle.positionOf(ang - headAngleB, x2, y2, headLength, result, 4);
            Circle.positionOf(ang, x2, y2, headOffset, (nx, ny) -> {
                result[2] = nx;
                result[3] = ny;
            });
            return result;
        }
    }

    public <R extends Rectangle2D> R addToBounds(R result) {
        if (result.isEmpty()) {
            result.setFrame(x1, y1, 1, 1);
        } else {
            result.add(x1, y1);
        }
        result.add(x2, y2);
        double[] hp = headPoints(true);
        for (int i = 0; i < hp.length; i += 2) {
            result.add(hp[i] + headLength, hp[i + 1] + headLength);
            result.add(hp[i] - headLength, hp[i + 1] - headLength);
        }
        hp = headPoints(false);
        for (int i = 0; i < hp.length; i += 2) {
            result.add(hp[i] + headLength, hp[i + 1] + headLength);
            result.add(hp[i] + headLength, hp[i + 1] + headLength);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Arrow("
                + GeometryStrings.lineToString(x1, y1, x2, y2)
                + " aa " + GeometryStrings.toString(headAngleA)
                + " ab " + GeometryStrings.toString(headAngleB)
                + " hl " + GeometryStrings.toString(headLength)
                + " ang " + Circle.angleOf(x1, y1, x2, y2)
                + ")";
    }

    @Override
    public Rectangle getBounds() {
        return addToBounds(new Rectangle());
    }

    @Override
    public Rectangle2D getBounds2D() {
        return addToBounds(new Rectangle2D.Double());
    }

    @Override
    public boolean contains(double x, double y) {
        return false;
    }

    @Override
    public boolean contains(Point2D p) {
        return false;
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return false;
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return false;
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return false;
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return false;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        double[] linePoints = new double[]{x1, y1, x2, y2};

//        double ang = angle();
//        if (aHead || bHead) {
//            EqLine ln = toLine();
//            EqPointDouble mid = ln.midPoint();
//            double rad = (ln.length() / 2) - 1.5;
//            if (aHead) {
//                Circle.positionOf(Angle.opposite(ang), mid.x, mid.y, rad, (newX1, newY1) -> {
//                    linePoints[0] = newX1;
//                    linePoints[1] = newY1;
//                });
//                System.out.println("do ahead");
//            }
//            if (bHead) {
//                Circle.positionOf(ang, mid.x, mid.y, length() - rad, (newX2, newY2) -> {
//                    linePoints[2] = newX2;
//                    linePoints[3] = newY2;
//                    System.out.println("do bhead");
//                });
//            }
//        }
        double[][] pts = new double[][]{
            headPoints(true),
            linePoints,
            headPoints(false)
        };
        if (at != null && !at.isIdentity()) {
            for (int i = 0; i < pts.length; i++) {
                at.transform(pts[i], 0, pts[i], 0, pts[i].length / 2);
            }
        }
        return new PI(pts);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    /**
     * Set the points of this line.
     *
     * @param pt1 The first point
     * @param pt2 The second point
     * @return this
     */
    public Arrow setPoints(Point2D pt1, Point2D pt2) {
        return setPoints(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());
    }

    /**
     * Set the line this arrow spans.
     *
     * @param line A line
     * @return this
     */
    public Arrow setLine(Line2D line) {
        return setPoints(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    /**
     * Set the points of the line this arrow spans.
     *
     * @param x1 The first x coordinate
     * @param y1 The first y coordinate
     * @param x2 The second x coordinate
     * @param y2 The second y coordinate
     * @return this
     */
    public Arrow setPoints(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        return this;
    }

    static class PI implements PathIterator {

        private final double[][] pts;
        private int outerCursor;
        private int innerCursor;

        PI(double[][] points) {
            this.pts = points;
            while (pts[outerCursor].length == 0) {
                outerCursor++;
            }
        }

        @Override
        public int getWindingRule() {
            return PathIterator.WIND_NON_ZERO;
        }

        @Override
        public boolean isDone() {
            return outerCursor >= pts.length
                    || (outerCursor == pts.length - 1
                    && innerCursor >= pts[outerCursor].length);
        }

        @Override
        public void next() {
            innerCursor += 2;
            if (innerCursor >= pts[outerCursor].length) {
                innerCursor = 0;
                outerCursor++;
                if (outerCursor < pts.length && innerCursor >= pts[outerCursor].length) {
                    outerCursor++;
                }
            }
        }

        @Override
        public int currentSegment(float[] coords) {
            double[] points = pts[outerCursor];
            coords[0] = Math.round(points[innerCursor]);
            coords[1] = Math.round(points[innerCursor + 1]);
            return innerCursor == 0 ? SEG_MOVETO : SEG_LINETO;
        }

        @Override
        public int currentSegment(double[] coords) {
            double[] points = pts[outerCursor];
            coords[0] = points[innerCursor];
            coords[1] = points[innerCursor + 1];
            return innerCursor == 0 ? SEG_MOVETO : SEG_LINETO;
        }
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setTitle("Arrow");
        ArrowComp comp = new ArrowComp();
        jf.setContentPane(comp);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.pack();
        jf.setVisible(true);
    }

    static final class ArrowComp extends JComponent implements ActionListener {

        private int tick;

        private final Timer timer = new Timer(150, this);

        @Override
        public void addNotify() {
            super.addNotify();
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        private Arrow arrow() {
            double angle = tick % 360;
            double w = getWidth() / 2;
            double h = getHeight() / 2;

            Circle circ = new Circle(w, h, Math.min(w - 10, h - 10));
            double[] a = circ.positionOf(angle);
            double[] b = circ.positionOf(angle + 180);
            Arrow result = new Arrow(a[0], a[1], b[0], b[1]);
            result.aHead = true;
            result.bHead = true;
            result.headAngleB = 335;
            return result;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(300, 300);
        }

        @Override
        protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr;
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(100, 100, 255, 128));
            Arrow arr = arrow();
            g.fill(arr.getBounds2D());
            g.setColor(new Color(255, 100, 100, 128));
            g.fill(arr.toLine().getBounds2D());

            g.setColor(Color.WHITE);
            g.draw(arr);
            tick++;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            repaint();
        }
    }
}
