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
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.geometry.Circle;
import static org.netbeans.paint.tools.RectangleTool.strokeC;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.EnhRectangle2D;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Circle", iconPath = "org/netbeans/paint/tools/resources/circle.svg")
@Tool(value=Surface.class, toolbarPosition=200)
public class CircleTool extends RectangleTool {

    public CircleTool(Surface surf) {
        super(surf);
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(CircleTool.class, "Circle");
    }

    @Override
    protected void draw(EnhRectangle2D toPaint, Graphics2D g2d, PaintingStyle style) {
        Circle circ = new Circle(toPaint.getCenterX(), toPaint.getCenterY(),
                Math.min(toPaint.getWidth(), toPaint.getHeight()));
        if (style.isFill()) {
            g2d.setPaint(paintC.get().getPaint());
            g2d.fill(circ);
        }
        if (style.isOutline()) {
            if (toPaint == TEMPLATE_RECTANGLE) {
                g2d.setStroke(scaledStroke());
            } else {
                g2d.setStroke(strokeC.get());
            }
            g2d.setPaint(outlineC.get());
            g2d.draw(circ);
        }
    }
}
