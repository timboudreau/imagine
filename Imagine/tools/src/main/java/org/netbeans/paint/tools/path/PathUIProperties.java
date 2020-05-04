package org.netbeans.paint.tools.path;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.function.Supplier;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import org.imagine.editor.api.CheckerboardBackground;
import org.imagine.editor.api.Zoom;
import org.netbeans.paint.tools.colors.Hues;

/**
 *
 * @author Tim Boudreau
 */
final class PathUIProperties {

    private final Supplier<ToolUIContext> ctx;
    private static final int BASE_POINT_RADIUS = 7;

    PathUIProperties(Supplier<ToolUIContext> ctx) {
        this.ctx = ctx;
    }

    Supplier<ToolUIContext> contextSupplier() {
        return ctx;
    }

    public double pointRadius() {
        return ctx.get().zoom().inverseScale(BASE_POINT_RADIUS);
    }

    public ToolUIContext ctx() {
        ToolUIContext result = ctx.get();
        if (result == null) {
            result = ToolUIContext.DUMMY_INSTANCE;
        }
        return result;
    }

    public Color lineDraw() {
        CheckerboardBackground bg = ctx().background();
        switch (bg) {
            case MEDIUM:
                return Color.BLACK;
            default:
                return ctx().background().contrasting();
        }
    }

    public Color destinationPointFill() {
        return ctx().background().nonContrasting();
    }

    public Color initialPointFill() {
        return Hues.Yellow.fromTemplateColor(drawBase());
    }

    public Color destinationPointDraw() {
        return ctx().background() == CheckerboardBackground.LIGHT
                ? lineDraw().brighter() : lineDraw().darker();
    }

    public Color hoveredDotDraw() {
        return invert(hoveredDotFill());
    }

    public Color hoveredDotFill() {
        return Color.YELLOW;
    }

    private Color drawBase() {
        Color templ;
        float sat;
        switch (ctx().background()) {
            case DARK:
                templ = lineDraw();
                sat = 0.7F;
                break;
            case MEDIUM:
                templ = new Color(90, 90, 180, 230);
                sat = 0.875F;
                break;
            case LIGHT:
            default:
                sat = 0.925F;
                templ = new Color(128, 128, 128, 200);
        }
        return Hues.Green.withBrightnessFrom(templ, sat);
    }

    private Color fillBase() {
        Color templ;
        float sat;
        switch (ctx().background()) {
            case DARK:
                templ = new Color(180, 180, 180, 210);
                sat = 0.7F;
                break;
            case MEDIUM:
                templ = new Color(200, 200, 250, 230);
                sat = 0.875F;
                break;
            case LIGHT:
            default:
                sat = 0.925F;
                templ = new Color(180, 180, 200, 200);
        }
        return Hues.Green.withBrightnessFrom(templ, sat);
    }

    public Color controlPointFill() {
        return Hues.Blue.fromTemplateColor(fillBase());
    }

    public Color controlPointDraw() {
        return Hues.Orange.fromTemplateColor(drawBase());
    }

    public Color connectorLineDraw() {
        return Hues.Green.midPoint(Hues.Orange)
                .withBrightnessFrom(ctx().background()
                        .nonContrasting(), 0.25F);
    }

    public Color proposedLineDraw() {
        return Hues.Orange.fromTemplateColor(drawBase());
    }

    public boolean hasLineShadows() {
//        return ctx().background() == CheckerboardBackground.MEDIUM;
        return true;
    }

    private Color alphaReduced(Color c) {
        int a = c.getAlpha();
        int alpha = a - (a / 4);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public Color lineShadow() {
        return alphaReduced(lineDraw());
    }

    public Color connectorLineShadow() {
        return alphaReduced(connectorLineDraw());
    }
    static float[] SCRATCH = new float[3];

    private Color invert(Color c) {
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), SCRATCH);
        SCRATCH[1] = 1F - SCRATCH[1];
        SCRATCH[2] = 1F - SCRATCH[2];
        Color result = new Color(Color.HSBtoRGB(SCRATCH[0], SCRATCH[1], SCRATCH[2]));
        if (c.getAlpha() != 255) {
            result = new Color(result.getRed(), result.getGreen(), result.getBlue(), c.getAlpha());
        }
        return result;
    }
    private BasicStroke lineStroke;
    private float lineStrokeZoom;

    public BasicStroke lineStroke() {
        Zoom zoom = ctx().zoom();
        float z = zoom.getZoom();
        if (lineStroke != null && z == lineStrokeZoom) {
            return lineStroke;
        }
        lineStrokeZoom = z;
        return lineStroke = zoom.getStroke(1.125);
    }

    private BasicStroke proposedLineStroke;
    private float proposedLineStrokeZoom;

    public BasicStroke proposedLineStroke() {
        Zoom zoom = ctx().zoom();
        float z = zoom.getZoom();
        if (proposedLineStroke != null && z == proposedLineStrokeZoom) {
            return proposedLineStroke;
        }
        proposedLineStrokeZoom = z;
        return proposedLineStroke = zoom.getStroke(0.75);
    }

    public Color proposedLineShadow() {
        return alphaReduced(proposedLineDraw());
    }

    private BasicStroke connectorStroke;
    private float connectorStrokeZoom = -1;

    public BasicStroke connectorStroke() {
        Zoom zoom = ctx().zoom();
        float z = zoom.getZoom();
        if (connectorStroke != null && z == connectorStrokeZoom) {
            return connectorStroke;
        }
        connectorStrokeZoom = z;
        float zoomedStrokeWidth = zoom.inverseScale(1);
        float[] dashes = new float[]{zoom.inverseScale(1.5F), zoom.inverseScale(2.5F)};
        return connectorStroke = new BasicStroke(zoomedStrokeWidth, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 1F, dashes, 0F);
    }
}
