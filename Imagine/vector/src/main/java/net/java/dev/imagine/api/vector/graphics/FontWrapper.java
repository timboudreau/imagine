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
import java.util.Objects;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;

/**
 * Sets the Font of a Graphics2D
 *
 * @author Tim Boudreau
 */
public class FontWrapper implements Primitive, Attribute<Font>, Transformable {

    public static FontWrapper create(String name, float size, int style) {
        return new FontWrapper(name, size, style);
    }

    public static FontWrapper create(String name, int style, float size, AffineTransform transform) {
        return new FontWrapper(name, size, style);
    }

    public static FontWrapper create(Font f) {
        return new FontWrapper(f);
    }
    public long serialVersionUID = 5_620_138L;
    public String name;
    public float size;
    public int style;
    public AffineTransform transform;

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
    }

    public String getFontName() {
        return name;
    }

    public float getFontSize() {
        return size;
    }

    public void setFontSize(float size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be > 0: " + size);
        }
        this.size = size;
    }

    public void setFontName(String fontName) {
        this.name = fontName;
    }

    public void setFontStyle(int style) {
        switch (style) {
            case Font.PLAIN:
            case Font.BOLD:
            case Font.ITALIC:
            case Font.BOLD | Font.ITALIC:
                break;
            default:
                throw new IllegalArgumentException("Illegal font style " + style);
        }
    }

    public int getFontStyle() {
        return style;
    }

    public AffineTransform getTransform() {
        return transform == null ? AffineTransform.getTranslateInstance(0, 0)
                : new AffineTransform(transform);
    }

    public void setTransform(AffineTransform xform) {
        this.transform = xform;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform.isIdentity()) {
            return;
        }
        if (transform != null) {
            transform.concatenate(xform);
        } else {
            transform = xform;
        }
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
        return create(name, style, size, finalTransform);
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
                + (transform != null ? transform.toString() : "");
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
        return FontWrapper.create(name, style, size, transform);
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
        } else if (transform == null && other.transform != null) {
            if (!other.transform.isIdentity()) {
                return false;
            }
        } else if (transform != null && other.transform == null) {
            if (!transform.isIdentity()) {
                return false;
            }
        } else if (transform != null && other.transform != null) {
            if (!other.transform.equals(transform)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Float.floatToIntBits(this.size);
        hash = 97 * hash + this.style;
        hash = 97 * hash + Objects.hashCode(this.transform);
        return hash;
    }
}
