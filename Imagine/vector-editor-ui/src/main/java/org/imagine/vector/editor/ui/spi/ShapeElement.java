/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.spi;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.painting.VectorWrapperGraphics;
import org.imagine.editor.api.PaintingStyle;

/**
 *
 * @author Tim Boudreau
 */
public interface ShapeElement {

    /**
     * Get a unique id for this shape.
     * @return The id
     */
    long id();

    /**
     * Create a new element which is a copy of this one
     * with a new ID.
     *
     * @return A copy
     */
    ShapeElement duplicate();

    /**
     * Create a copy of this shape that share the same ID
     * (so it is effectively the same object for undo purposes).
     *
     * @return A copy
     */
    ShapeElement copy();

    Rectangle getBounds();

    boolean isDraw();

    boolean isFill();

    boolean isPaths();

    Shaped item();

    Shape shape();

    BasicStroke stroke();

    void toPaths();

    ControlPoint[] controlPoints(double size, Consumer<ControlPoint> c);

    void changed();

    default DoubleConsumer wrap(DoubleConsumer c) {
        return v -> {
            c.accept(v);
            changed();
        };
    }

    void setShape(Shaped shape);

    default void setShape(Shape shape) {
        setShape(VectorWrapperGraphics.primitiveFor(shape, isFill()));
    }

    void applyTransform(AffineTransform xform);

    boolean canApplyTransform(AffineTransform xform);

    boolean paint(Graphics2D g, Rectangle clip);

    Paint fill();

    Paint outline();

    void translate(double x, double y);

    public void setPaintingStyle(PaintingStyle style);

    public PaintingStyle getPaintingStyle();

    public void setFill(Paint fill);

    public void setDraw(Paint draw);

    public Paint getFill();

    public Paint getDraw();
}
