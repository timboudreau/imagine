/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.tools.brushes.tips;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Constants;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.netbeans.paint.api.components.explorer.Customizable;
import org.netbeans.paint.tools.spi.BrushTip;
import org.openide.util.NbBundle;

/**
 * A tip that draws a grid.
 * 
 * @author Tim Boudreau
 */
public class HashTip implements BrushTip, Customizable {
    Customizer<Integer> vertDist = Customizers.getCustomizer(Integer.class,
            NbBundle.getMessage(HashTip.class,"hashVertical"), 0, 100); //NOI18N
    
    Customizer<Integer> horizDist = Customizers.getCustomizer(Integer.class, NbBundle.getMessage(HashTip.class, "hashHorizontal"),
            0, 100); //NOI18N
    Customizer<Float> stroke = Customizers.getCustomizer(Float.class, Constants.STROKE, 0.05F, 20F); //NOI18N
//    Customizer<Integer> rot = Customizers2.getCustomizer(Integer.class, NbBundle.getMessage(HashTip.class, "rotation"), 0, 359); //NOI18N
    AggregateCustomizer c = new AggregateCustomizer("foo", vertDist, horizDist, stroke);

    public Rectangle draw(Graphics2D g, Point p, int size) {
        int x = p.x;
        int y = p.y;
        int hDist = horizDist.get();
        int vDist = vertDist.get();
        int xOff = hDist == 0 ? 0 : p.x % hDist;
        int yOff = vDist == 0 ? 0 : p.y % vDist;
        x -= xOff;
        y -= yOff;
//        AffineTransform t = g.getTransform();
//        AffineTransform xform = null;
//        if (rot.get() != 0) {
//            g.setTransform(xform = AffineTransform.getRotateInstance(Math.toRadians(rot.get())));
//        }
        g.setStroke(new BasicStroke(stroke.get()));
        if (vDist > 0) {
            int ct = size / vDist;
            for (int i=0; i < ct; i++) {
                g.drawLine(x, y + (i * vDist), x + size - xOff, y + (i * vDist));
            }
        }
        if (hDist > 0) {
            int ct = size / hDist;
            for (int i=0; i < ct; i++) {
                g.drawLine(x + (i * hDist), y, x + (i * hDist), y + size - yOff);
            }
        }
//        g.setTransform(t);
        Rectangle result = new Rectangle (x, y, x + size, y + size);
//        if (xform != null) {
//            result = xform.createTransformedShape(result).getBounds();
//        }
        return result;
    }

    public String getName() {
        return NbBundle.getMessage(HashTip.class, "hash"); //XXX
    }

    public Object get() {
        return null;
    }

    public JComponent getCustomizer() {
        return c.getComponent();
    }
}
