/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 *
 * @author Tim Boudreau
 */
class SharedLayoutIndentBorder implements Border {

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        // do nothing
        if (TitledPanel2.isDebugLayout()) {
            JComponent jc = (JComponent) c;
            Color color = (Color) jc.getClientProperty("randomBorderColor2");
            if (color == null) {
                color = new Color(Color.HSBtoRGB(ThreadLocalRandom.current().nextFloat(), 0.9F, 0.625F));
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 150);
                jc.putClientProperty("randomBorderColor2", color);
            }
            Insets ins = getBorderInsets(c);
            g.setColor(color);
            g.fillRect(x, y, ins.left, y + height - ins.bottom);
            g.fillRect(x, y + height - ins.bottom, width, ins.bottom);
            g.fillRect(x + ins.left, y, x + width, ins.top);
            g.fillRect(x + width - ins.right, y, x + width, height - ins.bottom);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(getClass().getSimpleName());
            int h = fm.getMaxAscent() + fm.getMaxDescent();
            if (ins.top > 0) {
                g.drawString(getClass().getSimpleName(), x + (width / 2) - (w / 2), y + height - h);
            } else {
                if (ins.bottom == 0) {
                    g.setColor(Color.BLACK);
                }
                g.drawString(getClass().getSimpleName(), x + (width / 2) - (w / 2), y + fm.getMaxAscent() + 1);
            }
        }

    }

    private static final Insets EMPTY = new Insets(0, 0, 0, 0);

    private Insets empty() {
        return (Insets) EMPTY.clone();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        // Handle the special case of a parent and child which *both* have
        // borders that want to do the indenting
        if (c.getParent() instanceof TitledPanel2) {
            // XXX this logic could go in ChildPanel.getInsets()
            TitledPanel2 tp2 = (TitledPanel2) c.getParent();
            if (tp2.isExpanded() && tp2.center() instanceof SharedLayoutPanel) {
                return empty();
            }
        }
        SharedLayoutData data = SharedLayoutData.find(c);
        if (data == null) {
            return empty();
        }
        int ind = data.indentFor((Container) c);
        return new Insets(0, ind, 0, 0);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

}
