/*
 * FontWrapper.java
 *
 * Created on September 29, 2006, 5:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.graphics;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Objects;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.Versioned;
import org.imagine.utils.java2d.GraphicsUtils;

/**
 * Sets the Font of a Graphics2D
 *
 * @author Tim Boudreau
 */
public class FontWrapper implements Primitive, Attribute<Font>, Transformable, Versioned {

    private static long serialVersionUID = 5_620_138L;

    public static FontWrapper create(String name, float size, int style) {
        return new FontWrapper(name, size, style);
    }

    public static FontWrapper create(String name, int style, float size, AffineTransform transform) {
        return new FontWrapper(name, size, style, transform);
    }

    public static FontWrapper create(Font f, boolean removeTranslation) {
        if (!removeTranslation) {
            return create(f);
        }
        AffineTransform xf = f.getTransform();
        if (xf != null && !xf.isIdentity()) {
            xf = removeTranslation(xf);
            create(f.deriveFont(xf));
        }
        return create(f);
    }

    public static FontWrapper create(Font f) {
        return new FontWrapper(f);
    }
    private String name;
    private float size;
    private int style;
    private AffineTransform transform;
    private int rev;

    public Runnable restorableSnapshot() {
        String s = name;
        float sz = size;
        int st = style;
        return () -> {
            name = s;
            size = sz;
            style = st;
        };
    }

    @Override
    public int rev() {
        return rev;
    }

    private void change() {
        rev++;
    }

    private FontWrapper(FontWrapper other) {
        this.rev = other.rev;
        this.name = other.name;
        this.size = other.size;
        this.style = other.style;
        this.transform = other.transform == null
                ? null
                : new AffineTransform(other.transform);
    }

    private FontWrapper(Font f) {
        name = f.getName();
        size = f.getSize2D();
        style = f.getStyle();
        AffineTransform at = f.getTransform();
        if (at != null && !at.isIdentity()) {
            transform = at;
        }
    }

    private FontWrapper(String name, float size, int style) {
        this(name, size, style, null);
    }

    private FontWrapper(String name, float size, int style, AffineTransform xform) {
        this.name = name;
        this.size = size;
        this.style = style;
        this.transform = xform == null ? null : xform.isIdentity() ? null
                : removeTranslation(xform);
    }

    public AffineTransform getTransform() {
        return transform == null ? AffineTransform.getTranslateInstance(0, 0)
                : new AffineTransform(transform);
    }

    public void setTransform(AffineTransform xform) {
        if (!Objects.equals(xform, this.transform)) {
            this.transform = xform;
            change();
        }
    }

    public static AffineTransform removeTranslation(AffineTransform xform) {
        double x = xform.getTranslateX();
        double y = xform.getTranslateY();
        AffineTransform nue = new AffineTransform(xform);
        nue.translate(-x, -y);
        return nue;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            return;
        }
        if (transform != null) {
            transform.concatenate(removeTranslation(xform));
        } else {
            transform = xform;
        }
        change();
    }

    @Override
    public FontWrapper copy(AffineTransform transform) {
        if (transform.isIdentity()) {
            return copy();
        }
        AffineTransform finalTransform;
        if (this.transform != null) {
            AffineTransform copy = new AffineTransform(this.transform);
            copy.concatenate(transform);
            finalTransform = transform;
        } else {
            finalTransform = transform;
        }
        FontWrapper result = new FontWrapper(this);
        result.setTransform(finalTransform);
        return result;
    }

    public Font toFont() {
        Font result = new Font(name, style, 1).deriveFont(size);
        if (transform != null) {
            result = result.deriveFont(transform);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Font: " + name + ", " + size
                + ", " + styleToString(style)
                + transformString();
    }

    private String transformString() {
        if (transform == null || transform.isIdentity()) {
            return "-identity-xform-";
        } else {
            double[] mx = new double[6];
            transform.getMatrix(mx);
            return Arrays.toString(mx);
        }
    }

    private static String styleToString(int style) {
        StringBuilder b = new StringBuilder();
        if ((style & Font.BOLD) != 0) {
            b.append("BOLD");
        }
        if ((style & Font.ITALIC) != 0) {
            if (b.length() != 0) {
                b.append(", ");
            }
            b.append("ITALIC");
        }
        if (b.length() == 0) {
            b.append("PLAIN");
        }
        return b.toString();
    }

    @Override
    public void paint(Graphics2D g) {
        g.setFont(toFont());
    }

    @Override
    public FontWrapper copy() {
        return new FontWrapper(this);
    }

    @Override
    public Font get() {
        return toFont();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof FontWrapper)) {
            return false;
        }
        final FontWrapper other = (FontWrapper) obj;
        if (this.size != other.size) {
            return false;
        } else if (this.style != other.style) {
            return false;
        } else if (!name.equals(other.name)) {
            return false;
        }
        return GraphicsUtils.transformsEqual(transform, other.transform);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Float.floatToIntBits(this.size);
        hash = 97 * hash + this.style;
        hash = 97 * hash + GraphicsUtils.transformHashCode(transform);
        return hash;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        if (!name.equals(this.name)) {
            this.name = name;
            change();
        }
    }

    /**
     * @return the size
     */
    public float getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(float size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be > 0: " + size);
        }
        if (size != this.size) {
            this.size = size;
            change();
        }
    }

    /**
     * @return the style
     */
    public int getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(int style) {
        if (style != this.style) {
            switch (style) {
                case Font.PLAIN:
                case Font.BOLD:
                case Font.ITALIC:
                case Font.BOLD | Font.ITALIC:
                    if (style != this.style) {
                        this.style = style;
                        change();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Illegal font style " + style);
            }
        }
    }
}
