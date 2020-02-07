/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.SnapPointsConsumer.SnapPoints;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import org.imagine.vector.editor.ui.spi.HitTester;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
public class Shapes implements Hibernator, HitTester {

    private List<ShapeEntry> shapes = new ArrayList<>(20);

    public Shapes() {
    }

    public ShapeEntry add(Shaped vect, Paint bg, Paint fg, BasicStroke stroke, boolean draw, boolean fill) {
//        System.out.println("ADD TO SHAPES " + vect + " " + bg + " " + fg + " " + fill + " " + draw);
        if (bg == null && fg == null) {
            bg = Color.BLACK;
            fg = Color.BLACK;
        }

        ShapeEntry prev = shapes.isEmpty() ? null : shapes.get(shapes.size() - 1);
        if (prev != null) {
            if (vect.equals(prev.vect)) {
                if (prev.isFill() && !fill) {
                    prev.setOutline(fg);
                    return prev;
                }
            }
        }
        ShapeEntry se = new ShapeEntry(vect, bg, fg, stroke, draw, fill);
        shapes.add(0, se);
        pts = null;
        return se;
    }

    public Shapes clip(Shape shape) {
        Shapes nue = new Shapes();
        Rectangle2D shapeBounds = shape.getBounds2D();
        for (ShapeEntry e : shapes) {
            Shape otherShape = e.vect.toShape();
            Rectangle2D otherBounds = otherShape.getBounds2D();
            if (shapeBounds.contains(otherBounds)) {
                nue.shapes.add(e.copy());
            } else if (otherShape.intersects(otherBounds)) {
                Area area = new Area(otherShape);
                area.intersect(new Area(shape));
                ShapeEntry n = e.copy();
                n.vect = new PathIteratorWrapper(area.getPathIterator(null), e.isFill());
                nue.shapes.add(n);
            }
        }
        return nue;
    }

    public void addAll(Shapes shapes, Shape clip) {
        if (clip != null) {
            shapes = shapes.clip(clip);
            for (ShapeEntry e : shapes.shapes) {
                this.shapes.add(e);
            }
        } else {
            for (ShapeEntry e : shapes.shapes) {
                this.shapes.add(e.copy());
            }
        }
    }

    Shapes snapshot() {
        Shapes result = new Shapes();
        for (ShapeEntry e : shapes) {
            result.shapes.add(e.copy());
        }
        return result;
    }

    Rectangle restore(Shapes snapshot) {
        Rectangle bds = getBounds();

        shapes.clear();
        shapes.addAll(snapshot.shapes);

        bds.add(getBounds());
        return bds;
    }

    public int hits(Point2D pt, Consumer<? super ShapeElement> c) {
        int count = 0;
        for (ShapeEntry se : shapes) {
            if (se.contains(pt)) {
                c.accept(se);
                count++;
            }
        }
        return count;
    }

    @Override
    public void hibernate() {
        shapes.forEach(ShapeEntry::hibernate);
    }

    @Override
    public void wakeup(boolean immediately, Runnable notify) {
        for (ShapeEntry sh : shapes) {
            sh.wakeup(immediately, null);
        }
    }

    public boolean paint(Graphics2D g, Rectangle bounds) {
        int max = shapes.size() - 1;
        boolean result = false;
        for (int i = max; i >= 0; i--) {
            ShapeEntry se = shapes.get(i);
            result |= se.paint(g, bounds);
        }
        return result;
    }

    public Rectangle getBounds() {
        Rectangle result = new Rectangle();
        for (ShapeEntry se : shapes) {
            result.add(se.getBounds());
        }
        return result;
    }

    void onChange() {
        pts = null;
    }

    SnapPoints pts;

    Supplier<SnapPoints> snapPoints(double radius) {
        return () -> {
            SnapPoints result = pts;
            if (result != null) {
                return result;
            }
            SnapPoints.Builder b = SnapPoints.builder(radius);
            for (ShapeEntry se : shapes) {
                se.addTo(b);
            }
            return pts = b.build();
        };
    }
}
