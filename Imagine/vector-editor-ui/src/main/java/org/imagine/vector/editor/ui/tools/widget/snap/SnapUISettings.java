package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import org.imagine.editor.api.ImageEditorBackground;
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
    private float wedgeStrokeSize = 2;
    private double wedgeSize = 37;
    private double captionLineOffset = 7;
    private double lineOffset = 9;
    private double sizingHeadLength = 6.5;
    private double arrowHeadLength = 5;
    private double captionMargin = 3;

    private SnapUISettings() {
        lastIndicatorZoom = 1;
    }

    public static SnapUISettings getInstance() {
        return INSTANCE;
    }

    public double captionLineOffset(Zoom zoom) {
        return zoom.inverseScale(captionLineOffset);
    }

    public double wedgeSize(SnapKind kind, Zoom zoom) {
        return zoom.inverseScale(wedgeSize);
    }

    public double lineOffset(Zoom zoom) {
        return zoom.inverseScale(lineOffset);
    }

    public double sizingHeadLength(Zoom zoom) {
        return zoom.inverseScale(sizingHeadLength);
    }

    public double arrowHeadLength(Zoom zoom) {
        return zoom.inverseScale(arrowHeadLength);
    }

    public double captionMargin(Zoom zoom) {
        return zoom.inverseScale(captionMargin);
    }

    public BasicStroke decorationStroke(SnapKind kind, ShapeElement selection, Zoom zoom) {
        float z = zoom.getZoomAsFloat();
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
        float z = zoom.getZoomAsFloat();
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
        switch (ImageEditorBackground.getDefault().style()) {
            case LIGHT:
                return new Color(60, 60, 80, 182);
            default:
                return new Color(180, 180, 255, 182);
        }
    }

    public Paint drawShadowColor(SnapKind kind, ShapeElement selection) {
        switch (ImageEditorBackground.getDefault().style()) {
            case LIGHT:
                return new Color(255, 255, 255, 182);
            default:
                return new Color(100, 100, 100, 182);
        }
    }

    public Paint captionColor(SnapKind kind) {
        switch (ImageEditorBackground.getDefault().style()) {
            case LIGHT:
                return new Color(80, 80, 80, 140);
            default:
                return new Color(255, 255, 255, 220);
        }

    }

    public Paint captionFillColor(SnapKind kind) {
        switch (ImageEditorBackground.getDefault().style()) {
            case LIGHT:
                return new Color(255, 255, 255, 180);
            default:
                return new Color(10, 10, 10, 200);
        }
    }

    public Paint originFillColor(SnapKind kind, ShapeElement selection) {
        return new Color(110, 255, 110, 210);
    }

    public Paint originDrawColor(SnapKind kind, ShapeElement selection) {
        return new Color(20, 110, 20, 210);
    }

    public Paint indicatorLineColor(SnapKind kind, ShapeElement selection) {
        return new Color(100, 100, 130, 140);
    }

    public double drawShadowOffset(SnapKind kind, Zoom zoom) {
        return zoom.inverseScale(1.5);
    }

    public double targetPointRadius(SnapKind kind, ShapeElement selection, Zoom zoom) {
        double result = targetPointRadius(kind, selection);
        return zoom.inverseScale(result);
    }

    public double originPointRadius(SnapKind kind, ShapeElement selection, Zoom zoom) {
        double result = originPointRadius(kind, selection);
        return zoom.inverseScale(result);
    }

    public double targetPointRadius(SnapKind kind, ShapeElement selection) {
        return pointRadius;
    }

    public double originPointRadius(SnapKind kind, ShapeElement selection) {
        return originPointRadius;
    }

    public float pointStrokeSize(SnapKind kind) {
        switch (kind) {
            case CORNER:
            case EXTENT:
                return 2;
            default:
                return 1.125F;
        }
    }
}
