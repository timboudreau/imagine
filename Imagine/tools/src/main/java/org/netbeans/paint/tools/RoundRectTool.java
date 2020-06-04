/*
 * RoundRectTool.java
 *
 * Created on September 29, 2006, 4:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Constants;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.help.api.HelpItem;
import org.imagine.help.api.annotations.Help;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Rounded_Rectangle", iconPath = "org/netbeans/paint/tools/resources/roundrect.svg")
@Tool(value = Surface.class, toolbarPosition = 400)
@Help(id = "RoundRectangleTool", related = {"RectangleTool", "OvalTool", "CircleTool", "TriangleTool", "RhombusTool"}, content = {
    @Help.HelpText(language = "en", country = "US",
            value = "# Rounded RectangleTool\n\n"
            + "The Rounded Rectangle Tool allows you to draw rectangles with rounded corners, consisting"
            + "of an upper-left-corner position, a width and height, and two arc values which determine"
            + "the degree of rounding.  Its customizer lets you "
            + "configure the fill and outline painting style, and the line stroke the outline"
            + "is drawn with (if any).\n\n"
            + "Holding down Shift results in a shape which is a square.")})
public class RoundRectTool extends RectangleTool {

    public RoundRectTool(Surface surface) {
        super(surface);
    }


    @Override
    public HelpItem getHelpItem() {
        return HelpItems.RoundRectangleTool;
    }

    static final RoundRectangle2D.Double scratchRR = new RoundRectangle2D.Double();

    @Override
    protected void draw(EnhRectangle2D toPaint, Graphics2D g2d, PaintingStyle style) {
        scratchRR.setFrame(toPaint);
        double arcWidthPercentage = arcWc.get();
        double arcHeightPercentage = arcHc.get();
        scratchRR.arcwidth = arcWidthPercentage * toPaint.getWidth();
        scratchRR.archeight = arcHeightPercentage * toPaint.getHeight();

        if (style.isFill()) {
            g2d.setPaint(paintC.get().getPaint());
            g2d.fill(scratchRR);
//            g2d.fillRoundRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height, arcWidth, arcHeight);
        }
        if (style.isOutline()) {
            if (toPaint == TEMPLATE_RECTANGLE) {
                g2d.setStroke(scaledStroke());
            } else {
                g2d.setStroke(strokeC.get());
            }
            g2d.setColor(outlineC.get());
//            g2d.drawRoundRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height, arcWidth, arcHeight);
            g2d.draw(scratchRR);
        }
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(getClass(), "Rounded_Rectangle");
    }

    @Override
    public Customizer getCustomizer() {
        Customizer c = super.getCustomizer();
        AggregateCustomizer nue = new AggregateCustomizer("foo", arcWc, arcHc, c);
        return nue;
    }

    private final Customizer<Double> arcWc = Customizers.getCustomizer(
            Double.class, Constants.ARC_WIDTH_PERCENTAGE, 0D, 1D, 0.25D, RoundRectTool::toPercentage);
    private final Customizer<Double> arcHc = Customizers.getCustomizer(
            Double.class, Constants.ARC_HEIGHT_PERCENTAGE, 0D, 1D, 0.25D, RoundRectTool::toPercentage);

    private static final DecimalFormat FMT = new DecimalFormat("##0.#%");

    static String toPercentage(double val) {
        return FMT.format(val);
    }
}
