package org.imagine.vector.editor.ui.tools.widget.painting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.Rhombus;
import com.mastfrog.geometry.Triangle2D;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.netbeans.api.visual.model.ObjectState;

/**
 *
 * @author Tim Boudreau
 */
public class DesignerProperties {

    private static final DesignerProperties INSTANCE = new DesignerProperties();
    private double controlPointSize = 7;
    private double snapRadius = 5;
    private Color focusStrokeXOR = Color.GRAY;
    private Color selectionStrokeXOR = Color.LIGHT_GRAY;
    private float focusStrokeSize = 2;
    private float selectionStrokeSize = 1;
    private float[] selectionStrokeSections
            = new float[]{3, 5, 3};
    private float[] focusStrokeSections
            = new float[]{7, 2, 5, 2};
    private Color focusedControlPointBackground
            = new Color(240, 240, 30);
    private Color focusedControlPointForeground
            = new Color(212, 40, 40);

    private int selectionStrokeCount;
    private int focusStrokeCount;
    private DecorationController dec;
    private final Map<ControlPointKind, Color> controlPointFill
            = new EnumMap<>(ControlPointKind.class);
    private final Map<ControlPointKind, Color> controlPointDraw
            = new EnumMap<>(ControlPointKind.class);
    private Rectangle2D.Double rect = new Rectangle2D.Double();
    private Circle circle = new Circle(0, 0, 1);
    private final float[] controlPointConnectorStrokePattern
            = new float[]{2, 3, 2};
    private Color connectorLineColor = Color.LIGHT_GRAY;

    DesignerProperties() {
        for (ControlPointKind k : ControlPointKind.values()) {
            switch (k) {
                case TEXT_BASELINE:
                    controlPointDraw.put(k, new Color(80, 80, 122));
                    controlPointFill.put(k, new Color(120, 120, 140, 100));
                    break;
                case START_POINT:
                    controlPointDraw.put(k, new Color(80, 128, 80));
                    controlPointFill.put(k, new Color(128, 255, 128, 100));
                    break;
                case CUBIC_CONTROL_POINT:
                case QUADRATIC_CONTROL_POINT:
                    controlPointDraw.put(k, Color.ORANGE);
                    controlPointFill.put(k, new Color(128, 128, 220));
                    break;
                case EDGE_HANDLE:
                case RADIUS:
                    controlPointDraw.put(k, new Color(80, 80, 180));
                    controlPointFill.put(k, new Color(128, 220, 128));
                    break;
                default:
                    controlPointDraw.put(k, Color.BLUE);
                    controlPointFill.put(k, Color.GRAY);
            }
        }
    }

    public double snapRadius(double zoom) {
        return snapRadius * (1D / zoom);
    }

    public static DesignerProperties get() {
        return INSTANCE;
    }

    public float[] selectionStrokeSections() {
        return selectionStrokeSections;
    }

    public float[] focusStrokeSections() {
        return focusStrokeSections;
    }

    public int selectionStrokeCount() {
        if (selectionStrokeCount == 0) {
            float sum = 0;
            for (int i = 0; i < selectionStrokeSections.length; i++) {
                sum += selectionStrokeSections[i];
            }
            selectionStrokeCount = (int) Math.round(sum);
        }
        return selectionStrokeCount;
    }

    public int focusStrokeCount() {
        if (focusStrokeCount == 0) {
            float sum = 0;
            for (int i = 0; i < focusStrokeSections.length; i++) {
                sum += focusStrokeSections[i];
            }
            focusStrokeCount = (int) Math.round(sum);
        }
        return focusStrokeCount;
    }

    public float focusStrokeSize() {
        return focusStrokeSize;
    }

    public float selectionStrokeSize() {
        return selectionStrokeSize;
    }

    public double controlPointSize() {
        return controlPointSize;
    }

    public double focusedControlPointSize() {
        return controlPointSize + 3;
    }

    public Color focusXORColor() {
        return focusStrokeXOR;
    }

    public Color selectionXORColor() {
        return selectionStrokeXOR;
    }

    public DecorationController decorationController() {
        if (dec == null) {
            dec = new DecorationController(this);
        }
        return dec;
    }

    public Paint fillForControlPoint(ControlPointKind kind, ObjectState state) {
        if (state.isFocused()) {
            return focusedControlPointBackground;
        }
        return controlPointFill.getOrDefault(kind, Color.WHITE);
    }

    public Paint drawForControlPoint(ControlPointKind kind, ObjectState state) {
        if (state.isFocused()) {
            return focusedControlPointForeground;
        }
        return controlPointFill.getOrDefault(kind, Color.GRAY);
    }

    public Shape shapeForControlPoint(ShapeControlPoint point, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        if (!point.isValid()) {
            return invalid();
        }
        switch (point.kind()) {
            case CHARACTER_POSITION:
                return rhom(point, localCoords, zoom, forHitTest, state);
            case TEXT_BASELINE:
            case EDGE_HANDLE:
                return rect(point, localCoords, zoom, forHitTest, state);
            case RADIUS:
                return tri(point, localCoords, zoom, forHitTest, state);
            default:
                return circle(point, localCoords, zoom, forHitTest, state);
        }
    }

