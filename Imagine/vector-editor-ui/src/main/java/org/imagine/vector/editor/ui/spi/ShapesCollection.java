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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoint;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoints;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPointsConsumer;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Shaped;
import org.imagine.vector.editor.ui.tools.CSGOperation;
import org.imagine.vector.editor.ui.undo.UndoRedoHookable;

/**
 *
 * @author Tim Boudreau
 */
public interface ShapesCollection extends Hibernator, Iterable<ShapeElement> {

    ShapeElement add(Shaped vect, Paint bg, Paint fg, BasicStroke stroke,
            boolean draw, boolean fill);

    void addAll(ShapesCollection shapes, Shape clip);

    ShapesCollection applyTransform(AffineTransform xform);

    boolean canApplyTransform(AffineTransform xform);

    ShapesCollection clip(Shape shape);

    /**
     * Initiate an edit of one shape.
     *
     * @param name The presentation name of the edit (for undo)
     * @param el The element to be edited
     * @param r A runnable which will perform the edit
     */
    UndoRedoHookable edit(String name, ShapeElement el, Runnable r);

    Supplier<SnapPoints> snapPoints(double radius, BiConsumer<SnapPoint, SnapPoint> onSnap);

    /**
     * Initiate an edit of the geometry of multiple shapes (e.g., applying an
     * Affine Transform to all shapes or a collection thereof).
     *
     * @param name The presentation name of the edit
     * @param r A runnable which will perform the edit
     */
    UndoRedoHookable geometryEdit(String name, Runnable r);

    UndoRedoHookable contentsEdit(String name, Runnable r);

    void addToBounds(Rectangle2D b);

    public Runnable contentsSnapshot();

    /**
     * Wrap a DoubleConsumer in a call not edit a particular shape, such than if
     * the DoubleConsumer is called, the change it makes can be recorded as an
     * edit.
     *
     * @param name The presentation name of the edit (for undo)
     * @param target The shape to be edited
     * @param orig The setter on the shape
     * @return A DoubleConsumer that proxies the setter, and notifies for
     * repaints or creates undo events as needed
     */
    default DoubleConsumer wrapInEdit(String name, ShapeElement target, DoubleConsumer orig) {
        return val -> {
            edit(name, target, () -> {
                target.wrap(orig).accept(val);
            });
        };
    }

    void getBounds(Rectangle2D into);

    Rectangle getBounds();

    boolean paint(Graphics2D g, Rectangle bounds);

    ShapesCollection snapshot();

    ShapeElement[] replaceShape(ShapeElement entry, Shaped... replacements);

    boolean deleteShape(ShapeElement entry);

    List<? extends ShapeElement> shapesAtPoint(double x, double y);

    boolean csg(CSGOperation op, ShapeElement lead,
            List<? extends ShapeElement> elements,
            BiConsumer<? super Set<? extends ShapeElement>, ? super ShapeElement> removedAndAdded);

    boolean toFront(ShapeElement en);

    boolean toBack(ShapeElement en);

    List<? extends ShapeElement> possiblyOverlapping(ShapeElement el);

    ShapeElement duplicate(ShapeElement el);

    int size();

    ShapeElement get(int index);

    int indexOf(ShapeElement el);

    boolean contains(ShapeElement el);

    Set<? extends ShapeElement> absent(Collection<? extends ShapeElement> from);
}
