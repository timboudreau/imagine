/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.Graphics2D;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapAxis;
import org.imagine.editor.api.snap.SnapCoordinate;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
abstract class OneTypePainter {

    protected abstract void requestRepaint(RepaintHandle handle);

    protected final void paintDecorations(Graphics2D g, Zoom zoom, ShapeElement selected) {
        if (!active) {
            return;
        }
        paint(g, zoom, selected);
    }

    protected abstract void paint(Graphics2D g, Zoom zoom, ShapeElement selected);

    private boolean active = false;

    void activate() {
        active = true;
    }

    protected boolean active() {
        return active;
    }

    protected void resign() {
        active = false;
    }

    public boolean onSnap(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        active = x != null || y != null;
        if (x != null && y != null) {
            assert x.axis() == SnapAxis.X;
            assert y.axis() == SnapAxis.Y;
            active = true;
            return doSnapBoth(x, y, zoom, selection);
        } else if (x != null) {
            assert x.axis() == SnapAxis.X;
            active = true;
            return doSnapX(x, zoom, selection);
        } else if (y != null) {
            assert y.axis() == SnapAxis.Y;
            active = true;
            return doSnapY(y, zoom, selection);
        } else {
            active = false;
        }
        return false;
    }

    private boolean doSnapX(SnapCoordinate<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        return active = snapX(s, zoom, selection);
    }

    protected boolean snapX(SnapCoordinate<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        return snapOne(s, zoom, selection);
    }

    private boolean doSnapY(SnapCoordinate<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        return active = snapY(s, zoom, selection);
    }

    protected boolean snapY(SnapCoordinate<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        return snapOne(s, zoom, selection);
    }

    protected boolean snapOne(SnapCoordinate<ShapeSnapPointEntry> s, Zoom zoom, ShapeElement selection) {
        return false;
    }

    private final boolean doSnapBoth(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        return active = snapBoth(x, y, zoom, selection);
    }

    protected boolean snapBoth(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y, Zoom zoom, ShapeElement selection) {
        return false;
    }
}
