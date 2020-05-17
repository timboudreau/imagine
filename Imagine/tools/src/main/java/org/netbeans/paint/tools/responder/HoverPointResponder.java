/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.responder;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import org.imagine.geometry.EqPointDouble;

/**
 * Many responder implementations need to keep track of the point the last mouse
 * event occurred on.
 *
 * @author Tim Boudreau
 */
public abstract class HoverPointResponder extends Responder {

    private final EqPointDouble hoverPoint = new EqPointDouble();
    private boolean hasHoverPoint;

    public final HoverPointResponder setHoverPoint(Point2D pt) {
        setHoverPoint(pt.getX(), pt.getY());
        return this;
    }

    public final HoverPointResponder clearHoverPoint() {
        setHasHoverPoint(false);
        return this;
    }

    public final HoverPointResponder setHoverPoint(double x, double y) {
        hoverPoint.setLocation(x, y);
        setHasHoverPoint(true);
        return this;
    }

    protected final boolean hasHoverPoint() {
        return hasHoverPoint;
    }

    @Override
    void onTakeoverFrom(Responder prev) {
        if (prev instanceof HoverPointResponder) {
            ((HoverPointResponder) prev).copyHoverPointTo(this);
        }
    }

    protected EqPointDouble moveHoverPoint(double dx, double dy) {
        hoverPoint.x += dx;
        hoverPoint.y += dy;
        setHasHoverPoint(true);
        return hoverPoint;
    }

    private void setHasHoverPoint(boolean hasHoverPoint) {
        if (hasHoverPoint != this.hasHoverPoint) {
            this.hasHoverPoint = hasHoverPoint;
            onHasHoverPointChanged(hoverPoint.x, hoverPoint.y, hasHoverPoint);
        }
    }

    protected void onHasHoverPointChanged(double x, double y, boolean hasHoverPoint) {

    }

    protected final Responder copyHoverPointTo(Responder other) {
        if (hasHoverPoint() && other instanceof HoverPointResponder) {
            ((HoverPointResponder) other).setHoverPoint(hoverPoint);
        }
        return other;
    }

    @Override
    protected void onAnyMouseEvent(double x, double y, MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            setHasHoverPoint(false);
        } else {
            setHasHoverPoint(true);
        }
        hoverPoint.setLocation(x, y);
    }

    protected final EqPointDouble hoverPoint() {
        return hasHoverPoint ? hoverPoint : null;
    }

    protected final boolean withHoverPoint(Consumer<EqPointDouble> pt) {
        if (hasHoverPoint) {
            pt.accept(hoverPoint);
        }
        return hasHoverPoint;
    }

}
