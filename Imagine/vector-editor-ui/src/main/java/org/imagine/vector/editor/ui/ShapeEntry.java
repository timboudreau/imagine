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
import net.dev.java.imagine.api.tool.aspects.SnapPointsConsumer;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ControlPointController;
import net.java.dev.imagine.api.vector.design.ControlPointFactory;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.util.Size;
import org.imagine.editor.api.PaintingStyle;

/**
 *
 * @author Tim Boudreau
 */
public final class ShapeEntry implements Hibernator, ShapeElement {

    private static long IDS = 0;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private PaintReference bg;
    private PaintReference fg;
    private PaintingStyle paintingStyle;
    Shaped vect;
    private Rectangle2D.Double bds = new Rectangle2D.Double();
    private Shape shape;
    private BasicStroke stroke;
    private final long id;
    private static final ControlPointFactory CPF = new ShapeElementControlPointFactory();

    ShapeEntry(Shaped vector, Paint background, Paint foreground, BasicStroke stroke, boolean draw, boolean fill) {
        id = IDS++;
        if (background != null) {
            bg = new PaintReference(background);
        }
        if (foreground != null) {
            fg = new PaintReference(foreground);
        }
        this.vect = vector;
        vect.getBounds(bds);
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
    }

    ShapeEntry(Shaped vector, PaintReference bg, PaintReference fg, boolean draw, boolean fill, BasicStroke stroke) {
        id = IDS++;
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
    }

    ShapeEntry(Shaped vector, PaintReference bg, PaintReference fg, boolean draw, boolean fill, BasicStroke stroke, long id) {
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
        this.id = id;
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
        bg = fill == null ? null : new PaintReference(fill);
    }

    @Override
    public void setDraw(Paint draw) {
        fg = draw == null ? null : new PaintReference(draw);
    }

    @Override
    public Paint getFill() {
        return bg == null ? TRANSPARENT : bg.get();
    }

    @Override
    public Paint getDraw() {
        return fg == null ? TRANSPARENT : fg.get();
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
    public ControlPoint[] controlPoints(double size, Consumer<ControlPoint> c) {
        if (vect instanceof Adjustable) {
            Adjustable adj = (Adjustable) vect;
            return CPF.getControlPoints(adj, new ControlPointController() {
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
        return new ControlPoint[0];
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
            fg = new PaintReference(paint);
            paintingStyle = paintingStyle.andDrawn();
        }
    }

    @Override
    public Paint fill() {
        return bg == null ? null : bg.get();
    }

    @Override
    public Paint outline() {
        return fg == null ? null : fg.get();
    }

    @Override
    public ShapeEntry copy() {
        return new ShapeEntry(vect.copy(), copy(bg), copy(fg),
                paintingStyle.isOutline(), paintingStyle.isFill(),
                stroke, id);
    }

    @Override
    public ShapeEntry duplicate() {
        return new ShapeEntry(vect.copy(), copy(bg), copy(fg),
                paintingStyle.isOutline(), paintingStyle.isFill(),
                stroke);
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

    private PaintReference copy(PaintReference orig) {
        return orig == null ? null : orig.copy();
    }

    @Override
    public void setShape(Shaped shape) {
        if (shape != vect) {
            vect = shape;
            changed();
        }
    }

    void addTo(SnapPointsConsumer.SnapPoints.Builder bldr) {
        if (vect instanceof Adjustable) {
            Adjustable adj = (Adjustable) vect;
            int cpCount = adj.getControlPointCount();
            double[] pts = new double[cpCount * 2];
            adj.getControlPoints(pts);
            int[] virt = adj.getVirtualControlPointIndices();
            for (int i = 0; i < cpCount * 2; i += 2) {
                int ptIx = i / 2;
                if (Arrays.binarySearch(virt, ptIx) < 0) {
                    bldr.add(SnapPointsConsumer.Axis.X, pts[i]);
                    bldr.add(SnapPointsConsumer.Axis.Y, pts[i + 1]);
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
            g.setPaint(bg.get());
        }
        if (isFill()) {
            g.fill(sh);
        }
        if (fg != null) {
            g.setPaint(fg.get());
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
        if (bg != null) {
            bg.hibernate();
        }
        if (fg != null) {
            fg.hibernate();
        }
    }

    @Override
    public void wakeup(boolean immediately, Runnable notify) {
        if (bg != null) {
            bg.wakeup(immediately, notify);
        }
        if (fg != null) {
            fg.wakeup(immediately, notify);
        }
        synchronized (this) {
            shape = vect.toShape();
            updateBoundsFromShape(shape);
        }
    }

    @Override
    public int hashCode() {
//        int hash = 7;
//        hash = 71 * hash + Objects.hashCode(this.bg);
//        hash = 71 * hash + Objects.hashCode(this.fg);
//        hash = 71 * hash + Objects.hashCode(this.paintingStyle);
//        hash = 71 * hash + Objects.hashCode(this.vect);
//        hash = 71 * hash + Objects.hashCode(this.stroke);
//        return hash;
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

//        if (obj instanceof ShapeElement && !(obj instanceof ShapeEntry)) {
//            obj = Wrapper.find(obj, ShapeEntry.class);
//            if (obj == null) {
//                return false;
//            }
//        }
//        final ShapeEntry other = (ShapeEntry) obj;
//        if (!Objects.equals(this.bg, other.bg)) {
//            return false;
//        } else if (!Objects.equals(this.fg, other.fg)) {
//            return false;
//        } else if (this.paintingStyle != other.paintingStyle) {
//            return false;
//        } else if (!Objects.equals(this.vect, other.vect)) {
//            return false;
//        } else {
//            return Objects.equals(this.stroke, other.stroke);
//        }
    }
}
