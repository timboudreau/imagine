/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.utils.java2d;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "LIGHT=Light",
    "DARK=Dark",
    "MEDIUM=Medium",})
public enum CheckerboardBackground {

    MEDIUM("org/imagine/utils/backgroundpattern.png"), //NOI18N
    LIGHT("org/imagine/utils/backgroundpattern-light.png"), //NOI18N
    DARK("org/imagine/utils/backgroundpattern-dark.png"), //NOI18N
    ;
    private final String resource;
    private TexturePaint paint;

    CheckerboardBackground(String resource) {
        this.resource = resource;
    }

    public String toString() {
        return NbBundle.getMessage(CheckerboardBackground.class, name());
    }

    public TexturePaint getPaint() {
        return paint == null ? paint = new TexturePaint(
                ((BufferedImage) ImageUtilities.loadImage(
                        resource)),
                new Rectangle(0, 0, 16, 16)) : paint;
    }

    public void fill(Graphics2D g, Rectangle bounds) {
        g.setPaint(getPaint());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
