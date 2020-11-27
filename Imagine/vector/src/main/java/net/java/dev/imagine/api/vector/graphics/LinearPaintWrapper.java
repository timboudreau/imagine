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
import static java.lang.Double.doubleToLongBits;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;
import com.mastfrog.geometry.util.FloatList;
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
        if (centerX == focusX && centerY == focusY) {
            this.focusX += 0.001;
            this.focusY += 0.001;
        }
        this.fractions = fixFractions(colors.length, fractions);
        this.colors = colors;
        this.cycleMethod = cycleMethod;
        this.colorSpaceType = colorSpaceType;
        this.xpar = xpar;
        this.transform = transform;
//        checkFractions(fractions);
    }

    static float[] fixFractions(int colorsSize, float[] fractions) {
        float last = fractions[0];
        boolean bad = false;
        for (int i = 1; i < fractions.length; i++) {
            float f = fractions[i];
            if (f < 0 || f > 1) {
                bad = true;
                break;
            }
            if (last >= f) {
                bad = true;
                break;
            }
            last = f;
        }
        if (bad) {
            // Some saved files with duplicate fractions exist
            FloatList fl = new FloatList(fractions.length);
            for (int i = 0; i < fractions.length; i++) {
                float f = fractions[i];
                if (f < 0 || f > 1) {
                    if (i == 0) {
                        fl.add(0);
                    } else {
                        if (fl.isEmpty()) {
                            fl.add(0);
                        }
                        float prev = fl.isEmpty() ? -0.001F : fl.getFloat(fl.size() - 1);
                        fl.add(prev + 0.001F);
                    }
                    continue;
                }
                if (i > 0) {
                    float prev = fl.isEmpty() ? -0.001F : fl.getFloat(fl.size() - 1);
                    if (prev >= f) {
                        f = prev + 0.001F;
                    }
                }
                fl.add(f);
            }
            fl.sort();
            if (fl.size() < colorsSize) {
                if (fl.size() == 0) {
                    float frac = 1F / colorsSize;
                    for (int i = 0; i < colorsSize; i++) {
                        fl.add(frac * i);
                    }
                } else {
                    last = fl.last();
                    if (last >= 1F) {
                        int needAdd = (colorsSize - fl.size());
                        fl.setAsFloat(fl.size() - 1, fl.last() - (needAdd * 0.001F));
                        float val = fl.last();
                        for (int i = 0; i < needAdd; i++) {
                            val += 0.001F;
                            fl.add(val);
                        }
                    }
                }
            }
            return fl.toFloatArray();
        } else {
            return fractions;
        }

    }

    static void checkFractions(float[] fractions) {
        if (fractions.length == 0) {
            throw new IllegalArgumentException("Empty fractions array");
        }
        float last = fractions[0];
        for (int i = 1; i < fractions.length; i++) {
            float f = fractions[i];
            if (f < 0 || f > 1) {
                throw new IllegalArgumentException("Fractions must be increasing from 0 to 1, but "
                        + "found out-of-range value " + f + " at " + i + " in "
                        + Arrays.toString(fractions));
            }
            if (last >= f) {
                throw new IllegalArgumentException("Fractions must be increasing from 0 to 1, but "
                        + "found out-of-sequence value " + f + " at " + i + " in "
                        + Arrays.toString(fractions));
            }
            last = f;
        }
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
        long hash = 83 * ((93979 * doubleToLongBits(centerX))
                + (7 * doubleToLongBits(centerY))
                + (829 * doubleToLongBits(focusX))
                + (64151 * doubleToLongBits(focusY)))
                + (5 * Arrays.hashCode(this.fractions))
                + (433 * Arrays.hashCode(this.colors))
                + (3323 * Arrays.hashCode(this.transform));
        hash = (83 * hash + this.cycleMethod)
                + (83 * hash + colorSpaceType)
                + (83 * hash + xpar);
        return (((int) hash) ^ ((int) (hash >> 32)));
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
        if (cycleMethod != other.cycleMethod) {
            return false;
        } else if (colorSpaceType != other.colorSpaceType) {
            return false;
        } else if (xpar != other.xpar) {
            return false;
        } else if (centerX != other.centerX && Double.doubleToLongBits(centerX) != Double.doubleToLongBits(other.centerX)) {
            return false;
        } else if (centerY != other.centerY && Double.doubleToLongBits(centerY) != Double.doubleToLongBits(other.centerY)) {
            return false;
        } else if (focusX != other.focusX && Double.doubleToLongBits(focusX) != Double.doubleToLongBits(other.focusX)) {
            return false;
        } else if (focusY != other.focusY && Double.doubleToLongBits(focusY) != Double.doubleToLongBits(other.focusY)) {
            return false;
        } else if (!Arrays.equals(fractions, other.fractions)) {
            return false;
        } else if (!Arrays.equals(colors, other.colors)) {
            return false;
        } else {
            return Arrays.equals(transform, other.transform);
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
