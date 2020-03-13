/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import org.imagine.vector.editor.ui.tools.widget.util.ViewL;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class CoordinateSpaceUtils {

    private static final Point zeros = new Point();

    public final Point2D convertSceneToView(Scene scene, double zoom, Point2D sceneLocation) {
        Point location = scene.getLocation();
        double zoomFactor = scene.getZoomFactor();
        return new Point2D.Double((zoomFactor * (location.getX() + sceneLocation.getX())), (zoomFactor * (location.getY() + sceneLocation.getY())));
    }

    static AffineTransform sceneToLocal(Widget widget) {
        double zoom = widget.getScene().getZoomFactor();
        Point2D.Double widgetPoint = toPoint2D(widget.convertLocalToScene(zeros));
        Point2D sceneLoc = widget.getScene().getLocation();
        widgetPoint.x += sceneLoc.getX();
        widgetPoint.y += sceneLoc.getY();
//        Point2D.Double widgetPoint = toPoint2D(widget.convertLocalToScene(zeros));
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(zoom, zoom);
        AffineTransform xform = AffineTransform.getTranslateInstance(widgetPoint.x, widgetPoint.y);
        scaleTransform.concatenate(xform);
        return scaleTransform;
    }

    static AffineTransform localToScene(Widget widget) {
        try {
            return sceneToLocal(widget).createInverse();
        } catch (NoninvertibleTransformException ex) {
            ex.printStackTrace();
            return AffineTransform.getTranslateInstance(0, 0);
        }
    }

    static Point2D.Double floatingPoint(Widget widget, WidgetAction.WidgetMouseEvent wme) {
        Point2D.Double result = ViewL.lastPoint(widget);
        AffineTransform sceneToLocal = sceneToLocal(widget);
        sceneToLocal.transform(result, result);
        return result;
    }

    static Point2D.Double toPoint2D(Point point) {
        return new Point2D.Double(point.x, point.y);
    }

    public static Point2D.Double convertLocalToScene(Widget widget, Point2D pt) {
        Point2D.Double result = new Point2D.Double(0, 0);
        localToScene(widget).transform(pt, result);
        return result;
    }

    static Point2D.Double sceneToLocal(Widget widget, Point2D pt) {
        Point2D.Double result = new Point2D.Double(0, 0);
        sceneToLocal(widget).transform(pt, result);
        return result;
    }

}
