package org.imagine.vector.editor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.util.function.Supplier;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.snap.SnapCoordinate;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.OnSnap;
import org.imagine.editor.api.snap.SnapKind;
import static org.imagine.editor.api.snap.SnapKind.CORNER;
import com.mastfrog.geometry.Arrow;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.CornerAngle;
import com.mastfrog.geometry.PieWedge;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;

/**
 *
 * @author Tim Boudreau
 */
public final class SnapLinesPainter implements OnSnap<ShapeSnapPointEntry> {

    private final Line2D.Double x = new Line2D.Double(0, 0, 0, 0);
    private final Line2D.Double y = new Line2D.Double(0, 0, 0, 0);

    private final Arrow xArrow = new Arrow(0, 0, 2, 2);
    private final Arrow yArrow = new Arrow(0, 0, 2, 2);
    private static final float ARROW_ANGLE = 22.5F;
    private static final int RULE_ANGLE = 90;
    private final Arc2D.Double arc = new Arc2D.Double(0, 0, 1, 0, 0, 90, Arc2D.PIE);
    private static final Circle circle = new Circle(0, 0, 1);

    private SnapCoordinate<ShapeSnapPointEntry> xp;
    private SnapCoordinate<ShapeSnapPointEntry> yp;
    private final RepaintHandle handle;
    private final Supplier<Rectangle> bounds;

    private final PieWedge pie1 = new PieWedge();
    private final PieWedge pie2 = new PieWedge();

    public SnapLinesPainter(RepaintHandle handle, Supplier<Rectangle> bounds) {
        this.handle = handle;
        this.bounds = bounds;
    }

    private boolean hasPie1, hasPie2;

    void setSnapPoint(SnapCoordinate<ShapeSnapPointEntry> pt, Line2D.Double line, Arrow arrow) {
        handle.repaintArea(lastBoundsPainted);
        if (pt == null) {
            line.x1 = line.x2 = line.y1 = line.y2 = -100000;
            arrow.x1 = arrow.x2 = arrow.y1 = arrow.y2 = -100000;
        } else {
            Rectangle r = bounds.get();
            switch (pt.kind()) {
                case CORNER:
                    ShapeSnapPointEntry en = pt.value();
                    CornerAngle ca = CornerAngle.decodeCornerAngle(en.sizeOrAngle).normalized();
                    ShapeEntry se = en.entry;
                    int ct = se.getControlPointCount();
                    int px1 = en.controlPoint1;
                    int px2 = en.controlPoint2;
                    if (px2 == -1) {
                        px2 = ct - 1;
                    }
                    ShapeControlPoint[] pts = se.controlPoints(0, null);
                    if (px1 >= 0 && px1 < pts.length) {
                        ShapeControlPoint pt1 = pts[px1];
                        pie1.setAngleAndExtent(ca.trailingAngle(), ca.extent());
                        pie1.setRadius(20);
                        pie1.setCenter(pt1.getX(), pt1.getY());
                        hasPie1 = true;
                    } else {
                        hasPie1 = false;
                    }
                    pie2.setAngleAndExtent(ca.trailingAngle(), ca.extent());
                    pie2.setRadius(20);
//                        pie1.setCenter(pt1.getX(), pt1.getY());
                    switch (pt.axis()) {
                        case X:
                            pie2.setCenterX(pt.coordinate());
                            break;
                        case Y:
                            pie2.setCenterY(pt.coordinate());
                            break;
                    }
                    hasPie2 = true;
                    break;
                case DISTANCE:
                    line.x1 = line.x2 = line.y1 = line.y2 = -100000;
                    arrow.headAngleA = RULE_ANGLE;
                    arrow.headAngleB = RULE_ANGLE;
                    ShapeSnapPointEntry e = pt.value();
                    if (e.controlPoint1 < 0 || e.controlPoint2 < 0) {
                        Rectangle bds = e.entry.getBounds();
                        switch (pt.axis()) {
                            case X:
                                arrow.x1 = bds.x;
                                arrow.x2 = bds.x + bds.width;
                                arrow.y1 = arrow.y2 = bds.height + bds.y + arrow.headLength;
                                break;
                            case Y:
                                arrow.y1 = bds.y;
                                arrow.y2 = bds.y + bds.height;
                                arrow.x1 = arrow.x2 = bds.width + bds.x + arrow.headLength;
                                break;
                        }
                    } else {
                        double[] d = new double[e.entry.getControlPointCount() * 2];
                        e.entry.vect.as(Adjustable.class).getControlPoints(d);
                        int ix1 = e.controlPoint1 * 2;
                        int ix2 = e.controlPoint2 * 2;
                        arrow.x1 = d[ix1];
                        arrow.x2 = d[ix2];
                        arrow.y1 = d[ix1 + 1];
                        arrow.y2 = d[ix2 + 1];
                    }
                    break;
                case ANGLE:
                    line.x1 = line.x2 = line.y1 = line.y2
                            = arrow.x1 = arrow.x2 = arrow.y1 = arrow.y2
                            = -100000;
                    e = pt.value();
                    double[] d = new double[e.entry.getControlPointCount() * 2];
                    e.entry.vect.as(Adjustable.class).getControlPoints(d);
                    int ix1 = e.controlPoint1 * 2;
                    int ix2 = e.controlPoint2 * 2;
                    arrow.x1 = d[ix1] + 5;
                    arrow.x2 = d[ix2] + 5;
                    arrow.y1 = d[ix1 + 1] + 5;
                    arrow.y2 = d[ix2 + 1] + 5;
                    break;
                case GRID:
                    line.x1 = line.x2 = line.y1 = line.y2
                            = arrow.x1 = arrow.x2 = arrow.y1 = arrow.y2
                            = -100000;
                    switch (pt.axis()) {
                        case X:
                            circle.setCenter(pt.coordinate(), circle.centerY());
                            circle.setRadius(5);
                            break;
                        case Y:
                            circle.setCenter(circle.centerX(), pt.coordinate());
                            circle.setRadius(5);
                            break;
                    }
                    break;
                case POSITION:
                    arrow.headAngleA = ARROW_ANGLE;
                    arrow.headAngleB = ARROW_ANGLE;
                    switch (pt.axis()) {
                        case X:
                            line.x1 = pt.coordinate();
                            line.x2 = pt.coordinate();
                            line.y1 = r.y;
                            line.y2 = r.y + r.height;
                            arrow.y1 = r.y + (r.height / 2);
                            arrow.y2 = arrow.y1;
                            arrow.x1 = r.x + 3;
                            arrow.x2 = pt.coordinate() - 3;
                            break;
                        case Y:
                            line.y1 = pt.coordinate();
                            line.y2 = pt.coordinate();
                            line.x1 = r.x;
                            line.x2 = r.x + r.width;
                            arrow.x1 = r.x + (r.height / 2);
                            arrow.x2 = arrow.x1 + 3;
                            arrow.y1 = r.y;
                            arrow.y2 = pt.coordinate() - 3;
                            break;
                        default:
                            throw new AssertionError(pt.axis());
                    }
                    break;
            }
            repaint();
        }
    }

