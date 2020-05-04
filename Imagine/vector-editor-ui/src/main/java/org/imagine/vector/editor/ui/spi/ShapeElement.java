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
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import net.java.dev.imagine.api.vector.painting.VectorWrapperGraphics;
import org.imagine.awt.key.PaintKey;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.PaintingStyle;

/**
 *
 * @author Tim Boudreau
 */
public interface ShapeElement {

    /**
     * Get a unique id for this shape.
     *
     * @return The id
     */
    long id();

    /**
     * Create a new element which is a copy of this one with a new ID.
     *
     * @return A copy
     */
    ShapeElement duplicate();

    /**
     * Create a copy of this shape that share the same ID (so it is effectively
     * the same object for undo purposes).
     *
     * @return A copy
     */
    ShapeElement copy();

    boolean isNameExplicitlySet();

    Rectangle getBounds();

    boolean isDraw();

    boolean isFill();

    boolean isPaths();

    Shaped item();

    Shape shape();

    BasicStroke stroke();

    boolean setStroke(BasicStrokeWrapper wrapper);

    default boolean setStroke(BasicStroke stroke) {
        return setStroke(new BasicStrokeWrapper(stroke));
    }

    void toPaths();

    void setName(String name);

    String getName();

    ShapeControlPoint[] controlPoints(double size, Consumer<ShapeControlPoint> c);

    void addToBounds(Rectangle2D r);

    boolean isNameSet();

    int getControlPointCount();

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

    boolean paint(RenderingGoal goal, Graphics2D g, Rectangle clip, AspectRatio ratio);

    Paint fill(AspectRatio ratio);

    Paint outline(AspectRatio ratio);

    void translate(double x, double y);

    void setPaintingStyle(PaintingStyle style);

    PaintingStyle getPaintingStyle();

    void setFill(PaintKey<?> fill);

    void setFill(Paint fill);

    void setDraw(PaintKey<?> fill);

    void setDraw(Paint draw);

    Paint getFill();

    Paint getDraw();

    PaintKey<?> getFillKey();

    PaintKey<?> getDrawKey();

    Runnable restorableSnapshot();

    Runnable geometrySnapshot();

    ShapeInfo shapeInfo();
}
