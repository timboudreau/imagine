package org.imagine.vector.editor.ui.tools.widget.util;

/**
 *
 * @author Tim Boudreau
 */
public interface UIState {

    boolean connectorLinesVisible();

    boolean selectedShapeControlPointsVisible();

    boolean controlPointsVisible();

    boolean controlPointsDraggable();

    boolean shapesDraggable();

    boolean focusDecorationsPainted();

    boolean useProductionPaints();

    @FunctionalInterface
    interface UIStateListener {

        void onChange(Props prop, boolean val);
    }

    enum Props {
        CONNECTOR_LINES_VISIBLE,
        CONTROL_POINTS_VISIBLE,
        SHAPES_DRAGGABLE,
        CONTROL_POINTS_DRAGGABLE,
        FOCUS_DECORATIONS_PAINTED,
        SELECTED_SHAPE_CONTROL_POINTS_VISIBLE,
        USE_PRODUCTION_PAINTS
    }

    UIState listen(UIStateListener listener);

    UIState unlisten(UIStateListener listener);

    UIState setFocusDecorationsPainted(boolean painted);

    UIState setConnectorLinesVisible(boolean vis);

    UIState setControlPointsVisible(boolean vis);

    UIState setSelectedControlPointsVisible(boolean vis);

    UIState setControlPointsDraggable(boolean vis);

    UIState setShapesDraggable(boolean vis);

    UIState setUseProductionPaints(boolean productionPainting);

    UIState copy();

    UIState restore(UIState from);
}
