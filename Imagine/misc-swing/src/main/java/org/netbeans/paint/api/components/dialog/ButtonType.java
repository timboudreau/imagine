/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.dialog;

import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@NbBundle.Messages(value = {"OK=O&K", "CANCEL=&Cancel", "YES=&Yes", "NO=&No", "CLOSE=&Close"})
public enum ButtonType {
    OK, YES, NO, CANCEL, CLOSE;

    @Override
    public String toString() {
        return NbBundle.getMessage(ButtonType.class, name());
    }

    public ButtonMeaning meaning(ButtonSet context) {
        switch (this) {
            case OK:
            case YES:
                return ButtonMeaning.AFFIRM;
            case CANCEL:
                switch (context) {
                    case OK_CANCEL:
                        return ButtonMeaning.DENY;
                    case YES_NO_CANCEL:
                        return ButtonMeaning.IGNORE;
                    case CLOSE:
                    case YES_NO:
                        throw new AssertionError(this + " " + context);
                }
                return ButtonMeaning.DENY;
            case CLOSE:
                switch (context) {
                    case CLOSE:
                        return ButtonMeaning.AFFIRM;
                    case OK_CANCEL:
                        return ButtonMeaning.DENY;
                    case YES_NO:
                        throw new AssertionError(this + " " + context);
                    case YES_NO_CANCEL:
                        return ButtonMeaning.IGNORE;
                    default:
                        throw new AssertionError(this + " " + context);
                }
            case NO:
                switch (context) {
                    case YES_NO:
                    case YES_NO_CANCEL:
                        return ButtonMeaning.DENY;
                    default:
                        throw new AssertionError(this + " " + context);
                }
            default:
                throw new AssertionError(this);
        }
    }

}
