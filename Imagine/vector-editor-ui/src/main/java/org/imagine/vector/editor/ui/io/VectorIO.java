/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.io;

import com.mastfrog.util.strings.Strings;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.elements.Arc;
import net.java.dev.imagine.api.vector.elements.CircleWrapper;
import net.java.dev.imagine.api.vector.elements.Clear;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.Line;
import net.java.dev.imagine.api.vector.elements.Oval;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper.PointVisitor;
import net.java.dev.imagine.api.vector.elements.PathText;
import net.java.dev.imagine.api.vector.elements.Polygon;
import net.java.dev.imagine.api.vector.elements.Polyline;
import net.java.dev.imagine.api.vector.elements.Rectangle;
import net.java.dev.imagine.api.vector.elements.RoundRect;
import net.java.dev.imagine.api.vector.elements.StringWrapper;
import net.java.dev.imagine.api.vector.elements.Text;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import org.imagine.io.KeyReader;
import org.imagine.io.KeyWriter;
import static org.openide.util.Parameters.notNull;

/**
 *
 * @author Tim Boudreau
 */
public class VectorIO {

    private static final byte ENTRY_MAGIC_1 = 121;
    private static final byte ENTRY_MAGIC_2 = -107;
    private static final byte REV = 1;

    private static final byte CIRCLE = 1;
    private static final byte RECTANGLE = 2;
    private static final byte ROUND_RECT = 3;
    private static final byte OVAL = 4;
    private static final byte TRIANGLE = 5;
    private static final byte PATH = 6;
    private static final byte TEXT = 7;
    private static final byte POLYGON = 8;
    private static final byte POLYLINE = 9;
    private static final byte ARC = 10;
    private static final byte CLEAR = 11;
    private static final byte LINE = 12;
    private static final byte IMAGE = 13;
    private static final byte STROKE = 14;
    private static final byte STRING = 15;
    private static final byte FONT = 16;
    private static final byte PATH_TEXT = 17;

    private HashInconsistencyBehavior hashInconsistencyBehavior
            = HashInconsistencyBehavior.defaultBehavior();

    private static byte typeIdFor(Primitive shaped) throws IOException {
        if (shaped instanceof StringWrapper) {
            return STRING;
        } else if (shaped instanceof FontWrapper) {
            return FONT;
        } else if (shaped instanceof PathText) {
            return PATH_TEXT;
        } else if (shaped instanceof CircleWrapper) {
            return CIRCLE;
        } else if (shaped instanceof Rectangle) {
            return RECTANGLE;
        } else if (shaped instanceof RoundRect) {
            return ROUND_RECT;
        } else if (shaped instanceof Oval) {
            return OVAL;
        } else if (shaped instanceof TriangleWrapper) {
            return TRIANGLE;
        } else if (shaped instanceof PathIteratorWrapper) {
            return PATH;
        } else if (shaped instanceof Text) {
            return TEXT;
        } else if (shaped instanceof Polygon) {
            return POLYGON;
        } else if (shaped instanceof Polyline) {
            return POLYLINE;
        } else if (shaped instanceof Arc) {
            return ARC;
        } else if (shaped instanceof Clear) {
            return CLEAR;
        } else if (shaped instanceof Line) {
            return LINE;
        } else if (shaped instanceof ImageWrapper) {
            return IMAGE;
        } else if (shaped instanceof BasicStrokeWrapper) {
            return STROKE;
        } else {
            throw new IOException("Unrecognized shape type "
                    + shaped.getClass().getName());
        }
    }

    private byte writeEntryStart(Primitive shaped, KeyWriter writer) throws IOException {
        byte type = typeIdFor(shaped);
        writer.writeByte(ENTRY_MAGIC_1);
        writer.writeByte(ENTRY_MAGIC_2);
        writer.writeByte(type);
        writer.writeByte(REV);
        writer.writeInt(shaped.hashCode());
        return type;
    }

    private byte readEntryStart(KeyReader reader) throws IOException {
        byte m1 = reader.readByte();
        byte m2 = reader.readByte();
        if (m1 != ENTRY_MAGIC_1 || m2 != ENTRY_MAGIC_2) {
            throw new IOException("Wrong magic numbers: " + Strings.toPaddedHex(new byte[]{m1, m2})
                    + " expected "
                    + Strings.toPaddedHex(new byte[]{ENTRY_MAGIC_1, ENTRY_MAGIC_2})
                    + " from " + reader);
        }
        byte type = reader.readByte();
        return type;
    }

