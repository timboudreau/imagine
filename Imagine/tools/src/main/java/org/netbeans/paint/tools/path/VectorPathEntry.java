package org.netbeans.paint.tools.path;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.path.PathElementKind;

/**
 * One element (a point and zero or more control points) of a Path2D - because
 * we don't have enough other classes around that model this exact same thing
 * :-/
 *
 * @author Tim Boudreau
 */
final class VectorPathEntry {

    private final Supplier<PathUIProperties> uiProperties;
    private final EqPointDouble[] points;
    private final int type;
    private int editCursor;
    private final Circle hitCircle = new Circle(0, 0, 1);

    final Exception creation;

    public VectorPathEntry(int type, Supplier<PathUIProperties> uiProperties, EqPointDouble... points) {
        assert uiProperties != null : "Null ui properties, or init before assigned";
        assert type == PathIterator.SEG_LINETO
                || type == PathIterator.SEG_MOVETO
                || type == PathIterator.SEG_CLOSE
                || type == PathIterator.SEG_CUBICTO
                || type == PathIterator.SEG_QUADTO : "Unknown path entry type" + type;
        this.uiProperties = uiProperties;
        this.points = points;
        this.type = type;
        creation = new Exception(toString());
    }

    boolean isStraightLineEndpoint() {
        return type == PathIterator.SEG_LINETO || type == PathIterator.SEG_MOVETO;
    }

    EqPointDouble lastPoint() {
        if (points.length > 0) {
            return points[points.length - 1];
        }
        return null;
    }

    PointEditor hit(double x, double y, int tolerance, Rectangle bds, Consumer<Rectangle> repainter) {
        double rad = uiProperties.get().ctx().zoom().inverseScale(tolerance);
        hitCircle.setRadius(rad);
        for (int i = 0; i < points.length; i++) {
            double dx = Math.abs(x - points[i].getX());
            double dy = Math.abs(y - points[i].getY());
            if (dx <= tolerance && dy <= tolerance) {
                return new PointEditor(points[i], bds, repainter, () -> null);
            }
        }
        return null;
    }
    final EqLine scratchLine = new EqLine();

    void paintPoints(Graphics2D g, Circle circle, Color ctrlFill, Color fill, Color ctrlDraw, Color draw, Rectangle clip, Rectangle addTo, BasicStroke dashStroke) {
        EqPointDouble lp = lastPoint();
        BasicStroke stroke = (BasicStroke) g.getStroke();
        PathUIProperties ui = this.uiProperties.get();
        g.setColor(ui.connectorLineDraw());
        g.setStroke(ui.connectorStroke());
        // Draw control lines
        for (int i = 0; i < points.length - 1; i++) {
            EqPointDouble p = points[i];
            if (clip != null && !clip.contains(p) && !clip.contains(lp)) {
                continue;
            }
            scratchLine.setLine(lp.getX(), lp.getY(), p.getX(), p.getY());
            g.draw(scratchLine);
        }
        g.setStroke(stroke);
        // draw points, coloring control points differently
        for (int i = 0; i < points.length; i++) {
            boolean last = i == points.length - 1;
            EqPointDouble p = points[i];
            if (clip != null && !clip.contains(p)) {
                continue;
            }
            if (!last) {
                g.setColor(ctrlFill);
            } else {
                g.setColor(fill);
            }
            circle.setCenter(p.getX(), p.getY());
            g.fill(circle);
            if (!last) {
                g.setColor(ctrlDraw);
            } else {
                g.setColor(draw);
            }
            g.draw(circle);
            addTo.add(circle.getBounds());
        }
    }

    void addTo(Path2D path) {
        switch (type) {
            case PathIterator.SEG_MOVETO:
                path.moveTo(points[0].getX(), points[0].getY());
                break;
            case PathIterator.SEG_LINETO:
                path.lineTo(points[0].getX(), points[0].getY());
                break;
            case PathIterator.SEG_CUBICTO:
                path.curveTo(points[0].getX(), points[0].getY(), points[1].getX(), points[1].getY(), points[2].getX(), points[2].getY());
                break;
            case PathIterator.SEG_QUADTO:
                path.quadTo(points[0].getX(), points[0].getY(), points[1].getX(), points[1].getY());
                break;
        }
    }

    EqPointDouble currentPoint() {
        if (editCursor >= points.length - 1) {
            return null;
        }
        return points[editCursor];
    }

    EqPointDouble nextPoint() {
        editCursor++;
        return currentPoint();
    }

    PointEditor editor(Rectangle bds, Consumer<Rectangle> onChange) {
        if (points.length < 2) {
            return null;
        }
        DynamicNextPointSupplier supp = new DynamicNextPointSupplier(bds, onChange);
        return new PointEditor(points[0], bds, onChange, supp);
    }

    public int type() {
        return type;
    }

    public String toString() {
        return PathElementKind.of(type) + " " + Arrays.toString(points);
    }

    class DynamicNextPointSupplier implements Supplier<PointEditor> {

        private final Rectangle bds;
        private final Consumer<Rectangle> onChange;

        public DynamicNextPointSupplier(Rectangle bds, Consumer<Rectangle> onChange) {
            this.bds = bds;
            this.onChange = onChange;
        }

        @Override
        public PointEditor get() {
            EqPointDouble np = nextPoint();
            if (np != null) {
                return new PointEditor(np, bds, onChange, this);
            }
            return null;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Arrays.deepHashCode(this.points);
        hash = 83 * hash + this.type;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VectorPathEntry other = (VectorPathEntry) obj;
        if (this.type != other.type) {
            return false;
        }
        if (!Arrays.deepEquals(this.points, other.points)) {
            return false;
        }
        return true;
    }

}
