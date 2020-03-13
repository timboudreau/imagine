/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.dialog;

/**
 *
 * @author Tim Boudreau
 */
public enum ButtonMeaning {
    AFFIRM, DENY, IGNORE;

    public boolean isAffirmitive() {
        return this == AFFIRM;
    }

    public boolean isDenial() {
        return this == DENY;
    }

    public boolean isNonAffirmitive() {
        return this != AFFIRM;
    }

}