    public VectorIO setHashInconsistencyBehavior(HashInconsistencyBehavior behavior) {
        notNull("hashInconsistencyBehavior", hashInconsistencyBehavior);
        this.hashInconsistencyBehavior = behavior;
        return this;
    }

    public Primitive readShape(KeyReader reader) throws IOException {
        return readShape(reader, hashInconsistencyBehavior);
    }

    public Primitive readShape(KeyReader reader, HashInconsistencyBehavior behavior) throws IOException {
        byte type = readEntryStart(reader);
        byte rev = reader.readByte();
        if (rev != REV) {
            throw new IOException("Unrecognized file revision " + rev + " expected " + REV);
        }
        int hash = reader.readInt();
        Primitive result = null;
        switch (type) {
            case CIRCLE:
                result = readCircle(reader);
                break;
            case ARC:
                result = readArc(reader);
                break;
            case CLEAR:
                result = readClear(reader);
                break;
            case IMAGE:
                result = readImage(reader);
                break;
            case LINE:
                result = readLine(reader);
                break;
            case OVAL:
                result = readOval(reader);
                break;
            case PATH:
                result = readPath(reader);
                break;
            case POLYGON:
                result = readPolygon(reader);
                break;
            case POLYLINE:
                result = readPolyline(reader);
                break;
            case RECTANGLE:
                result = readRect(reader);
                break;
            case ROUND_RECT:
                result = readRoundRect(reader);
                break;
            case TEXT:
                result = readText(reader);
                break;
            case TRIANGLE:
                result = readTriangle(reader);
                break;
            case STROKE:
                result = readStroke(reader);
                break;
            case STRING :
                result = readStringWrapper(reader);
                break;
            case FONT :
                result = readFontWrapper(reader);
                break;
            case PATH_TEXT :
                result = readPathText(reader);
                break;
            default:
                throw new AssertionError(type + " unknown ");
        }
        assert result != null;
        behavior.apply(result, hash, result.hashCode());
        return result;
    }

    public void writeShape(Primitive shape, KeyWriter writer) throws IOException {
        int type = writeEntryStart(shape, writer);
        switch (type) {
            case CIRCLE:
                writeCircle((CircleWrapper) shape, writer);
                break;
            case ARC:
                writeArc((Arc) shape, writer);
                break;
            case CLEAR:
                writeClear((Clear) shape, writer);
                break;
            case IMAGE:
                writeImage((ImageWrapper) shape, writer);
                break;
            case LINE:
                writeLine((Line) shape, writer);
                break;
            case OVAL:
                writeOval((Oval) shape, writer);
                break;
            case PATH:
                writePath((PathIteratorWrapper) shape, writer);
                break;
            case POLYGON:
                writePolygon((Polygon) shape, writer);
                break;
            case POLYLINE:
                writePolyline((Polyline) shape, writer);
                break;
            case RECTANGLE:
                writeRectangle((Rectangle) shape, writer);
                break;
            case ROUND_RECT:
                writerRoundRect((RoundRect) shape, writer);
                break;
            case TEXT:
                writeText((Text) shape, writer);
                break;
            case TRIANGLE:
                writeTriangle((TriangleWrapper) shape, writer);
                break;
            case STROKE:
                writeStroke((BasicStrokeWrapper) shape, writer);
                break;
            case STRING :
                writeStringWrapper((StringWrapper) shape, writer);
                break;
            case FONT :
                writeFontWrapper((FontWrapper) shape, writer);
                break;
            case PATH_TEXT :
                writePathText((PathText) shape, writer);
                break;
            default:
                throw new AssertionError(type + " " + shape);
        }
    }

    public void writeCircle(CircleWrapper wrapper, KeyWriter writer) throws IOException {
        writer.writeDouble(wrapper.centerX);
        writer.writeDouble(wrapper.centerY);
        writer.writeDouble(wrapper.radius);
    }

