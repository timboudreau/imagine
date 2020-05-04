/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.colors;

/**
 *
 * @author Tim Boudreau
 */
public enum Hues implements Hue {
    Blue(0.6666667F),
    Green(0.33333334F),
    Red(0.0F),
    Pink(0.0F),
    Yellow(0.16666667F),
    Orange(0.13071896F),
    Magenta(0.8333333F),
    Cyan(0.5F);
    private final float hue;

    static final float[] SCRATCH = new float[3];

    Hues(float hue) {
        this.hue = hue;
    }

    @Override
    public float hue() {
        return hue;
    }
}
