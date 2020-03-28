/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget;

import com.mastfrog.util.collections.IntSet;
import java.awt.Rectangle;
import static java.lang.Math.floor;
import java.util.function.IntPredicate;
import org.imagine.geometry.util.GeometryUtils;

/**
 * During painting, we use a BitSet-based IntSet to keep track of which
 * rectangular regions the size of one control point diameter have already been
 * painted, so we don't paint umpteen control points on top of each other at low
 * zoom levels, but instead show a lower level of detail. So, the set is divided
 * up into columns and rows, one for each block of pixels that is the size that
 * one control point takes up at the current zoom level.
 * <p>
 * Basically this gives us a way to coarse-grainedly convert floating point
 * coordinates into an integer space with finite resolution for collision
 * detection, such that two points that would sit within the same integer bounds
 * cannot actually collide. Internally it stores such "cells" as an integer
 * <code>(y * width) + (x % width)</code>.
 * </p>
 * This also makes it possible to minimize detailed lookups of points for hit
 * testing where no point was painting.
 *
 * @author Tim Boudreau
 */
public final class Cells {

    private double cellStride = 1;
    private int blocked = 0;
    private double pxPerCell = 1;
    final IntSet cells = IntSet.create(512);
    private final Rectangle currentBounds = new Rectangle(0, 0, 1, 1);

    /**
     * Clear the last cell set and prepare for a new set of calls to occupy().
     *
     * @param bounds The rectangle being painted
     * @param itemSize The number of physical pixels per item (the x/y
     * coordinates passed to occupy() and normalized will be at the center of
     * the cell's bounding rectangle)
     * @param zoom The zoom factor used to scale the cell size such that the
     * cell size is the same number of visual pixels, so that more cells are
     * available when the zoom level is higher, less when it is lower; zoom
     * factory is the inverse of the scale applied when painting (e.g. double
     * zoom = 2, zoom out by half is 0.5)
     * @param run A runnable within which calls to occupy() will be made
     */
    public void run(Rectangle bounds, double itemSize, double zoom, Runnable run) {
        assert itemSize > 0 : "Illegal item size " + itemSize;
        assert zoom > 0 : "Illegal zoom " + zoom;
        assert run != null : "Null runnable";
        assert bounds != null : "Null bounds";
        cells.clear();
        currentBounds.setBounds(bounds);
        cellStride = itemSize;
        pxPerCell = cellStride * (1D / zoom);
        blocked = 0;
        run.run();
    }

    /**
     * Occupy the rectangular cell for a given pair of coordinates, returning
     * true if it was not already occupied.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return True if the cell has newly been occupied
     */
    public boolean occupy(double x, double y) {
        assert currentBounds != null : "Called outside run()";
        return withCell(x, y, target -> {
            if (target < 0) {
                System.out.println("Got negative result for "
                        + x + ", " + y + ": " + target + " within "
                        + GeometryUtils.toString(currentBounds));
                return false;
            }
            boolean result = cells.add(target);
            if (!result) {
                blocked++;
            }
            return result;
        });
    }

    public Rectangle bounds() {
        return currentBounds;
    }

    private boolean withCell(double x, double y, IntPredicate test) {
        double w = currentBounds.width / pxPerCell;
        double left = floor((x - currentBounds.x) / pxPerCell) % w;
        double down = floor((y - currentBounds.y) / pxPerCell) * w;
        return test.test((int) (left + down));
    }

    public boolean wasInLastBounds(double x, double y) {
        return currentBounds.contains(x, y);
    }

    /**
     * Determine if the cell for these coordinates was used on the last call to
     * <code>run()</code>.
     *
     * @param x An x coordinate
     * @param y A y coordinate
     * @return True if the cell was used
     */
    public boolean wasOccupied(double x, double y) {
        return withCell(x, y, target -> {
            if (target < 0) {
                return false;
            }
            return cells.contains(target);
        });
    }

    /**
     * Get the number of occupied cells.
     *
     * @return The number of cells
     */
    public int occupied() {
        return cells.size();
    }

    /**
     * Determine if no cells were used in the last call of <code>run()</code>.
     *
     * @return True if no cells were used
     */
    public boolean isEmpty() {
        return cells.isEmpty();
    }

    /**
     * Get the number of calls to <code>occupy(x,y)</code> that returned false
     * during the last run.
     *
     * @return The number of attempts to occupy that failed during the last run
     */
    public int lastConcealedPoints() {
        return blocked;
    }
}
