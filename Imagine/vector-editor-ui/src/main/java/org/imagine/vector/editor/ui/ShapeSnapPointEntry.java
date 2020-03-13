package org.imagine.vector.editor.ui;

import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
public final class ShapeSnapPointEntry {

    public final ShapeEntry entry;
    public final int controlPoint1;
    public final int controlPoint2;
    public final double sizeOrAngle;

    public ShapeSnapPointEntry(ShapeEntry entry, int controlPoint1, int controlPoint2, double sizeOrAngle) {
        this.entry = entry;
        this.controlPoint1 = controlPoint1;
        this.controlPoint2 = controlPoint2;
        this.sizeOrAngle = sizeOrAngle;
    }

    public ShapeSnapPointEntry(ShapeEntry entry, int controlPoint1, int controlPoint2) {
        this.entry = entry;
        this.controlPoint1 = controlPoint1;
        this.controlPoint2 = controlPoint2;
        this.sizeOrAngle = 0;
    }

    @Override
    public String toString() {
        return entry + " (" + controlPoint1 + ", " + controlPoint2 + ")";
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Objects.hashCode(this.entry);
        hash = 71 * hash + this.controlPoint1;
        hash = 71 * hash + this.controlPoint2;
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
        if (!(obj instanceof ShapeSnapPointEntry)) {
            return false;
        }
        final ShapeSnapPointEntry other = (ShapeSnapPointEntry) obj;
        if (this.controlPoint1 != other.controlPoint1) {
            return false;
        }
        if (this.controlPoint2 != other.controlPoint2) {
            return false;
        }
        return Objects.equals(this.entry, other.entry);
    }
}
