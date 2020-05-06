package org.netbeans.paint.tools.waffle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.editor.api.CheckerboardBackground;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.netbeans.paint.api.components.Cursors;
import org.netbeans.paint.tools.path.PathUIProperties;
import org.netbeans.paint.tools.responder.PaintingResponder;
import org.netbeans.paint.tools.responder.Responder;
import org.netbeans.paint.tools.responder.ResponderTool;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Waffle", iconPath = "org/netbeans/paint/tools/resources/waffle2.svg",
        category = "vector")
@Tool(value = Surface.class, toolbarPosition = 2200)
public class WaffleTool extends ResponderTool implements Supplier<PathUIProperties> {

    private List<EqPointDouble> points = new ArrayList<>(20);
    private PathUIProperties colors;
    private Path2D.Double lastShape;
    private int hashAtLastShape;
    private static final Customizer<Boolean> close = Customizers.getCustomizer(
            Boolean.class, "Close", true);
    private static final Customizer<Double> frequency = Customizers
            .getCustomizer(Double.class, "Frequency", 1D, 500D, 30D);
    private static final Customizer<Double> offsetX = Customizers
            .getCustomizer(Double.class, "OffsetX", 2D, 250D, 10D);
    private static final Customizer<Double> offsetY = Customizers
            .getCustomizer(Double.class, "OffsetY", 1D, 250D, 20D);

    public WaffleTool(Surface obj) {
        super(obj);
    }

    @Override
    public Customizer<?> getCustomizer() {
        return new AggregateCustomizer<>("Waffle",
                frequency, offsetX, offsetY, fillC, strokeC);
    }

    public PathUIProperties get() {
        if (colors == null) {
            return new PathUIProperties(this::ctx);
        }
        return colors;
    }

    @Override
    protected void onAttach() {
        colors = new PathUIProperties(this::ctx);
    }

    @Override
    protected void onDetach() {
        colors = null;
    }

    @Override
    protected void reset() {
        setCursor(Cursor.getDefaultCursor());
        points.clear();
        lastShape = null;
        hashAtLastShape = -1;
    }

    @Override
    protected Responder firstResponder() {
        return new FirstResponder();
    }

    @Override
    protected void onAttachRepainter(PaintParticipant.Repainter repainter) {
        boolean dark = ImageEditorBackground.getDefault().style() == CheckerboardBackground.DARK;
        Cursors cursors = dark ? Cursors.dark() : Cursors.light();
        repainter.setCursor(cursors.star());
    }

    @Override
    protected Rectangle paintCommit(Graphics2D g) {
        boolean finish = close.get();
        if (finish) {
            finish();
        }
        Path2D shape = currentShape(finish);
        BasicStroke stroke = ResponderTool.strokeC.get();
        g.setStroke(stroke);
        g.setPaint(ResponderTool.paintC.get().getPaint());
        g.draw(shape);
        EnhRectangle2D rect = new EnhRectangle2D();
        rect.add(shape, stroke);
        return rect.getBounds();
    }

    @Override
    protected Rectangle paintLive(Graphics2D g, Rectangle layerBounds) {
        Path2D shape = currentShape(false);
        if (shape == null) {
            return new Rectangle();
        }
        BasicStroke stroke = ResponderTool.strokeC.get();
        if (stroke != null) {
            g.setStroke(stroke);
        }
        Paint p = ResponderTool.paintC.get().getPaint();
        if (p == null) {
            p = Color.BLACK;
        }
        g.setPaint(p);
        g.draw(shape);
        EnhRectangle2D rect = new EnhRectangle2D();
        rect.add(shape, stroke);
        BasicStroke lineStroke = get().lineStroke();
        for (int i = 0; i < points.size(); i++) {
            configureCircle(points.get(i));
            if (layerBounds == null || layerBounds.contains(circ.getBounds())) {
                g.setStroke(lineStroke);
                g.setColor(i == 0 ? get().initialPointFill() : get().destinationPointFill());
                g.fill(circ);
                g.setColor(i == 0 ? get().destinationPointDraw() : get().destinationPointDraw());
                g.draw(circ);
                rect.add(circ, lineStroke);
            }
        }
        return rect.getBounds();
    }

