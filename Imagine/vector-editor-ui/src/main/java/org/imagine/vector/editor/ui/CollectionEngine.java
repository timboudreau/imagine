package org.imagine.vector.editor.ui;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import net.java.dev.imagine.api.vector.painting.VectorWrapperGraphics;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.utils.painting.RepaintHandle;

/**
 * Collects shapes as they are painted by tools when committing
 * and handles undo support.
 *
 * @author Tim Boudreau
 */
class CollectionEngine implements RepaintHandle {

    final VectorWrapperGraphics graphics;
    final String opName;
    private final ShapeCollector collector = new ShapeCollector();
    private final Rectangle bounds = new Rectangle();
    private final RepaintHandle handle;
    private final Point loc;
    private final int w;
    private final int h;

    public CollectionEngine(String opName, RepaintHandle handle, Point loc, int w, int h) {
        this.graphics = new VectorWrapperGraphics(collector, GraphicsUtils.noOpGraphics(), new Point(), 1, 1);
        this.opName = opName;
        this.handle = handle;
        this.loc = loc;
        this.w = w;
        this.h = h;
    }

    Graphics2D graphics() {
        return GraphicsUtils.wrap(handle, graphics, loc, w, h);
    }

    boolean hasItems() {
        return !collector.isEmpty();
    }

    Rectangle finish(Shapes shapes) {
        if (!collector.isEmpty()) {
            collector.replay(shapes);
        }
        return collector.getBounds();
    }

    @Override
    public void repaintArea(int x, int y, int w, int h) {
        bounds.add(new Rectangle(x, y, w, h));
    }

    @Override
    public void setCursor(Cursor cursor) {
        // do nothing
    }

}
