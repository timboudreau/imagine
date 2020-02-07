package org.imagine.vector.editor.ui;

import org.imagine.vector.editor.ui.spi.ShapeElement;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import net.dev.java.imagine.api.tool.aspects.SnapPointsConsumer;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;

/**
 *
 * @author Tim Boudreau
 */
public final class ShapeEntry implements Hibernator, ShapeElement {

    private PaintReference bg;
    private PaintReference fg;
    Shaped vect;
    private Rectangle2D.Double bds = new Rectangle2D.Double();
    private Shape shape;
    private BasicStroke stroke;
    private boolean draw;
    private boolean fill;

    ShapeEntry(Shaped vector, Paint background, Paint foreground, BasicStroke stroke, boolean draw, boolean fill) {
        if (background != null) {
            bg = new PaintReference(background);
        }
        if (foreground != null) {
            fg = new PaintReference(foreground);
        }
        this.vect = vector;
        vect.getBounds(bds);
        this.stroke = stroke;
        this.draw = draw;
        this.fill = fill;
    }

    ShapeEntry(Shaped vector, PaintReference bg, PaintReference fg, boolean draw, boolean fill, BasicStroke stroke) {
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.fill = fill;
        this.draw = draw;
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

    public String toString() {
        return "Sh(" + vect.getClass().getSimpleName()
                + (draw ? " drawn":"") + (fill ? " filled" :"")
                + vect;
    }

    @Override
    public boolean isPaths() {
        return vect instanceof PathIteratorWrapper;
    }

    @Override
    public void toPaths() {
        vect = new PathIteratorWrapper(vect.toShape().getPathIterator(null), isFill());
    }

    void setOutline(Paint paint) {
        if (paint != null) {
            fg = new PaintReference(paint);
            draw = true;
        }
    }

    @Override
    public ShapeEntry copy() {
        return new ShapeEntry(vect.copy(), copy(bg), copy(fg), draw, fill, stroke);
    }

    @Override
    public BasicStroke stroke() {
        return stroke;
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public boolean isDraw() {
        return draw;
    }

    private PaintReference copy(PaintReference orig) {
        return orig == null ? null : orig.copy();
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
        if (fill) {
            g.fill(sh);
        }
        if (fg != null) {
            g.setPaint(fg.get());
        }
        if (draw) {
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
        if (b == null) {
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
        }
    }

}
