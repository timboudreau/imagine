/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author eppleton
 */
@ServiceProvider(service = StatusLineElementProvider.class, position = 10)
public class PositionStatusLineElementProvider implements StatusLineElementProvider {

    private JLabel statusLineLabel = new MinSizeLabel();

    public PositionStatusLineElementProvider() {
        statusLineLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
    }

    public Component getStatusLineElement() {
        return statusLineLabel;
    }

    public void setStatus(String statusMessage) {
        statusLineLabel.setText(statusMessage);
    }

    static class MinSizeLabel extends JLabel {

        private boolean firstPaint = true;
        private int charWidth = -1;

        MinSizeLabel() {
            setFont(getFont().deriveFont(AffineTransform.getScaleInstance(0.9, 0.9)));
        }

        @Override
        public void paint(Graphics g) {
            if (firstPaint) {
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics(getFont());
                charWidth = fm.stringWidth("0");
                firstPaint = false;
            }
            super.paint(g);
        }

        @Override
        public void setFont(Font font) {
            firstPaint = true;
            charWidth = -1;
            super.setFont(font);
        }

        @Override
        public Dimension getPreferredSize() {
            Insets ins = getInsets();
            Dimension result = super.getPreferredSize();
            int w = charWidth;
            if (w == -1) {
                w = 12;
            }
            result.width = Math.max((ins.left + ins.right) + (w * 10), result.width);
            result.height = Math.max(10, result.height);
            return result;
        }
    }
}
