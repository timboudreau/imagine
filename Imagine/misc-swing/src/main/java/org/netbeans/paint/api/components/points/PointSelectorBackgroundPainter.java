package org.netbeans.paint.api.components.points;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface PointSelectorBackgroundPainter {

    void paintBackground(Graphics2D g, Point2D target, Rectangle2D frame,
            double angle, PointSelectorMode mode, PointSelector sel);

}
