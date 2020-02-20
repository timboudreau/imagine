/*
 * GradientFill.java
 *
 * Created on October 15, 2005, 9:23 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.fills;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.java.dev.colorchooser.ColorChooser;
import org.imagine.editor.api.AspectRatio;
import org.netbeans.paint.api.components.points.PointSelector;
import org.netbeans.paint.api.components.points.PointSelectorMode;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Timothy Boudreau
 */
public class GradientFill extends BaseFill {

    protected ColorChooser ch = null;
    private final Point2D.Float first = new Point2D.Float(0, 0);
    private final Point2D.Float second = new Point2D.Float(200, 200);

    @Override
    public JComponent getCustomizer() {
        JPanel result = (JPanel) super.getCustomizer();
        if (ch == null) {
            ch = new ColorChooser();
            //so it's not the same color as the other one
            ch.setColor(Color.ORANGE);
            JLabel lbl = new JLabel(secondChooserCaption());
            ch.addActionListener(this);
            result.add(lbl);
            result.add(ch);
            ch.setPreferredSize(new Dimension(16, 16));
            AspectRatio ratio = Utilities.actionsGlobalContext().lookup(AspectRatio.class);
            if (ratio == null) {
                ratio = AspectRatio.create(() -> new Dimension(640, 480));
            }

            PointSelector sel = new PointSelector(ratio.rectangle());
            sel.setMode(PointSelectorMode.POINT_AND_LINE);
            sel.setTargetPoint(new Point2D.Double(0, 0));
            sel.setFocusPoint(new Point2D.Double(ratio.width(), ratio.height()));
            sel.setBackgroundPainter((Graphics2D g, Point2D target, Rectangle2D frame, double angle, PointSelectorMode mode, PointSelector sel1) -> {
                g.setPaint(getPaint());
                g.fill(frame);
            });
            sel.addPropertyChangeListener("targetPoint", evt -> {
                first.setLocation((Point2D) evt.getNewValue());
            });
            sel.addPropertyChangeListener("focusPoint", evt -> {
                second.setLocation((Point2D) evt.getNewValue());
            });
            result.add(sel);
        }
        return result;
    }

    protected String secondChooserCaption() {
        return NbBundle.getMessage(GradientFill.class,
                "LBL_GradientSecond");
    }

    @Override
    protected String getChooserCaption() {
        return NbBundle.getMessage(GradientFill.class, "LBL_GradientFirst");
    }

    @Override
    public java.awt.Paint getPaint() {
        Color a = (Color) super.getPaint();
        Color b = ch == null ? Color.BLACK : ch.getColor();
        return new GradientPaint(first.x, first.y, a,
                second.x, second.y, b, true);
    }
}
