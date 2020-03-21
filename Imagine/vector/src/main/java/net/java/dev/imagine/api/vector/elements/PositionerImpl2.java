package net.java.dev.imagine.api.vector.elements;

import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import net.java.dev.imagine.api.vector.util.plot.Positioner;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;

/**
 *
 * @author Tim Boudreau
 */
class PositionerImpl2 implements Positioner {

    final GlyphVector gv;
    final AffineTransform scaler;
    final float[] glyphOffsets;
    final List<Shape> allShapes;
    final int glyphCount;
    private Path2D.Double tanLine;
    private int glyphCursor;
    private boolean includeTanLine;
    private final double height;

    PositionerImpl2(GlyphVector gv, AffineTransform scaler, float[] glyphOffsets, List<Shape> allShapes, int glyphs, double height) {
        this.gv = gv;
        this.scaler = scaler;
        this.glyphOffsets = glyphOffsets;
        this.allShapes = allShapes;
        this.glyphCount = glyphs;
        this.height = height;
    }

    public void debugIncludeTangentLine() {
        if (!includeTanLine && glyphCursor == 0) {
            tanLine = new Path2D.Double();
            includeTanLine = true;
            allShapes.add(tanLine);
        } else if (glyphCursor != 0) {
            throw new IllegalStateException("Plot already started");
        }
    }
    private final PlotState state = new PlotState();

    public void finish() {
        state.finish();
    }

    class PlotState {

        private int ticks;
        private Shape curr;
        private Shape last;
        private EqLine tangentAtLeftEdge;
        private EqLine tangentAtRightEdge;
        private EqLine tangentAtCenter;
        private double[] points = new double[6];
        private Rectangle2D currGlyphBounds;
        private double halvedGlyphWidth;

        PlotState() {
            prepNextShape();
        }

        private double currentAngle() {
            //                return (angleAtLeftEdge.angle() + angleAtRightEdge.angle() + angleAtCenter.angle()) % 3;
            // XXX average all, or average 1 and 2?
            double a1 = tangentAtLeftEdge.angle();
            double a2 = tangentAtCenter.angle();
            double a3 = tangentAtRightEdge.angle();
//            boolean same1 = Angle.subtractAngles(Math.max(a1, a2), Math.min(a1, a2)) < 120;
//            boolean same2 = Angle.subtractAngles(Math.max(a1, a2), Math.min(a1, a2)) < 120;
            double mid;
//            if (same1 && same2) {
            mid = Angle.averageAngle(a1, a2, a3);
//            } else if (same1) {
//                mid = Angle.averageAngle(a1, a2);
//            } else if (same2) {
//                mid = Angle.averageAngle(a2, a3);
//            } else {
//                mid = a2;
//            }
            return mid;
        }

        public void finish() {
            if (tangentAtRightEdge == null || tangentAtCenter == null || tangentAtLeftEdge == null) {
                // never competed a single tick
                return;
            }
            if (!isDone()) {
                EqLine curPosition;
                EqLine prevPosition;
                switch (ticks % 3) {
                    case 0:
                        prevPosition = tangentAtRightEdge;
                        curPosition = tangentAtLeftEdge;
                        break;
                    case 1:
                        prevPosition = tangentAtLeftEdge;
                        curPosition = tangentAtCenter;
                        break;
                    case 2:
                        prevPosition = tangentAtCenter;
                        curPosition = tangentAtRightEdge;
                        break;
                    default:
                        throw new AssertionError();
                }
                Circle circ = new Circle(prevPosition.midPoint());
                circ.setRadius(lastReturned);
                double pos = lastReturned;
                double[] xy = new double[2];
//                double ang = circ.angleOf(curPosition.midPoint()) - 180;
                double ang = Angle.normalize(curPosition.angle() + 90);
                circ.setCenter(curPosition.midPoint());
                do {
                    circ.positionOf(ang, pos, xy);
                    circ.setCenter(xy[0], xy[1]);
                    circ.setRadius(height);
                    EqLine tan = circ.line(ang + 90);
                    pos = tick(xy[0], xy[1], tan);
                } while (!isDone());
            }
        }

