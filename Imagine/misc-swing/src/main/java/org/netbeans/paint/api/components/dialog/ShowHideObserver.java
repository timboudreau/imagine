package org.netbeans.paint.api.components.dialog;

import javax.swing.JComponent;
import javax.swing.JDialog;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ShowHideObserver<C extends JComponent> {

    void onTransition(boolean hide, C component, String key, DialogController ctrllr, JDialog dlg);

    default ShowHideObserver<C> andThen(ShowHideObserver<? super C> other) {
        return (boolean hide, C component, String key, DialogController ctrllr, JDialog dlg) -> {
            onTransition(hide, component, key, ctrllr, dlg);
            other.onTransition(hide, component, key, ctrllr, dlg);
        };
    }

}
