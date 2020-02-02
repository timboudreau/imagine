/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.common;

import java.awt.Color;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public enum BackgroundStyle {
    TRANSPARENT,
    WHITE,
    BLACK,
    GRAY;

    @Override
    public String toString() {
        return NbBundle.getMessage(BackgroundStyle.class, name());
    }

    public static ComboBoxModel<BackgroundStyle> model() {
        DefaultComboBoxModel<BackgroundStyle> mdl = new DefaultComboBoxModel<>();
        for (BackgroundStyle b : values()) {
            mdl.addElement(b);;
        }
        return mdl;
    }

    public Color toColor() {
        switch (this) {
            case TRANSPARENT:
                return null;
            case WHITE:
                return Color.WHITE;
            case BLACK:
                return Color.BLACK;
            case GRAY:
                return new Color(128, 128, 128);
            default:
                throw new AssertionError(this);
        }
    }
}
