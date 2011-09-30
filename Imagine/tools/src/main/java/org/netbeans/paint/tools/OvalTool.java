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
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
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
        return NbBundle.getMessage (RectangleTool.class, "Oval");
    }

    @Override
    protected void draw (Rectangle toPaint, Graphics2D g2d, boolean fill) {
        if (fill) {
            g2d.setPaint (paintC.get().getPaint());
            g2d.fillOval(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
            g2d.setColor (outlineC.get());
            g2d.drawOval(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
        } else {
            g2d.drawOval(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
        }
    }
}
