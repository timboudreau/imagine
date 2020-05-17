package org.netbeans.paint.tools.responder;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.function.Supplier;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import org.imagine.editor.api.EditorBackground;
import org.imagine.editor.api.Zoom;
import org.netbeans.paint.tools.colors.Hues;

/**
 *
 * @author Tim Boudreau
 */
public final class PathUIProperties {

    private final Supplier<ToolUIContext> ctx;
    private static final int BASE_POINT_RADIUS = 7;
    private static final int BASE_HIT_RADIUS = 9;

    public PathUIProperties(Supplier<ToolUIContext> ctx) {
        this.ctx = ctx;
    }

    public Supplier<ToolUIContext> contextSupplier() {
        return ctx;
    }

    public double hitRadius() {
        ToolUIContext ctx = this.ctx.get();
        if (ctx == null) {
            ctx = ToolUIContext.DUMMY_INSTANCE;
        }
        return ctx.zoom().inverseScale(BASE_HIT_RADIUS);
    }

    public double pointRadius() {
        ToolUIContext ctx = this.ctx.get();
        if (ctx == null) {
            ctx = ToolUIContext.DUMMY_INSTANCE;
        }
        return ctx.zoom().inverseScale(BASE_POINT_RADIUS);
    }

    public ToolUIContext ctx() {
        ToolUIContext result = ctx.get();
        if (result == null) {
            result = ToolUIContext.DUMMY_INSTANCE;
        }
        return result;
    }

    public boolean isBrightBackground() {
        return ctx().background().isBright();
    }

    public Color lineDraw() {
        return ctx().background().isMedium() ? Color.BLACK : ctx().background().contrasting();
    }

    public Color selectedPointFill() {
        if (isBrightBackground()) {
            return Hues.Blue.toColor(0.75F, 0.875F);
        }
        return Hues.Blue.withBrightnessFrom(ctx().background().midContrasting(), 0.75F);
    }

    public Color selectedPointDraw() {
        if (isBrightBackground()) {
            return Hues.Blue.toColor(0.75F, 0.75F);
        }
        return Hues.Blue.withBrightnessFrom(ctx().background().lowContrasting(), 0.625F);
    }

    public Color selectionShapeDraw() {
        Color base = ctx().background().contrasting();
        return withAlpha(base, 170);
    }

    public Color selectionShapeFill() {
        Color base = ctx().background().contrasting();
        return withAlpha(base, 90);
    }

    public Color proposedPointFill() {
        return Hues.Green.withBrightnessFrom(ctx().background().midContrasting(), 0.875F);
    }

    public Color proposedPointDraw() {
        return Hues.Green.withBrightnessFrom(ctx().background().midContrasting(), 0.625F);
    }

    public Color destinationPointFill() {
        return ctx().background().nonContrasting();
    }

    public Color initialPointFill() {
        return Hues.Magenta.fromTemplateColor(ctx().background().contrasting());
    }

    public Color destinationPointDraw() {
        return ctx().background().isBright()
                ? lineDraw().brighter() : lineDraw().darker();
    }

    public Color hoveredDotDraw() {
        if (isBrightBackground()) {
            return Color.BLUE;
        }
        return alphaReduced(hoveredDotFill());
    }

    public Color hoveredDotFill() {
        if (isBrightBackground()) {
            return Color.ORANGE;
        }
        return withAlpha(Hues.Orange.fromTemplateColor(ctx().background().midContrasting()), 255);
    }

    private Color drawBase() {
        Color templ;
        float sat;
        EditorBackground bg = ctx().background();
        if (bg.isDark()) {
            templ = lineDraw();
            sat = 0.7F;
        } else if (bg.isMedium()) {
            templ = new Color(90, 90, 180, 230);
            sat = 0.875F;
        } else {
            sat = 0.925F;
            templ = new Color(128, 128, 128, 200);
        }
        return Hues.Green.withBrightnessFrom(templ, sat);
    }

    private Color fillBase() {
        Color templ;
        float sat;
        EditorBackground bg = ctx().background();
        if (bg.isDark()) {
            templ = new Color(180, 180, 180, 210);
            sat = 0.7F;
        } else if (bg.isMedium()) {
            templ = new Color(200, 200, 250, 230);
            sat = 0.875F;
        } else {
            sat = 0.925F;
            templ = new Color(180, 180, 200, 200);
        }
        return Hues.Green.withBrightnessFrom(templ, sat);
    }

    public Color controlPointFill() {
        if (isBrightBackground()) {
            return new Color(128, 128, 255);
        }
        return Hues.Blue.fromTemplateColor(fillBase());
    }

    public Color controlPointDraw() {
        if (isBrightBackground()) {
            return new Color(90, 90, 255, 200);
        }
        return Hues.Orange.fromTemplateColor(drawBase());
    }

    public Color connectorLineDraw() {
        if (isMediumBackground()) {
            return Color.BLACK;
        }
        return Hues.Green.midPoint(Hues.Orange)
                .withBrightnessFrom(ctx().background()
                        .nonContrasting(), 0.25F);
    }

    public Color proposedLineDraw() {
        if (hasLineShadows()) {
            return Hues.Blue.fromTemplateColor(drawBase());
        }
        return Hues.Orange.fromTemplateColor(drawBase());
    }

    public boolean hasLineShadows() {
        return ctx().background().isMedium();
    }

    private Color alphaReduced(Color c) {
        int a = c.getAlpha();
        int alpha = a - (a / 4);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public Color lineShadow() {
//        return alphaReduced(ctx().background().nonContrasting());
        return alphaReduced(ctx().background().contrasting());
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
    private double lineStrokeZoom;

    private boolean isMediumBackground() {
        return ctx().background().isMedium();
    }

    public BasicStroke lineStroke() {
        Zoom zoom = ctx().zoom();
        double z = zoom.getZoom();
        if (lineStroke != null && z == lineStrokeZoom) {
            return lineStroke;
        }
        lineStrokeZoom = z;
        return lineStroke = zoom.getStroke(isMediumBackground() ? 2.5 : 1.125);
    }

    private BasicStroke proposedLineStroke;
    private double proposedLineStrokeZoom;

    public BasicStroke proposedLineStroke() {
        Zoom zoom = ctx().zoom();
        double z = zoom.getZoom();
        if (proposedLineStroke != null && z == proposedLineStrokeZoom) {
            return proposedLineStroke;
        }
        proposedLineStrokeZoom = z;
        return proposedLineStroke = zoom.getStroke(isMediumBackground() ? 1.25 : 0.75);
    }

    public Color proposedLineShadow() {
        return alphaReduced(proposedLineDraw());
    }

    private BasicStroke connectorStroke;
    private double connectorStrokeZoom = -1;

    public BasicStroke connectorStroke() {
        Zoom zoom = ctx().zoom();
        double z = zoom.getZoom();
        if (connectorStroke != null && z == connectorStrokeZoom) {
            return connectorStroke;
        }
        connectorStrokeZoom = z;
        float zoomedStrokeWidth = zoom.inverseScale(isMediumBackground() ? 2F : 1.25F);
        float[] dashes = new float[]{zoom.inverseScale(1.5F), zoom.inverseScale(2.5F)};
        return connectorStroke = new BasicStroke(zoomedStrokeWidth, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 1F, dashes, 0F);
    }
}