    private CircleWrapper readCircle(KeyReader r) throws IOException {
        return new CircleWrapper(r.readDouble(), r.readDouble(), r.readDouble());
    }

    private void writeArc(Arc arc, KeyWriter writer) {
        writer.writeDouble(arc.x);
        writer.writeDouble(arc.y);
        writer.writeDouble(arc.width);
        writer.writeDouble(arc.height);
        writer.writeDouble(arc.startAngle);
        writer.writeDouble(arc.arcAngle);
    }

    private Arc readArc(KeyReader r) throws IOException {
        return new Arc(r.readDouble(), r.readDouble(), r.readDouble(),
                r.readDouble(), r.readDouble(), r.readDouble(), false);
    }

    private void writeClear(Clear clear, KeyWriter writer) {
        writer.writeInt(clear.x);
        writer.writeInt(clear.y);
        writer.writeInt(clear.width);
        writer.writeInt(clear.height);
    }

    private Clear readClear(KeyReader r) throws IOException {
        return new Clear(r.readInt(), r.readInt(), r.readInt(),
                r.readInt());
    }

    private void writeLine(Line line, KeyWriter writer) {
        writer.writeDouble(line.x1());
        writer.writeDouble(line.y1());
        writer.writeDouble(line.x2());
        writer.writeDouble(line.y2());
    }

    private Line readLine(KeyReader r) throws IOException {
        return new Line(r.readDouble(), r.readDouble(), r.readDouble(),
                r.readDouble());
    }

    private void writeOval(Oval oval, KeyWriter writer) {
        writer.writeDouble(oval.x());
        writer.writeDouble(oval.y());
        writer.writeDouble(oval.width());
        writer.writeDouble(oval.height());
    }

    private Oval readOval(KeyReader r) throws IOException {
        return new Oval(
                r.readDouble(), r.readDouble(), r.readDouble(),
                r.readDouble(), false);
    }

    private void writeRectangle(Rectangle rectangle, KeyWriter writer) {
        writer.writeDouble(rectangle.x());
        writer.writeDouble(rectangle.y());
        writer.writeDouble(rectangle.width());
        writer.writeDouble(rectangle.height());
    }

    private Rectangle readRect(KeyReader r) throws IOException {
        return new Rectangle(
                r.readDouble(), r.readDouble(), r.readDouble(),
                r.readDouble(), false);
    }

    private void writerRoundRect(RoundRect rectangle, KeyWriter writer) {
        writer.writeDouble(rectangle.x());
        writer.writeDouble(rectangle.y());
        writer.writeDouble(rectangle.width());
        writer.writeDouble(rectangle.height());
        writer.writeDouble(rectangle.arcWidth());
        writer.writeDouble(rectangle.arcHeight());
    }

    private RoundRect readRoundRect(KeyReader r) throws IOException {
        return new RoundRect(r.readDouble(), r.readDouble(),
                r.readDouble(), r.readDouble(), r.readDouble(),
                r.readDouble(), false);
    }

    private void writeTriangle(TriangleWrapper triangleWrapper, KeyWriter writer) {
        writer.writeDouble(triangleWrapper.ax());
        writer.writeDouble(triangleWrapper.ay());
        writer.writeDouble(triangleWrapper.bx());
        writer.writeDouble(triangleWrapper.by());
        writer.writeDouble(triangleWrapper.cx());
        writer.writeDouble(triangleWrapper.cy());
    }

    private TriangleWrapper readTriangle(KeyReader r) throws IOException {
        return new TriangleWrapper(
                r.readDouble(),
                r.readDouble(),
                r.readDouble(),
                r.readDouble(),
                r.readDouble(),
                r.readDouble());
    }

    private void writePolygon(Polygon polygon, KeyWriter writer) {
        writer.writeDoubleArray(polygon.xpoints());
        writer.writeDoubleArray(polygon.ypoints());
    }

    private Polygon readPolygon(KeyReader r) throws IOException {
        double[] xpoints = r.readDoubleArray();
        double[] ypoints = r.readDoubleArray();
        if (xpoints.length != ypoints.length) {
            throw new IOException("Different xpoints and ypoints array lengths: "
                    + xpoints.length + " vs " + ypoints.length);
        }
        return new Polygon(xpoints, ypoints, false);
    }

