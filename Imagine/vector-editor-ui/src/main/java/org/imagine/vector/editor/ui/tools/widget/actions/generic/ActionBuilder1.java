/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import javax.swing.KeyStroke;

/**
 *
 * @author Tim Boudreau
 */
public final class ActionBuilder1 extends ActionBuilder<ActionBuilder1> {

    ActionBuilder1(String displayName, ActionsBuilder factory) {
        super(displayName, factory);
    }

    public ActionBuilder1 withKeyBinding(KeyStroke ks) {
        keyBindings.add(ks);
        return this;
    }

    public <T> SensitivityBuilder<T, FinishableActionBuilder1<T>> sensitiveTo(Class<T> type) {
        return new SensitivityBuilder<>(type, (tp, sense) -> {
            return new FinishableActionBuilder1<T>(this, tp, sense);
        });
    }

}
