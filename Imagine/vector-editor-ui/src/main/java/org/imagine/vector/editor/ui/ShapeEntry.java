package org.imagine.vector.editor.ui;

import org.imagine.vector.editor.ui.spi.ShapeElement;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.function.Consumer;
import net.dev.java.imagine.api.tool.aspects.snap.Axis;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoints;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPointsConsumer;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ControlPointController;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.util.Size;
import org.imagine.awt.GradientManager;
import org.imagine.awt.key.PaintKey;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;

/**
 *
 * @author Tim Boudreau
 */
public final class ShapeEntry implements Hibernator, ShapeElement {

    private static long IDS = 0 /* - System.currentTimeMillis() */;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private PaintKey<?> bg;
    private PaintKey<?> fg;
    private PaintingStyle paintingStyle;
    Shaped vect;
    private Rectangle2D.Double bds = new Rectangle2D.Double();
    private Shape shape;
    private BasicStroke stroke;
    private String name;
    private final long id;
    private static final ShapeElementControlPointFactory CPF
            = new ShapeElementControlPointFactory();

    ShapeEntry(Shaped vector, Paint background, Paint foreground, BasicStroke stroke, boolean draw, boolean fill) {
        id = IDS++;
        if (background != null) {
            bg = PaintKey.forPaint(background);
        }
        if (foreground != null) {
            fg = PaintKey.forPaint(foreground);
        }
        this.vect = vector;
        vect.getBounds(bds);
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
    }

    ShapeEntry(Shaped vector, PaintKey<?> bg, PaintKey<?> fg, boolean draw, boolean fill, BasicStroke stroke, String name) {
        id = IDS++;
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
        this.name = name;
    }

    ShapeEntry(Shaped vector, PaintKey<?> bg, PaintKey<?> fg, boolean draw, boolean fill, BasicStroke stroke, long id, String name) {
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
        this.id = id;
        this.name = name;
    }

    @Override
    public void addToBounds(Rectangle2D r) {
        vect.addToBounds(r);
    }

    @Override
    public boolean isNameSet() {
        return name != null;
    }

    @Override
    public Runnable restorableSnapshot() {
        PaintKey<?> obg = bg;
        PaintKey<?> ofg = fg;
        Runnable vectSnap = vect.restorableSnapshot();
        String on = name;
        BasicStroke os = stroke;
        return () -> {
            bg = obg;
            fg = ofg;
            name = on;
            stroke = os;
            vectSnap.run();
            changed();
        };
    }

    @Override
    public Runnable geometrySnapshot() {
        Runnable r = vect.restorableSnapshot();
        return () -> {
            r.run();
            changed();
        };
    }

    @Override
    public String getName() {
        if (name == null) {
            return ShapeNames.nameOf(vect) + "-" + Long.toString(id, 36);
        }
        return name;
    }

