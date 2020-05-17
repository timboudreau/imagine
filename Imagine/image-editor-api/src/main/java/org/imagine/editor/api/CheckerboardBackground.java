/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
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
public enum CheckerboardBackground implements EditorBackground {

    MEDIUM("org/imagine/editor/api/backgroundpattern.png"), //NOI18N
    LIGHT("org/imagine/editor/api/backgroundpattern-light.png"), //NOI18N
    DARK("org/imagine/editor/api/backgroundpattern-dark.png"), //NOI18N
    ;
    private final String resource;
    private TexturePaint paint;
    private Icon icon;
    private static final int LIGHT_ALPHA = 220;
    private static final int DARK_ALPHA = 172;

    CheckerboardBackground(String resource) {
        this.resource = resource;
    }

    public Icon icon() {
        return icon == null ? icon = new EditorBackgroundIcon(this) : icon;
    }

    @Override
    public boolean isBright() {
        return this == LIGHT;
    }

    @Override
    public boolean isDark() {
        return this == DARK;
    }

    @Override
    public boolean isMedium() {
        return this == MEDIUM;
    }

    @Override
    public double meanBrightness() {
        switch (this) {
            case DARK:
                return 0.125;
            case MEDIUM:
                return 0.625;
            case LIGHT:
                return 0.875;
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public Color contrasting() {
        switch (this) {
            case LIGHT:
                return new Color(10, 10, 10, LIGHT_ALPHA);
            default:
                return new Color(255, 255, 255, DARK_ALPHA);
        }
    }

    @Override
    public Color midContrasting() {
        switch (this) {
            case LIGHT:
                return new Color(80, 80, 80, LIGHT_ALPHA);
            case MEDIUM:
                return new Color(220, 220, 220, LIGHT_ALPHA);
            default:
                return new Color(200, 200, 200, LIGHT_ALPHA);
        }
    }

    @Override
    public Color lowContrasting() {
        switch (this) {
            case LIGHT:
                return new Color(120, 120, 120, LIGHT_ALPHA);
            case MEDIUM:
                return new Color(190, 190, 190, LIGHT_ALPHA);
            default:
                return new Color(150, 150, 150, LIGHT_ALPHA);
        }
    }

    @Override
    public Color nonContrasting() {
        switch (this) {
            case LIGHT:
                return Color.BLACK;
            case DARK:
                return Color.GRAY;
            default:
                return Color.WHITE;
        }
    }

    public String toString() {
        return NbBundle.getMessage(CheckerboardBackground.class, name());
    }

    @Override
    public TexturePaint getPaint() {
        return paint == null ? paint = new TexturePaint(
                ((BufferedImage) ImageUtilities.loadImage(
                        resource)),
                new Rectangle(0, 0, 16, 16)) : paint;
    }

    @Override
    public void fill(Graphics2D g, Rectangle bounds) {
        g.setPaint(getPaint());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
