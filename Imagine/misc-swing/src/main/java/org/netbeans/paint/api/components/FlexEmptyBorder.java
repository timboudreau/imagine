/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.api.components;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.Border;

/**
 *
 * @author Tim Boudreau
 */
public class FlexEmptyBorder implements Border {

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        // do nothing
    }

    @Override
    public Insets getBorderInsets(Component c) {
        FontMetrics fm = c.getFontMetrics(c.getFont());
        int lineHeight = fm.getHeight();
        int horizInsets = (int) Math.ceil(lineHeight * 1.5);
        int vertInsets = lineHeight * 2;
        return new Insets(vertInsets, horizInsets, vertInsets, horizInsets);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

}
