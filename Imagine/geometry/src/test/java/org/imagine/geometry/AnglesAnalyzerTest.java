/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntIntMap;
import com.mastfrog.util.collections.IntIntMap.IntIntMapConsumer;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.collections.IntMap.IntMapConsumer;
import com.mastfrog.util.collections.IntSet;
import com.mastfrog.util.strings.Strings;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.imagine.geometry.util.GeometryStrings;
import com.mastfrog.function.state.Bool;
import com.mastfrog.function.state.Int;
import java.awt.geom.PathIterator;
import org.imagine.geometry.analysis.VectorVisitor;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class AnglesAnalyzerTest {

    private static final boolean VISUAL_ASSERT = true;

    @Test
    public void testRectangleSampleAngles() {
        analyze(rect()).assertAnglesSampleCorrectly();
    }

    @Test
    public void testTestPolygon() throws InterruptedException {
        ShapeInfo info = analyze(scale(testPoly(), 0.25));
        info.assertAnglesSampleCorrectly();
//        info.show();
//        Thread.sleep(120000);
    }

    private static Shape scale(Shape shape, double by) {
        return AffineTransform.getScaleInstance(by, by).createTransformedShape(shape);
    }

    @Test
    public void testComplexSampleAngles() {
        analyze(xs()).assertAnglesSampleCorrectly();
    }

    @Test
    public void testComplexShifted() {
        analyze(xs(5)).assertAnglesSampleCorrectly();
    }

    @Test
    public void testRectShifted() {
        analyze(rectShifted()).assertAnglesSampleCorrectly();
    }

    @Test
    public void testRectShifted2() {
        analyze(rectShifted2()).assertAnglesSampleCorrectly();
    }

    @Test
    public void testComplexShifted2() {
        analyze(xs(7)).assertAnglesSampleCorrectly();
    }

    @Test
    public void testComplexWithCurves() {
        analyze(mangled(XS, true, 4, 8)).assertAnglesSampleCorrectly();
    }

    @Test
    public void testComplexReversed() {
        analyze(xsReversed()).assertAnglesSampleCorrectly();
    }

    @Test
    public void testRectReversed() {
        analyze(rectReversed()).assertAnglesSampleCorrectly();
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

    private static double[] XSx = new double[]{
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
    private static double[] XS = new double[]{
        10, 10,
        20, 20,
        30, 10,
        40, 20,
        50, 10,
        50, 20,
        40, 10,
        30, 20,
        20, 10,
        // added
        9, 15,
        55, 12.5,
        55, 17.5,
        0, 17.5

    };

    static {
        for (int i = 0; i < XS.length; i++) {
            XS[i] += 40;
        }
    }

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
    Polygon2D p2d = new Polygon2D(pts);

    private Shape testPoly() {
        return p2d;
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

    private Shape mangled(double[] points, boolean cub, int... mangles) {
        IntSet set = IntSet.of(mangles);
        System.out.println("Mangle at " + set);
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_NON_ZERO);
        for (int i = 0; i < points.length; i += 2) {
            if (i == 0) {
                path.moveTo(points[i], points[i + 1]);
            } else {
                if (set.contains(i)) {
                    double x = points[i];
                    double y = points[i + 1];
                    if (cub) {
                        double px = points[i - 2];
                        double py = points[i - 1];
                        double nx = points[(i + 2) % points.length];
                        double ny = points[(i + 3) % points.length];
                        EqPointDouble a = new EqLine(x, y, px, py).midPoint();
                        EqPointDouble b = new EqLine(x, y, nx, ny).midPoint();
                        path.curveTo(a.x, a.y, b.x, b.y, x, y);
                    } else {
                        double px = points[i - 2];
                        double py = points[i - 1];
                        EqPointDouble a = new EqLine(x, y, px, py).midPoint();
                        path.quadTo(a.x, a.y, x, y);
                    }
                } else {
                    path.lineTo(points[i], points[i + 1]);
                }
            }
            if (i == points.length - 2) {
                path.closePath();
            }
        }
        return path;
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
        ShapeInfo info = new ShapeInfo(shape);
        RotationDirection dir = VectorVisitor.analyze(shape, (int pointIndex, LineVector vect, int subpathIndex, RotationDirection subpathRotationDirection, Polygon2D approximate, int prevPointIndex, int nextPointIndex) -> {
            info.add(pointIndex, new PointInfo(pointIndex, vect, subpathRotationDirection, approximate));
        });
        info.dir = dir;
        return info;
    }

    static final class ShapeInfo {

        private RotationDirection dir;
        private final Shape shape;
        private final IntMap<PointInfo> forPoints = CollectionUtils.intMap();

        public ShapeInfo(Shape shape) {
            this.shape = shape;
        }

        void add(int point, PointInfo info) {
            assertNull(forPoints.put(point, info), "Got point " + point + " more than once: " + info);
        }

        void show(PointInfo... highlight) {
            new ShapeComp(this).showComp(highlight);
        }

        static double SAMPLE_DIST = 1.5;

        public void assertAnglesSampleCorrectly() {
            Set<Point2D.Double> good = new HashSet<>();
            Set<Point2D.Double> bad = new HashSet<>();
            IntIntMap samplingsForKey = IntIntMap.create(forPoints.size());
            Set<PointInfo> failed = new HashSet<>();
            Bool hasFails = Bool.create();
            forPoints.forEach((IntMapConsumer<PointInfo>) (key, pi) -> {
                Circle circ = new Circle(pi.x(), pi.y(), SAMPLE_DIST);
                int samps = pi.angles().sample(pi.x(), pi.y(), SAMPLE_DIST, (x, y) -> {
                    boolean result = pi.approximate.contains(x, y);
                    double ang = circ.angleOf(x, y);

                    if (!pi.angles().contains(ang)) {
                        new IllegalArgumentException(pi.angles()
                                + " passes sample points on angles it cannot contain: "
                                + GeometryStrings.toString(x, y) + " at angle "
                                + GeometryStrings.toDegreesString(ang)
                        ).printStackTrace();
                    }

                    if (!result) {
                        failed.add(pi);
                        hasFails.set();
                        bad.add(new Point2D.Double(x, y));
                    } else {
                        good.add(new Point2D.Double(x, y));
                    }
                    return result;
                });
                samplingsForKey.put(key, samps);
            });
            if (hasFails.getAsBoolean()) {
                StringBuilder sb = new StringBuilder();
                samplingsForKey.forEachPair((IntIntMapConsumer) (k, v) -> {
                    if (v != 3) {
                        sb.append("Sampling of ")
                                .append(k).append(" failed: ").append(forPoints.get(k))
                                .append('\n');
                    }
                });
                if (VISUAL_ASSERT) {
                    ShapeComp comp = new ShapeComp(this);
                    comp.setGoodBad(good, bad);
                    comp.showComp(failed.toArray(new PointInfo[0]));
                    System.out.println("\nFAILS: \n" + sb
                            + "/ " + Strings.join(',', bad, (pt) -> {
                                return GeometryStrings.toString(pt.x, pt.y);
                            }));
                    try {
                        Thread.sleep(110000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AnglesAnalyzerTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                fail(sb.toString());
            }
        }
    }

    private static final class PointInfo {

        private final int pointIndex;
        private final RotationDirection subpathRotationDirection;
        private final Polygon2D approximate;
        private final LineVector vect;
        private CornerAngle cachedCorner;

        PointInfo(int pointIndex, LineVector vect, RotationDirection subpathRotationDirection, Polygon2D approximate) {
            this.pointIndex = pointIndex;
            this.vect = vect;
            this.subpathRotationDirection = subpathRotationDirection;
            this.approximate = approximate;
        }

        public double x() {
            return vect.apexX();
        }

        public double y() {
            return vect.apexY();
        }

        public CornerAngle angles() {
            return cachedCorner == null ? cachedCorner = vect.corner() : cachedCorner;
        }

        @Override
        public String toString() {
            return pointIndex + ". "
                    + GeometryStrings.toShortString(x(), y())
                    + " " + angles().toShortString()
                    + " " + subpathRotationDirection
                    //                    + " " + (intersections == 0 ? ""
                    //                            : " i" + Integer.toString(intersections))
                    + " mid " + GeometryStrings.toDegreesStringShort(angles().midAngle())
                    + " qt " + GeometryStrings.toDegreesStringShort(angles().quarterAngle())
                    + " 3qt " + GeometryStrings.toDegreesStringShort(angles().threeQuarterAngle()) //                    + " as Sect " + angles.toSector()
                    ;

        }
    }

    static class ShapeComp extends JComponent {

        private final ShapeInfo info;
        private final int margin = 0;
        private float scale = 16;
        private final PieWedge wedge = new PieWedge();
        private final Circle circ = new Circle();
        private final Set<Point2D.Double> good = new HashSet<>();
        private final Set<Point2D.Double> bad = new HashSet<>();

        ShapeComp(ShapeInfo info) {
            this.info = info;
        }

        ShapeComp setGoodBad(Set<Point2D.Double> good, Set<Point2D.Double> bad) {
            this.good.addAll(good);
            this.bad.addAll(bad);
            return this;
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
            EventQueue.invokeLater(() -> {
                try {
                    JFrame jf = new JFrame();
                    jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    jf.getContentPane().setLayout(new BorderLayout());
                    jf.getContentPane().add(this, BorderLayout.CENTER);
                    jf.pack();
                    jf.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
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

            Font f = g.getFont();
            g.setFont(f.deriveFont(AffineTransform.getScaleInstance(invScale * 1.5, invScale * 1.5)));
            Int txtX = Int.of(5);
            Bool approxDrawn = Bool.create();
            info.forPoints.forEach((IntMapConsumer<PointInfo>) (key, pi) -> {

                circ.setRadius(5 * (1D / scale));
                circ.setCenter(pi.x(), pi.y());
                if (highlight.contains(pi)) {
                    g.setColor(Color.RED);
                    g.setStroke(new BasicStroke(invScale * 2));
                } else {
                    g.setStroke(new BasicStroke(invScale));
                    g.setColor(new Color(40, 40, 120));
                    g.setStroke(baseStroke);
                }
                g.draw(circ);

                Shape bounding = pi.angles().boundingShape(pi.x(), pi.y(), invScale * 50);
                g.setColor(new Color(255, 128, 20, 180));
                g.fill(bounding);
                g.setColor(new Color(100, 100, 20, 180));
                g.draw(bounding);

                wedge.setCenter(pi.x(), pi.y());
                wedge.setRadius(30 * (1D / scale));
                wedge.setAngleAndExtent(pi.angles().toSector());
                if (!highlight.contains(pi)) {
                    g.setColor(new Color(230, 10, 10));
                } else {
                    if (pi.angles().isNormalized()) {
                        g.setColor(Color.BLACK);
                    } else {
                        g.setColor(new Color(0, 0, 180));
                    }
                }
                g.draw(wedge);
                float textOff = invScale * 7;

                float ttx = (float) pi.x() + textOff;
                float tty = (float) pi.y() + textOff;
                if (highlight.contains(pi)) {
                    g.setColor(new Color(230, 10, 10));
                } else {
//                    if (pi.intersections % 2 != 0) {
//                        g.setColor(new Color(30, 30, 130));
//                    } else {
//                        g.setColor(Color.BLACK);
//                    }
                    g.setColor(Color.BLACK);
                }

                String txt = pi.toString() + " / " + info.dir
                        + (((CornerAngle) pi.angles()).isNormalized() ? " norm"
                        : " nonNorm");
                float txtY = 5;
                g.drawString(txt + " / " + GeometryStrings.toDegreesStringShort(pi.angles().extent()), txtY, txtX.get());
                txtX.increment(g.getFontMetrics().getHeight());

                circ.setRadius(35 * invScale);
                circ.setCenter(pi.x(), pi.y());
                rect.width = 3 * invScale;
                rect.height = 3 * invScale;
                double amt = 35 * invScale;
                g.setColor(new Color(128, 255, 128, 200));
                g.setStroke(new BasicStroke(0.75F * invScale));
                for (double xx = pi.x() - amt; xx < pi.x() + amt; xx += 0.5) {
                    for (double yy = pi.y() - amt; yy < pi.y() + amt; yy += 0.5) {
                        if (circ.contains(xx, yy)) {
                            double ang = circ.angleOf(xx, yy);
                            if (pi.angles().contains(ang)) {
                                rect.x = xx - (0.5 * invScale);
                                rect.y = yy - (0.5 * invScale);
                                g.fill(rect);
                            }
                        }
                    }
                }
                g.setColor(Color.BLUE.darker());
                ln.x1 = pi.x();
                ln.y1 = pi.y();
                g.setStroke(new BasicStroke(invScale));
                ln.setAngleAndLength(pi.angles().midAngle(), 20 * invScale);
                g.draw(ln);
                Font fnt = g.getFont();
                g.setFont(g.getFont().deriveFont(Font.BOLD));
                if (pi.angles().isNormalized()) {
                    g.setColor(Color.BLUE);
                } else {
                    g.setColor(Color.BLACK);
                }

                g.drawString(Integer.toString(key), ttx, tty);
                g.setFont(fnt);

                approxDrawn.runAndSet(() -> {
                    g.setColor(new Color(255, 255, 255, 200));
                    g.fill(pi.approximate);
                    g.setColor(Color.WHITE);
                    g.draw(pi.approximate);
                });

//                for (double xx=pi.x-10; xx < pi.x+10; xx++ ) {
//                    for (double yy=pi.y-10; yy < pi.y + 10; yy++) {
//                    }
//                }
            });
            circ.setRadius(5 * invScale);
            g.setColor(Color.GREEN.darker());
            for (Point2D.Double gd : good) {
                circ.setCenter(gd);
                g.draw(circ);
            }
            g.setColor(Color.RED.darker());
            for (Point2D.Double bd : bad) {
                circ.setCenter(bd);
                g.draw(circ);
            }

            g.translate(-margin, -margin);
            g.scale(1D / scale, 1D / scale);
        }
        private static final Rectangle.Double rect = new Rectangle2D.Double(0, 0, 1, 1);
        private static final EqLine ln = new EqLine();
    }

}
