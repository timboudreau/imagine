package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
public class SnapUISettings {

    private static SnapUISettings INSTANCE = new SnapUISettings();
    private final float[] indicatorStrokeSections
            = new float[]{2, 7, 2, 7};
    private double lastIndicatorZoom = 1;
    private BasicStroke indicatorStroke;
    private float indicatorStrokeSize = 0.75F;
    private double lastStrokeZoom;
    private BasicStroke decorationStroke;
    private double pointRadius = 11;
    private double originPointRadius = 7;

    private SnapUISettings() {
        lastIndicatorZoom = 1;
    }

    public static SnapUISettings getInstance() {
        return INSTANCE;
    }

    public BasicStroke decorationStroke(SnapKind kind, ShapeElement selection, Zoom zoom) {
        float z = zoom.getZoom();
        if (z != lastStrokeZoom || decorationStroke == null) {
            float factor = 1F / z;
            float sz = pointStrokeSize(kind) * factor;
            decorationStroke = new BasicStroke(sz);
        }
        return decorationStroke;
    }

    public float indicatorStrokeSize(SnapKind kind) {
        return indicatorStrokeSize;
    }

    public BasicStroke indicatorStroke(SnapKind kind, ShapeElement selection, Zoom zoom) {
        float z = zoom.getZoom();
        if (z != lastIndicatorZoom || indicatorStroke == null) {
            lastIndicatorZoom = z;
            float factor = 1F / z;
            float[] indicatorStrokeSectionsForLastZoom = new float[indicatorStrokeSections.length];
            for (int i = 0; i < indicatorStrokeSectionsForLastZoom.length; i++) {
                indicatorStrokeSectionsForLastZoom[i]
                        = indicatorStrokeSections[i] * factor;
            }
            float sz = indicatorStrokeSize(kind) * factor;
            indicatorStroke = new BasicStroke((float) sz,
                    BasicStroke.CAP_SQUARE,
                    BasicStroke.JOIN_BEVEL,
                    1F, indicatorStrokeSectionsForLastZoom, 0);
        }
        return indicatorStroke;
    }

    public Paint fillColor(SnapKind kind, ShapeElement selection) {
        return new Color(255, 255, 255, 180);
    }

    public Paint drawColor(SnapKind kind, ShapeElement selection) {
        return new Color(80, 80, 80, 140);
    }

    public Paint captionColor(SnapKind kind) {
        return new Color(80, 80, 80, 140);
    }

    public Paint captionFillColor(SnapKind kind) {
        return new Color(255, 255, 255, 180);
    }

    public Paint originFillColor(SnapKind kind, ShapeElement selection) {
        return new Color(110, 255, 110, 210);
    }

    public Paint originDrawColor(SnapKind kind, ShapeElement selection) {
        return new Color(20, 110, 20, 210);
    }

    public Paint indicatorLineColor(SnapKind kind, ShapeElement selection) {
        return new Color(100, 100, 130, 80);
    }

    public double targetPointRadius(SnapKind kind, ShapeElement selection, Zoom zoom) {
        double result = targetPointRadius(kind, selection);
        double factor = 1D / zoom.getZoom();
        return result * factor;
    }

    public double originPointRadius(SnapKind kind, ShapeElement selection, Zoom zoom) {
        double result = originPointRadius(kind, selection);
        double factor = 1D / zoom.getZoom();
        return result * factor;
    }

    public double targetPointRadius(SnapKind kind, ShapeElement selection) {
        return pointRadius;
    }

    public double originPointRadius(SnapKind kind, ShapeElement selection) {
        return originPointRadius;
    }

    public float pointStrokeSize(SnapKind kind) {
        return 0.75F;
    }
}
