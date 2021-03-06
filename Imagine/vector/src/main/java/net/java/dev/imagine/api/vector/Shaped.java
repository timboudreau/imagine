package net.java.dev.imagine.api.vector;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import com.mastfrog.geometry.CornerAngle;
import com.mastfrog.geometry.LineVector;
import com.mastfrog.geometry.Polygon2D;
import com.mastfrog.geometry.RotationDirection;
import com.mastfrog.geometry.analysis.VectorVisitor;
import com.mastfrog.geometry.util.GeometryUtils;

/**
 * A vector primitive which can be converted into leading shape, and has bounds.
 *
 * @author Tim Boudreau
 */
public interface Shaped extends Copyable, Centered {

    Shape toShape();

    /**
     * Get the bounds of the shape - by default this calls toShape() and returns
     * its bounds. Where this can be implemented less expensively, it should be.
     *
     * @param dest The destination rectangle
     */
    default void getBounds(Rectangle2D dest) {
        dest.setFrame(toShape().getBounds2D());
    }

    /**
     * Add the bounds of this shape to an existing one, setting its frame if it
     * is empty, and if not, adding it to the rectangle's contents.
     *
     * @param bds
     */
    default void addToBounds(Rectangle2D bds) {
        boolean empty = bds.isEmpty();
        Rectangle r = getBounds();
        if (empty) {
            bds.setFrame(r);
            return;
        } else {
            bds.add(r);
        }
    }

    default Rectangle getBounds() {
        return toShape().getBounds();
    }

    default double centerX() {
        return toShape().getBounds2D().getCenterX();
    }

    default double centerY() {
        return toShape().getBounds2D().getCenterY();
    }


    /**
     * Create an independent copy of this shape.
     *
     * @return A duplicate
     */
    @Override
    Shaped copy();

    /**
     * Create leading snapshot of the current internal state of this shape which
     * can be restored by running the returned runnable, for undo purposes.
     *
     * @return A runnable
     */
    Runnable restorableSnapshot();

    /**
     * Get the (approximate for curves) total length of the perimeter of this
     * shape.
     *
     * @return The length
     */
    default double cumulativeLength() {
        return GeometryUtils.shapeLength(toShape());
    }

    /**
     * Collect internal distances between points (typically those that are right
     * angles), for use in snap-to-size features.
     *
     * @param c The collector
     */
    default void collectSizings(SizingCollector c) {

    }

    interface SizingCollector {

        void dimension(double size, boolean vertical, int cpIx1, int cpIx2);
    }

    /**
     * Collect angles of lines within this shape.
     *
     * @param c A collector of angles
     */
    default void collectAngles(Shaped.AngleCollector c) {
        // No way to inherit interface method impls from two places,
        // so we do this horribleness here
        if (this instanceof Shaped && this instanceof Adjustable && !(this instanceof Textual)) {
            VectorVisitor.analyze(((Shaped) this).toShape(), new VectorVisitor() {
                @Override
                public void visit(int pointIndex, LineVector vect, int subpathIndex, RotationDirection subpathRotationDirection, Polygon2D approximate, int prevPointIndex, int nextPointIndex) {
                    CornerAngle ang = vect.corner();
                    c.angle(ang.trailingAngle(), prevPointIndex, pointIndex);
                }
            });
        }
    }

    interface AngleCollector {

        void angle(double angle, int cpIx1, int cpIx2);
    }
}
