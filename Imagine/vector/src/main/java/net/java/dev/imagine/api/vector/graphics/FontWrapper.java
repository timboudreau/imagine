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
import java.util.Objects;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;

/**
 * Sets the Font of a Graphics2D
 *
 * @author Tim Boudreau
 */
public class FontWrapper implements Primitive, Attribute <Font> {
    public long serialVersionUID = 5620138L;
    public final String name;
    public final int size;
    public final int style;

    public FontWrapper(Font f) {
        name = f.getName();
        size = f.getSize();
        style = f.getStyle();
    }

    private FontWrapper (String name, int size, int style) {
        this.name = name;
        this.size = size;
        this.style = style;
    }

    public Font toFont() {
        return new Font (name, style, size);
    }

    public String toString() {
        return "Font: " + name  + ", " + size
                + ", " + styleToString (style);
    }

    private static String styleToString (int style) {
        StringBuilder b = new StringBuilder();
        if ((style & Font.BOLD) != 0) {
            b.append ("BOLD");
        }
        if ((style & Font.ITALIC) != 0) {
            if (b.length() != 0) {
                b.append (", ");
            }
            b.append ("ITALIC");
        }
        if (b.length() == 0) {
            b.append ("PLAIN");
        }
        return b.toString();
    }

    public void paint(Graphics2D g) {
        g.setFont (toFont());
    }

    public FontWrapper copy() {
        return new FontWrapper (name, size, style);
    }

    public Font get() {
        return toFont();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.name);
        hash = 83 * hash + this.size;
        hash = 83 * hash + this.style;
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
        final FontWrapper other = (FontWrapper) obj;
        if (this.size != other.size) {
            return false;
        }
        if (this.style != other.style) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }


}
