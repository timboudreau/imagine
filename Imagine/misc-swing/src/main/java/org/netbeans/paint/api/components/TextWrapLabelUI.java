package org.netbeans.paint.api.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.LabelUI;

/**
 *
 * @author Tim Boudreau
 */
public class TextWrapLabelUI extends LabelUI {

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }
    
    @Override
    public Dimension getPreferredSize(JComponent c) {
        FontMetrics fm = c.getFontMetrics(c.getFont());
        String txt = ((JLabel) c).getText();
        Insets ins = c.getInsets();
        Color fg = c.getForeground();
        Font f = c.getFont();
        return doPaint (null, ins, txt, fm, fg, f, 1.0D);
    }

    @Override
    public int getBaseline(JComponent c, int width, int height) {
        Font f = c.getFont();
        FontMetrics fm = c.getFontMetrics(f);
        Insets ins = c.getInsets();
        if (fm != null) {
            return ins.top + fm.getMaxAscent();
        } else {
            return height / 2;
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        String txt = ((JLabel) c).getText();
        Insets ins = c.getInsets();
        doPaint((Graphics2D) g, ins, txt, null, c.getForeground(), c.getFont(), 1.0D);
    }
    
    private static final int WRAP_TRIGGER = 20;
    private static final int WRAP_POINT = 20;
    
    public static Dimension doPaint(Graphics2D gg, Insets ins, String txt, FontMetrics fm, Paint fg, Font f, double leading) {
        String[] words;
        if (txt.length() < WRAP_TRIGGER) {
            words = new String[]{txt};
        } else {
            words = txt.split("\\s");
            if (words.length < 2) {
                words = new String[] { txt };
            }
        }
        int x = ins.left;
        int y = ins.top;
        if (gg != null) {
            gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gg.setFont(f);
            gg.setPaint(fg);
            fm = gg.getFontMetrics();
        }
        int spaceWidth = fm.stringWidth(" ");
        int top = y + fm.getMaxAscent();
        int charCount = 0;
        int maxX = ins.left + ins.right;
        int maxY = ins.top + ins.bottom + fm.getHeight();
        int lineGap = (int) Math.ceil((double) fm.getHeight() * leading);
        
        for (int i=0; i < words.length; i++) {
            String word = words[i];
            
            int w = fm.stringWidth(word);
            if (gg != null) {
                gg.drawString(word, x, top);
            }
            charCount += word.length() + 1;
            x += w;
            
            maxX = Math.max(maxX, x);
            if (i != words.length - 1) {
                x += spaceWidth;
                maxX = Math.max (maxX, x);
                int nextWordLength = words[i+1].length();
                if (charCount + nextWordLength > WRAP_POINT) {
                    x = ins.left;
                    top += lineGap;
                    maxY += lineGap;
                    charCount = 0;
                }
            }
            maxY = Math.max(maxY, y);
        }
        return new Dimension(maxX + ins.left + ins.right, maxY + ins.top + ins.bottom);
    }
}
