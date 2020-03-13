/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.api.components.dialog;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JDialog;

/**
 *
 * @author Tim Boudreau
 */
final class ObserverDispatcher<C extends JComponent> implements ShowHideObserver<C> {

    private final List<ShowHideObserver<? super C>> all = new ArrayList<>(3);

    void add(ShowHideObserver<? super C> obs) {
        all.add(obs);
    }

    void remove(ShowHideObserver<? super C> obs) {
        all.remove(obs);
    }

    @Override
    public void onTransition(boolean hide, C component, String key, DialogController ctrllr, JDialog dlg) {
        for (ShowHideObserver<? super C> obs : new ArrayList<>(all)) {
            obs.onTransition(hide, component, key, ctrllr, dlg);
        }
    }

}
