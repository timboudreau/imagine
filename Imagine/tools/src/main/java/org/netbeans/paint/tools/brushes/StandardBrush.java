/*
 * RoundBrush.java
 *
 * Created on October 15, 2005, 7:10 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.netbeans.paint.tools.brushes;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.toolcustomizers.Constants;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.netbeans.paint.api.components.explorer.Customizable;
import org.netbeans.paint.api.components.explorer.FolderPanel;
import org.netbeans.paint.tools.fills.FillCustomizer;
import org.netbeans.paint.tools.spi.Brush;
import org.netbeans.paint.tools.spi.BrushTip;

/**
 *
 * @author Timothy Boudreau
 */
public class StandardBrush implements Customizable, Brush {
    private final Customizer<Integer> sizeC = Customizers.getCustomizer(
            Integer.class, Constants.SIZE, 1, 200);
    private final Customizer<Boolean> aaC = Customizers.getCustomizer(
            Boolean.class, Constants.ANTIALIAS);
    
    public JComponent getCustomizer() {
        return createCustomizer();
    }

    private Paint getPaint() {
        return FillCustomizer.getDefault().get().getPaint();
    }
    
    public boolean isAntialiased() {
        try {
            return aaC.get();
        } catch (NullPointerException npe) {
            return true;
        }
    }
    
    public Rectangle paint(Graphics2D g, Point p, int modifiers) {
        Paint paint = getPaint();
        Paint old = g.getPaint();
        g.setPaint (paint);
        Object o = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (isAntialiased()) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_OFF);
        }        
        Rectangle result = draw (g, p, sizeC.get());
        g.setPaint (old);
        if (o != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, o);
        }
        return result;
    }
    
    private FolderPanel<BrushTip> tipSelector = null;
    private JPanel createCustomizer() {
	JPanel result = new JPanel(new BorderLayout());
        JPanel jp = new JPanel(new BorderLayout());
        Component js = sizeC.getComponent();
        jp.add (js, BorderLayout.NORTH);
        jp.add (aaC.getComponent(), BorderLayout.CENTER);
	result.add (jp, BorderLayout.NORTH);
        tipSelector = new FolderPanel ("brushtips", BrushTip.class); //NOI18N
	result.add (tipSelector, BorderLayout.CENTER);
        result.add(FillCustomizer.getDefault().getComponent(), 
                BorderLayout.SOUTH);
	return result;
    }
    
    private BrushTip getTip() {
	if (tipSelector != null) {
	    BrushTip c = tipSelector.getSelection();
            return (BrushTip) c;
	}
	return null;
    }

    public Rectangle draw(Graphics2D g, Point p, int size) {
	BrushTip tip = getTip();
	if (tip != null) {
	    return tip.draw (g, p, size);
	} else {
	    //If no tips installed, just use basic painting
	    int half = size / 2;
	    Rectangle result = new Rectangle (p.x - half, p.y - half, size, size);
	    g.fillOval (result.x, result.y, result.width, result.height);
	    return result;
	}
    }
}