        private void prepNextShape() {
            if (ticks % 3 >= glyphCount) {
                curr = null;
                return;
            }
            //                System.out.println("tick " + ticks + " glyph " + (ticks / 3));
            curr = gv.getGlyphOutline(ticks / 3, 0F, 0F);
            curr = normalize(curr);
            if (scaler != null) {
                curr = scaler.createTransformedShape(curr);
                curr = normalize(curr);
            }
            currGlyphBounds = curr.getBounds2D();
            halvedGlyphWidth = currGlyphBounds.getWidth() / 2;
            tangentAtLeftEdge = null;
            tangentAtCenter = null;
            tangentAtRightEdge = null;
        }

        boolean isDone() {
            return ticks / 3 >= glyphCount;
        }

        private double angleAtLastEmit;

        private double emit() {
            if (curr != null) {
                double ang = currentAngle();
                AffineTransform rot = AffineTransform.getRotateInstance(Math.toRadians(ang), 0, 0);
                curr = rot.createTransformedShape(curr);
                AffineTransform xl = AffineTransform.getTranslateInstance(points[0], points[1]);
                curr = xl.createTransformedShape(curr);

                /*
                if (false && last != null) {
                    // XXX need a highly accurate intersection. This is not one.
                    Rectangle2D lastBounds = last.getBounds2D();
                    Rectangle2D currBounds = curr.getBounds2D();
                    if (lastBounds.intersects(currBounds)) {
                        Area a = new Area(last);
                        a.intersect(new Area(curr));
                        Rectangle2D isect = a.getBounds2D();
                        if (!isect.isEmpty()) {
                            ticks -= 1;
                            points[0] = points[2];
                            points[1] = points[3];
                            points[2] = points[4];
                            points[3] = points[5];

                            tangentAtLeftEdge = tangentAtCenter;
                            tangentAtCenter = tangentAtRightEdge;
                            System.out.println((ticks / 3) + ". INTERSECTION "
                                    + GeometryUtils.toString(isect));

                            allShapes.add(a);

                            return Math.max(isect.getWidth(), isect.getHeight());
                        }
                    }
                }
                */

                last = curr;
                if (curr == null) {
                    throw new IllegalStateException("Huh?");
                }
                allShapes.add(curr);
                angleAtLastEmit = ang;
            }
            if (isDone()) {
                return -1;
            }
            double result = Math.max(0.1, glyphOffsets[ticks / 3] - Point2D.distance(points[0], points[1], points[4], points[5]));
            prepNextShape();
            return result;
        }

        private double initialTick(double x, double y, EqLine tangent) {
            tangentAtRightEdge = tangentAtCenter = tangentAtLeftEdge = tangent;
            points[0] = points[2] = points[4] = x;
            points[1] = points[3] = points[5] = y;
            return halvedGlyphWidth;
        }

        private double midTick(double x, double y, EqLine tangent) {
            tangentAtRightEdge = tangentAtCenter = tangent;
            points[2] = points[4] = x;
            points[3] = points[5] = y;
            return halvedGlyphWidth;
        }

        private double emitTick(double x, double y, EqLine tangent) {
            tangentAtRightEdge = tangent;
            points[4] = x;
            points[5] = y;
            return emit();
        }
        double lastReturned;

        double tick(double x, double y, EqLine tangent) {
            if (isDone()) {
                return -1;
            }
            try {
                switch (ticks % 3) {
                    case 0:
                        return lastReturned = initialTick(x, y, tangent);
                    case 1:
                        return lastReturned = midTick(x, y, tangent);
                    case 2:
                        return lastReturned = emitTick(x, y, tangent);
                    default:
                        throw new AssertionError();
                }
            } finally {
                ticks++;
            }
        }
    }

    @Override
    public double position(double x, double y, EqLine tangent) {
        if (includeTanLine) {
            if (glyphCursor == 0) {
                tanLine.moveTo(tangent.x2, tangent.y2);
            } else {
                tanLine.lineTo(tangent.x2, tangent.y2);
            }
        }
        return state.tick(x, y, tangent);
    }

    private static Shape normalize(Shape s) {
        Rectangle2D r = s.getBounds2D();
        AffineTransform xl = AffineTransform.getTranslateInstance(-r.getX(), 0);
        s = xl.createTransformedShape(s);
        return s;
    }

}
