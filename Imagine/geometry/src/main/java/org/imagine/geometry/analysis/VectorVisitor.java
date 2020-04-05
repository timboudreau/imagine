package org.imagine.geometry.analysis;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import org.imagine.geometry.LineVector;
import org.imagine.geometry.Polygon2D;
import org.imagine.geometry.RotationDirection;

/**
 * For analyzing the angles within potentially complex paths.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface VectorVisitor {

    /**
     * Called once for (at a minimum) each point which begins the third of three
     * consecutive straight lines within a shape, such that there is vector
     * which constitutes a straight-line angle.
     *
     * @param pointIndex The <i>absolute</i> index of the point within the shape
     * (including any preceding cubic or quadratic control points)
     * @param vect The line vector at this point, with the angle correctly
     * normalized
     * @param subpathIndex The index of this subpath within the shape, if it
     * contains multiple paths, in the order encountered in its PathIterator
     * @param subpathRotationDirection The overall rotation direction of this
     * sub-path - the majority of angles turn clockwise or counterclockwise?
     * @param approximate A polygon which *approximates* the entire shape, for
     * hit-testing and similar - reliably implements contains(x,y) which some
     * shapes don't, but contains minimal detail for cubic and quadratic curves.
     */
    void visit(int pointIndex, LineVector vect, int subpathIndex, RotationDirection subpathRotationDirection, Polygon2D approximate,
            int prevPointIndex, int nextPointIndex);

    default RotationDirection analyze(Shape shape) {
        return analyze(shape, (AffineTransform) null);
    }

    default RotationDirection analyze(Shape shape, AffineTransform xform) {
        AnglesAnalyzer ana = new AnglesAnalyzer();
        RotationDirection result = ana.analyzeShape(shape, xform);
        ana.visitAll(this);
        return result;
    }

    default RotationDirection analyze(PathIterator iter) {
        AnglesAnalyzer ana = new AnglesAnalyzer();
        RotationDirection result = ana.analyze(iter);
        ana.visitAll(this);
        return result;
    }

    public static RotationDirection analyze(Shape shape, VectorVisitor vv) {
        return analyze(shape, null, vv);
    }

    public static RotationDirection analyze(Shape shape, AffineTransform xform, VectorVisitor vv) {
        return vv.analyze(shape, xform);
    }

    public static RotationDirection analyze(PathIterator it, VectorVisitor vv) {
        return vv.analyze(it);
    }
}