    private void writePolyline(Polyline polyline, KeyWriter writer) {
        writer.writeIntArray(polyline.xpoints);
        writer.writeIntArray(polyline.ypoints);
    }

    private Polyline readPolyline(KeyReader r) throws IOException {
        int[] xpoints = r.readIntArray();
        int[] ypoints = r.readIntArray();
        if (xpoints.length != ypoints.length) {
            throw new IOException("Different xpoints and ypoints array lengths: "
                    + xpoints.length + " vs " + ypoints.length);
        }
        return new Polyline(xpoints, ypoints, xpoints.length, false);
    }

    private void writeText(Text text, KeyWriter writer) {
        writer.writeDouble(text.x());
        writer.writeDouble(text.y());
        writer.writeString(text.getText());
        AffineTransform xf
                = text.transform() == null || text.transform().isIdentity()
                ? null
                : text.transform();
        if (xf == null) {
            writer.writeDoubleArray(new double[0]);
        } else {
            double[] mx = new double[6];
            xf.getMatrix(mx);
            writer.writeDoubleArray(mx);
        }

        FontWrapper fnt = text.font();
        writer.writeFloat(fnt.getSize());
        writer.writeInt(fnt.getStyle());
        writer.writeString(fnt.getName());
        if (fnt.getTransform() == null || fnt.getTransform().isIdentity()) {
            writer.writeDoubleArray(new double[0]);
        } else {
            double[] dbls = new double[6];
            fnt.getTransform().getMatrix(dbls);
            writer.writeDoubleArray(dbls);
        }
    }

    private Text readText(KeyReader r) throws IOException {
        double x = r.readDouble();
        double y = r.readDouble();

        // TRANSFORM!
        String text = r.readString();

        double[] mx = r.readDoubleArray();
        AffineTransform masterTransform = null;
        if (mx.length == 6) {
            masterTransform = new AffineTransform(mx);
        } else if (mx.length != 0) {
            throw new IOException("Wrong size for AffineTransform matrix "
                    + "is " + mx.length + " should be 6");
        }

        float size = r.readFloat();
        int style = r.readInt();
        switch (style) {
            case Font.PLAIN:
            case Font.BOLD:
            case Font.ITALIC:
            case Font.BOLD | Font.ITALIC:
                break;
            default:
                throw new IOException("Invalid font style " + style);
        }
        String name = r.readString();
        double[] xformMatrix = r.readDoubleArray();
        AffineTransform xform = AffineTransform.getTranslateInstance(0, 0);
        if (xformMatrix.length != 0) {
            if (xformMatrix.length != 6) {
                throw new IOException("Wrong length for AffineTransform matrix: " + xformMatrix.length);
            }
            xform.setTransform(xformMatrix[0], xformMatrix[1], xformMatrix[2],
                    xformMatrix[3], xformMatrix[4], xformMatrix[5]);
        }
        StringWrapper string = new StringWrapper(text, x, y);
        FontWrapper font = FontWrapper.create(name, style, size, xform);
        return new Text(string, font, masterTransform);
    }

    private void writePath(PathIteratorWrapper pathIteratorWrapper, KeyWriter writer) {
        int sz = pathIteratorWrapper.size();
        double[][] points = new double[sz][];
        byte[] types = new byte[sz];
        pathIteratorWrapper.visitPoints((PointVisitor) (ix, type, data) -> {
            types[ix] = (byte) type;
            points[ix] = data;
        });
        writer.writeByteArray(types);
        for (int i = 0; i < sz; i++) {
            if (points[i] == null) {
                points[i] = new double[0];
            }
            writer.writeDoubleArray(points[i]);
        }
    }

    private PathIteratorWrapper readPath(KeyReader r) throws IOException {
        byte[] types = r.readByteArray();
        double[][] dbls = new double[types.length][];
        for (int i = 0; i < types.length; i++) {
            dbls[i] = r.readDoubleArray();
        }
        return new PathIteratorWrapper(types, dbls);
    }

