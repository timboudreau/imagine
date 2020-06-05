package org.imagine.markdown.uiapi;

import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;

/**
 * A Dimension implementation with actual floating point internal storage.
 *
 * @author Tim Boudreau
 */
final class DimensionImpl extends Dimension2D {

    private final double width;
    private final double height;
    public static final DimensionImpl EMPTY = new DimensionImpl(0,0);

    DimensionImpl(double width, double height) {
        if (width < 0) {
            throw new IllegalArgumentException("Negative width");
        }
        if (height < 0) {
            throw new IllegalArgumentException("Negative height");
        }
        this.width = width;
        this.height = height;
    }

    DimensionImpl(BufferedImage img) {
        this(img.getWidth(), img.getHeight());
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setSize(double width, double height) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.width) ^ (Double.doubleToLongBits(this.width) >>> 32));
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.height) ^ (Double.doubleToLongBits(this.height) >>> 32));
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
        final DimensionImpl other = (DimensionImpl) obj;
        if (Double.doubleToLongBits(this.width) != Double.doubleToLongBits(other.width)) {
            return false;
        }
        if (Double.doubleToLongBits(this.height) != Double.doubleToLongBits(other.height)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return Double.toString(width) + "x" + Double.toString(height);
    }
}
