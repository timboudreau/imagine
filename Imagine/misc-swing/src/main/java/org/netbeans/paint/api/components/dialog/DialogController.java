/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.dialog;

import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JComponent;

/**
 * Reusable control of a dialog.
 *
 * @author Tim Boudreau
 */
public abstract class DialogController<C extends JComponent> {

    DialogController() {
    }

    /**
     * Add a callback which will be invoked when the dialog is shown or hidden.
     *
     * @param obs An observer
     * @return this
     */
    public abstract DialogController<C> onShowOrHideDialog(ShowHideObserver<? super C> obs);

    /**
     * <b>Must be called from the AWT event thread, and cannot be used with
     * non-modal dialogs:</b> open the dialog, and on close, return with the
     * result of applying the passed function to the content component; the
     * function is called <i>only</i> if the close button used is the
     * affirmative one.
     *
     * @param <T> The return type
     * @param result
     * @return The output of the function
     */
    public abstract <T> T openDialog(Function<C, T> result);

    /**
     * Open a dialog <i>asynchronously even from the AWT event thread</i>, and,
     * when it is closed by an affirmative button-press, call the passed
     * consumer with the output of the function as applied to the content
     * object; when it is closed by a non-affirmative keypress, pass the passed
     * consumer null.
     *
     * @param <T>
     * @param func A conversion function
     * @param consumer The consumer of the output of the conversion function
     * @return this
     */
    public abstract <T> DialogController<C> openDialog(Function<C, T> func, Consumer<T> consumer);

    /**
     * Update the set of buttons displayed in the dialog, whether or not it is
     * open. Thread-safe.
     *
     * @param buttons The button set to use
     * @param defaultMeaning The default button that should be invoked if the
     * user presses enter in the dialog; if null, the default affirmative button
     * for this set is used.
     * @return this
     */
    public abstract DialogController<C> updateButtons(ButtonSet buttons, ButtonMeaning defaultMeaning);

    /**
     * Update the set of buttons displayed in the dialog, whether or not it is
     * open. Thread-safe.
     *
     * @param buttons The button set to use
     * @return this
     */
    public final DialogController<C> updateButtons(ButtonSet buttons) {
        return updateButtons(buttons, buttons.defaultDefaultButtonMeaning());
    }

    /**
     * Update the enablement of the affirmative dialog buttons (use this
     * to disable the OK button as needed).
     *
     * @param valid Whether or not the affirmative button should be enabled
     * @return this
     */
    public abstract DialogController<C> setValidity(boolean valid);

    /**
     * Close any visible dialog, <b>no matter what</b> - since it is
     * possible to get into a state where <i>no</i> button can close
     * a dialog, this provides a means to bypass that state.  It will call
     * the predicate this controller was created with, but will close the
     * dialog regardless of the result.
     *
     * @return this
     */
    public abstract DialogController<C> abort();

    /**
     * Open the dialog.  Synchronous when called from the AWT event thread,
     * asynchronous otherwise.
     *
     * @return this
     */
    public abstract DialogController<C> openDialog();

    /**
     * Shortcut for setValidity(true).
     *
     * @return this
     */
    public final DialogController<C> allOptionsOK() {
        setValidity(true);
        return this;
    }

    /**
     * Shortcut for setValidity(false).
     *
     * @return this
     */
    public final DialogController<C> invalid() {
        setValidity(false);
        return this;
    }

}
