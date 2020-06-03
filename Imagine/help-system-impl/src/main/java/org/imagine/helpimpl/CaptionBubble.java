package org.imagine.helpimpl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.uirect.MutableRectangle2D;
import static org.imagine.geometry.uirect.MutableRectangle2D.*;

/**
 *
 * @author Tim Boudreau
 */
public class CaptionBubble {

    private final Rectangle mainWindowBounds;
    private final Rectangle targetBounds;
    private final Point2D locusPoint;
    private Shape shape;

    public CaptionBubble(Rectangle mainWindowBounds, Rectangle targetBounds, Point2D locusPoint) {
        this.mainWindowBounds = mainWindowBounds;
        this.targetBounds = targetBounds;
        this.locusPoint = locusPoint;
    }

    public Shape toShape() {
        if (shape == null) {
            shape = computeShape();
        }
        return shape;
    }

    private List<Point2D> points = new ArrayList<>();

    private void points(Point2D... pts) {
        for (Point2D p : pts) {
            this.points.add(new EqPointDouble(p));
        }
    }

    private Shape computeShape() {
        Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);
        float pxPer = 64;
        float pxOff = 24;
        MutableRectangle2D rect = new MutableRectangle2D(targetBounds);
        if (rect.isEmpty()) {
            return rect;
        }
        float numCyclesX = (int) Math.floor(rect.width / pxPer);
        float numCyclesY = (int) Math.floor(rect.height / pxPer);
        float pxPerX = (float) rect.width / Math.max( 1, numCyclesX);
        float pxPerY = (float) rect.height / Math.max(1, numCyclesY);

        int nearestEdge = rect.nearestEdge(locusPoint);

        float bestCoordinate;
        switch (nearestEdge) {
            case EAST:
            case WEST:
                double minV = rect.y;
                double maxV = rect.y + rect.height;
                if (locusPoint.getY() >= minV && locusPoint.getY() <= maxV) {
                    bestCoordinate = (float) locusPoint.getY();
                } else if (locusPoint.getY() > maxV) {
                    bestCoordinate = (float) maxV - (pxPerY * 2);
                } else {
                    bestCoordinate = (float) minV + pxPerY;
                }
                break;
            default:
                double minH = rect.x;
                double maxH = rect.x + rect.width;
                if (locusPoint.getX() >= minH && locusPoint.getX() <= maxH) {
                    bestCoordinate = (float) locusPoint.getX();
                } else if (locusPoint.getX() > maxH) {
                    bestCoordinate = (float) maxH - (pxPerX * 2);
                } else {
                    bestCoordinate = (float) minH + pxPerX;
                }
        }

        path.moveTo(rect.x, rect.y);
        int cyc = 0;
        System.out.println("pxPerX " + pxPerX + " pxPerY " + pxPerY);
        // North edge
        for (float x = (float) rect.x + pxPerX; x <= rect.x + rect.width + (pxPerX / 2); x += pxPerX, cyc++) {
            if (nearestEdge == NORTH && (x - pxPerX <= bestCoordinate) && x >= bestCoordinate) {
                path.lineTo(locusPoint.getX(), locusPoint.getY());
                path.lineTo(x, rect.y);
                continue;
            }
            float midX = x - (pxPerX / 2);
            path.curveTo(midX, rect.y - pxOff, midX, rect.y + pxOff, x, rect.y);
        }
        // East edge
        for (float y = (float) rect.y + pxPerY; y < rect.y + rect.height + (pxPerY / 2); y += pxPerY, cyc++) {
            if (nearestEdge == EAST && (y - pxPerY) <= bestCoordinate && y >= bestCoordinate) {
                path.lineTo(locusPoint.getX(), locusPoint.getY());
                path.lineTo(rect.x + rect.width, y);
                continue;
            }
            float midY = y - (pxPerY / 2);
            path.curveTo(rect.x + rect.width + pxOff, midY,
                    rect.x + rect.width - pxOff, midY,
                    rect.x + rect.width, y);
        }
        // South edge
        for (float x = (float) (rect.width + rect.x); x >= rect.x - (pxPerX / 2); x -= pxPerX, cyc++) {
            if (nearestEdge == SOUTH && (x - pxPerX <= bestCoordinate && x >= bestCoordinate)) {
                path.lineTo(locusPoint.getX(), locusPoint.getY());
                path.lineTo(x, rect.y + rect.height);
                continue;
            }
            float midX = x + (pxPerX / 2);
            path.curveTo(midX, (rect.y + rect.height + pxOff), midX, (rect.y + rect.height - pxOff), x, rect.y + rect.height);
        }
        // West edge
        for (float y = (float) (rect.y + rect.height - pxPerY); y >= rect.y - pxPerY / 2; y -= pxPerY, cyc++) {
            if (nearestEdge == WEST && (y - pxPerY < bestCoordinate) && y >= bestCoordinate) {
                path.lineTo(locusPoint.getX(), locusPoint.getY());
                path.lineTo(rect.x, y);
                continue;
            }
            float midY = y + (pxPerY / 2);
            path.curveTo(rect.x - pxOff, midY, rect.x + pxOff, midY, rect.x, y);
        }
        path.closePath();

