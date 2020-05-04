package org.netbeans.paint.tools.path;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.Polygon2D;
import org.imagine.geometry.Triangle2D;
import org.imagine.geometry.util.DoubleList;

/**
 * Holds the model of a shape as it is being drawn point by point.
 */
final class VectorPathModel {

    private final List<VectorPathEntry> entries = new ArrayList<>();
    private final Rectangle bounds = new Rectangle();
    private Shape stroked;
    private final Circle scratchCircle = new Circle(0, 0, 6);
    private final Supplier<PathUIProperties> colors;
    private boolean containsCurves;
    private boolean isClosed;

    VectorPathModel(EqPointDouble p, Supplier<PathUIProperties> colors) {
        this.colors = colors;
        addPoint(p, PathIterator.SEG_MOVETO, null);
    }

    public int size() {
        return entries.size();
    }

    public VectorPathEntry entry(int index) {
        return entries.get(index);
    }

    public EqPointDouble firstPoint() {
        if (entries.isEmpty()) {
            return null;
        }
        return entries.get(0).lastPoint();
    }

    public VectorPathModel removeLast() {
        if (!entries.isEmpty()) {
            entries.remove(entries.size() - 1);
        }
        return this;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Rectangle paint(Graphics2D g, Rectangle clip, boolean isCommit) {
        Rectangle painted = new Rectangle();
        Shape path = toPath();
        if (path != null) {
            g.draw(path);
            painted.add(path.getBounds());
        }
        if (!isCommit) {
            Stroke oldStroke = g.getStroke();
            Paint oldPaint = g.getPaint();
            PathUIProperties colors = this.colors.get();
            double radius = colors.pointRadius();
            scratchCircle.setRadius(radius);
            BasicStroke stroke = colors.lineStroke();
            BasicStroke dashed = colors.connectorStroke();
            try {
                g.setStroke(stroke);
                for (int i = 0; i < entries.size(); i++) {
                    VectorPathEntry se = entries.get(i);
                    Color fill = i == 0 ? colors.initialPointFill()
                            : colors.destinationPointFill();
                    se.paintPoints(g, scratchCircle, colors.controlPointFill(), fill,
                            colors.controlPointDraw(), colors.destinationPointDraw(), clip, painted, dashed);
                }
            } finally {
                if (oldStroke != null) {
                    g.setStroke(oldStroke);
                }
                g.setPaint(oldPaint);
            }
        }
        return painted;
    }

    public Hit hit(EqPointDouble p, Consumer<Rectangle> repainter) {
        for (VectorPathEntry e : entries) {
            Hit result = e.hit(p.getX(), p.getY(), 5, bounds, (r) -> {
                bounds.setBounds(r);
                repainter.accept(r);
            });
            if (result != null) {
                return result;
            }
        }
        if (stroked != null && stroked.contains(p.x, p.y)) {
            return Hit.NON_POINT;
        }
        return null;
    }

    public EqPointDouble lastPoint() {
        if (entries.size() == 1) {
            return firstPoint();
        }
        int ix = entries.size() - 1;
        EqPointDouble result = null;
        while (result == null && ix > 0) {
            result = entries.get(ix).lastPoint();
        }
        return result;
    }

    public Shape toPath() {
        if (entries.size() == 1) {
            return null;
        }
        if (!containsCurves) {
            if (entries.size() == 3 || (entries.size() == 4 && entries.get(3).type() == PathIterator.SEG_CLOSE)) {
                Triangle2D tri = new Triangle2D(entries.get(0).lastPoint(),
                        entries.get(1).lastPoint(), entries.get(2).lastPoint());
                tri.normalize();
                return tri;
            }
            // If no curves, Polygon2D is lighter-weight, more performant
            // and offers a few additional editor features
            DoubleList lst = new DoubleList(entries.size() * 2);
            for (VectorPathEntry e : entries) {
                EqPointDouble p = e.lastPoint();
                lst.add(p.x);
                lst.add(p.y);
            }
            Polygon2D p2d = new Polygon2D(lst.toDoubleArray());
            p2d.normalize();
            return p2d;
        }
        Path2D.Double pth = new Path2D.Double();
        for (VectorPathEntry se : entries) {
            se.addTo(pth);
        }
        BasicStroke hitStroke = colors.get().ctx().zoom().getStroke(10F);
        stroked = hitStroke.createStrokedShape(pth);
        return pth;
    }

    public PointEditor addPoint(EqPointDouble point, int type, Consumer<Rectangle> onRepaint) {
        point = point == null ? null : new EqPointDouble(point);
        if (entries.isEmpty()) {
            type = PathIterator.SEG_MOVETO;
        }
        if (type != PathIterator.SEG_CLOSE) {
            bounds.add(point);
        }
        VectorPathEntry entry;
        switch (type) {
            case PathIterator.SEG_MOVETO:
            case PathIterator.SEG_LINETO:
                entry = new VectorPathEntry(type, colors, point.copy());
                break;
            case PathIterator.SEG_CUBICTO:
                containsCurves = true;
                entry = new VectorPathEntry(type, colors, point.copy(), point.copy(), point.copy());
                break;
            case PathIterator.SEG_QUADTO:
                containsCurves = true;
                entry = new VectorPathEntry(type, colors, point.copy(), point.copy());
                break;
            case PathIterator.SEG_CLOSE:
                isClosed = true;
                entry = new VectorPathEntry(type, colors);
                break;
            default:
                throw new AssertionError("Unknown type " + type);
        }
        System.out.println("ADD TO ENTRIES " + entries + ": " + entry);
        if (entries.size() > 0 && entry.equals(entries.get(entries.size()-1))) {
            // WTF?
            new Exception("Duplicate entry: " + entry, entries.get(entries.size()-1).creation).printStackTrace();;
            return entries.get(entries.size()-1).editor(bounds, onRepaint);
        }
        entries.add(entry);
        return entry.editor(bounds, onRepaint);
    }
}
