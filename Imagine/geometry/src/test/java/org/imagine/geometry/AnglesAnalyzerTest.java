/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.collections.IntMap.IntMapConsumer;
import com.sun.javafx.geom.PathIterator;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.geometry.util.function.Int;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class AnglesAnalyzerTest {

    @Test
    public void testRectangle() throws Throwable {
        ShapeInfo info = analyze(xsReversed());
        info.show(info.forPoints.get(1));
        analyze(rectShifted()).show();
        Thread.sleep(250000);
    }

    private Shape rect() {
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_NON_ZERO);
        path.moveTo(10, 10);
        path.lineTo(20, 10);
        path.lineTo(20, 20);
        path.lineTo(10, 20);
        path.closePath();
        return path;
    }

    private Shape rectShifted() {
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_NON_ZERO);
        path.moveTo(20, 10);
        path.lineTo(20, 20);
        path.lineTo(10, 20);
        path.lineTo(10, 10);
        path.closePath();
        return path;
    }

    private Shape rectShifted2() {
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_NON_ZERO);
        path.moveTo(20, 20);
        path.lineTo(10, 20);
        path.lineTo(10, 10);
        path.lineTo(20, 10);
        path.closePath();
        return path;
    }

    private Shape rectReversed() {
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_NON_ZERO);
        path.moveTo(10, 10);
        path.lineTo(10, 20);
        path.lineTo(20, 20);
        path.lineTo(20, 10);
        path.closePath();
        return path;
    }

    private static double[] XS = new double[]{
        10, 10,
        20, 20,
        30, 10,
        40, 20,
        50, 10,
        50, 20,
        40, 10,
        30, 20,
        20, 10
    };

    static {
        for (int i = 0; i < XS.length; i++) {
            XS[i] += 40;
        }
    }

    private Shape xs() {
        return xs(0);
    }

    private Shape xs(int offset) {
        return shape(XS, offset);
    }

    private Shape xsReversed() {
        return xsReversed(0);
    }

    private Shape xsReversed(int offset) {
        return shape(reversed(XS), offset);
    }

    private double[] reversed(double[] data) {
        data = (double[]) data.clone();
        Polygon2D.reversePointsInPlace(data);
        return data;
    }

    private Shape shape(double[] points, int offset) {
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_NON_ZERO);
        for (int i = offset * 2; i < points.length; i += 2) {
            if (i == offset * 2) {
                path.moveTo(points[i], points[i + 1]);
            } else {
                path.lineTo(points[i], points[i + 1]);
            }
            if (offset == 0 && i == points.length - 2) {
                path.closePath();
            }
        }
        if (offset > 0) {
            for (int i = 0; i < offset * 2; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
            path.closePath();
        }
        return path;
    }

    private ShapeInfo analyze(Shape shape) {
        AnglesAnalyzer ana = new AnglesAnalyzer();
        RotationDirection dir = ana.analyzeShape(shape, null);
        ShapeInfo info = new ShapeInfo(dir, shape, ana);
        ana.visitAll((int pointIndex, double x, double y, CornerAngle angles, int intersections, RotationDirection subpathRotationDirection, Polygon2D approximate) -> {
            System.out.println("VISIT " + pointIndex + " " + GeometryUtils.toShortString(x, y) + " " + angles.toShortString() + " is " + intersections
                    + " approx size " + approximate.pointCount() + " sub " + subpathRotationDirection);
            info.add(pointIndex, new PointInfo(pointIndex, x, y, angles, intersections, subpathRotationDirection, approximate));
        });
        return info;
    }

    static final class ShapeInfo {

        private final RotationDirection dir;
        private final Shape shape;
        private final AnglesAnalyzer ana;
        private final IntMap<PointInfo> forPoints = CollectionUtils.intMap();

        public ShapeInfo(RotationDirection dir, Shape shape, AnglesAnalyzer ana) {
            this.dir = dir;
            this.shape = shape;
            this.ana = ana;
        }

        void add(int point, PointInfo info) {
            assertNull(forPoints.put(point, info), "Got point " + point + " more than once: " + info);
        }

        void show(PointInfo... highlight) {
            new ShapeComp(this).showComp(highlight);
        }
    }

    private static final class PointInfo {

        private final int pointIndex;
        private final double x;
        private final double y;
        private final Sector angles;
        private final int intersections;
        private final RotationDirection subpathRotationDirection;
        private final Polygon2D approximate;

        PointInfo(int pointIndex, double x, double y, Sector angles, int intersections, RotationDirection subpathRotationDirection, Polygon2D approximate) {
            this.pointIndex = pointIndex;
            this.x = x;
            this.y = y;
            this.angles = angles;
            this.intersections = intersections;
            this.subpathRotationDirection = subpathRotationDirection;
            this.approximate = approximate;
        }

        @Override
        public String toString() {
            return pointIndex + ". "
                    + GeometryUtils.toShortString(x, y)
                    + " " + angles.toShortString()
                    + " " + subpathRotationDirection
                    + " " + (intersections == 0 ? ""
                            : " i" + Integer.toString(intersections));

        }
    }

    static class ShapeComp extends JComponent {

        private final ShapeInfo info;
        private final int margin = 0;
        private float scale = 8;
        private final PieWedge wedge = new PieWedge();
        private final Circle circ = new Circle();

        ShapeComp(ShapeInfo info) {
            this.info = info;
        }

        @Override
        public Dimension getPreferredSize() {
            Rectangle bds = info.shape.getBounds();
            bds.width += margin * 2;
            bds.height += margin * 2;
            Dimension res = new Dimension(bds.x + bds.width,
                    bds.y + bds.height);
            res.width += 50;
            res.height += 50;
            res.width *= scale;
            res.height *= scale;
            return res;
        }

        private Set<PointInfo> highlight = new HashSet<>();

        public void showComp(PointInfo... highlight) {
            this.highlight.addAll(Arrays.asList(highlight));
            JFrame jf = new JFrame();
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jf.getContentPane().setLayout(new BorderLayout());
            jf.getContentPane().add(this, BorderLayout.CENTER);
            jf.pack();
            jf.setVisible(true);
        }

        @Override
        public void paintComponent(Graphics g) {
            paintComponent((Graphics2D) g);
        }

        public void paintComponent(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.scale(scale, scale);
            float invScale = 1F / scale;
            g.translate(margin, margin);
            g.setColor(new Color(200, 200, 40));

            g.fill(info.shape);

            BasicStroke baseStroke = new BasicStroke(invScale);
            g.setStroke(baseStroke);
            g.setColor(new Color(40, 40, 200));
            g.draw(info.shape);

            circ.setRadius(5 * (1D / scale));

            Font f = g.getFont();
            g.setFont(f.deriveFont(AffineTransform.getScaleInstance(invScale, invScale)));
            Int txtX = Int.of(20);
            info.forPoints.forEach((IntMapConsumer<PointInfo>) (key, pi) -> {
                circ.setCenter(pi.x, pi.y);
                if (highlight.contains(pi)) {
                    g.setColor(Color.RED);
                    g.setStroke(new BasicStroke(invScale * 3));
                } else {
                    g.setColor(new Color(40, 40, 120));
                    g.setStroke(baseStroke);
                }
                g.draw(circ);
                wedge.setCenter(pi.x, pi.y);
                wedge.setRadius(30 * (1D / scale));
                wedge.setAngleAndExtent(pi.angles);
                if (!highlight.contains(pi)) {
                    g.setColor(new Color(230, 10, 10));
                }
                g.draw(wedge);
                float textOff = invScale * 7;

                g.setColor(Color.BLACK);

                float ttx = (float) pi.x + textOff;
                float tty = (float) pi.y + textOff;
                g.drawString(Integer.toString(key), ttx, tty);

                String txt = pi.toString() + " / " + info.dir
                        + (((CornerAngle) pi.angles).isNormalized() ? " norm"
                        : " nonNorm");
//                float txtX = (float) pi.x + textOff;
//                float txtY = (float) pi.x + textOff;
                float txtY = 5;
                g.drawString(txt, txtY, txtX.get());
                txtX.increment(g.getFontMetrics().getHeight());
            });

            g.translate(-margin, -margin);
            g.scale(1D / scale, 1D / scale);
        }

    }

}