    private final Circle circ = new Circle();

    private class FirstResponder extends Responder {

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            points.add(new EqPointDouble(x, y));
            return new AddPointsResponder();
        }

        @Override
        protected void activate(Rectangle addTo) {
            boolean dark = ImageEditorBackground.getDefault().style() == CheckerboardBackground.DARK;
            Cursors cursors = dark ? Cursors.dark() : Cursors.light();
            setCursor(cursors.star());
        }

        @Override
        protected void resign(Rectangle addTo) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private EnhRectangle2D shapeBounds() {
        return shapeBounds(false);
    }

    private EnhRectangle2D shapeBounds(boolean includePointRadii) {
        Shape shape = currentShape(false);
        EnhRectangle2D result;
        if (shape == null) {
            result = new EnhRectangle2D();
            if (points.size() > 0) {
                for (EqPointDouble pt : points) {
                    repaintPoint(pt);
                }
            }
            return result;
        } else {
            result = EnhRectangle2D.of(shape.getBounds2D());
        }
        if (includePointRadii) {
            BasicStroke stroke = get().lineStroke();
            for (EqPointDouble p : points) {
                configureCircle(p);
                result.add(circ, stroke);
            }
        }
        return result;
    }

    private double circleRadius() {
        return get().pointRadius();
    }

    private void configureCircle(EqPointDouble pt) {
        configureCircle(pt.x, pt.y);
    }

    private void configureCircle(double x, double y) {
        circ.setCenterAndRadius(x, y, circleRadius());
    }

    private void repaintPoint(EqPointDouble pt) {
        repaintPoint(pt.x, pt.y);
    }

    private void repaintPoint(double x, double y) {
        configureCircle(x, y);
        repaint(circ, get().lineStroke());
    }

    private void addAllPoints(EnhRectangle2D to) {
        BasicStroke stroke = get().lineStroke();
        for (EqPointDouble pt : points) {
            configureCircle(pt);
            to.add(circ, stroke);
        }
    }

    private EqPointDouble targetPointWithinRadius(double x, double y) {
        double rad = get().pointRadius();
        EnhRectangle2D rect = new EnhRectangle2D(0, 0, rad, rad);
        rect.setCenter(x, y);
        for (EqPointDouble pt : points) {
            if (rect.contains(pt)) {
                return pt;
            }
        }
        return null;
    }

    abstract class AbstractWaffleToolResponder extends Responder {

