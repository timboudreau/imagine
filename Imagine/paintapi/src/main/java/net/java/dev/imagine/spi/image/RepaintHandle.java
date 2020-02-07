/*
 * RepaintHandle.java
 *
 * Created on October 15, 2005, 8:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.spi.image;

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
public interface RepaintHandle {

    public void repaintArea(int x, int y, int w, int h);

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

    default void repaintArea(Shape shape) {
        repaintArea(shape.getBounds2D());
    }

    public void setCursor(Cursor cursor);
}
