/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import javax.swing.Icon;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.Surface;
import org.netbeans.paint.api.util.RasterConverter;
import org.netbeans.paint.tools.spi.MouseDrivenTool;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name="TOOL_SMUDGE", iconPath="org/netbeans/paint/tools/resources/smudge.png")
@Tool(Surface.class)
public class SmudgeTool extends MouseDrivenTool {
    private int radius = 20;
    public SmudgeTool(Surface surface) {
        super(surface);
    }

    @Override
    protected void dragged(Point p, int modifiers) {
        assert layer != null;
        BufferedImage img = surface.getImage();
        /*
        if (img == null) {
            RasterConverter conv = RasterConverter.getDefault();
            if (conv != null) {
                Layer newLayer = RasterConverter.askUserToConvert(layer, null, this);
                if (newLayer != null) {
//                    super.deactivate();
                    super.attach(newLayer);
                    surface = newLayer.getSurface();
                    img = surface.getImage();
                    if (img == null) {
                        return;
                    }
                    surface.setTool(this);
                }
            }
        }
        */
        if (img == null) {
            return;
        }
        if (p.x < 0 || p.y < 0 || p.x >= img.getWidth() || p.y >= img.getHeight()) {
            return;
        }
        if (img.getType() == 0) { //Our NIO raster images
            Hibernator hib = layer.getLookup().lookup(Hibernator.class);
            hib.wakeup(true, null);
            img = surface.getImage();
        }
        float[] matrix = {
                0.111f, 0.111f, 0.111f, 
                0.111f, 0.111f, 0.111f, 
                0.111f, 0.111f, 0.111f, 
            };
        BufferedImageOp op = new ConvolveOp( new Kernel(3, 3, matrix) );
        BufferedImage sub = img.getSubimage(p.x, p.y, radius, radius);
        surface.getGraphics().drawImage(sub, op, p.x, p.y);
    }
    
    private static final class I implements Icon {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor (Color.ORANGE);
            g.fillRect (x, y, 10, 10);
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 16;
        }
        
    }

}