        return path;
    }

    private Shape xcomputeShape() {
        // Okay, so for each corner, we want to go a little bit down from the next line
        // from that corner, and then put the quadratic control point equidistant from that
        // corner in the opposite direction, in a line with the next edge
        //
        // For the locus point, we want to place our target point towards it, with the
        // control point toward the center of the target bounding rect the same distance

        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        MutableRectangle2D rect = new MutableRectangle2D(targetBounds);
        rect.x = targetBounds.x;
        rect.y = targetBounds.y;
        rect.width = targetBounds.width;
        rect.height = targetBounds.height;

        int edge = rect.nearestEdge(locusPoint);

        EqLine line = new EqLine();
        EqPointDouble controlPoint = new EqPointDouble();
        EqPointDouble targetPoint = new EqPointDouble();
        double curveSpan = rect.width * 0.225;
        double ptOff = rect.width * 0.1875;
        double deg = 7.5;
        double deg2 = -18.75;
//        double deg2 = 30;
        boolean first = true;
        for (int corner : new int[]{NW, NE, SE, SW, NW}) {
            Point2D pt = rect.getPoint(corner);
            Point2D next;
            int currentEdge;
            boolean horizontal;
            switch (corner) {
                case NW:
                    if (first) {
                        Circle.positionOf(deg2 + 90, rect.x, rect.y, curveSpan, path::moveTo);
                        first = false;
                    }
                    next = new EqPointDouble(rect.x + rect.width, rect.y);
                    Circle.positionOf(deg, next.getX(), next.getY(), ptOff, controlPoint);
                    Circle.positionOf(deg2 + 180, next.getX(), next.getY(), curveSpan, targetPoint);
                    currentEdge = NORTH;
                    horizontal = true;
                    break;
                case NE:
                    next = new EqPointDouble(rect.x + rect.width, rect.y + rect.height);
                    Circle.positionOf(deg + 90, next.getX(), next.getY(), ptOff, controlPoint);
                    Circle.positionOf(deg2 + 180 + 90, next.getX(), next.getY(), curveSpan, targetPoint);
                    currentEdge = EAST;
                    horizontal = false;
                    break;
                case SE:
                    next = new EqPointDouble(rect.x, rect.y + rect.height);
                    Circle.positionOf(deg + 180, next.getX(), next.getY(), ptOff, controlPoint);
                    Circle.positionOf(deg2, next.getX(), next.getY(), curveSpan, targetPoint);
                    currentEdge = SOUTH;
                    horizontal = true;
                    break;
                case SW:
                    next = new EqPointDouble(rect.x, rect.y);
                    Circle.positionOf(deg + 270, next.getX(), next.getY(), ptOff, controlPoint);
                    Circle.positionOf(deg2 + 90, next.getX(), next.getY(), curveSpan, targetPoint);
                    currentEdge = WEST;
                    horizontal = false;
                    break;
                default:
                    throw new AssertionError(corner);
            }
            points(next, targetPoint, controlPoint);
            if (edge == currentEdge) {
                rect.getEdge(currentEdge, line);
                EqPointDouble a = line.midPoint();
                EqPointDouble b = a.copy();
                if (horizontal) {
                    a.x -= ptOff / 2D;
                    b.x += ptOff / 2D;
                } else {
                    a.y -= ptOff / 2D;
                    b.y += ptOff / 2D;
                }
                if (currentEdge == SOUTH || currentEdge == WEST) {
                    path.quadTo(b.x, b.y, locusPoint.getX(), locusPoint.getY());
                    path.quadTo(locusPoint.getX(), locusPoint.getY(), a.x, a.y);
                } else {
                    path.quadTo(a.x, a.y, locusPoint.getX(), locusPoint.getY());
                    path.quadTo(locusPoint.getX(), locusPoint.getY(), b.x, b.y);
                }
            }
            path.quadTo(controlPoint.x, controlPoint.y, targetPoint.x, targetPoint.y);
            if (corner == SW) {
                path.closePath();
            }
        }
        return path;
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame("Demo");
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(new TestComp());
        jf.pack();
        jf.setVisible(true);
    }

    static class TestComp extends JComponent {

        private Point2D target;
        private Point2D rectBase;
        private final Circle circ = new Circle();
        private int rad = 12;

        public TestComp() {
            MouseAdapter adap = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.isShiftDown()) {
                        rectBase = e.getPoint();
                    } else {
                        target = e.getPoint();
                    }
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    mouseClicked(e);
                }

            };
            addMouseListener(adap);
            addMouseMotionListener(adap);
        }

        protected void paintComponent(Graphics g) {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            paintComponent((Graphics2D) g);
        }

        protected void paintComponent(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            CaptionBubble cap = new CaptionBubble(new Rectangle(0, 0, getWidth(), getHeight()), base(), targetPoint());
            Shape s = cap.toShape();
            g.setStroke(new BasicStroke(2));
            g.setColor(Color.ORANGE);
            g.fill(s);
            g.setColor(Color.WHITE);
            g.draw(s);

            circ.setCenter(rectBase());
            circ.setRadius(rad);
            g.setColor(Color.BLUE);
            g.fill(circ);
            g.setColor(Color.DARK_GRAY);
            g.draw(circ);

            circ.setCenter(targetPoint());
            g.setColor(Color.PINK);
            g.fill(circ);
            g.setColor(Color.DARK_GRAY);
            g.draw(circ);

//            circ.setRadius(7);
//            for (int i = 0; i < cap.points.size(); i++) {
//                Color c = Color.BLACK;
//                switch (i % 3) {
//                    case 0:
//                        c = Color.GREEN.darker();
//                        break;
//                    case 1:
//                        c = Color.RED.darker();
//                        break;
//                    case 2:
//                        c = Color.BLUE.darker();
//                        break;
//                }
//                g.setColor(c);
//                circ.setCenter(cap.points.get(i));
//                g.draw(circ);
//            }
//            g.setColor(Color.BLACK);
            g.draw(base());
        }

        public Dimension getPreferredSize() {
            return new Dimension(800, 800);
        }

        public Rectangle base() {
            Point2D base = rectBase();
            MutableRectangle2D bdsRect = new MutableRectangle2D(0, 0, getWidth(), getHeight());
            Rectangle rect = new Rectangle();
            int thirdX = getWidth() / 2;
            int thirdY = getHeight() / 4;
            int dim = Math.min(getWidth(), getHeight()) / 3;

            int nearestFeature = bdsRect.nearestFeature(base);
            Point2D featurePoint;
            EqLine ln = new EqLine();
            switch (nearestFeature) {
                case NORTH:
                case SOUTH:
                case EAST:
                case WEST:
                    bdsRect.getEdge(nearestFeature, ln);
                    featurePoint = ln.midPoint();
                    break;
                default:
                    featurePoint = bdsRect.getPoint(nearestFeature);
                    break;
            }
            ln.setLine(base, featurePoint);
            rect.setFrameFromCenter(base, featurePoint);
            if (rect.x < 0) {
                rect.translate(-rect.x, 0);
            }
            if (rect.y < 0) {
                rect.translate(0, -rect.y);
            }
//            if (rect.width > thirdX) {
            rect.width = thirdX;
//            }
//            if (rect.height > thirdY) {
            rect.height = thirdY;
//            }
            return rect;
        }

        public Point2D rectBase() {
            if (rectBase == null) {
                Rectangle r = new Rectangle(0, 0, getWidth() / 2, getHeight() / 2);
                return new Point2D.Double(r.getCenterX(), r.getCenterY());
            }
            return rectBase;
        }

        public Point2D targetPoint() {
            if (target == null) {
                Rectangle r = new Rectangle(0, 0, getWidth(), getHeight());
                return new EqPointDouble(r.getCenterX(), r.getCenterY());
            }
            return target;
        }
    }

}
