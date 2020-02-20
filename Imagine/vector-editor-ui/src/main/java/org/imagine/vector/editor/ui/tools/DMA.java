package org.imagine.vector.editor.ui.tools;

import com.mastfrog.function.DoubleBiConsumer;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import org.netbeans.api.visual.widget.Widget;

/**
 * Move provider which handles control points, etc.
 *
 * @author Tim Boudreau
 */
class DMA implements DoubleMoveProvider {

    static final DMA INSTANCE = new DMA();
    private Point2D lastLocation;

    @Override
    public void movementStarted(Widget widget) {
        Point loc = widget.getLocation();
        System.out.println("set pref loc to " + loc);
        TranslateHandler th = widget.getLookup().lookup(TranslateHandler.class);
        if (th != null) {
            th.onStart(loc);
        }
        widget.setPreferredLocation(loc);
    }

    @Override
    public void movementFinished(Widget widget) {
        if (lastLocation != null) {
            TranslateHandler th = widget.getLookup().lookup(TranslateHandler.class);
            if (th != null) {
                withOffsets(widget, th::translate);
            }
            lastLocation = null;
        }
        widget.setPreferredLocation(null);
        widget.revalidate();
    }

    @Override
    public Point2D getOriginalLocation(Widget widget) {
        return new Point2D.Double(0, 0);
    }

    private void withOffsets(Widget widget, DoubleBiConsumer c) {
        AffineTransform xform = DoubleMoveAction.localToScene(widget);
        double[] vals = new double[]{0, 0, lastLocation.getX(), lastLocation.getY()};
        xform.transform(vals, 0, vals, 0, 2);
        double offX = vals[2] - vals[0];
        double offY = vals[3] - vals[1];
        System.out.println("MOVE to " + lastLocation.getX() + ", " + lastLocation.getY() + " translated to " + Arrays.toString(vals) + " offsets " + offX + ", " + offY);
        c.accept(offX, offY);
    }

    @Override
    public void setNewLocation(Widget widget, Point2D location) {
        lastLocation = location;
        TranslateHandler th = widget.getLookup().lookup(TranslateHandler.class);
        if (th != null) {
            withOffsets(widget, th::onMove);
        }
        widget.setPreferredLocation(new Point((int) location.getX(), (int) location.getY()));
    }

    interface TranslateHandler {

        default void onStart(Point p) {

        }

        default void onMove(double offX, double offY) {

        }

        void translate(double x, double y);
    }

}
