/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.snap;

import com.mastfrog.function.DoubleBiConsumer;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.Supplier;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapAxis;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapPoint;
import org.imagine.geometry.Arrow;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
public class PositionPainter extends OneTypePainter {

    protected final Circle circle = new Circle();
    private final AxisPainter x = new XPainter();
    private final AxisPainter y = new YPainter();
    private boolean xActive, yActive;
    private final Supplier<Rectangle> bds;

    PositionPainter(Supplier<Rectangle> bds) {
        this.bds = bds;
    }

    @Override
    protected void requestRepaint(RepaintHandle handle) {
        if (xActive && yActive) {
            handle.repaintArea(circle);
        }
        if (xActive) {
            x.requestRepaint(handle);
        }
        if (yActive) {
            y.requestRepaint(handle);
        }
    }

    @Override
    protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
        SnapUISettings settings = SnapUISettings.getInstance();
        BasicStroke stroke = zoom.getStroke(settings.pointStrokeSize(SnapKind.MATCH));
        g.setStroke(stroke);
        g.setPaint(settings.fillColor(SnapKind.MATCH, selected));
        if (xActive && yActive) {
            g.fill(circle);
        }
        g.setPaint(settings.drawColor(SnapKind.MATCH, selected));
        if (xActive && yActive) {
            g.draw(circle);
        }
        if (xActive) {
            x.paint(g, zoom, selected);
        }
        if (yActive) {
            y.paint(g, zoom, selected);
        }
    }

    @Override
    protected boolean snapBoth(SnapPoint<ShapeSnapPointEntry> x, SnapPoint<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        xActive = false;
        yActive = false;
        // bitwise or intentional
        boolean xSnap = snapOne(x, zoom, selection);
        boolean ySnap = snapOne(y, zoom, selection);
        boolean result = xSnap || ySnap;
        if (result) {
            circle.setRadius(zoom.inverseScale(SnapUISettings.getInstance().targetPointRadius(SnapKind.MATCH, selection)));
            circle.setCenter(x.coordinate(), y.coordinate());
        } else {
            circle.setCenter(-100000, -10000);
            circle.setRadius(0);
        }
        if (xSnap && ySnap) {
            if (x.value().entry != null && x.value().entry.equals(y.value().entry)) {
                this.y.setCircleActive(false);
            } else {
                this.y.setCircleActive(true);
            }
        } else {
            this.y.setCircleActive(true);
        }
        return result;
    }

    @Override
    protected boolean snapX(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        boolean result = super.snapX(s, zoom, selection);
        yActive = false;
        return result;
    }

    @Override
    protected boolean snapY(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        boolean result = super.snapY(s, zoom, selection);
        xActive = false;
        return result;
    }

    @Override
    protected boolean snapOne(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        Rectangle sceneBounds = bds.get();
        boolean result;
        switch (s.axis()) {
            case X:
                result = x.snap(s, zoom, selection, sceneBounds);
                if (result) {
                    xActive = true;
                }
                break;
            case Y:
                result = y.snap(s, zoom, selection, sceneBounds);
                if (result) {
                    yActive = true;
                }
                break;
            default:
                throw new AssertionError(s.axis());
        }
        return result;
    }

    static abstract class AxisPainter {

        private static final double ARROW_HEAD_BASE_SIZE = 9;
        protected final Arrow arrow = new Arrow();
        protected final EqLine line = new EqLine();
        protected final Circle circle = new Circle();
        protected final TextPainter text = new TextPainter();
        private final Rectangle lastBounds = new Rectangle();
        private boolean circleActive = true;
        private double[] controlPointsCache = new double[6];

        AxisPainter() {
            arrow.headAngleA = 15;
            arrow.headAngleB = 15;
            arrow.headLength = ARROW_HEAD_BASE_SIZE;
        }

        void setCircleActive(boolean val) {
            circleActive = val;
        }

        protected void requestRepaint(RepaintHandle handle) {
            handle.repaintArea(arrow);
            handle.repaintArea(line);
            text.requestRepaint(handle);
            if (circleActive) {
                handle.repaintArea(circle);
            }
        }

        protected void paint(Graphics2D g, Zoom zoom, ShapeElement selected) {
            SnapUISettings settings = SnapUISettings.getInstance();
            arrow.headLength = zoom.inverseScale(ARROW_HEAD_BASE_SIZE);
            g.draw(arrow);
            g.setPaint(settings.indicatorLineColor(SnapKind.MATCH, selected));
            g.setStroke(settings.indicatorStroke(SnapKind.MATCH, selected, zoom));
            g.draw(line);

            if (circleActive) {
                g.setPaint(settings.originFillColor(SnapKind.MATCH, selected));
                g.fill(circle);
                g.setStroke(settings.decorationStroke(SnapKind.MATCH, selected, zoom));
                g.setPaint(settings.originDrawColor(SnapKind.MATCH, selected));
                g.draw(circle);
            }
            g.setPaint(settings.drawColor(SnapKind.MATCH, selected));
            text.paint(g, zoom, arrow, this instanceof XPainter ? SnapAxis.X : SnapAxis.Y, lastBounds);
        }

        protected final boolean snap(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom,
                ShapeElement selection, Rectangle sceneBounds) {
            SnapUISettings settings = SnapUISettings.getInstance();
            this.lastBounds.setBounds(sceneBounds);
            text.setValue(s.coordinate());
            circle.setRadius(zoom.scale(settings
                    .targetPointRadius(SnapKind.MATCH, selection)));
            return doSnap(s, zoom, selection, sceneBounds);
        }

        protected abstract boolean doSnap(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom,
                ShapeElement selection, Rectangle sceneBounds);

        protected boolean withOriginPoint(SnapPoint<ShapeSnapPointEntry> s, DoubleBiConsumer c) {
            boolean hasAdjustable = s.value() != null && s.value().entry
                    != null && s.value().entry.item().is(Adjustable.class);
            if (hasAdjustable) {
                Adjustable adj = s.value().entry.item().as(Adjustable.class);
                int cpCount = adj.getControlPointCount();
                int targetArraySize = cpCount * 2;
                if (controlPointsCache.length < targetArraySize) {
                    controlPointsCache = new double[targetArraySize];
                }
                adj.getControlPoints(controlPointsCache);
                int whichControlPoint = s.value().controlPoint1;
                if (whichControlPoint < 0) {
                    whichControlPoint = cpCount - 1;
                }
                double cx = controlPointsCache[whichControlPoint * 2];
                double cy = controlPointsCache[(whichControlPoint * 2) + 1];
                c.accept(cx, cy);
                return true;
            }
            return false;

        }

        protected void configureCircle(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection, Rectangle sceneBounds) {
            boolean hasOrigin = withOriginPoint(s, (cx, cy) -> {
                SnapUISettings settings = SnapUISettings.getInstance();
                double radius = settings.originPointRadius(SnapKind.MATCH, selection, zoom);
                circle.setCenterAndRadius(cx, cy, radius);
            });
            if (!hasOrigin) {
                deconfigureCircle();
            }
        }

        protected void deconfigureCircle() {
            circle.setRadius(0);
            circle.setCenter(-100000, -100000);
        }
    }

    static class XPainter extends AxisPainter {

        @Override
        protected boolean doSnap(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection, Rectangle sceneBounds) {
            configureCircle(s, zoom, selection, sceneBounds);
            return withOriginPoint(s, (ox, oy) -> {
                line.x1 = line.x2 = s.coordinate();
                line.y1 = sceneBounds.y;
                line.y2 = sceneBounds.y + sceneBounds.height;
                arrow.x1 = sceneBounds.x + 5;
                if (oy < sceneBounds.y) {
                    oy = sceneBounds.y + (sceneBounds.height / 2);
                }
                arrow.y1 = arrow.y2 = oy;
                arrow.x2 = ox - 6;
            });
        }

    }

    static class YPainter extends AxisPainter {

        @Override
        protected boolean doSnap(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection, Rectangle sceneBounds) {
            configureCircle(s, zoom, selection, sceneBounds);
            return withOriginPoint(s, (ox, oy) -> {
                line.y1 = line.y2 = s.coordinate();
                line.x1 = sceneBounds.x;
                line.x2 = sceneBounds.x + sceneBounds.width;
                arrow.x1 = arrow.x2 = ox;
                arrow.y1 = sceneBounds.y + 5;
                arrow.y2 = oy - 6;
            });
        }
    }
}
