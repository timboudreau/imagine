/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.cursor;

import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import com.mastfrog.geometry.Quadrant;

/**
 *
 * @author Tim Boudreau
 */
final class CursorsProxy implements Cursors, PropertyChangeListener {

    private final JComponent component;

    CursorsProxy(JComponent comp) {
        this.component = comp;
        comp.addPropertyChangeListener("graphicsConfiguration", this);
    }

    private CursorsImpl cached;

    private Cursors get() {
        if (cached != null) {
            return cached;
        }
        cached = CursorsImpl.rawCursorsForComponent(component);
        return cached;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        cached = null;
    }

    @Override
    public Cursor star() {
        return get().star();
    }

    @Override
    public Cursor barbell() {
        return get().barbell();
    }

    @Override
    public Cursor x() {
        return get().x();
    }

    @Override
    public Cursor hin() {
        return get().hin();
    }

    @Override
    public Cursor no() {
        return get().no();
    }

    @Override
    public Cursor horizontal() {
        return get().horizontal();
    }

    @Override
    public Cursor vertical() {
        return get().vertical();
    }

    @Override
    public Cursor southWestNorthEast() {
        return get().southWestNorthEast();
    }

    @Override
    public Cursor southEastNorthWest() {
        return get().southEastNorthWest();
    }

    @Override
    public Cursor rhombus() {
        return get().rhombus();
    }

    @Override
    public Cursor rhombusFilled() {
        return get().rhombusFilled();
    }

    @Override
    public Cursor triangleDown() {
        return get().triangleDown();
    }

    @Override
    public Cursor triangleDownFilled() {
        return get().triangleDownFilled();
    }

    @Override
    public Cursor triangleRight() {
        return get().triangleRight();
    }

    @Override
    public Cursor triangleRightFilled() {
        return get().triangleRightFilled();
    }

    @Override
    public Cursor triangleLeft() {
        return get().triangleLeft();
    }

    @Override
    public Cursor triangleLeftFilled() {
        return get().triangleLeftFilled();
    }

    @Override
    public Cursor arrowsCrossed() {
        return get().arrowsCrossed();
    }

    @Override
    public Cursor multiMove() {
        return get().multiMove();
    }

    @Override
    public Cursor rotate() {
        return get().rotate();
    }

    @Override
    public Cursor rotateMany() {
        return get().rotateMany();
    }

    @Override
    public Cursor dottedRect() {
        return get().dottedRect();
    }

    @Override
    public Cursor arrowPlus() {
        return get().arrowPlus();
    }

    @Override
    public Cursor shortArrow() {
        return get().shortArrow();
    }

    @Override
    public Cursor closeShape() {
        return get().closeShape();
    }

    @Override
    public Cursor arrowTilde() {
        return get().arrowTilde();
    }

    @Override
    public Cursor cursorPerpendicularTo(double angle) {
        return get().cursorPerpendicularTo(angle);
    }

    @Override
    public Cursor cursorPerpendicularToQuadrant(Quadrant quad) {
        return get().cursorPerpendicularToQuadrant(quad);
    }
}
