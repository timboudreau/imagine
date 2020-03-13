package org.imagine.vector.editor.ui.tools.widget.painting;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
public final class DecorationController {

    private static final float SEL_HIGHLIGHT_STROKE_WIDTH = 1.5F;
    private BasicStroke[] selected;
    private BasicStroke[] focused;
    private double lastFocusZoom = -1;
    private double lastSelZoom = -1;
    private static int strokeIter;

    private final DesignerProperties props;

    DecorationController() {
        this(DesignerProperties.get());
    }

    DecorationController(DesignerProperties props) {
        this.props = props;
    }

    public DesignerProperties properties() {
        return props;
    }

    public BasicStroke[] selectedStrokeForZoom(double zoom) {
        if (zoom == lastSelZoom) {
            return selected;
        }
        lastSelZoom = zoom;
        float factor = (float) (props.selectionStrokeSize() / zoom);
        float sz = SEL_HIGHLIGHT_STROKE_WIDTH * factor;
        selected = new BasicStroke[props.selectionStrokeCount()];
        float[] fl = props.selectionStrokeSections();
        fl = Arrays.copyOf(fl, fl.length);
        float invZoom = 1F / (float) zoom;
        for (int i = 0; i < fl.length; i++) {
            fl[i] *= invZoom;
        }

        for (int i = 0; i < selected.length; i++) {
            selected[i] = new BasicStroke(sz,
                    BasicStroke.CAP_SQUARE,
                    BasicStroke.JOIN_BEVEL,
                    1F, fl, i + 1);
        }
        return selected;
    }

    public BasicStroke[] focusedStrokeForZoom(double zoom) {
        if (zoom == lastFocusZoom) {
            return focused;
        }
        lastFocusZoom = zoom;
        float factor = (float) (props.focusStrokeSize() / zoom);
        float sz = props.focusStrokeSize() * factor;
        focused = new BasicStroke[props.focusStrokeCount()];

        float[] strokePattern = props.selectionStrokeSections();
        strokePattern = Arrays.copyOf(strokePattern, strokePattern.length);
        float invZoom = 1F / (float) zoom;
        for (int i = 0; i < strokePattern.length; i++) {
            strokePattern[i] *= invZoom;
        }

        for (int i = 0; i < focused.length; i++) {
            focused[i] = new BasicStroke(sz, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL,
                    1F, strokePattern, i + 1);
        }
        return focused;
    }

    public Stroke selectedStroke(double zoom) {
        return selectedStrokeForZoom(zoom)[strokeIter++ % selected.length];
    }

    public Stroke focusedStroke(double zoom) {
        return focusedStrokeForZoom(zoom)[strokeIter++ % focused.length];
    }

    public void setupSelectedPainting(double zoom, Graphics2D g, Consumer<Graphics2D> c) {
//        g.setXORMode(props.selectionXORColor());
        g.setColor(props.selectionXORColor());
        Stroke old = g.getStroke();
        g.setStroke(selectedStroke(zoom));
        try {
            c.accept(g);
        } finally {
            g.setStroke(old);
//            g.setPaintMode();
        }
    }

    public void setupFocusedPainting(double zoom, Graphics2D g, Consumer<Graphics2D> c) {
//        g.setXORMode(props.focusXORColor());
        g.setColor(props.focusXORColor());
        Stroke old = g.getStroke();
        g.setStroke(selectedStroke(zoom));
        try {
            c.accept(g);
        } finally {
            g.setStroke(old);
//            g.setPaintMode();
        }
    }
}