    private Rectangle boundsOf(Line2D.Double l) {
        Rectangle r = l.getBounds();
        if (l.x1 == l.x2) {
            r.height += 2;
            r.y -= 1;
        } else {
            r.x -= 1;
            r.width += 2;
        }
        return r;
    }

    private void repaint() {
        if (xp != null) {
            switch (xp.kind()) {
                case GRID:
                    handle.repaintArea(circle);
                    break;
                case POSITION:
                    handle.repaintArea(x);
                    break;
                case CORNER:
                    if (hasPie1) {
                        handle.repaintArea(pie1);
                    }
                    if (hasPie2) {
                        handle.repaintArea(pie2);
                    }
                    break;
                case ANGLE:
                case DISTANCE:
                    handle.repaintArea(xArrow);
                    break;
            }
        }
        if (yp != null) {
            switch (yp.kind()) {
                case ANGLE:
                case DISTANCE:
                    handle.repaintArea(yArrow);
                    break;
                case POSITION:
                    handle.repaintArea(y);
                    break;
                case CORNER:
                    if (hasPie1) {
                        handle.repaintArea(pie1);
                    }
                    if (hasPie2) {
                        handle.repaintArea(pie2);
                    }
                    break;
            }
        }
        if (yp != null && yp.kind() == SnapKind.POSITION) {
            if (xp != null) {
                handle.repaintArea(xArrow);
            }
            if (yp != null) {
                handle.repaintArea(yArrow);
            }
        }
    }

    private Rectangle lastBoundsPainted = new Rectangle();

    public void paint(Graphics2D g, Zoom zoom) {
        lastBoundsPainted.setRect(0, 0, 0, 0);
        if (xp == null && yp == null) {
            return;
        }
        float factor = zoom.inverseScale(1);
        Stroke old = g.getStroke();
        BasicStroke stroke = new BasicStroke(factor,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL,
                1F, new float[]{5F * factor, 4F * factor, 5F * factor}, 0);

        if (xp != null && xp.kind() == CORNER || yp != null && yp.kind() == CORNER) {
            g.setColor(Color.BLACK);
            if (hasPie1) {
                g.draw(pie1);
            }
            if (hasPie2) {
                g.draw(pie2);
            }
        }

        g.setColor(Color.BLACK);
        if (xp != null) {
            switch (xp.kind()) {
                case GRID:
                    g.setXORMode(Color.GREEN);
                    g.fill(circle);
                    g.setXORMode(Color.RED);
                    g.draw(circle);
                    lastBoundsPainted.add(circle.getBounds());
                    g.setPaintMode();
                    break;
                case POSITION:
                    g.setStroke(stroke);
                    g.setColor(Color.BLACK);
                    g.draw(x);
                    lastBoundsPainted.add(x.getBounds());
                    break;
                case ANGLE:
                case DISTANCE:
                    g.setColor(Color.BLACK);
                    g.draw(xArrow);
                    lastBoundsPainted.add(xArrow.getBounds());
                    break;
            }
        }
        if (yp != null) {
            switch (yp.kind()) {
                case ANGLE:
                case DISTANCE:
                    g.setColor(Color.BLACK);
                    g.draw(yArrow);
                    lastBoundsPainted.add(yArrow.getBounds());
                    break;
                case POSITION:
                    g.setStroke(stroke);
                    g.setColor(Color.BLACK);
                    g.draw(y);
                    lastBoundsPainted.add(y.getBounds());
                    break;
            }
        }
        g.setStroke(old);
        if (yp != null && yp.kind() == SnapKind.POSITION) {
            g.setColor(Color.GRAY);
            if (xp != null) {
                g.draw(xArrow);
                lastBoundsPainted.add(xArrow.getBounds());
            }
            if (yp != null) {
                g.draw(yArrow);
                lastBoundsPainted.add(yArrow.getBounds());
            }
        }
    }

    @Override
    public boolean onSnap(SnapCoordinate<ShapeSnapPointEntry> xp, SnapCoordinate<ShapeSnapPointEntry> yp) {
        repaint();
        this.xp = xp;
        this.yp = yp;
        setSnapPoint(xp, x, xArrow);
        setSnapPoint(yp, y, yArrow);
        repaint();
        SnapStatusLineElementProvider.setSnapPoints(xp, yp);
        return true;
    }
}
