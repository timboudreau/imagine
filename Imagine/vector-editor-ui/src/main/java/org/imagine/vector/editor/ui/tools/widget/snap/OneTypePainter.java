/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.Graphics2D;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapAxis;
import org.imagine.editor.api.snap.SnapPoint;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
abstract class OneTypePainter {

    protected abstract void requestRepaint(RepaintHandle handle);

    protected abstract void paint(Graphics2D g, Zoom zoom, ShapeElement selected);

    public boolean onSnap(SnapPoint<ShapeSnapPointEntry> x, SnapPoint<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        if (x != null && y != null) {
            assert x.axis() == SnapAxis.X;
            assert y.axis() == SnapAxis.Y;
            return snapBoth(x, y, zoom, selection);
        } else if (x != null) {
            assert x.axis() == SnapAxis.X;
            return snapX(x, zoom, selection);
        } else if (y != null) {
            assert y.axis() == SnapAxis.Y;
            return snapY(y, zoom, selection);
        }
        return false;
    }

    protected boolean snapX(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        return snapOne(s, zoom, selection);
    }

    protected boolean snapY(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        return snapOne(s, zoom, selection);
    }

    protected boolean snapOne(SnapPoint<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        return false;
    }

    protected boolean snapBoth(SnapPoint<ShapeSnapPointEntry> x, SnapPoint<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        return false;
    }
}
