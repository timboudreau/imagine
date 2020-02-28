/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.Font;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public enum FontStyle {
    PLAIN, BOLD, ITALIC, BOLD_ITALIC;

    @Override
    public String toString() {
        return NbBundle.getMessage(FontStyle.class, name());
    }

    public int toFontConstant() {
        switch (this) {
            case PLAIN:
                return Font.PLAIN;
            case BOLD:
                return Font.BOLD;
            case ITALIC:
                return Font.ITALIC;
            case BOLD_ITALIC:
                return Font.BOLD | Font.ITALIC;
            default:
                throw new AssertionError();
        }
    }

    public static FontStyle fromFontConstant(int val) {
        switch (val) {
            case Font.PLAIN:
                return PLAIN;
            case Font.BOLD:
                return BOLD;
            case Font.ITALIC:
                return ITALIC;
            case Font.BOLD | Font.ITALIC:
                return BOLD_ITALIC;
            default:
                return PLAIN;
        }
    }

}
