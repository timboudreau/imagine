/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.colors;

import java.awt.Color;

/**
 *
 * @author Tim Boudreau
 */
public interface Hue {

    float hue();

    static Hue forColor(Color templ) {
        Color.RGBtoHSB(templ.getRed(), templ.getGreen(), templ.getBlue(), Hues.SCRATCH);
        final float h = Hues.SCRATCH[0];
        return () -> h;
    }

    static Hue forHue(float hue) {
        if (hue > 1.0) {
            float base = (int) Math.ceil(hue);
            float diff = base - hue;
            return () -> 1F - diff;
        }
        return () -> hue;
    }

    default Color toColor(float saturation, float value) {
        return new Color(Color.HSBtoRGB(hue(), saturation, value));
    }

    default Color toColor(float saturation, float value, float alpha) {
        Color res = new Color(Color.HSBtoRGB(hue(), saturation, value));
        if (alpha != 1F) {
            int alph = (int) Math.max(0, Math.min(255, alpha * 255));
            res = new Color(res.getRed(), res.getGreen(), res.getBlue(), alph);
        }
        return res;
    }

    default Color fromTemplateColor(Color templ) {
        Color.RGBtoHSB(templ.getRed(), templ.getGreen(), templ.getBlue(), Hues.SCRATCH);
        Hues.SCRATCH[0] = hue();
        if (Hues.SCRATCH[1] == 0) {
            Hues.SCRATCH[1] = Math.max(Hues.SCRATCH[2], 0.45F);
        }
        Color result = new Color(Color.HSBtoRGB(Hues.SCRATCH[0], Hues.SCRATCH[1], Hues.SCRATCH[2]));
        if (templ.getAlpha() != 255) {
            result = new Color(result.getRed(), result.getGreen(), result.getBlue(), templ.getAlpha());
        }
        return result;
    }

    default Color withBrightnessFrom(Color templ, float saturation) {
        Color.RGBtoHSB(templ.getRed(), templ.getGreen(), templ.getBlue(), Hues.SCRATCH);
        Hues.SCRATCH[0] = hue();
        Hues.SCRATCH[1] = saturation;
        Color result = new Color(Color.HSBtoRGB(Hues.SCRATCH[0], Hues.SCRATCH[1], Hues.SCRATCH[2]));
        if (templ.getAlpha() != 255) {
            result = new Color(result.getRed(), result.getGreen(), result.getBlue(), templ.getAlpha());
        }
        return result;
    }

    default Hue midPoint(Hue other) {
        return () -> {
            float a = hue();
            float b = other.hue();
            float first = Math.min(a, b);
            float second = Math.max(a, b);
            float diff = second - first;
            return first + (diff / 2F);
        };
    }

    default Hue shiftToward(Hue other, float by) {
        return () -> {
            float a = hue();
            float b = other.hue();
            float first = Math.min(a, b);
            float second = Math.max(a, b);
            float diff = (second - first) * by;
            return Math.min(1F, Math.max(0F, first + (diff / 2F)));
        };
    }
}