    @Override
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            this.name = null;
        } else {
            this.name = name.trim();
        }
    }

    @Override
    public long id() {
        return id;
    }

    public void setStroke(BasicStroke stroke) {
        this.stroke = stroke;
        changed();
    }

    @Override
    public void setPaintingStyle(PaintingStyle style) {
        this.paintingStyle = style;
    }

    @Override
    public PaintingStyle getPaintingStyle() {
        return paintingStyle;
    }

    @Override
    public void setFill(Paint fill) {
        bg = fill == null ? null : PaintKey.forPaint(fill);
    }

    @Override
    public void setDraw(Paint draw) {
        fg = draw == null ? null : PaintKey.forPaint(draw);
    }

    @Override
    public Paint getFill() {
        return bg == null ? TRANSPARENT : GradientManager.getDefault().findPaint(bg);
    }

    @Override
    public Paint getDraw() {
        return fg == null ? TRANSPARENT : GradientManager.getDefault().findPaint(fg);
    }

    private void updateBoundsFromShape(Shape s) {
        bds.setFrame(s.getBounds2D());
        if (stroke != null) {
            double width = stroke.getLineWidth()
                    + stroke.getMiterLimit();
            double w2 = width * 2;
            bds.x -= width;
            bds.y -= width;
            bds.height += w2;
            bds.width += w2;
        }
    }

    @Override
    public void translate(double x, double y) {
        vect.as(Transformable.class, t -> {
            t.translate(x, y);
            shape = null;
            updateBoundsFromShape(shape());
        });
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        vect.as(Transformable.class, t -> {
            t.applyTransform(xform);
            shape = null;
            updateBoundsFromShape(shape());
        });
    }

    @Override
    public boolean canApplyTransform(AffineTransform xform) {
        Transformable t = vect.as(Transformable.class);
        return t != null && t.canApplyTransform(xform);
    }

    @Override
    public void changed() {
        shape = null;
        updateBoundsFromShape(shape());
    }

    @Override
    public int getControlPointCount() {
        Adjustable adj = vect.as(Adjustable.class);
        if (adj != null) {
            return adj.getControlPointCount();
        }
        return 0;
    }

    @Override
    public ShapeControlPoint[] controlPoints(double size, Consumer<ControlPoint> c) {
        if (vect instanceof Adjustable) {
            return CPF.getControlPoints(this, new ControlPointController() {
                @Override
                public void changed(ControlPoint pt) {
                    c.accept(pt);
                    ShapeEntry.this.changed();
                }

                @Override
                public Size getControlPointSize() {
                    return new Size(size, size);
                }
            });
        }
        return new ShapeControlPoint[0];
    }

    public boolean contains(Point2D p) {
        if (vect instanceof Volume) {
            Volume v = (Volume) vect;
            if (v.getBounds().contains(p)) {
                return shape().contains(p);
            }
            return false;
        } else {
            return shape().contains(p);
        }
    }

    @Override
    public Shaped item() {
        return vect;
    }

    @Override
    public String toString() {
        return "Sh(" + id() + " "
                + Integer.toString(System.identityHashCode(this), 36)
                + " " + vect.getClass().getSimpleName()
                + paintingStyle
                + vect + ")";
    }

    @Override
    public boolean isPaths() {
        return vect instanceof PathIteratorWrapper;
    }

    @Override
    public void toPaths() {
        if (!(vect instanceof PathIteratorWrapper)) {
            vect = new PathIteratorWrapper(vect.toShape().getPathIterator(null), isFill());
            changed();
        }
    }

    void setOutline(Paint paint) {
        if (paint != null) {
            fg = PaintKey.forPaint(paint);
            paintingStyle = paintingStyle.andDrawn();
        }
    }

    @Override
    public Paint fill() {
        return bg == null ? null : GradientManager.getDefault().findPaint(bg);
    }

    @Override
    public Paint outline() {
        return fg == null ? null : GradientManager.getDefault().findPaint(bg);
    }

    @Override
    public ShapeEntry copy() {
        return new ShapeEntry(vect.copy(), bg, fg,
                paintingStyle.isOutline(), paintingStyle.isFill(),
                stroke, id, name);
    }

    @Override
    public ShapeEntry duplicate() {
        return new ShapeEntry(vect.copy(), bg, fg,
                paintingStyle.isOutline(), paintingStyle.isFill(),
                stroke, name);
    }

    @Override
    public BasicStroke stroke() {
        return stroke;
    }

    @Override
    public boolean isFill() {
        return paintingStyle.isFill();
    }

    @Override
    public boolean isDraw() {
        return paintingStyle.isOutline();
    }

    @Override
    public void setShape(Shaped shape) {
        if (shape != vect) {
            vect = shape;
            changed();
        }
    }

    void addTo(SnapPoints.Builder bldr) {
        if (vect instanceof Adjustable) {
            Adjustable adj = (Adjustable) vect;
            int cpCount = adj.getControlPointCount();
            double[] pts = new double[cpCount * 2];
            adj.getControlPoints(pts);
            int[] virt = adj.getVirtualControlPointIndices();
            for (int i = 0; i < pts.length; i += 2) {
                int ptIx = i / 2;
                if (Arrays.binarySearch(virt, ptIx) < 0) {
                    bldr.add(Axis.X, pts[i]);
                    bldr.add(Axis.Y, pts[i + 1]);
                }
            }
        }
    }

    @Override
    public boolean paint(Graphics2D g, Rectangle bounds) {
        Shape sh = shape();
        //            System.out.println("paint shape " + sh);
        if (bds != null && !sh.intersects(bds)) {
//            System.out.println("  does not intersect");
//            return false;
        }
        Stroke oldStroke = null;
        if (stroke != null) {
            oldStroke = g.getStroke();
            g.setStroke(stroke);
        }
        if (bg != null) {
            g.setPaint(GradientManager.getDefault().findPaint(bg));
        }
        if (isFill()) {
            g.fill(sh);
        }
        if (fg != null) {
            g.setPaint(GradientManager.getDefault().findPaint(fg));
        }
        if (isDraw()) {
            g.draw(sh);
        }
        if (oldStroke != null) {
            g.setStroke(oldStroke);
        }
        return true;
    }

    @Override
    public Shape shape() {
        if (shape == null) {
            synchronized (this) {
                if (shape == null) {
                    shape = vect.toShape();
                }
            }
        }
        return shape;
    }

    @Override
    public Rectangle getBounds() {
        Rectangle2D.Double b = bds;
        if (b == null || b.getWidth() == 0 || b.getHeight() == 0) {
            b = bds = new Rectangle2D.Double();
            vect.getBounds(b);
            return b.getBounds();
        }
        return b.getBounds();
    }

    @Override
    public void hibernate() {
        synchronized (this) {
            shape = null;
        }
    }

    @Override
    public void wakeup(boolean immediately, Runnable notify) {
        synchronized (this) {
            shape = vect.toShape();
            updateBoundsFromShape(shape);
        }
    }

    @Override
    public int hashCode() {
        long idx = id * 99371;
        return (int) (idx ^ (idx >> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof ShapeEntry)) {
            return false;
        }
        return id == ((ShapeEntry) obj).id();
    }
}
