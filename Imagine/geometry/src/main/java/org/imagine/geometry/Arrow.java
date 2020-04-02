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
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * A shape for drawing arrows and rules without recapitulating the code to do it
 * all over the place.
 *
 * @author Tim Boudreau
 */
public class Arrow implements Shape {

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
        this(headLength, 22.5, aHead, bHead, x1, y1, x2, y2);
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

    public double length() {
        return Point2D.distance(x1, y1, x2, y2);
    }

    public EqLine toLine() {
        return new EqLine(x1, y1, x2, y2);
    }

    public void setLengthFromPoint1(double newLength) {
        setLength(newLength, false);
    }

    public void setLengthFromPoint2(double newLength) {
        setLength(newLength, true);
    }

    private void setLength(double length, boolean fromPoint2) {
        EqLine ln = toLine();
        ln.setLength(length, !fromPoint2);
        x1 = ln.x1;
        y1 = ln.y1;
        x2 = ln.x2;
        y2 = ln.y2;
    }

    public void setPointAndAngle(double x, double y, double angle, double distance) {
        Circle circ = new Circle(x, y, distance);
        x1 = x;
        y1 = y;
        circ.positionOf(angle, (xb, yb) -> {
            x2 = xb;
            y2 = yb;
        });
    }

    double[] headPoints(boolean a) {
        if (a) {
            if (!aHead) {
                return new double[0];
            }
            Circle circle = new Circle(x1, y1, headLength);
            double ang = circle.angleOf(x2, y2);
            double[] pos1 = circle.positionOf(ang + headAngleA);
            double[] pos2 = circle.positionOf(ang - headAngleA);
            return new double[]{pos1[0], pos1[1], x1, y1, pos2[0], pos2[1]};
        } else {
            if (!bHead) {
                return new double[0];
            }
            Circle circle = new Circle(x2, y2, headLength);
            double ang = circle.angleOf(x1, y1);
            double[] pos1 = circle.positionOf(ang + headAngleB);
            double[] pos2 = circle.positionOf(ang - headAngleB);
            return new double[]{pos1[0], pos1[1], x2, y2, pos2[0], pos2[1]};
        }
    }

    private <R extends Rectangle2D> R addToBounds(R result) {
        result.add(x1, y1);
        result.add(x2, y2);
        double[] hp = headPoints(true);
        for (int i = 0; i < hp.length; i += 2) {
            result.add(hp[i], hp[i + 1]);
        }
        hp = headPoints(false);
        for (int i = 0; i < hp.length; i += 2) {
            result.add(hp[i], hp[i + 1]);
        }
        return result;

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
        double[][] pts = new double[][]{
            headPoints(true),
            new double[]{x1, y1, x2, y2},
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
//            Arrow result = new Arrow(a[0], a[1], circ.centerX, circ.centerY);
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
            g.setColor(Color.WHITE);
            Arrow arr = arrow();
            g.draw(arr);
            tick++;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            repaint();
        }
    }
}
