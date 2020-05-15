/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import org.imagine.geometry.Triangle2D;

/**
 *
 * @author Tim Boudreau
 */
final class ExpIcon implements Icon {

    private final Triangle2D tri = new Triangle2D();
    private boolean expanded = false;
    private boolean hovered = false;
    private final JComponent owner;

    ExpIcon(JComponent owner) {
        this.owner = owner;
    }

    public void setHovered(boolean val) {
        hovered = val;
    }

    public void setExpanded(boolean val) {
        expanded = val;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D gg = (Graphics2D) g;
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = c.getWidth();
        int h = c.getHeight();
//        int workingHeight = h - (((JComponent) c).getInsets().bottom + y);
//        int workingWidth = w - (((JComponent) c).getInsets().right + x);
        int workingHeight = w;
        int workingWidth = h;
        if (workingHeight % 2 != 0) {
            workingHeight--;
        }
        x = 0;
        y = 0;
//        x+=3;
//        y += 1;
//        workingHeight -= 1;
//        workingWidth -= 1;
        if (!expanded) {
            tri.setPoints(x, y, x, y + workingHeight, x + workingWidth, y + (workingHeight / 2));
        } else {
            tri.setPoints(x, y, x + workingWidth, y, x + (workingWidth / 2), y + workingHeight);
        }
        gg.setStroke(new BasicStroke(1.5F));
        g.setColor(hovered ? Color.ORANGE : UIManager.getColor("controlDkShadow"));
        gg.fill(tri);
        g.setColor(UIManager.getColor("textText"));
        gg.draw(tri);
    }

    @Override
    public int getIconWidth() {
        FontMetrics fm = owner.getFontMetrics(owner.getFont());
        return fm.getHeight();
    }

    @Override
    public int getIconHeight() {
        return getIconWidth();
    }

}
