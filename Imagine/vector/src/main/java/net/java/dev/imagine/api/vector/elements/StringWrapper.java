/*
 * StringWrapper.java
 *
 * Created on September 27, 2006, 6:46 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import static java.lang.Double.doubleToLongBits;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 * Represents a string painted to a graphics context
 *
 * @author Tim Boudreau
 */
public class StringWrapper implements Vector {

    public long serialVersionUID = 72_305_123_414L;
    public String string;
    public double x;
    public double y;

    public StringWrapper(String string, double x, double y) {
        this.x = x;
        this.y = y;
        this.string = string;
        assert string != null;
    }

    public Runnable restorableSnapshot() {
        double ox = x;
        double oy = y;
        String s = string;
        return () -> {
            x = ox;
            y = oy;
            string = s;
        };
    }

    @Override
    public String toString() {
        return "StringWrapper '" + string + "' @ "
                + x + ", " + y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getText() {
        return string;
    }

    public void setText(String txt) {
        if (txt == null) {
            this.string = "";
        } else {
            this.string = txt.trim();
        }
    }

    public boolean isEmpty() {
        return string == null || string.trim().isEmpty();
    }

    @Override
    public void translate(double x, double y) {
        this.x += x;
        this.y += y;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        // do nothing
    }

    @Override
    public Shape toShape() {
        // XXX create a combined font + string primitive which
        // uses a scratch graphics + GlyphVector to create a
        // useful shape
        return new java.awt.Rectangle(0, 0, 0, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        }
        boolean result = o instanceof StringWrapper;
        if (result) {
            StringWrapper sw = (StringWrapper) o;
            result = string.equals(sw.string)
                    && x == sw.x && y == sw.y;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return string.hashCode() * 31
                + (int) ((doubleToLongBits(x) * 431)
                + (doubleToLongBits(y) * 7));
    }

    @Override
    public void paint(Graphics2D g) {
        g.drawString(string, (float) x, (float) y);
    }

    @Override
    public void getBounds(Rectangle2D r) {
        //XXX fixme
        r.setRect(x, y, 1_000, 20);
    }

    @Override
    public StringWrapper copy() {
        return new StringWrapper(string, x, y);
    }

    @Override
    public Pt getLocation() {
        return new Pt(x, y);
    }

    @Override
    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void clearLocation() {
        setLocation(0, 0);
    }

    @Override
    public StringWrapper copy(AffineTransform transform) {
        double[] pts = new double[]{x, y};
        transform.transform(pts, 0, pts, 0, 1);
        return new StringWrapper(string, pts[0], pts[1]);
    }

    @Override
    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }
}
