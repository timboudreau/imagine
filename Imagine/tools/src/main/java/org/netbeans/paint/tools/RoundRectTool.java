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
import java.awt.Rectangle;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Constants;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.openide.util.NbBundle;
/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name="Rounded_Rectangle", iconPath="org/netbeans/paint/tools/resources/roundrect.png")
@Tool(Surface.class)
public class RoundRectTool extends RectangleTool {
    public RoundRectTool (Surface surface) {
        super(surface);
    }

    @Override
    protected void draw (Rectangle toPaint, Graphics2D g2d, boolean fill) {
        int arcWidth = arcWc.get();
        int arcHeight = arcHc.get();
        if (fill) {
            g2d.fillRoundRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height, arcWidth, arcHeight);
            g2d.setPaint(paintC.get().getPaint());
            g2d.drawRoundRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height, arcWidth, arcHeight);
        } else {
            g2d.drawRoundRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height, arcWidth, arcHeight);
        }
    }

    @Override
    public String toString() {
        return NbBundle.getMessage (getClass(), "Rounded_Rectangle");
    }

    private Customizer <Integer> arcHeightCustomizer;
    private Customizer <Integer> arcWidthCustomizer;
    private Customizer customizer;

    @Override
    public Customizer getCustomizer() {
        Customizer c = super.getCustomizer();
        AggregateCustomizer nue = new AggregateCustomizer ("foo", c, arcWc, arcHc);
        return nue;
    }
//    @Override
//    public Customizer<PaintAttributes> getCustomizer() {
//        if (customizer == null) {
//            customizer = superGetCustomizer();
//        }
//        return customizer;
//    }
//
//    private Customizer <PaintAttributes> superGetCustomizer() {
//        Customizer base = super.getCustomizer();
//
//        arcWidthCustomizer = customizers.getIntegerCustomizer(NbBundle.getMessage (getClass(), "Arc_Width"));
//        arcHeightCustomizer = customizers.getIntegerCustomizer (NbBundle.getMessage (getClass(), "Arc_Height"));
//
//        Customizer <PaintAttributes> result = new AggregateCustomizer <PaintAttributes> (NbBundle.getMessage(getClass(), "Round_Rect_Properties"),
//                base, arcWidthCustomizer, arcHeightCustomizer);
//        result.addChangeListener(WeakListeners.change(this, result));
//
//        return result;
//    }
    
    private final Customizer<Integer> arcWc = Customizers.getCustomizer(Integer.class, Constants.ARC_WIDTH, 0, 50);
    private final Customizer<Integer> arcHc = Customizers.getCustomizer(Integer.class, Constants.ARC_HEIGHT, 0, 50);

}
