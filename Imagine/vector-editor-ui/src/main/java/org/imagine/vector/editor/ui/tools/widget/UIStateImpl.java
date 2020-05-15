/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget;

import org.imagine.vector.editor.ui.tools.widget.util.UIState;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
class UIStateImpl implements UIState {

    private List<UIStateListener> listeners;
    private final Set<Props> props;

    public UIStateImpl() {
        props = EnumSet.allOf(Props.class);
        props.remove(Props.USE_PRODUCTION_PAINTS);
    }

    public UIStateImpl(boolean controlPointsDraggable, boolean connectorLinesVisible,
            boolean controlPointsVisible, boolean shapesDraggable,
            boolean focusDecorationsPainted, boolean selectedShapeControlPointsVisible,
            boolean productionPainting) {

        props = EnumSet.noneOf(Props.class);
        change(Props.CONTROL_POINTS_VISIBLE, controlPointsVisible);
        change(Props.CONTROL_POINTS_DRAGGABLE, controlPointsDraggable);
        change(Props.CONNECTOR_LINES_VISIBLE, connectorLinesVisible);
        change(Props.SHAPES_DRAGGABLE, shapesDraggable);
        change(Props.FOCUS_DECORATIONS_PAINTED, focusDecorationsPainted);
        change(Props.SELECTED_SHAPE_CONTROL_POINTS_VISIBLE, selectedShapeControlPointsVisible);
        change(Props.USE_PRODUCTION_PAINTS, productionPainting);
    }

    private UIStateImpl(Set<Props> props) {
        this.props = EnumSet.copyOf(props);
    }

    @Override
    public UIStateImpl copy() {
        return new UIStateImpl(props);
    }

    @Override
    public UIStateImpl restore(UIState other) {
        setControlPointsDraggable(other.controlPointsDraggable())
                .setConnectorLinesVisible(other.connectorLinesVisible())
                .setControlPointsVisible(other.controlPointsVisible())
                .setShapesDraggable(other.shapesDraggable())
                .setFocusDecorationsPainted(other.focusDecorationsPainted())
                .setSelectedControlPointsVisible(other.selectedShapeControlPointsVisible())
                .setUseProductionPaints(other.useProductionPaints());
        return this;
    }

    private boolean is(Props prop) {
        return props.contains(prop);
    }

    private UIStateImpl doChange(Props prop, boolean val) {
        boolean curr = props.contains(prop);
        if (curr != val) {
            if (val) {
                props.add(prop);
                change(prop, val);
            } else {
                props.remove(prop);
                change(prop, val);
            }
        }
        return this;
    }

    @Override
    public UIStateImpl listen(UIStateListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>(3);
        }
        listeners.add(listener);
        return this;
    }

    @Override
    public UIStateImpl unlisten(UIStateListener listener) {
        if (listeners == null) {
            return this;
        }
        listeners.remove(listener);
        return this;
    }

    @Override
    public UIStateImpl setConnectorLinesVisible(boolean vis) {
        doChange(Props.CONNECTOR_LINES_VISIBLE, vis);
        return this;
    }

    @Override
    public UIStateImpl setControlPointsVisible(boolean vis) {
        doChange(Props.CONTROL_POINTS_VISIBLE, vis);
        return this;
    }

    @Override
    public UIStateImpl setControlPointsDraggable(boolean vis) {
        doChange(Props.CONTROL_POINTS_DRAGGABLE, vis);
        return this;
    }

    @Override
    public UIStateImpl setShapesDraggable(boolean vis) {
        doChange(Props.SHAPES_DRAGGABLE, vis);
        return this;
    }

    @Override
    public boolean useProductionPaints() {
        return is(Props.USE_PRODUCTION_PAINTS);
    }

    @Override
    public UIState setUseProductionPaints(boolean productionPainting) {
        doChange(Props.USE_PRODUCTION_PAINTS, productionPainting);
        return this;
    }

    @Override
    public UIState setSelectedControlPointsVisible(boolean vis) {
        doChange(Props.SELECTED_SHAPE_CONTROL_POINTS_VISIBLE, vis);
        return this;
    }

    @Override
    public boolean selectedShapeControlPointsVisible() {
        return is(Props.SELECTED_SHAPE_CONTROL_POINTS_VISIBLE);
    }

    @Override
    public boolean connectorLinesVisible() {
        return is(Props.CONNECTOR_LINES_VISIBLE);
    }

    @Override
    public boolean controlPointsVisible() {
        return is(Props.CONTROL_POINTS_VISIBLE);
    }

    @Override
    public boolean controlPointsDraggable() {
        return is(Props.CONTROL_POINTS_DRAGGABLE);
    }

    @Override
    public boolean shapesDraggable() {
        return is(Props.SHAPES_DRAGGABLE);
    }

    @Override
    public boolean focusDecorationsPainted() {
        return is(Props.FOCUS_DECORATIONS_PAINTED);
    }

    @Override
    public UIStateImpl setFocusDecorationsPainted(boolean val) {
        doChange(Props.FOCUS_DECORATIONS_PAINTED, val);
        return this;
    }

    private void change(Props prop, boolean val) {
        if (listeners == null) {
            return;
        }
        UIStateListener[] ll = listeners.toArray(
                new UIStateListener[listeners.size()]);
        for (UIStateListener l : ll) {
            l.onChange(prop, val);
        }
    }
}
