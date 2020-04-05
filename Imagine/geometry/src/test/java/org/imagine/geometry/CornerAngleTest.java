package org.imagine.geometry;

import com.mastfrog.function.DoubleBiPredicate;
import static com.mastfrog.util.collections.CollectionUtils.setOf;
import com.mastfrog.util.collections.DoubleSet;
import com.mastfrog.util.strings.Strings;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import static org.imagine.geometry.AssertionFacade.TOL;
import org.imagine.geometry.util.GeometryStrings;
import com.mastfrog.function.state.Int;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class CornerAngleTest {

    private static final double[] PROBLEMATIC_PAIRS = {
        45.0, 315.0,
        0, 90,
        100, 10,
        190, 170,
        270, 135,
        345, 15,
        180, 225,
        315, 0,
        315, 45,
        65.55604521958344, 86.88915934650925,
        359, 2
    };

    @Test
    public void testContainsStraddlingZero() {
        new CA(345, 15)
                .assertA(345)
                .assertB(15)
                .assertContains(0, 359, 1, 10, 12, 14, 346, 350)
                .assertDoesNotContain(15, 344, 180, 45, 16, 270)
                .withSelf(ca -> {
                    ca.assertContains(ca.midAngle());
                    ca.assertContains(ca.ca.quarterAngle());
                    ca.assertContains(ca.ca.threeQuarterAngle());
                });
    }

    @Test
    public void testProblematicSampling() {
//        if (true) {
//            return;
//        }
        // Test that we can really sample points correctly
        // and that the points sampled are actually within the angle
        // in question
        Asserter asserter = new Asserter(true);
        for (int i = 0; i < PROBLEMATIC_PAIRS.length; i += 2) {
            testSampling(PROBLEMATIC_PAIRS[i], PROBLEMATIC_PAIRS[i + 1], asserter);
        }
        asserter.rethrow();
    }

    private void testSampling(double a, double b, Asserter asserter) {
        testSampling(a, b, 0.5, asserter);
        testSampling(a, b, 2, asserter);
        testSampling(a, b, 4, asserter);
    }

    private static final double SAMPLING_TOLERANCE = 0.01;

    private void testSampling(double a, double b, double distance, Asserter asserter) {
        CornerAngle ca = new CornerAngle(a, b);
        sampleOneCornerAngle(ca, '\'' + ca.toString() + '\'', distance, asserter);
        CornerAngle inv = ca.inverse();
        sampleOneCornerAngle(inv, '\'' + inv.toString() + "' (inverse "
                + "of '" + ca + "')", distance, asserter);
    }

    private static class PointsPainter implements Asserter.Painter {

        private final CornerAngle ang;
        private final Circle circle;
        private final Shape tri;
        private final Set<Point2D.Double> bad;
        private final Set<Point2D.Double> good;
        private final Circle scratchCirc = new Circle();
        private final Rectangle2D.Double dot = new Rectangle2D.Double();
        private final Line2D.Double line = new Line2D.Double();

        private static final double RANGE = 20;
        private static final int PT_RAD = 5;

        public PointsPainter(CornerAngle ang, Circle circle, Shape tri, Set<Point2D.Double> bad, Set<Point2D.Double> good) {
            this.ang = ang;
            this.circle = circle;
            this.tri = tri;
            this.bad = bad;
            this.good = good;
        }

        @Override
        public void paint(Graphics2D g, double scale) {
            Rectangle r = circle.getBounds();
            r.add(tri.getBounds());
            g.translate(-r.x + 5, -r.y + 5);
            float invScale = (float) (1D / scale);
            BasicStroke stroke = new BasicStroke(invScale);
            g.setStroke(stroke);
            g.setColor(Color.WHITE);
            g.fill(circle);
            g.setColor(Color.DARK_GRAY);
            g.draw(circle);
            g.setColor(new Color(255, 255, 160, 128));
            g.fill(tri);
            g.setColor(Color.DARK_GRAY);
            g.draw(tri);
            g.setColor(new Color(0, 0, 255, 168));
            dot.width = 2 * invScale;
            dot.height = 2 * invScale;
            double off = invScale * 0.5;
            double wid = circle.radius;
            double minX = circle.centerX() - wid;
            double maxX = circle.centerX() + wid;
            double minY = circle.centerY() - wid;
            double maxY = circle.centerY() + wid;
            double ival = dot.width * 3;
            for (double y = minY; y < maxY; y += ival) {
                for (double x = minX; x < maxX; x += ival) {
                    double angle = circle.angleOf(x, y);
                    if (ang.contains(angle) && circle.contains(x, y)) {
                        dot.x = x - off;
                        dot.y = y - off;
                        g.fill(dot);
                    }
                }
            }
            g.setColor(Color.GRAY);
            circle.positionOf(ang.midAngle(), (x, y) -> {
                line.x1 = circle.centerX();
                line.y1 = circle.centerY();
                line.x2 = x;
                line.y2 = y;
                g.draw(line);
            });
            g.setColor(Color.ORANGE);
            circle.positionOf(ang.quarterAngle(), (x, y) -> {
                line.x1 = circle.centerX();
                line.y1 = circle.centerY();
                line.x2 = x;
                line.y2 = y;
                g.draw(line);
            });
            g.setColor(Color.CYAN);
            circle.positionOf(ang.threeQuarterAngle(), (x, y) -> {
                line.x1 = circle.centerX();
                line.y1 = circle.centerY();
                line.x2 = x;
                line.y2 = y;
                g.draw(line);
            });

            scratchCirc.setRadius(5 * invScale);
            g.setColor(Color.RED);
            for (Point2D.Double p : bad) {
                scratchCirc.setCenter(p);
                g.draw(scratchCirc);
            }
            g.setColor(new Color(0, 140, 0));
            for (Point2D.Double p : good) {
                scratchCirc.setCenter(p);
                g.draw(scratchCirc);
            }
            Rectangle2D b = tri.getBounds2D();
            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(AffineTransform.getScaleInstance(invScale, invScale)));
            String txt = ang.toShortString();
            int w = g.getFontMetrics().stringWidth(txt);
            g.drawString(txt, (float) b.getCenterX() - (w / 2), (float) b.getCenterY());
        }

        @Override
        public Rectangle requiredBounds() {
            Rectangle result = circle.getBounds();
            result.add(tri.getBounds());
            scratchCirc.setRadius(PT_RAD);
            for (Point2D.Double p : bad) {
                scratchCirc.setCenter(p);
                result.add(scratchCirc.getBounds());
            }
            for (Point2D.Double p : good) {
                scratchCirc.setCenter(p);
                result.add(scratchCirc.getBounds());
            }
            result.add(circle.centerX() - 10, circle.centerY() - 10);
            result.add(circle.centerX() + 10, circle.centerY() + 10);
            return result;
        }

    }

    private void sampleOneCornerAngle(CornerAngle ca, String desc, double distance, Asserter asserter) {
        // A circle to get us points on the triangle we need
        Circle cir = new Circle(60, 60, 20);
        cir.positionOf(ca.leadingAngle(), (bx, by) -> {
            cir.positionOf(ca.trailingAngle(), (cx, cy) -> {
                // Our test triangle incorporating the angles in question,
                // so the CornerAngle should feed us points that are inside it
                // -- if it doesn't, something is wrong
//                Triangle2D tri = new Triangle2D(cir.centerX(), cir.centerY(),
//                        bx, by, cx, cy);
                Shape tri = ca.boundingShape(cir.centerX(), cir.centerY(), cir.radius() + 5);
                DoubleBiPredicate test = tri::contains;
                // Make a set of the angles we expect; due to rounding errors,
                // we fuzzy-match them using containsApproximate()
                DoubleSet expected = DoubleSet.ofDoubles(ca.quarterAngle(),
                        ca.midAngle(), ca.threeQuarterAngle());
                // Collect the points we test here
                Set<Point2D.Double> tested = new HashSet<>();
                Set<Point2D.Double> good = new HashSet<>();
                Set<Point2D.Double> bad = new HashSet<>();
                // Ensure we really get called once for the quarterAngle, midAngle
                // and threeQuarterAngle
                Int attempts = Int.create();
                // This will return the number of points the triangle said it
                // contains
                PointsPainter pp = new PointsPainter(ca, cir, tri, bad, good);
                int count = ca.sample(60, 60, distance, (tx, ty) -> {
                    double ang = cir.angleOf(tx, ty);
                    attempts.increment();
                    // We actually do not want the equals behavior of EqPointDouble
                    // here, as they can be coalesced if they are a very small
                    // fractional distance from each other
                    tested.add(new Point2D.Double(tx, ty));
                    asserter.assertTrue(expected.containsApproximate(ang, SAMPLING_TOLERANCE),
                            "Tested an angle that "
                            + "should not be present - expected one of " + expected
                            + " but got " + GeometryStrings.toString(ang)
                            + " for " + GeometryStrings.toString(tx, ty)
                            + " in " + desc + " at"
                            + " distance " + GeometryStrings.toString(distance),
                            pp
                    );

                    boolean result = test.test(tx, ty);
                    if (!result) {
                        bad.add(new Point2D.Double(tx, ty));
                        System.out.println("Not present in bounding shape: "
                                + GeometryStrings.toString(tx, ty) + " for "
                                + " " + desc + " at " + distance);
                    } else {
                        good.add(new Point2D.Double(tx, ty));
                    }
                    return result;
                });
                asserter.assertEquals(3, attempts.getAsInt(), "3 points should"
                        + " have been tried for " + desc + " at " + distance, pp);
                asserter.assertEquals(3, count, "Did not get expected number "
                        + "of samples " + "for " + desc + ". Tested "
                        + attempts.getAsInt() + " points: "
                        + Strings.join('/', tested,
                                pt -> GeometryStrings.toString(pt.x, pt.y))
                        + " in " + tri + " at distance " + distance, pp
                );
                if (count == 3) {
//                    System.out.println("OK " + desc);
//                    asserter.add(desc, pp);
                }
            });
        });
    }

    @Test
    public void testMidBug() {
        CornerAngle c = new CornerAngle(270, 180);
        assertEquals(270D, c.trailingAngle(), TOL);
        assertEquals(180D, c.leadingAngle(), TOL);
        assertEquals(-90D, c.extent(), TOL);
        assertEquals(45D, c.midAngle(), TOL, "Wrong mid angle");
        assertEquals(112.5D, c.threeQuarterAngle(), TOL, "Wrong three quarter angle");
        assertEquals(337.5D, c.quarterAngle(), TOL, "Wrong quarter angle");

        new CA(270, 180)
                .withSelf(ca -> {
                    ca.sector(sa -> {
                        assertEquals(ca.midAngle(), sa.ca.midAngle(),
                                "Mid angles do not match with corresponding sector");
                    });
                })
                .assertExtent(-90)
                .assertA(270)
                .assertB(180)
                .assertMidAngle(45)
                .assertQuarterAngle(337.5)
                .assertThreeQuartersAngle(112.5);
    }

    @Test
    public void testEmpty() {
        new CA(10, 10).assertEmpty()
                .assertMidAngle(10).assertQuarterAngle(10)
                .assertThreeQuartersAngle(10)
                .assertExtent(0);

        new CA(5, 5, 5, 5, 5, 5).assertEmpty();
    }

    @Test
    public void testEndingAtZero() {
        new CA(315, 1).withSelf(ca -> {
            ca.assertExtent(-314)
                    .assertA(315)
                    .assertB(1)
                    .assertNotNormalized();
        });

        new CA(315, 0).withSelf(ca -> {
            ca.assertExtent(-315)
                    .assertA(315)
                    .assertB(0)
                    .assertNotNormalized()
                    .assertContains(ca.ca.midAngle(), "Does not contain its own mid-angle")
                    .assertContains(ca.ca.quarterAngle(), "Does not contain its own quarter-angle")
                    .assertContains(ca.ca.threeQuarterAngle(), "Does not contain its own three-quarter-angle");
        });
    }

    @Test
    public void testOppositeNorm() {
        CornerAngle ca = new CornerAngle(0, 90);
        assertEquals(90, ca.extent(), TOL);
        CornerAngle exp = new CornerAngle(270, 180);
        assertEquals(-90, exp.extent(), TOL);
        CornerAngle opp = ca.opposite();
        assertEquals(exp, opp, "Wrong opposite");
        assertEquals(270, opp.trailingAngle(), TOL);
        assertEquals(180, opp.leadingAngle(), TOL);
        assertEquals(-90, opp.extent(), TOL);
        assertEquals(ca, opp.opposite());
    }

    @Test
    public void testOppositeInverse() {
        CornerAngle ca = new CornerAngle(90, 0);
        assertEquals(ca, ca, "Does not equal self");
        assertEquals(ca, new CornerAngle(90, 0), "Does not equal duplicate");
        assertEquals(-90, ca.extent(), TOL);

        CornerAngle exp = new CornerAngle(180, 270);
        CornerAngle opp = ca.opposite();
        assertEquals(90, opp.extent(), TOL);

        assertEquals(exp, opp, "Opposite of 90,0 should be 180, 270 not " + opp);

        CornerAngle oppOpp = opp.opposite();
        assertEquals(ca, oppOpp, "Orig '" + ca + "' -> opp '"
                + opp + "' -> '" + oppOpp + "' - calling opposite on the opposite of "
                + "an initial angle should return an angle equal to the original");
        assertEquals(-90, oppOpp.extent(), TOL, "Extent of original does not "
                + "match yet they are equal: '" + ca + "' and '" + oppOpp + "'");
    }

    @Test
    public void testMultipleInverse() {
        CornerAngle ca = new CornerAngle(32.5, 15);
        CornerAngle o = ca.inverse();
        assertEquals(15, o.trailingAngle());
        assertEquals(32.5, o.leadingAngle());
        CornerAngle invInv = o.inverse();
        assertEquals(ca, invInv);
    }

    @Test
    public void testOppositeStraddlesZero() {
//        CornerAngle ca = new CornerAngle(180 - 45, 180 + 45);
        CornerAngle ca = new CornerAngle(135, 225);
        CornerAngle exp = new CornerAngle(315, 45);
        CornerAngle opp = ca.opposite();
        assertEquals(exp, opp, "Unexpected opposite for '" + ca + "' is " + opp);
        assertEquals(exp.start(), opp.start(), "Wrong start: " + opp);
        assertEquals(exp.extent(), opp.extent(), "Wrong extent: " + opp);
    }

    @Test
    public void testFromLines() throws Throwable {
//        if (true) {
//            return;
//        }
        // a 90 degree line and a 180 degree line
        Rectangle rect = new Rectangle(0, 0, 10, 10);
        new CA(0, 0, 10, 0, 10, 10)
                .withSelf(ca -> {
                    ca.assertA(180)
                            .assertB(270)
                            .assertExtent(90);

                    Set<Point2D.Double> good = new HashSet<>(3);
                    Set<Point2D.Double> bad = new HashSet<>(3);
                    ca.ca.sample(10, 0, (x, y) -> {
                        if (rect.contains(x, y)) {
                            good.add(new Point2D.Double(x, y));
                        } else {
                            bad.add(new Point2D.Double(x, y));
                        }
                        return true;
                    });
                    assertEquals(3, good.size());
                    assertEquals(0, bad.size());
                    ca.inverse(iv -> {
                        ca.opposite(op -> {
                            AffineTransform xf = AffineTransform.getScaleInstance(4, 4);
                            for (Point2D p : good) {
                                xf.transform(p, p);
                            }
                            for (Point2D p : bad) {
                                xf.transform(p, p);
                            }
                            /*
                            CAComp.show(ca.ca, iv.ca, op.ca)
                                    .setPoly(new Polygon2D(0, 0, 40, 0, 40, 40))
                                    .setPoints(good, bad);
                            Thread.sleep(40000);
                             */
                        });
                    });
                });
    }

    @Test
    public void testStraddlingCornerCases() {
        new CA(135, 225).withSelf(ca -> {
            ca.assertRight()
                    .assertCoordinates(135, 225)
                    .assertA(135)
                    .assertB(225)
                    .assertExtent(90)
                    .inverse(in -> {
                        in.assertCoordinates(225, 135)
                                .assertExtent(-90)
                                .assertCounterClockwise()
                                .assertRight();
                    })
                    .opposite(op -> {
                        op.assertExtent(-270)
                                .assertA(315)
                                .assertB(45)
                                .assertRight()
                                .opposite(opBack -> {
                                    opBack.assertA("Opposite not symmetrical", 135)
                                            .assertB("Opposite not symmetrical", 225)
                                            .assertExtent("Opposite not symmetrical", 90)
                                            .assertEquals(ca, "Opposite not symmetrical");
                                });
                    })
                    .sector(sa -> {
                        sa.assertExtent(90)
                                .assertStartingAngle(135)
                                .assertMinAngle(135)
                                .assertMaxAngle(225);
                    });

        });
    }

    @Test
    public void testRightAngle() {
        new CA(0, 90).assertA(0)
                .assertB(90)
                .assertExtent(90)
                .assertRight()
                .assertClockwise()
                .assertContains(45)
                .assertContains(0)
                .assertContains(89.99)
                .assertContains(360)
                .assertContains(360.1)
                .assertDoesNotContain(91)
                .assertDoesNotContain(180)
                .assertDoesNotContain(181)
                .assertDoesNotContain(270)
                .assertDoesNotContain(270)
                .assertDoesNotContain(90)
                .assertDoesNotContain(359.759)
                .assertMidAngle(45)
                .assertQuarterAngle(45D / 2D)
                .assertThreeQuartersAngle(45D + (45D / 2D))
                .opposite(op -> {
                    op.assertExtent(-90)
                            .assertA(270)
                            .assertB(180)
                            .assertRight()
                            .assertCounterClockwise();
                })
                .normalized(nm -> {
                    nm.assertEquals(new CornerAngle(0, 90));
                })
                .sector(sa -> {
                    sa.assertStartingAngle("Should be 0", 0)
                            .assertExtent("Sector from 0, 90", 90)
                            .assertMinAngle("Sector from 0, 90", 0)
                            .assertMaxAngle("Sector from 0, 90", 90);
                });
    }

    @Test
    public void testOffsetRightAngle() {
        new CA(10, 100)
                .assertA(10)
                .assertB(100)
                .assertExtent(90)
                .assertRight()
                .assertClockwise()
                .assertMidAngle(55)
                .assertQuarterAngle(55D - (45D / 2D))
                .assertThreeQuartersAngle(55D + (45D / 2D))
                .sector(sa -> {
                    sa.assertStartingAngle("Sector from 10, 100", 10)
                            .assertExtent("Sector from 10, 100", 90)
                            .assertMaxAngle("Sector from 10, 100", 100);
                });
    }

    @Test
    public void testEndsWithZero() {
        new CA(90, 0)
                .assertA(90)
                .assertB(0)
                .assertExtent(-90)
                .assertCounterClockwise()
                .assertContains(90, 91, 180, 270, 359, 359.9)
                .assertDoesNotContain(0)
                .assertMidAngle(90D + (270D / 2D))
                .assertQuarterAngle(90D + (270D / 4D))
                .assertThreeQuartersAngle(90D + (270D * 0.75));
    }

    @Test
    public void testInverted() {
        new CA(100, 10)
                .assertA(100)
                .assertB(10)
                .assertExtent(-90)
                .assertCounterClockwise()
                .assertRight()
                .sector(sa -> {
                    sa.assertStartingAngle(100)
                            .assertExtent(270)
                            .assertMaxAngle(100)
                            .assertMinAngle(10);
                })
                .assertMidAngle(Angle.opposite(55))
                .assertQuarterAngle(Angle.opposite(55D) - (270D * 0.25))
                .assertThreeQuartersAngle(Angle.opposite(55D) + (270D * 0.25))
                .assertContains(100, 101, 145, 180, 270, 271, 359, 360, 0, 1, 9)
                .assertDoesNotContain(20, 30, 40, 50, 60, 10)
                .withSelf(ca -> {

                    assertNotEquals(55D, ca.midAngle(), TOL, "Mid angle "
                            + "computed backwards");

                    Circle circ = new Circle(0, 0, 5);
                    Set<EqPointDouble> points = setOf(
                            circ.getPosition(ca.ca.midAngle()),
                            circ.getPosition(ca.ca.quarterAngle()),
                            circ.getPosition(ca.ca.threeQuarterAngle())
                    );
                    ca.assertSample(3, "Sampling failed", 5, (x, y) -> {
                        EqPointDouble pt = new EqPointDouble(x, y);
                        assertTrue(points.contains(pt), "Unexpected point " + pt
                                + " not in " + points);
                        return points.contains(pt);
                    });
                })
                .opposite(op -> {
                    op.assertExtent(90)
                            .assertA(190)
                            .assertB(280)
                            .assertClockwise()
                            .assertRight();
                })
                .inverse(in -> {
                    in.assertA(10)
                            .assertB(100)
                            .assertExtent(90)
                            .assertClockwise()
                            .assertRight()
                            .inverse(i2 -> {
                                i2.assertA(100)
                                        .assertB(10)
                                        .assertExtent(-90)
                                        .assertEquals(new CornerAngle(100, 10));
                            });
                });
    }

    static final class CAComp extends JComponent {

        private final CornerAngle[] angs;
        private final float scale = 3;
        private final Shape[] shapes;
        private static final int GAP = 25;
        private static final int TOP = 30;
        private static final int LEFT = 10;
        private static final int RAD = 40;
        private Polygon2D poly;

        CAComp(CornerAngle... angs) {
            int x = LEFT;
            int y = 60;
            int r = 40;
            this.angs = angs;
            shapes = new Shape[angs.length];
            for (int i = 0; i < angs.length; i++) {
                shapes[i] = angs[i].toShape(x, y, r);
                Rectangle bds = shapes[i].getBounds();
                ((PieWedge) shapes[i]).translate(-bds.x, -bds.y);
                ((PieWedge) shapes[i]).translate(x, y);
                x += r + GAP;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension result = new Dimension(shapes.length * (RAD + GAP),
                    RAD + TOP + 60);

//            for (Shape s : shapes) {
//                Rectangle r = s.getBounds();
//                result.width += r.width + GAP;
//                result.height = Math.max(r.y + r.height + TOP, result.height);
//            }
//            result.height += 20 + TOP;
//            result.width += LEFT;
            double rh = result.height * scale;
            double rw = result.width * scale;
            result.setSize(rh, rw);
            return result;
        }

        public static CAComp show(CornerAngle... angs) {
            assert angs.length > 0;
            return new CAComp(angs).showFrame();
        }

        public static CAComp show(CornerAngle ca, Polygon2D poly) {
            return new CAComp(ca).setPoly(poly).showFrame();
        }

        CAComp setPoly(Polygon2D poly) {
            this.poly = poly;
            poly.applyTransform(AffineTransform.getTranslateInstance(LEFT, 60));
            return this;
        }

        Set<Point2D.Double> good = new HashSet<>();
        Set<Point2D.Double> bad = new HashSet<>();

        CAComp setPoints(Set<Point2D.Double> good, Set<Point2D.Double> bad) {
            this.good.addAll(good);
            this.bad.addAll(bad);
            return this;
        }

        public CAComp showFrame() {
            JFrame jf = new JFrame();
            jf.setContentPane(this);
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jf.pack();
            jf.setVisible(true);
            return this;
        }

        protected void paintComponent(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.scale(scale, scale);
            float invScale = 1F / scale;
            g.setStroke(new BasicStroke(2 * invScale));
            Font f = g.getFont();
            g.setFont(f.deriveFont(AffineTransform.getScaleInstance(invScale, invScale)));
            for (int i = 0; i < shapes.length; i++) {
                g.setColor(Color.ORANGE);
                g.fill(shapes[i]);
                g.setColor(Color.BLACK);
                g.draw(shapes[i]);
                Rectangle r = shapes[i].getBounds();
                float txtX = r.x;
                float txtY = r.y - g.getFontMetrics().getHeight();
                g.drawString(angs[i].toShortString(), txtX, txtY);
            }
            if (poly != null) {
                g.setColor(Color.BLUE);
                g.draw(poly);
            }
            if (!good.isEmpty()) {
                g.setStroke(new BasicStroke(1 * invScale));
                g.setColor(Color.GREEN.darker());
                for (Point2D.Double p : good) {
                    circ.setCenter(p);
                    circ.translate(LEFT, 60);
                    circ.setRadius(5 * invScale);
                    g.draw(circ);
                }
            }
            if (!bad.isEmpty()) {
                g.setStroke(new BasicStroke(1.5F * invScale));
                g.setColor(Color.RED.darker());
                for (Point2D.Double p : bad) {
                    circ.setCenter(p);
                    circ.translate(LEFT, 60);
                    circ.setRadius(5 * invScale);
                    g.draw(circ);
                }
            }
        }
        private final Circle circ = new Circle();

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            paintComponent((Graphics2D) g);
        }
    }
}
