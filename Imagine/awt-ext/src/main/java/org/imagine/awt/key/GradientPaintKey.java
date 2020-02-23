package org.imagine.awt.key;

import org.imagine.awt.util.IdPathBuilder;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.geom.Point2D;
import java.io.IOException;
import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;
import java.util.Arrays;
import java.util.Comparator;
import org.imagine.awt.io.PaintKeyReader;
import org.imagine.awt.io.PaintKeyWriter;

/**
 *
 * @author Tim Boudreau
 */
public final class GradientPaintKey extends PaintKey<GradientPaint> implements Comparator<Point2D> {

    public static final String ID_BASE = "gradient";
    final int x1, y1, x2, y2, color1, color2;
    final boolean cyclic;

    GradientPaintKey(boolean cyclic, int x1, int y1, int x2, int y2, int color1, int color2) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    public GradientPaintKey(double x1, double y1, double x2, double y2, int color1, int color2, boolean cyclic) {
        this.x1 = floatToIntBits(roundOff(x1));
        this.y1 = floatToIntBits(roundOff(y1));
        this.x2 = floatToIntBits(roundOff(x2));
        this.y2 = floatToIntBits(roundOff(y2));
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    public GradientPaintKey(GradientPaint paint) {
        Point2D p0 = paint.getPoint1();
        Point2D p1 = paint.getPoint2();
        // One bit of normalization we can do:
        // If we have the same points and colors reversed,
        // we have the same paint
        Point2D[] points = new Point2D[]{p0, p1};
        Arrays.sort(points, this);
        if (points[0] == p0) {
            color1 = paint.getColor1().getRGB();
            color2 = paint.getColor2().getRGB();
        } else {
            color1 = paint.getColor2().getRGB();
            color2 = paint.getColor1().getRGB();
        }
        x1 = floatToIntBits(roundOff(points[0].getX()));
        y1 = floatToIntBits(roundOff(points[0].getY()));
        x2 = floatToIntBits(roundOff(points[1].getX()));
        y2 = floatToIntBits(roundOff(points[1].getY()));
        cyclic = paint.isCyclic();
    }

    @Override
    public PaintKeyKind kind() {
        return StandardPaintKeyKinds.GRADIENT;
    }

    public float x1() {
        return intBitsToFloat(x1);
    }

    public float y1() {
        return intBitsToFloat(y1);
    }

    public float x2() {
        return intBitsToFloat(x2);
    }

    public float y2() {
        return intBitsToFloat(y2);
    }

    @Override
    public GradientPaint toPaint() {
        return new GradientPaint(
                intBitsToFloat(x1),
                intBitsToFloat(y1),
                color1(),
                intBitsToFloat(x2),
                intBitsToFloat(y2),
                color2(),
                cyclic);
    }

    @Override
    public int compare(Point2D o1, Point2D o2) {
        int result = Double.compare(o1.getY(), o2.getY());
        if (result == 0) {
            result = Double.compare(o1.getX(), o2.getX());
        }
        return result;
    }

    @Override
    protected int computeHashCode() {
        int result = (719 * x1)
                + (56891 * y1)
                + (98251 * x2)
                + (3 * y2);
        result = (83 * result + color1)
                + (17 * result + color2);
        if (cyclic) {
            result *= 2;
        }
        return result;
    }

    private String info() {
        return x1 + "\t" + y1 + "\t" + x2 + "\t" + y2 + "\t" + rgb(color1())
                + " " + rgb(color2());
    }

    @Override
    protected void buildId(IdPathBuilder bldr) {
        bldr.add(cyclic ? "c" : "n")
                .add(color1).add(color2).add(x1).add(y1)
                .add(x2).add(y2);
    }

    @Override
    public String idBase() {
        return ID_BASE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GradientPaintKey)) {
            return false;
        }
        final GradientPaintKey other = (GradientPaintKey) obj;
        return this.cyclic == other.cyclic
                && this.color1 == other.color1
                && this.color2 == other.color2
                && this.x1 == other.x1
                && this.y1 == other.y1
                && this.x2 == other.x2
                && this.y2 == other.y2;
    }

    @Override
    protected Class<GradientPaint> type() {
        return GradientPaint.class;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Gradient(");
        rgb(color1(), sb).append(" @ ").append(x1())
                .append(',').append(y1())
                .append(" ");
        return rgb(color2(), sb).append(" @ ").append(x2())
                .append(',').append(y2())
                .append(" cyclic? ").append(cyclic)
                .append(")").toString();
    }

    public Color color1() {
        return new Color(color1, true);
    }

    public Color color2() {
        return new Color(color2, true);
    }

    public boolean isCyclic() {
        return cyclic;
    }

    @Override
    public void writeTo(PaintKeyWriter writer) {
        writer.writeInt(x1);
        writer.writeInt(y1);
        writer.writeInt(x2);
        writer.writeInt(y2);
        writer.writeInt(color1);
        writer.writeInt(color2);
        writer.writeByte(cyclic ? (byte) 1 : (byte) 2);
    }

    public static GradientPaintKey read(PaintKeyReader reader) throws IOException {
        int x1 = reader.readInt();
        int y1 = reader.readInt();
        int x2 = reader.readInt();
        int y2 = reader.readInt();
        int c1 = reader.readInt();
        int c2 = reader.readInt();
        byte b = reader.readByte();
        if (b != 1 && b != 2) {
            throw new IOException("Cyclic value out of range " + b);
        }
        GradientPaintKey result = new GradientPaintKey(b == 1, x1, y1, x2, y2, c1, c2);
        return result;
    }
}
