/*
 * RepaintHandle.java
 *
 * Created on October 15, 2005, 8:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.imagine.utils.painting;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * Object passed to the PictureImplementation constructor. Allows the Picture
 * implementation (and indirectly its individual Layer and Surface objects) to
 * notify when some part of the image has been modified, and the UI displaying
 * it should repaint. Also enables tools to set the cursor.
 *
 * @author Timothy Boudreau
 */
@FunctionalInterface
public interface RepaintHandle {

    public void repaintArea(int x, int y, int w, int h);

    default void repaintArea(double x, double y, double w, double h) {
        int xx = (int) Math.floor(x);
        int yy = (int) Math.floor(y);
        int ww = (int) Math.ceil(w + (x - xx));
        int hh = (int) Math.ceil(w + (y - yy));
        repaintArea(xx, yy, ww, hh);
    }

    default void repaintArea(Rectangle r) {
        repaintArea(r.x, r.y, r.width, r.height);
    }

    default void repaintArea(Rectangle2D bds) {
        int x = (int) Math.floor(bds.getX());
        int y = (int) Math.floor(bds.getY());
        int w = (int) Math.ceil(bds.getWidth()
                + (bds.getX() - Math.floor(bds.getX())));
        int h = (int) Math.ceil(bds.getHeight()
                + (bds.getX() - Math.floor(bds.getY())));
        repaintArea(x, y, w, h);
    }

    default void repaintArea(Shape shape, double strokeWidth) {
        Rectangle2D r = shape.getBounds2D();
        r.add(r.getX() - strokeWidth, r.getY() - strokeWidth);
        r.add(r.getX() + r.getWidth() + (strokeWidth * 2),
                r.getY() + r.getHeight() + (strokeWidth * 2));
    }

    default void repaintArea(Shape shape, BasicStroke stroke) {
        if (stroke == null) {
            repaintArea(shape);
        } else {
            repaintArea(shape, stroke.getLineWidth());
        }
    }

    default void repaintArea(Shape shape) {
        repaintArea(shape.getBounds2D());
    }

    default void setCursor(Cursor cursor) {

    }
}
