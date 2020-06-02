/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.markdown.uiapi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 *
 * @author Tim Boudreau
 */
final class ComponentMarkdownUIProperties implements MarkdownUIProperties {

    private static float BLUE = 0.6666667F;
    private final JComponent component;
    private final Ellipse2D.Float ell = new Ellipse2D.Float();
    private final Rectangle2D.Float rect = new Rectangle2D.Float();
    private final float[] hsbScratch = new float[3];

    public ComponentMarkdownUIProperties(JComponent component) {
        this.component = component;
    }

    @Override
    public Color linkColor() {
        return withHsb(component.getBackground(), (backHue, backSat, backVal) -> {
            return withHsb(component.getForeground(), (foreHue, foreSat, foreVal) -> {
                float brightnessDiff = backVal - foreVal;
                float factor = backVal > foreVal ? 0.75F : 0.5F;
                float targetBrightness = Math.max(0, Math.min(1, foreVal + (factor * brightnessDiff)));
                return new Color(Color.HSBtoRGB(BLUE, 0.975F, targetBrightness));
            });
        });
    }

    @Override
    public Shape bulletShape(int depth, float x1, float y1, float size) {
        depth /= 2;
        if ((depth + 1) % 2 != 0) {
            ell.x = x1;
            ell.y = y1;
            ell.width = size;
            ell.height = size;
            return ell;
        } else {
            rect.x = x1;
            rect.y = y1;
            rect.width = size;
            rect.height = size;
            return rect;
        }
    }

    @Override
    public boolean isBulletShapeFilled(int depth) {
        depth /= 2;
        return depth % 2 == 0;
    }

    @Override
    public Font getFont() {
        return component.getFont();
    }

    @Override
    public Color textColor() {
        return component.getForeground();
    }

    @Override
    public Color blockquoteSidebarColor() {
        return UIManager.getColor("Tree.line");
    }

    @Override
    public Color horizontalRuleHighlightColor() {
        return UIManager.getColor("controlShadow");
    }

    @Override
    public Color horizontalRuleShadowColor() {
        return UIManager.getColor("controlDkShadow");
    }

    Color withHsb(Color c, HSBConsumer consumer) {
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsbScratch);
        return consumer.withHsb(hsbScratch[0], hsbScratch[1], hsbScratch[2]);
    }

    interface HSBConsumer {

        Color withHsb(float hue, float sat, float val);
    }
}
