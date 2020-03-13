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
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import org.imagine.editor.api.snap.Axis;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ControlPointController;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import net.java.dev.imagine.api.vector.util.Size;
import org.imagine.awt.GradientManager;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.editor.api.snap.SnapPointsBuilder;
import org.imagine.io.KeyReader;
import org.imagine.io.KeyWriter;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.io.VectorIO;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;

/**
 *
 * @author Tim Boudreau
 */
public final class ShapeEntry implements Hibernator, ShapeElement {

    private static long IDS = 0 /* - System.currentTimeMillis() */;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final PaintKey XPAR_KEY = PaintKey.forPaint(TRANSPARENT);
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

    private static final byte IO_REV = 1;

    public void writeTo(VectorIO io, KeyWriter writer) throws IOException {
        byte flags = 0;
        if (name != null) {
            flags |= 1;
        }
        if (bg != null && !bg.equals(XPAR_KEY)) {
            flags |= 2;
        }
        if (fg != null && !fg.equals(XPAR_KEY)) {
            flags |= 4;
        }
        if (stroke != null && !stroke.equals(new BasicStroke(1))) {
            flags |= 8;
        }
        writer.writeByte(IO_REV);
        writer.writeByte(flags);
        writer.writeLong(id);
        writer.writeEnum(paintingStyle);
        if ((flags & 2) != 0) {
            PaintKeyIO.write(writer, bg);
        }
        if ((flags & 4) != 0) {
            PaintKeyIO.write(writer, fg);
        }
        if ((flags & 8) != 0) {
            io.writeShape(new BasicStrokeWrapper(stroke), writer);
        }
        io.writeShape(vect, writer);
        if ((flags & 1) != 0) {
            writer.writeString(name);
        }
    }

    public static ShapeEntry read(VectorIO io, KeyReader reader) throws IOException {
        int rev = reader.readByte();
        if (rev != IO_REV) {
            throw new IOException("Unknown io revision " + rev + " expected "
                    + IO_REV);
        }
        byte flags = reader.readByte();
        long id = reader.readLong();
        PaintingStyle style = reader.readEnum(PaintingStyle.class);
        PaintKey<?> bg, fg;
        bg = fg = null;
        if ((flags & 2) != 0) {
            bg = PaintKeyIO.read(reader);
        }
        if ((flags & 4) != 0) {
            fg = PaintKeyIO.read(reader);
        }
        BasicStroke stroke = null;
        if ((flags & 8) != 0) {
            BasicStrokeWrapper strokeWrapper = (BasicStrokeWrapper) io.readShape(reader);
            stroke = strokeWrapper.toStroke();
        } else {
            stroke = new BasicStroke(1);
        }
        Shaped shape = (Shaped) io.readShape(reader);
        String name = null;
        if ((flags & 1) != 0) {
            name = reader.readString();
        }
        return new ShapeEntry(shape, bg, fg, style.isOutline(), style.isFill(), stroke, id, name);
    }

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

    public ShapeEntry(Shaped vector, PaintKey<?> bg, PaintKey<?> fg, boolean draw, boolean fill, BasicStroke stroke, String name) {
        id = IDS++;
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
        this.name = name;
    }

    public ShapeEntry(Shaped vector, PaintKey<?> bg, PaintKey<?> fg, PaintingStyle style, BasicStroke stroke, String name) {
        id = IDS++;
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = style;
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
    public void setFill(PaintKey<?> key) {
        bg = key;
    }

    @Override
    public void setDraw(PaintKey<?> draw) {
        fg = draw;
    }

    @Override
    public PaintKey<?> getFillKey() {
        return XPAR_KEY.equals(bg) ? null : bg;
    }

    @Override
    public PaintKey<?> getDrawKey() {
        return XPAR_KEY.equals(fg) ? null : fg;
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
            ShapeControlPoint[] result = CPF.getControlPoints(this, new ControlPointController() {
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
            return result == null ? new ShapeControlPoint[0] : result;
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
                + " " + vect.getClass().getSimpleName() + " "
                + paintingStyle + " "
                + vect + ")";
    }

    @Override
    public boolean isPaths() {
        return vect instanceof PathIteratorWrapper;
    }

    @Override
    public void toPaths() {
        if (!(vect instanceof PathIteratorWrapper)) {
            vect = new PathIteratorWrapper(vect.toShape());
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
                paintingStyle,
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

    void addTo(SnapPointsBuilder<ShapeSnapPointEntry> bldr) {
        if (vect instanceof Adjustable) {
            Adjustable adj = (Adjustable) vect;
            int cpCount = adj.getControlPointCount();
            double[] pts = new double[cpCount * 2];
            adj.getControlPoints(pts);
            int[] virt = adj.getVirtualControlPointIndices();
            for (int i = 0; i < pts.length; i += 2) {
                int ptIx = i / 2;
                if (Arrays.binarySearch(virt, ptIx) < 0) {
                    ShapeSnapPointEntry sn = new ShapeSnapPointEntry(this, ptIx, -1);
                    bldr.add(Axis.X, pts[i], sn);
                    bldr.add(Axis.Y, pts[i + 1], sn);
                }
            }
        }
        vect.collectSizings((double size, boolean vertical, int cpIx1, int cpIx2) -> {
            bldr.addDistance(Axis.forVertical(vertical), size,
                    new ShapeSnapPointEntry(this, cpIx1, cpIx2, size));
        });
        vect.collectAngles((double angle, int cpIx1, int cpIx2) -> {
            bldr.addAngle(angle, new ShapeSnapPointEntry(this, cpIx1, cpIx2, angle));
        });
    }

    @Override
    public boolean paint(Graphics2D g, Rectangle bounds) {
        Shape sh = shape();
        //            System.out.println("paint shape " + sh);
        if (bds != null && !sh.intersects(bds)) {
//            System.out.println("  does not intersect");
//            return false;
        }
        if (vect.is(ImageWrapper.class)) {
            ImageWrapper w = vect.as(ImageWrapper.class);
            System.out.println("PAINT IW " + w.img);
            w.paint(g);
        } else {
            Stroke oldStroke = null;
            if (stroke != null) {
                oldStroke = g.getStroke();
                AffineTransform xform = g.getTransform();
                g.setStroke(GraphicsUtils.createTransformedStroke(stroke, xform));
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
