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
 *
 * @author Tim Boudreau
 */
public class Cells {

    private double controlPointStride;
    private int blocked = 0;
    private double pxPerCell;
    final IntSet cells = IntSet.create(512);

    private Rectangle currentBounds;

    public void run(Rectangle bounds, double itemSize, double zoom, Runnable run) {
        cells.clear();
        currentBounds = bounds;
        controlPointStride = itemSize;
        pxPerCell = controlPointStride * (1D / zoom);
        blocked = 0;
        run.run();
    }

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

    public boolean wasOccupied(double x, double y) {
        return withCell(x, y, target -> {
            if (target < 0) {
                return false;
            }
            return cells.contains(target);
        });
    }

    public int occupied() {
        return cells.size();
    }

    public int lastConcealedPoints() {
        return blocked;
    }
}
