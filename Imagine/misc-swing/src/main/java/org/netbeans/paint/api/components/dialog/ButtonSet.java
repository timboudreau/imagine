/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.dialog;

import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import org.openide.awt.Mnemonics;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public enum ButtonSet {
    OK_CANCEL, YES_NO, YES_NO_CANCEL, CLOSE;

    JButton[] buttons(Consumer<ButtonMeaning> c) {
        ConsumerActionListener al = new ConsumerActionListener(c);
        ButtonType[] types = buttons();
        JButton[] result = new JButton[types.length];
        for (int i = 0; i < types.length; i++) {
            JButton curr = new JButton();
            Mnemonics.setLocalizedText(curr, types[i].toString());
            ButtonMeaning meaning = types[i].meaning(this);
            curr.setActionCommand(meaning.name());
            curr.addActionListener(al);
            curr.putClientProperty(ButtonSet.class.getName(), this);
            curr.putClientProperty(ButtonType.class.getName(), types[i]);
            curr.putClientProperty(ButtonMeaning.class.getName(), meaning);
            result[i] = curr;
        }
        boolean mac = Utilities.isMac();
        if (mac) {
            Arrays.sort(result, (a, b) -> {
                ButtonMeaning aMeaning = buttonMeaning(a);
                ButtonMeaning bMeaning = buttonMeaning(b);
                if (aMeaning.isAffirmitive()) {
                    return 1;
                } else if (bMeaning.isAffirmitive()) {
                    return -1;
                } else if (aMeaning.isDenial()) {
                    return 1;
                } else if (bMeaning.isDenial()) {
                    return -1;
                }
                return 0;
            });
        }
        return result;
    }

    static ButtonSet buttonSet(JComponent comp) {
        return (ButtonSet) comp.getClientProperty(ButtonSet.class.getName());
    }

    static ButtonMeaning buttonMeaning(JComponent comp) {
        return (ButtonMeaning) comp.getClientProperty(ButtonMeaning.class.getName());
    }

    static ButtonType buttonType(JComponent comp) {
        return (ButtonType) comp.getClientProperty(ButtonType.class.getName());
    }

    ButtonMeaning negativeMeaning() {
        switch (this) {
            case OK_CANCEL:
            case YES_NO_CANCEL:
            case YES_NO:
                return ButtonMeaning.DENY;
            case CLOSE:
                return ButtonMeaning.IGNORE;
            default:
                throw new AssertionError(this);
        }
    }

    ButtonMeaning defaultDefaultButtonMeaning() {
        switch (this) {
            case OK_CANCEL:
            case YES_NO:
            case YES_NO_CANCEL:
            case CLOSE:
                return ButtonMeaning.AFFIRM;
            default:
                throw new AssertionError(this);
        }
    }

    ButtonType[] buttons() {
        switch (this) {
            case OK_CANCEL:
                return Utilities.isMac()
                        ? new ButtonType[]{
                            ButtonType.CANCEL, ButtonType.OK
                        }
                        : new ButtonType[]{
                            ButtonType.OK, ButtonType.CANCEL
                        };
            case YES_NO:
                return Utilities.isMac()
                        ? new ButtonType[]{
                            ButtonType.NO, ButtonType.YES
                        } : new ButtonType[]{
                            ButtonType.YES, ButtonType.NO
                        };
            case CLOSE:
                return new ButtonType[]{ButtonType.CLOSE};
            case YES_NO_CANCEL:
                return Utilities.isMac()
                        ? new ButtonType[]{
                            ButtonType.CANCEL, ButtonType.NO, ButtonType.YES
                        }
                        : new ButtonType[]{
                            ButtonType.YES, ButtonType.NO, ButtonType.CANCEL
                        };
            default:
                throw new AssertionError(this);
        }
    }

    ButtonMeaning nonPositiveMeaning() {
        switch (this) {
            case OK_CANCEL:
            case YES_NO:
                return ButtonMeaning.DENY;
            case YES_NO_CANCEL:
            case CLOSE:
                return ButtonMeaning.IGNORE;
            default:
                throw new AssertionError(this);

        }
    }

}