    public Shape shapeForControlPoint(double x, double y, ControlPointKind kind, boolean valid, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        if (!valid) {
            return invalid();
        }
        switch (kind) {
            case CHARACTER_POSITION:
                return rhom(x, y, localCoords, zoom, forHitTest, state);
            case TEXT_BASELINE:
            case EDGE_HANDLE:
                return rect(x, y, localCoords, zoom, forHitTest, state);
            case RADIUS:
                return tri(x, y, localCoords, zoom, forHitTest, state);
            default:
                return circle(x, y, localCoords, zoom, forHitTest, state);
        }
    }

    private Shape invalid() {
        rect.width = 0;
        rect.height = 0;
        rect.x = 0;
        rect.y = 0;
        return rect;
    }

    private static final Triangle2D tri = new Triangle2D(0, 0, 1, 1, 0, 2);

    private static final Rhombus rhom = new Rhombus(new Rectangle(), 0);

    private Shape rhom(ShapeControlPoint point, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        return rhom(point.getX(), point.getY(), localCoords, zoom, forHitTest, state);
    }

    private Shape rhom(double px, double py, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        double invZoom = 1D / zoom;
        double base = state.isFocused() ? focusedControlPointSize() : controlPointSize();
        double sz = base * invZoom;
        if (forHitTest) {
            sz += 1D * invZoom;
        }
        double half = sz / 2D;
        if (localCoords) {
            rhom.setCenter(0, 0);
            rhom.setXRadius(half);
            rhom.setYRadius(half);
//            tri.setPoints(0, half,
//                    -half, half,
//                    half, half
//            );
        } else {
            rhom.setCenter(px, py);
            rhom.setXRadius(half);
            rhom.setYRadius(half);
//            tri.setPoints(
//                    px, py - half,
//                    px - half, py + half,
//                    px + half, py + half
//            );
        }
        return rhom;

    }

    private Shape tri(ShapeControlPoint point, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        return tri(point.getX(), point.getY(), localCoords, zoom, forHitTest, state);
    }

    private Shape tri(double px, double py, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        double invZoom = 1D / zoom;
        double base = state.isFocused() ? focusedControlPointSize() : controlPointSize();
        double sz = base * invZoom;
        if (forHitTest) {
            sz += 1D * invZoom;
        }
        double half = sz / 2D;
        if (localCoords) {
            tri.setPoints(0, half,
                    -half, half,
                    half, half
            );
        } else {
            tri.setPoints(
                    px, py - half,
                    px - half, py + half,
                    px + half, py + half
            );
        }
        return tri;
    }

    private Shape rect(ShapeControlPoint point, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        return rect(point.getX(), point.getY(), localCoords, zoom, forHitTest, state);
    }

    private Shape rect(double px, double py, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        double invZoom = 1D / zoom;
        double base = state.isFocused() ? focusedControlPointSize() : controlPointSize();
        double sz = base * invZoom;
        if (forHitTest) {
            sz += 1 * invZoom;
        }
        double half = sz / 2D;
        rect.width = sz;
        rect.height = sz;
        if (localCoords) {
            rect.x = -half;
            rect.y = rect.x;
        } else {
            rect.x = px - half;
            rect.y = px - half;
        }
        return rect;
    }

    private Circle circle(ShapeControlPoint point, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        return circle(point.getX(), point.getY(), localCoords, zoom, forHitTest, state);
    }

    private Circle circle(double px, double py, boolean localCoords, double zoom, boolean forHitTest, ObjectState state) {
        double invZoom = 1D / zoom;
        double base = state.isFocused() ? focusedControlPointSize() : controlPointSize();
        double sz = (base / 2D) * invZoom;
        if (forHitTest) {
            sz += 1 * invZoom;
        }
        // Could do this only in one branch of the if clause below,
        // but we need to reforce the point to refresh its contents
        // or the widget will render a little bit offset
        if (localCoords) {
            circle.setCenterAndRadius(0, 0, sz);
        } else {
            circle.setCenterAndRadius(px, py, sz);
        }
        return circle;
    }

    private double lastStrokeZoom = -1;
    private BasicStroke lastStroke;

    public BasicStroke strokeForControlPoint(double zoom) {
        if (lastStrokeZoom != zoom) {
            float inv = 1F / (float) zoom;
//            inv = Math.max(inv, 0.125F); // Java2D will not draw narrower strokes
            lastStrokeZoom = zoom;
            return lastStroke = new BasicStroke(inv);
        }
        return lastStroke;
    }

    public BasicStroke strokeForControlPointConnectorLine(double zoom) {
        double sz = 0.75 * (1D / zoom);

        float[] pattern = Arrays.copyOf(controlPointConnectorStrokePattern,
                controlPointConnectorStrokePattern.length);
        float iz = 1F / (float) zoom;
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] *= iz;
        }

        return new BasicStroke((float) sz,
                BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_BEVEL,
                1F, pattern, 0);
    }

    public Color colorForControlPointConnectorLine() {
        return connectorLineColor;
    }
}
