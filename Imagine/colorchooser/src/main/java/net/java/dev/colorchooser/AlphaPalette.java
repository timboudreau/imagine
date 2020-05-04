/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.colorchooser;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import javax.swing.UIManager;
import static net.java.dev.colorchooser.ColorChooser.getString;

/**
 *
 * @author Tim Boudreau
 */
public class AlphaPalette extends Palette {

    private final ColorChooser chooser;

//    private static final DecimalFormat FMT = new DecimalFormat(getString("alphaFormat"));
    private static final DecimalFormat FMT = new DecimalFormat(getString("##0.##% Alpha"));

    AlphaPalette(ColorChooser chooser) {
        this.chooser = chooser;
    }

    @Override
    public Color getColorAt(int x, int y) {
        Color base = chooser.getColor();
        int alpha = alphaAt(y);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    @Override
    public String getNameAt(int x, int y) {
        float fact = Math.max(0F, Math.min(1F, alphaFactorAt(y)));
        return FMT.format(fact);
    }

    private float alphaFactorAt(int y) {
        float h = getSize().height;
        float factor = y / h;
        System.out.println("factor " + y + ": " + factor);
        return 1F - factor;
    }

    private int alphaAt(int y) {
        int result = (int) (255F * alphaFactorAt(y));
        return Math.min(255, Math.max(0, result));
    }

    private final Color gray1 = new Color(164, 164, 164);
    private final Color gray2 = new Color(128, 128, 128);
    @Override
    public void paintTo(Graphics g) {
        Dimension sz = getSize();
        for (int x = 0; x < sz.width; x += 12) {
            int xix = x / 12;
            boolean evenX = xix % 2 == 0;
            for (int y = 0; y < sz.height; y += 12) {
                int yix = y / 12;
                boolean evenY = yix % 2 == 0;
                if (evenX == evenY) {
                    g.setColor(gray1);
                } else {
                    g.setColor(gray2);
                }
                g.fillRect(x, y, 12, 12);
            }
        }
        Color c1 = getColorAt(0, 0);
        Color c2 = getColorAt(0, sz.height);
        System.out.println("A1 " + c1.getAlpha() + " A2 " + c2.getAlpha());
        GradientPaint gp = new GradientPaint(0, 0, c1, 0,
                sz.height, c2);
        Graphics2D gg = (Graphics2D) g;
        gg.setPaint(gp);
        gg.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        gg.fillRect(1, 1, sz.width - 2, sz.height - 2);
        gg.setColor(UIManager.getColor("controlShadow"));
        gg.drawRect(0, 0, sz.width - 1, sz.height - 1);
    }

    @Override
    public Dimension getSize() {
        return new Dimension(120, 360);
    }

    @Override
    public String getDisplayName() {
        return getString("alpha");
    }
}
