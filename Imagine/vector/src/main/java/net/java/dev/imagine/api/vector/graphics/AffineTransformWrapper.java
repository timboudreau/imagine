/*
 * AffineTransformWrapper.java
 *
 * Created on October 30, 2006, 9:29 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.graphics;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;

/**
 *
 * @author Tim Boudreau
 */
public class AffineTransformWrapper implements Primitive, Transformable {

    public static final int TRANSLATE = 0;
    public static final int SCALE = 1;
    public static final int SHEAR = 2;
    public static final int ROTATE = 3;
    public static final int QUADROTATE = 4;
    public final double[] matrix = new double[6];

    public AffineTransformWrapper(AffineTransform xform) {
        xform.getMatrix(matrix);
    }

    public AffineTransformWrapper(int x, int y, int kind) {
        switch (kind) {
            case TRANSLATE:
                AffineTransform.getTranslateInstance(x, y).getMatrix(matrix);
                break;
            case SCALE:
                AffineTransform.getScaleInstance(x, y).getMatrix(matrix);
                break;
            case SHEAR:
                AffineTransform.getShearInstance(x, y).getMatrix(matrix);
                break;
            case ROTATE:
            case QUADROTATE:
            default:
                throw new IllegalArgumentException("Wrong constructor");

        }
    }

    /**
     * Quadrant rotate
     */
    public AffineTransformWrapper(int numquadrants) {
        AffineTransform.getQuadrantRotateInstance(numquadrants).getMatrix(matrix);
    }

    /**
     * Quadrant rotate
     */
    public AffineTransformWrapper(int numquadrants, double anchorx, double anchory) {
        AffineTransform.getQuadrantRotateInstance(numquadrants, anchorx, anchory).getMatrix(matrix);
    }

    /**
     * Rotate
     */
    public AffineTransformWrapper(double theta) {
        AffineTransform.getRotateInstance(theta).getMatrix(matrix);
    }

    /**
     * Rotate
     */
    public AffineTransformWrapper(double vecx, double vecy, int kind) {
        switch (kind) {
            case TRANSLATE:
                AffineTransform.getTranslateInstance(vecx, vecy).getMatrix(matrix);
                break;
            case ROTATE:
//                AffineTransform.getRotateInstance(vecx, vecy).getMatrix(matrix);
                break;
            case SCALE:
                AffineTransform.getScaleInstance(vecx, vecy).getMatrix(matrix);
                break;
            default:
                throw new IllegalArgumentException("Only TRANSLATE, ROTATE"
                        + "and SCALE allowed by this constructor");
        }
    }

    /**
     * Rotate
     */
    public AffineTransformWrapper(double theta, double vecx, double vecy) {
        AffineTransform.getRotateInstance(theta, vecx, vecy).getMatrix(matrix);
    }

    /**
     * Rotate
     */
    public AffineTransformWrapper(double vecx, double vecy, double anchorx, double anchory) {
        AffineTransform.getRotateInstance(vecx, vecy, anchorx, anchory).getMatrix(matrix);
    }

    public AffineTransform getAffineTransform() {
        return new AffineTransform(matrix);
    }

    public AffineTransformWrapper merge(AffineTransform at) {
        AffineTransform result = getAffineTransform();
        result.concatenate(at);
        return new AffineTransformWrapper(result);
    }

    @Override
    public void paint(Graphics2D g) {
        //XXX should concatenate to current transform?
        g.setTransform(getAffineTransform());
    }

    @Override
    public AffineTransformWrapper copy() {
        return new AffineTransformWrapper(getAffineTransform());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Arrays.hashCode(this.matrix);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof AffineTransformWrapper)) {
            return false;
        }
        final AffineTransformWrapper other = (AffineTransformWrapper) obj;
        return Arrays.equals(this.matrix, other.matrix);
    }

    @Override
    public String toString() {
        return "AffineTransformWrapper("
                + Arrays.toString(matrix) + ")";
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        AffineTransform xf = getAffineTransform();
        xf.concatenate(xform);
        xf.getMatrix(matrix);
    }

    @Override
    public AffineTransformWrapper copy(AffineTransform xform) {
        AffineTransform xf = getAffineTransform();
        xf.concatenate(xform);
        return new AffineTransformWrapper(new AffineTransform(xf));
    }
}
