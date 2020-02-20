/*
 * OvalTool.java
 *
 * Created on September 29, 2006, 4:05 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.netbeans.paint.tools;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import static org.netbeans.paint.tools.RectangleTool.strokeC;
import org.imagine.editor.api.PaintingStyle;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name="Oval", iconPath="org/netbeans/paint/tools/resources/oval.png")
@Tool(Surface.class)
public class OvalTool extends RectangleTool {

    /** Creates a new instance of OvalTool */
    public OvalTool(Surface surf) {
        super(surf);
    }

    @Override
    public String toString() {
        return NbBundle.getMessage (OvalTool.class, "Oval");
    }

    @Override
    protected void draw (Rectangle toPaint, Graphics2D g2d, PaintingStyle style) {
        if (style.isFill()) {
            g2d.setPaint (paintC.get().getPaint());
            g2d.fillOval(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
        }
        if (style.isOutline()) {
            g2d.setStroke(new BasicStroke(strokeC.get()));
            g2d.setColor (outlineC.get());
            g2d.drawOval(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
        }
    }
}
