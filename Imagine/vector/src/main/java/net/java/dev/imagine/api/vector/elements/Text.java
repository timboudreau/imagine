package net.java.dev.imagine.api.vector.elements;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.utils.java2d.GraphicsUtils;

/**
 * Wraps a string and the font used to render it so it can be turned into a
 * shape.
 *
 * @author Tim Boudreau
 */
public class Text implements Primitive, Vector {

    private StringWrapper text;
    private FontWrapper font;

    public Text(StringWrapper string, FontWrapper font) {
        this.text = string;
        this.font = font;
    }

    public Text(String text, Font font, double x, double y) {
        this.text = new StringWrapper(text, x, y);
        this.font = FontWrapper.create(font);
    }

    public FontWrapper font() {
        return font;
    }

    public StringWrapper text() {
        return text;
    }

    public String fontName() {
        return font.getFontName();
    }

    public float fontSize() {
        return font.getFontSize();
    }

    public int getFontStyle() {
        return font.getFontStyle();
    }

    public void setFontSize(float size) {
        font.setFontSize(size);
    }

    public void setFontName(String fontName) {
        font.setFontName(fontName);
    }

    public void setFontStyle(int style) {
        font.setFontStyle(style);
    }

    public void setTransform(AffineTransform xform) {
        font.setTransform(xform);
    }

    public void setText(StringWrapper text) {
        this.text = text;
    }

    public void setFont(FontWrapper font) {
        this.font = font;
    }

    public void setText(String txt) {
        text.setText(txt);
    }

    public double x() {
        return text.x();
    }

    public double y() {
        return text.y();
    }

    public void setX(double x) {
        text.setX(x);
    }

    public void setY(double y) {
        text.setY(y);
    }

    public String getText() {
        return text.getText();
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    @Override
    public void setLocation(double x, double y) {
        text.setLocation(x, y);
    }

    @Override
    public void clearLocation() {
        text.clearLocation();
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        font.applyTransform(xform);
        text.applyTransform(xform);
    }

    public Font getFont() {
        return font.toFont();
    }

    @Override
    public void paint(Graphics2D g) {
        font.paint(g);
        text.paint(g);
    }

    @Override
    public <T> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        if (type.isInstance(font)) {
            return type.cast(font);
        }
        if (type.isInstance(text)) {
            return type.cast(text);
        }
        T result = font.as(type);
        if (result == null) {
            result = text.as(type);
        }
        if (result == null) {
            result = Vector.super.as(type);
        }
        return result;
    }

    @Override
    public Pt getLocation() {
        return text.getLocation();
    }

    @Override
    public Text copy(AffineTransform transform) {
        StringWrapper txt = text.copy(transform);
        FontWrapper fnt = font.copy(transform);
        return new Text(txt, fnt);
    }

    @Override
    public Text copy() {
        StringWrapper txt = text.copy();
        FontWrapper fnt = font.copy();
        return new Text(txt, fnt);
    }

    @Override
    public void getBounds(Rectangle2D.Double dest) {
        Shape shape = toShape();
        if (shape == null) {
            new Exception("Got null shape").printStackTrace();
            return;
        }
        dest.setFrame(shape.getBounds2D());
    }

    @Override
    public Rectangle getBounds() {
        return toShape().getBounds();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.text);
        hash = 29 * hash + Objects.hashCode(this.font);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Text)) {
            return false;
        }
        final Text other = (Text) obj;
        return Objects.equals(this.text, other.text)
                && Objects.equals(this.font, other.font);
    }

    @Override
    public String toString() {
        return "Text{" + "text=" + text + ", font=" + font + '}';
    }

    private Shape cachedShape;
    private int hashAtShapeCache = -1;

    @Override
    public Shape toShape() {
        Shape result = cachedShape;
        int hash;
        if (result != null) {
            hash = hashCode();
            if (hash == hashAtShapeCache) {
                return result;
            }
            result = cachedShape = makeShape();
            hashAtShapeCache = hash;
        } else {
            hashAtShapeCache = hashCode();
            result = cachedShape = makeShape();
        }
        return result;
    }

    private Shape makeShape() {
        BufferedImage img = scratchImage();
        Graphics2D g = img.createGraphics();
        try {
            Font f = getFont();
            FontRenderContext frc = g.getFontRenderContext();
            GlyphVector gv = f.createGlyphVector(frc, getText());
            return gv.getOutline((float) x(), (float) y());
        } finally {
            g.dispose();
        }
    }

    static BufferedImage scratch;

    static BufferedImage scratchImage() {
        if (scratch == null) {
            scratch = GraphicsUtils.newBufferedImage(1, 1);
        }
        return scratch;
    }

    public String getFontName() {
        return font.name;
    }
}
