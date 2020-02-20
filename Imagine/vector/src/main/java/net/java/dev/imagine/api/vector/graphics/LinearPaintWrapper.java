package net.java.dev.imagine.api.vector.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;
import org.imagine.utils.java2d.GraphicsUtils;

/**
 *
 * @author Tim Boudreau
 */
public class LinearPaintWrapper implements Primitive, PaintWrapper, Attribute<LinearGradientPaint>, Transformable {

    private double centerX;
    private double centerY;
    private double focusX;
    private double focusY;
    private float[] fractions;
    private int[] colors;
    private int cycleMethod;
    private int colorSpaceType;
    private int xpar;
    private double[] transform;

    public LinearPaintWrapper(double centerX, double centerY,
            double focusX, double focusY, float[] fractions,
            int[] colors, int cycleMethod, int colorSpaceType,
            int xpar, double[] transform) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.focusX = focusX;
        this.focusY = focusY;
        this.fractions = fractions;
        this.colors = colors;
        this.cycleMethod = cycleMethod;
        this.colorSpaceType = colorSpaceType;
        this.xpar = xpar;
        this.transform = transform;
    }

    public LinearPaintWrapper(LinearGradientPaint p) {
        fractions = p.getFractions();
        centerX = p.getStartPoint().getX();
        centerY = p.getStartPoint().getY();
        focusX = p.getEndPoint().getX();
        focusY = p.getEndPoint().getY();
        Color[] colors = p.getColors();
        this.colors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            this.colors[i] = colors[i].getRGB();
        }
        cycleMethod = p.getCycleMethod().ordinal();
        colorSpaceType = p.getColorSpace().ordinal();
        xpar = p.getTransparency();
        AffineTransform xform = p.getTransform();
        this.transform = new double[6];
        xform.getMatrix(this.transform);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LinearPaintWrapper(");
        sb.append(" center ").append(centerX).append(',').append(centerY);
        sb.append(" focus ").append(focusX).append(',').append(focusY);
        sb.append(' ').append(CycleMethod.values()[cycleMethod]);
        for (int i = 0; i < colors.length; i++) {
            sb.append(' ').append(" @").append(fractions[i])
                    .append(" <");
            GradientPaintWrapper.rgb(new Color(colors[i], true), sb);
            sb.append('>');
        }
        return sb.append(')').toString();
    }

    private AffineTransform transform() {
        return new AffineTransform(this.transform);
    }

    public Color[] colors() {
        Color[] result = new Color[this.colors.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Color(colors[i], true);
        }
        return result;
    }

    @Override
    public Paint toPaint() {
        return get();
    }

    @Override
    public void paint(Graphics2D g) {
        g.setPaint(toPaint());
    }

    @Override
    public LinearPaintWrapper copy() {
        double[] newTransform = Arrays.copyOf(transform, transform.length);
        float[] newFractions = Arrays.copyOf(fractions, fractions.length);
        int[] newColors = Arrays.copyOf(colors, colors.length);
        return new LinearPaintWrapper(centerX, centerY, focusX, focusY,
                newFractions, newColors, cycleMethod, colorSpaceType, xpar,
                newTransform);
    }

    @Override
    public Color toColor() {
        return GraphicsUtils.average(colors());
    }

    @Override
    public LinearPaintWrapper createScaledInstance(AffineTransform xform) {
        double[] pts = new double[]{centerX, centerY, focusX, focusY};
        xform.transform(pts, 0, pts, 0, 2);
        return new LinearPaintWrapper(pts[0], pts[1], pts[2], pts[3],
                Arrays.copyOf(fractions, fractions.length), Arrays.copyOf(colors, colors.length),
                cycleMethod, colorSpaceType, xpar, transform);
    }

    @Override
    public LinearGradientPaint get() {
        return new LinearGradientPaint(new Point2D.Double(centerX, centerY),
                new Point2D.Double(focusX, focusY), fractions, colors(),
                CycleMethod.values()[cycleMethod],
                ColorSpaceType.values()[colorSpaceType],
                transform()
        );
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.centerX) ^ (Double.doubleToLongBits(this.centerX) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.centerY) ^ (Double.doubleToLongBits(this.centerY) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.focusX) ^ (Double.doubleToLongBits(this.focusX) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.focusY) ^ (Double.doubleToLongBits(this.focusY) >>> 32));
        hash = 83 * hash + Arrays.hashCode(this.fractions);
        hash = 83 * hash + Arrays.hashCode(this.colors);
        hash = 83 * hash + this.cycleMethod;
        hash = 83 * hash + this.colorSpaceType;
        hash = 83 * hash + this.xpar;
        hash = 83 * hash + Arrays.hashCode(this.transform);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof LinearPaintWrapper)) {
            return false;
        }
        final LinearPaintWrapper other = (LinearPaintWrapper) obj;
        if (this.cycleMethod != other.cycleMethod) {
            return false;
        } else if (this.colorSpaceType != other.colorSpaceType) {
            return false;
        } else if (this.xpar != other.xpar) {
            return false;
        } else if (Double.doubleToLongBits(this.centerX) != Double.doubleToLongBits(other.centerX)) {
            return false;
        } else if (Double.doubleToLongBits(this.centerY) != Double.doubleToLongBits(other.centerY)) {
            return false;
        } else if (Double.doubleToLongBits(this.focusX) != Double.doubleToLongBits(other.focusX)) {
            return false;
        } else if (Double.doubleToLongBits(this.focusY) != Double.doubleToLongBits(other.focusY)) {
            return false;
        } else if (!Arrays.equals(this.fractions, other.fractions)) {
            return false;
        } else if (!Arrays.equals(this.colors, other.colors)) {
            return false;
        } else {
            return Arrays.equals(this.transform, other.transform);
        }
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        AffineTransform curr = transform();
        curr.concatenate(xform);
        curr.getMatrix(transform);
    }

    @Override
    public LinearPaintWrapper copy(AffineTransform transform) {
        double[] newTransform = new double[this.transform.length];
        float[] newFractions = Arrays.copyOf(fractions, fractions.length);
        int[] newColors = Arrays.copyOf(colors, colors.length);
        AffineTransform curr = transform();
        curr.concatenate(transform);
        curr.getMatrix(newTransform);
        return new LinearPaintWrapper(centerX, centerY, focusX, focusY,
                newFractions, newColors, cycleMethod, colorSpaceType, xpar,
                newTransform);
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public double getFocusX() {
        return focusX;
    }

    public void setFocusX(double focusX) {
        this.focusX = focusX;
    }

    public double getFocusY() {
        return focusY;
    }

    public void setFocusY(double focusY) {
        this.focusY = focusY;
    }

    public float[] getFractions() {
        return Arrays.copyOf(fractions, fractions.length);
    }

    public void setFractions(float[] fractions) {
        checkFractions(fractions);
        this.fractions = fractions;
    }

    public int[] getColors() {
        return colors;
    }

    private void checkFractions(float[] fractions) {
        if (fractions == null) {
            throw new IllegalArgumentException("Null fractions array");
        }
        float last = Float.MIN_VALUE;
        for (int i = 0; i < fractions.length; i++) {
            if (fractions[i] <= last) {
                throw new IllegalArgumentException("Fractions out of order at "
                        + i + " in " + Arrays.toString(fractions));
            }
            if (fractions[i] < 0 || fractions[i] > 1) {
                throw new IllegalArgumentException("Fractions must be between "
                        + "0.0 and 1.0 but found " + fractions[i] + " at "
                        + i);
            }
            last = fractions[i];
        }
    }

    public void setColorsAndFractions(int[] colors, float[] fractions) {
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Colors and fractions are not the same");
        }
        checkFractions(fractions);
        this.colors = Arrays.copyOf(colors, colors.length);
        this.fractions = Arrays.copyOf(fractions, fractions.length);
    }

    public void setColorsAndFractions(Color[] colors, float[] fractions) {
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Colors and fractions are not the same");
        }
        checkFractions(fractions);
        int[] newColors = new int[colors.length];
        for (int i = 0; i < newColors.length; i++) {
            if (colors[i] == null) {
                throw new IllegalArgumentException("Null color in array at " + i);
            }
            newColors[i] = colors[i].getRGB();
        }
        this.colors = newColors;
        this.fractions = Arrays.copyOf(fractions, fractions.length);
    }

    public void setColors(int[] colors) {
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Colors and fractions are not the same");
        }
        this.colors = colors;
    }

    public int getCycleMethod() {
        return cycleMethod;
    }

    public void setCycleMethod(int cycleMethod) {
        if (cycleMethod < 0 || cycleMethod > CycleMethod.values().length) {
            throw new IllegalArgumentException("Invalid CycleMethod ordinal " + cycleMethod);
        }
        this.cycleMethod = cycleMethod;
    }

    public void setCycleMethod(CycleMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("Null cycle method");
        }
        this.cycleMethod = method.ordinal();
    }

    public CycleMethod cycleMethod() {
        return CycleMethod.values()[cycleMethod];
    }

    public ColorSpaceType colorSpaceType() {
        return ColorSpaceType.values()[colorSpaceType];
    }

    public void setColorSpaceType(ColorSpaceType type) {
        if (type == null) {
            throw new IllegalArgumentException("Null color space type");
        }
        colorSpaceType = type.ordinal();
    }

    public int getColorSpaceType() {
        return colorSpaceType;
    }

    public void setColorSpaceType(int colorSpaceType) {
        this.colorSpaceType = colorSpaceType;
    }

    public int getTransparency() {
        return xpar;
    }

    public void setTransparency(int xpar) {
        switch (xpar) {
            case Transparency.BITMASK:
            case Transparency.OPAQUE:
            case Transparency.TRANSLUCENT:
                break;
            default:
                throw new IllegalArgumentException("Invalid transparency " + xpar);
        }
        this.xpar = xpar;
    }

    public double[] getTransform() {
        return transform;
    }

    public void setTransform(double[] transform) {
        this.transform = transform;
    }
}