        @Override
        protected Responder onKeyPress(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                EnhRectangle2D rect = new EnhRectangle2D();
                addAllPoints(rect);
                rect.add(shapeBounds());
                EqPointDouble hoverPoint = hoverPoint();
                if (hoverPoint != null) {
                    repaintPoint(hoverPoint);
                }
                repaint(rect);
                reset();
                return firstResponder();
            }
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (points.size() > 1) {
                    commit();
                    return new FirstResponder();
                }
            }
            return this;
        }

        protected abstract EqPointDouble hoverPoint();

    }

    private class AddPointsResponder extends AbstractWaffleToolResponder implements PaintingResponder {

        private final EqPointDouble hoverPoint = new EqPointDouble();
        private boolean hoverSet;

        @Override
        protected EqPointDouble hoverPoint() {
            return hoverSet ? hoverPoint : null;
        }

        private class MoveExistingPointResponder extends AbstractWaffleToolResponder {

            private final EqPointDouble targetPoint;
            private final double offX;
            private final double offY;
            private boolean hasDrag;

            MoveExistingPointResponder(EqPointDouble targetPoint, double initX, double initY) {
                this.targetPoint = targetPoint;
                offX = initX - targetPoint.x;
                offY = initY - targetPoint.y;
            }

            @Override
            protected EqPointDouble hoverPoint() {
                return new EqPointDouble(targetPoint);
            }

            @Override
            protected Responder onDrag(double x, double y, MouseEvent e) {
                EnhRectangle2D old = shapeBounds();
                addAllPoints(old);
                BasicStroke lineStroke = get().lineStroke();
                if (!hasDrag) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                hasDrag = true;
                targetPoint.x = x + offX;
                targetPoint.y = y + offY;
                old.add(shapeBounds());
                configureCircle(targetPoint.x, targetPoint.y);
                old.add(circ, lineStroke);
                repaint(old);
                return this;
            }

            @Override
            protected Responder onRelease(double x, double y, MouseEvent e) {
                if (hasDrag) {
                    onDrag(x, y, e);
                }
                return AddPointsResponder.this;
            }

            @Override
            protected void resign(Rectangle addTo) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
        }

        @Override
        protected Responder onPress(double x, double y, MouseEvent e) {
            EqPointDouble target = targetPointWithinRadius(x, y);
            if (target != null) {
                if (hoverSet) {
                    repaintPoint(hoverPoint);
                    Path2D proposal = proposedAddition(hoverPoint);
                    if (proposal != null) {
                        repaint(proposal, get().proposedLineStroke());
                    }
                }
                return new MoveExistingPointResponder(target, x, y);
            }
            return this;
        }

        @Override
        protected void activate(Rectangle addTo) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        @Override
        protected void resign(Rectangle addTo) {
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            hoverPoint.setLocation(x, y);
            hoverSet = true;
            repaintPoint(hoverPoint);
            return this;
        }

        @Override
        protected Responder onEnter(double x, double y, MouseEvent e) {
            hoverSet = true;
            hoverPoint.setLocation(x, y);
            repaintPoint(hoverPoint);
            return this;
        }

        @Override
        protected Responder onExit(double x, double y, MouseEvent e) {
            if (hoverSet) {
                repaintPoint(hoverPoint.x, hoverPoint.y);
            }
            hoverSet = false;
            return this;
        }

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            if (hoverSet) {
                repaintPoint(hoverPoint);
            }
            EqPointDouble pt = new EqPointDouble(x, y);
            EqPointDouble match = targetPointWithinRadius(x, y);
            if (match == points.get(0)) {
                commit();
                return new FirstResponder();
            }
            if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
                commit();
                return new FirstResponder();
            }
            hoverSet = true;
            doRepaint(pt);
            points.add(pt);
            return this;
        }

        private void doRepaint(EqPointDouble pt) {
            doRepaint(pt.x, pt.y);
        }

        private void doRepaint(double currX, double currY) {
            if (hoverSet) {
                repaintPoint(hoverPoint);
                Path2D.Double proposal = proposedAddition(hoverPoint);
                if (proposal != null) {
                    repaint(proposal, get().lineStroke());
                }
                repaint(shapeBounds());
                repaintPoint(currX, currY);
            }
            hoverPoint.setLocation(currX, currY);
        }

        private Path2D.Double proposedAddition(EqPointDouble last) {
            Path2D.Double p2d = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            p2d.moveTo(last.x, last.y);
            applyPoints(points.size() % 2 == 0 ? 0 : 1, last, hoverPoint, p2d);
            return p2d;
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            switch (points.size()) {
                case 0:
                    return new Rectangle();
                case 1:
                    EqPointDouble single = points.iterator().next();
                    repaintPoint(single);
                    if (hoverSet) {
                        repaintPoint(hoverPoint);
                    }
                    return circ.getBounds();
                default:
                    if (hoverSet) {
                        EqPointDouble first = points.get(0);

                        EqPointDouble last = points.get(points.size() - 1);
                        Path2D.Double p2d = proposedAddition(last);
                        BasicStroke stroke = get().proposedLineStroke();
                        g.setStroke(stroke);
                        g.setColor(get().proposedLineDraw());
                        g.draw(p2d);
                        EqLine ln = new EqLine(last, hoverPoint);
                        ln.shiftPerpendicular(ctx().zoom().inverseScale(stroke.getLineWidth()));
                        g.translate(last.x - ln.x1, last.y - ln.y1);
                        g.setColor(get().proposedLineShadow());
                        g.draw(p2d);
                        g.translate(-(last.x - ln.x1), -(last.y - ln.y1));
                        EnhRectangle2D paintedBounds = new EnhRectangle2D();
                        paintedBounds.add(p2d, stroke);
                        configureCircle(hoverPoint);
                        EqPointDouble target = targetPointWithinRadius(hoverPoint.x, hoverPoint.y);
                        if (target == first) {
                            g.setColor(get().hoveredDotFill());
                        } else {
                            g.setColor(get().destinationPointFill());
                        }
                        g.fill(circ);
                        if (target == first) {
                            g.setColor(get().hoveredDotDraw());
                        } else {
                            g.setColor(get().destinationPointDraw());
                        }
                        g.draw(circ);
                        paintedBounds.add(circ, stroke);
                        return paintedBounds.getBounds();
                    }

            }
            return new Rectangle();
        }

    }

    private Path2D.Double currentShape(boolean close) {
        if (points.size() < 2) {
            return null;
        }
        int hash = points.hashCode();
        if (close) {
            hash *= 7;
        }
        if (lastShape != null) {
            if (hash == hashAtLastShape) {
                return lastShape;
            }
        }
        Path2D.Double p = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        Iterator<EqPointDouble> iter = points.iterator();
        EqPointDouble prev = iter.next();
        p.moveTo(prev.x, prev.y);
        int ix = 0;
        while (iter.hasNext()) {
            EqPointDouble curr = iter.next();
            applyPoints(ix++, prev, curr, p);
            prev = curr;
        }
        if (close) {
            p.closePath();
        }
        hashAtLastShape = hash;
        return lastShape = p;
    }

    private void finish() {
        if (!points.isEmpty() && points.size() > 1 && !points.get(0).equals(points.get(points.size() - 1))) {
            points.add(points.get(0));
        }
    }

    private void xapplyPoints(int index, EqPointDouble prev, EqPointDouble curr, Path2D into) {
        EqLine line = new EqLine(prev, curr);
        EqPointDouble cp1 = controlPoint1(index, line);
        EqPointDouble cp2 = controlPoint2(index, line);
        into.curveTo(cp1.getX(), cp1.getY(), cp2.getX(), cp2.getY(), curr.getX(), curr.getY());
    }

    private void applyPoints(int index, EqPointDouble prev, EqPointDouble curr, Path2D into) {
        EqLine line = new EqLine(prev, curr);
        double ang = line.angle();
        double len = line.length();
        double freq = frequency.get();
        double count = len / freq;
        int loops;
        if (count < 1) {
            count = loops = 1;
        } else if (len % freq != 0) {
            loops = (int) Math.floor(count);
            double lastPos = freq * Math.floor(count);
            if (lastPos == 0) {
                lastPos = freq;
                count = loops = 1;
            } else if (lastPos - (freq * loops) > freq % 2) {
                count = ++loops;
            }
        } else {
            loops = (int) Math.floor(count);
        }
        double distancePer = len / count;
        EqPointDouble lastPoint = prev.copy();
        for (int i = 0; i < loops; i++) {
            EqLine seg = EqLine.forAngleAndLength(lastPoint.x, lastPoint.y, ang, distancePer);
            lastPoint = applyPoints(index, i, seg, into);
        }
    }

    private EqPointDouble applyPoints(int index, int subIndex, EqLine seg, Path2D into) {
        int item = index + subIndex;

        EqPointDouble cp1 = controlPoint1(item, seg);
        EqPointDouble cp2 = controlPoint1(item, seg);
        into.curveTo(cp1.x, cp1.y, cp2.x, cp2.y, seg.x2, seg.y2);
        return seg.getP2();
    }

    private EqPointDouble controlPoint1(int lineIndex, EqLine line) {
        double offset = offsetX.get();
        if (lineIndex % 2 == 0) {
            offset *= -1;
        }
        line.shiftPerpendicular(offset);
        EqPointDouble result = line.midPoint();
        line.shiftPerpendicular(-offset);
        return result;

    }

    private EqPointDouble controlPoint2(int lineIndex, EqLine line) {
        double offset = offsetY.get();
        if (lineIndex % 2 != 0) {
            offset *= -1;
        }
        line.shiftPerpendicular(offset);
        EqPointDouble result = line.midPoint();
        line.shiftPerpendicular(-offset);
        return result;
    }
}