    private void writeImage(ImageWrapper imageWrapper, KeyWriter writer) {
        AffineTransform xf = imageWrapper.xform;
        double[] mx = xf == null ? new double[0] : new double[6];
        if (imageWrapper.xform != null) {
            imageWrapper.xform.getMatrix(mx);
        }
        writer.writeDoubleArray(mx);

        BufferedImage img = imageWrapper.img;
        writer.writeInt(img.getType());
        writer.writeInt(img.getWidth());
        writer.writeInt(img.getHeight());
        int[] pixels = img.getData().getPixels(0, 0, img.getWidth(), img.getHeight(), (int[]) null);
        writer.writeIntArray(pixels);
    }

    private ImageWrapper readImage(KeyReader reader) throws IOException {
        double[] mx = reader.readDoubleArray();
        AffineTransform xform = mx.length == 0 ? null : new AffineTransform(mx);
        int type = reader.readInt();
        int w = reader.readInt();
        int h = reader.readInt();
        int[] pixels = reader.readIntArray();
        BufferedImage img = new BufferedImage(w, h, type);
        img.getRaster().setPixels(0, 0, w, h, pixels);
        return new ImageWrapper(img, xform);
    }

    private void writeStroke(BasicStrokeWrapper s, KeyWriter writer) {
        writer.writeByte(s.endCap).writeByte(s.lineJoin)
                .writeFloat(s.lineWidth)
                .writeFloat(s.miterLimit)
                .writeFloatArray(s.dashArray == null ? new float[0] : s.dashArray)
                .writeFloat(s.dashPhase);
    }

    private BasicStrokeWrapper readStroke(KeyReader reader) throws IOException {
        byte endCap = reader.readByte();
        byte lineJoin = reader.readByte();
        float lineWidth = reader.readFloat();
        float miterLimit = reader.readFloat();
        float[] dashes = reader.readFloatArray();
        float dashPhase = reader.readFloat();
        return new BasicStrokeWrapper(miterLimit,
                dashes.length == 0 ? null : dashes,
                dashPhase, lineWidth,
                endCap, lineJoin);
    }

    private void writeFontWrapper(FontWrapper font, KeyWriter writer) {
        writer.writeString(font.getName());
        writer.writeFloat(font.getSize());
        writer.writeInt(font.getStyle());
        writeAffineTransform(font.getTransform(), writer);
    }

    private void writePathText(PathText text, KeyWriter writer) throws IOException {
        writeStringWrapper(text.text(), writer);
        writeFontWrapper(text.font(), writer);
        writeShape(text.shape(), writer);
        writeAffineTransform(text.transform(), writer);
    }

    private PathText readPathText(KeyReader reader) throws IOException {
        StringWrapper text = readStringWrapper(reader);
        FontWrapper font = readFontWrapper(reader);
        Shaped sh = readShape(reader).as(Shaped.class);
        AffineTransform xf = readAffineTransform(reader, false);
        return new PathText(sh, text, font, xf);
    }

    private FontWrapper readFontWrapper(KeyReader r) throws IOException {
        String fontName = r.readString();
        float size = r.readFloat();
        int style = r.readInt();
        AffineTransform xform = readAffineTransform(r, true);
        return FontWrapper.create(fontName, style, size, xform);
    }

    private void writeStringWrapper(StringWrapper stringWrapper, KeyWriter writer) {
        writer.writeDouble(stringWrapper.x());
        writer.writeDouble(stringWrapper.y());
        writer.writeString(stringWrapper.getText());
    }

    private StringWrapper readStringWrapper(KeyReader r) throws IOException {
        double x = r.readDouble();
        double y = r.readDouble();
        String txt = r.readString();
        return new StringWrapper(txt, x, y);
    }

    private void writeAffineTransform(AffineTransform xform, KeyWriter writer) {
        if (xform == null || xform.isIdentity()) {
            writer.writeDoubleArray(new double[0]);
            return;
        }
        double[] matrix = new double[6];
        xform.getMatrix(matrix);
        writer.writeDoubleArray(matrix);
    }

    private AffineTransform readAffineTransform(KeyReader reader) throws IOException {
        return readAffineTransform(reader, false);
    }

    private AffineTransform readAffineTransform(KeyReader reader, boolean nonNull) throws IOException {
        double[] dbl = reader.readDoubleArray();
        if (dbl.length == 0) {
            if (nonNull) {
                return new AffineTransform();
            } else {
                return null;
            }
        }
        return new AffineTransform(dbl);
    }
}
