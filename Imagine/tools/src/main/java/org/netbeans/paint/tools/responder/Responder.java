/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.responder;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Same idea as Apple's Cocoa responder tool - a responder which handles input
 * events, and the call to handle an input event can return a different
 * responder that handles the next such input event - this allows us to have
 * different editors for sequences of start point, control point, end point,
 * etc.
 */
public abstract class Responder {

    public static final Responder NO_OP = new Responder() {
    };
    private boolean active;

    protected final boolean isActive() {
        return active;
    }

    void _activate(Rectangle addTo) {
        active = true;
        activate(addTo);
    }

    void _resign(Rectangle addTo) {
        active = false;
        resign(addTo);
    }

    /**
     * Called when this Responder has become the one responsible for handling
     * input events.
     *
     * @param addTo A rectangle to add to if a repaint should be performed as a
     * consequence of activation
     */
    protected void activate(Rectangle addTo) {
    }

    /**
     * Called when another Responder has taken over handling input events.
     *
     * @param addTo A rectangle to alter if a repaint should be performed as a
     * consequence of resignation
     */
    protected void resign(Rectangle addTo) {
        // do nothing
    }

    protected void onAnyMouseEvent(double x, double y, MouseEvent e) {
        // do nothing
    }

    protected void onBeforeHandleInputEvent(InputEvent evt) {
        
    }

    /**
     * Override to handle mouse clicks
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onClick(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse presses
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onPress(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse releases.
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onRelease(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse drags.
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onDrag(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse motion events.
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onMove(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse enter events.
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onEnter(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse exit events
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onExit(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle key-typed events.
     *
     * @param e The event
     * @return The next responder or this
     */
    protected Responder onTyped(KeyEvent e) {
        return this;
    }

    /**
     * Override to handle key-pressed events.
     *
     * @param e The event
     * @return The next responder or this
     */
    protected Responder onKeyPress(KeyEvent e) {
        return this;
    }

    /**
     * Override to handle key-released events.
     *
     * @param e The event
     * @return The next responder or this
     */
    protected Responder onKeyRelease(KeyEvent e) {
        return this;
    }

    /**
     * Override to handle mouse-wheel events.
     *
     * @param e The event
     * @return The next responder or this
     */
    protected Responder onWheel(MouseWheelEvent e) {
        return this;
    }
}
