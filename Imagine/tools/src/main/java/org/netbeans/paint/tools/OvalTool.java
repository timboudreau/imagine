/*
 * OvalTool.java
 *
 * Created on September 29, 2006, 4:05 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import static org.netbeans.paint.tools.RectangleTool.strokeC;
import org.imagine.editor.api.PaintingStyle;
import com.mastfrog.geometry.EnhRectangle2D;
import org.imagine.help.api.HelpItem;
import org.imagine.help.api.annotations.Help;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Oval", iconPath = "org/netbeans/paint/tools/resources/oval.svg")
@Tool(value = Surface.class, toolbarPosition = 500)
@Help(id = "OvalTool", related = {"RectangleTool", "CircleTool", "RhombusTool", "TriangleTool"}, content = {
    @Help.HelpText(language = "en", country = "US",
            value = "# Oval Tool\n\n"
            + "The Oval Tool allows you to draw ovals;  its customizer lets you "
            + "configure the fill and outline painting style, and the line stroke the outline"
            + "is drawn with (if any).\n\n"
            + "Ovals created by this tool are defined by their four corners;  use the"
            + "Circle Tool to create perfectly circular shapes which are defined by their "
            + "center and radius, which is a more natural way to manage and position circles"
            + "in some cases..\n\n"
            + "Holding down Shift results in a shape which is effectivley circular (but may be"
            + "edited not to be later).")})
public class OvalTool extends RectangleTool {

    /**
     * Creates a new instance of OvalTool
     */
    public OvalTool(Surface surf) {
        super(surf);
    }

    @Override
    public HelpItem getHelpItem() {
        return HelpItems.OvalTool;
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(OvalTool.class, "Oval");
    }

    private final Ellipse2D.Double ell = new Ellipse2D.Double();

    @Override
    protected void draw(EnhRectangle2D toPaint, Graphics2D g2d, PaintingStyle style) {
        ell.setFrame(toPaint);
        if (style.isFill()) {
            g2d.setPaint(paintC.get().getPaint());
            g2d.fill(ell);
        }
        if (style.isOutline()) {
            g2d.setStroke(strokeC.get());
            g2d.setColor(outlineC.get());
            g2d.draw(ell);
        }
    }
}
