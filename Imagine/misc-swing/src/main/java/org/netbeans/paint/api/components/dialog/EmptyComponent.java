/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.api.components.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import javax.swing.JComponent;
import javax.swing.event.AncestorListener;

/**
 *
 * @author Tim Boudreau
 */
final class EmptyComponent extends JComponent {

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // do nothing
    }

    @Override
    public void addAncestorListener(AncestorListener listener) {
        // do nothing
    }

    @Override
    public void addFocusListener(FocusListener l) {
        // do nothing
    }

    @Override
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        // do nothing
    }

    @Override
    protected void fireVetoableChange(String propertyName, Object oldValue, Object newValue) throws PropertyVetoException {
        // do nothing
    }

    @Override
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
        // do nothing
    }

    @Override
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        // do nothing
    }

    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        // do nothing
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // do nothing
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        // do nothing
    }

    @Override
    public synchronized void addInputMethodListener(InputMethodListener l) {
        // do nothing
    }

    @Override
    public synchronized void addMouseWheelListener(MouseWheelListener l) {
        // do nothing
    }

    @Override
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        // do nothing
    }

    @Override
    public synchronized void addMouseListener(MouseListener l) {
        // do nothing
    }

    @Override
    public synchronized void addKeyListener(KeyListener l) {
        // do nothing
    }

    @Override
    public void addHierarchyBoundsListener(HierarchyBoundsListener l) {
        // do nothing
    }

    @Override
    public void addHierarchyListener(HierarchyListener l) {
        // do nothing
    }

    @Override
    public synchronized void addComponentListener(ComponentListener l) {
        // do nothing
    }

    @Override
    public void revalidate() {
        // do nothing
    }

    @Override
    public void repaint(Rectangle r) {
        // do nothing
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
        // do nothing
    }

    @Override
    public void setMinimumSize(Dimension minimumSize) {
        // do nothing
    }

    @Override
    public void setMaximumSize(Dimension maximumSize) {
        // do nothing
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        // do nothing
    }

    @Override
    protected boolean requestFocusInWindow(boolean temporary) {
        return false;
    }

    @Override
    public boolean requestFocusInWindow() {
        return false;
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        return false;
    }

    @Override
    public void requestFocus() {
        // do nothing
    }

    @Override
    public void setFocusable(boolean focusable) {
        // do nothing
    }

    @Override
    public void repaint(int x, int y, int width, int height) {
        // do nothing
    }

    @Override
    public void repaint(long tm) {
        // do nothing
    }

    @Override
    public void repaint() {
        // do nothing
    }

    @Override
    public void setDropTarget(DropTarget dt) {
        // do nothing
    }

}
