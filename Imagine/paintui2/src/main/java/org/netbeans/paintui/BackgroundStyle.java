/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paintui;

import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public enum BackgroundStyle {
    TRANSPARENT,
    WHITE;
    
    public String toString() {
        return NbBundle.getMessage(BackgroundStyle.class, name());
    }
}
