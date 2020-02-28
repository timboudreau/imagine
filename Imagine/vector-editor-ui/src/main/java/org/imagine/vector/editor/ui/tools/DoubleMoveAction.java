/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class DoubleMoveAction extends WidgetAction.LockedAdapter {

    private final DoubleMoveStrategy strategy;
    private final DoubleMoveProvider provider;

    private Widget movingWidget = null;
    private Point2D dragSceneLocation = null;
    private Point2D originalSceneLocation = null;
    private Point2D initialMouseLocation = null;

    private static final Point zeros = new Point();

    public DoubleMoveAction(DoubleMoveStrategy strategy, DoubleMoveProvider provider) {
        this.strategy = strategy;
        this.provider = provider;
    }

    @Override
    protected boolean isLocked() {
        return movingWidget != null;
    }

    public final Point2D convertSceneToView (Scene scene, double zoom, Point2D sceneLocation) {
        Point location = scene.getLocation ();
        double zoomFactor = scene.getZoomFactor();
        return new Point2D.Double ((zoomFactor * (location.getX() + sceneLocation.getX())), (zoomFactor * (location.getY() + sceneLocation.getY())));
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

    static Point2D.Double floatingPoint(Widget widget, WidgetMouseEvent wme) {
        Point2D.Double result = ViewL.lastPoint(widget);
        AffineTransform sceneToLocal = sceneToLocal(widget);
        sceneToLocal.transform(result, result);
        return result;
    }

    static Point2D.Double toPoint2D(Point point) {
        return new Point2D.Double(point.x, point.y);
    }

    static Point2D.Double convertLocalToScene(Widget widget, Point2D pt) {
        Point2D.Double result = new Point2D.Double(0, 0);
        localToScene(widget).transform(pt, result);
        return result;
    }

    static Point2D.Double sceneToLocal(Widget widget, Point2D pt) {
        Point2D.Double result = new Point2D.Double(0, 0);
        sceneToLocal(widget).transform(pt, result);
        return result;
    }

    public WidgetAction.State mousePressed(Widget widget, WidgetAction.WidgetMouseEvent event) {
        if (isLocked()) {
            return WidgetAction.State.createLocked(widget, this);
        }
        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1) {
            movingWidget = widget;
//            initialMouseLocation = event.getPoint ();
            initialMouseLocation = floatingPoint(widget, event);
            originalSceneLocation = provider.getOriginalLocation(widget);
            if (originalSceneLocation == null) {
                originalSceneLocation = new Point2D.Double();
            }
            Point2D.Double dsl = new Point2D.Double(initialMouseLocation.getX(), initialMouseLocation.getY());
            localToScene(widget).transform(dsl, dsl);
            dragSceneLocation = dsl;
            provider.movementStarted(widget);
            return WidgetAction.State.createLocked(widget, this);
        }
        return WidgetAction.State.REJECTED;
    }

    public WidgetAction.State mouseReleased(Widget widget, WidgetAction.WidgetMouseEvent event) {
        boolean state;
        if (initialMouseLocation != null && initialMouseLocation.equals(event.getPoint())) {
            state = true;
        } else {
            Point2D p2d = floatingPoint(widget, event);
            state = move(widget, p2d);
        }
        if (state) {
            movingWidget = null;
            dragSceneLocation = null;
            originalSceneLocation = null;
            initialMouseLocation = null;
            provider.movementFinished(widget);
        }
        return state ? WidgetAction.State.CONSUMED : WidgetAction.State.REJECTED;
    }

    public WidgetAction.State mouseDragged(Widget widget, WidgetAction.WidgetMouseEvent event) {
        return move(widget, floatingPoint(widget, event)) ? WidgetAction.State.createLocked(widget, this) : WidgetAction.State.REJECTED;
    }

    private boolean move(Widget widget, Point2D newLocation) {
        if (movingWidget != widget) {
            return false;
        }
        initialMouseLocation = null;
        newLocation = convertLocalToScene(widget, newLocation);
        Point2D.Double location = new Point2D.Double(
                originalSceneLocation.getX() + newLocation.getX() - dragSceneLocation.getX(), originalSceneLocation.getY() + newLocation.getY() - dragSceneLocation.getY());
        provider.setNewLocation(widget, strategy.locationSuggested(widget, originalSceneLocation, location));
        return true;
    }
}
