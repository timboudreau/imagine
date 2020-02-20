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
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Shaped;
import org.imagine.vector.editor.ui.tools.CSGOperation;

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

    void edit(String name, ShapeElement el, Runnable r);

    Rectangle getBounds();

    boolean paint(Graphics2D g, Rectangle bounds);

    ShapesCollection snapshot();

    ShapeElement[] replaceShape(ShapeElement entry, Shaped... replacements);

    boolean deleteShape(ShapeElement entry);

    List<? extends ShapeElement> shapesAtPoint(double x, double y);

    boolean csg(CSGOperation op, ShapeElement lead, List<? extends ShapeElement> elements, BiConsumer<Set<ShapeElement>, ShapeElement> removedAndAdded);

    boolean toFront(ShapeElement en);

    boolean toBack(ShapeElement en);

    List<? extends ShapeElement> possiblyOverlapping(ShapeElement el);

    ShapeElement duplicate(ShapeElement el);
}
