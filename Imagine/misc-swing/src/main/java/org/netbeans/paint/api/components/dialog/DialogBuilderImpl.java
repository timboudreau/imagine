/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.IllegalComponentStateException;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import org.netbeans.paint.api.components.FlexEmptyBorder;
import org.netbeans.paint.api.components.FlexEmptyBorder.Side;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
final class DialogBuilderImpl extends DialogBuilder {

//    static final DialogPool dialogPool = new DialogPool();
    static final Map<Window, DialogPool> dialogPools = new WeakHashMap<>();

    public DialogBuilderImpl(String key) {
        super(key);
    }

//    @Override
    public <C extends JComponent> DialogController<C> showDialog(C content, BiPredicate<? super C, ? super ButtonMeaning> onCloseAttempt) {
        DC dc = new DC<>(() -> content, onCloseAttempt);
        EventQueue.invokeLater(() -> {
            dc.openDialog();
        });
        return dc;
    }
//        DC dc = new DC(content, onCloseAttempt);
//        dc.openDialog();
//        return dc;
//    }

    @Override
    public <C extends JComponent> DialogController<C> forComponent(Supplier<C> contentSupplier, BiPredicate<? super C, ? super ButtonMeaning> onCloseAttempt) {
        return new DC<>(contentSupplier, onCloseAttempt);
    }

    private JDialog createOrBorrowDialog() {
        JDialog result;
        if (usePoolableDialog()) {
            result = pool().borrow(findWindowOwner());
        } else {
            result = new JDialog(findWindowOwner());
            result.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        }
        return result;
    }

    private DialogPool pool() {
        return pool(findWindowOwner());
    }

    private static DialogPool pool(Window owner) {
        if (owner == null) {
            owner = findDefaultWindowOwner();
        }
        // Note, DialogPool must not directly reference the owner or
        // it can never be gc'd
        DialogPool pool = dialogPools.get(owner);
        if (pool == null) {
            pool = new DialogPool();
            dialogPools.put(owner, pool);
        }
        return pool;
    }

    private Window findWindowOwner() {
        if (owner != null && owner.isDisplayable()) {
            return owner;
        }
        return findDefaultWindowOwner();
    }

    private static Window offscreenOwner;

    private static Window findDefaultWindowOwner() {
        KeyboardFocusManager mgr = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Window result = mgr.getFocusedWindow();
        if (result == null) {
            Frame[] f = Frame.getFrames();
            if (f.length > 0) {
                if (f[0].isDisplayable()) {
                    return f[0];
                }
            }
            Window[] w = Window.getWindows();
            if (w.length > 0) {
                result = w[w.length - 1];
            }
        }
        if (result == null) {
            if (offscreenOwner == null) {
                offscreenOwner = new JWindow();
            }
            result = offscreenOwner;
        }
        return result;
    }

    private boolean usePoolableDialog() {
        return decorated;
    }

    class DC<C extends JComponent> extends DialogController<C> {

        private C content;

        private final BiPredicate<? super C, ? super ButtonMeaning> onCloseAttempt;
        private JDialog dlg;
        private boolean valid = true;
        private JButton[] controlButtons;
        private ButtonMeaning defaultButtonMeaning;
        private final WindowListener wl = new WL();
        private final ObserverDispatcher<C> obs = new ObserverDispatcher<>();
        private ButtonMeaning lastAction;
        private final Supplier<C> contentSupplier;
        private ButtonSet buttons;

        DC(Supplier<C> contentSupplier, BiPredicate<? super C, ? super ButtonMeaning> onCloseAttempt) {
            this.onCloseAttempt = onCloseAttempt;
            this.contentSupplier = contentSupplier;
            this.buttons = DialogBuilderImpl.this.buttons;
            this.defaultButtonMeaning = DialogBuilderImpl.this.defaultButtonMeaning;
            if (DialogBuilderImpl.this.observer != null) {
                obs.add(DialogBuilderImpl.this.observer);
            }
        }

        void onButtonAction(ButtonMeaning meaning) {
            lastAction = meaning;
            boolean result = onCloseAttempt.test(content, meaning);
            if (result) {
                if (dlg != null) {
                    dlg.setVisible(false);
                    onDialogClosed();
                }
            }
        }

        public DialogController<C> updateButtons(ButtonSet buttons, ButtonMeaning defaultMeaning) {
            JDialog dialogLocal = null;
            JButton[] buttonsLocal;
            Container buttonsParent;
            synchronized (this) {
                if (buttons == this.buttons && defaultButtonMeaning == defaultMeaning) {
                    return this;
                }
                dialogLocal = dlg;
                buttonsLocal = this.controlButtons;
                if (buttonsLocal != null) {
                    buttonsParent = buttonsLocal[0].getParent();
                } else {
                    buttonsParent = null;
                }
            }
            Runnable r = () -> {
                this.buttons = buttons;
                this.defaultButtonMeaning = defaultMeaning;
                if (buttonsParent != null) {
                    buttonsParent.removeAll();
                    ButtonType[] types = buttons.buttons();
                    JButton[] nue = buttons.buttons(this::onButtonAction);
                    for (int i = 0; i < nue.length; i++) {
                        buttonsParent.add(nue[i]);
                    }
                    buttonsParent.invalidate();
                    buttonsParent.revalidate();
                    buttonsParent.repaint();
                }
            };
            if (dialogLocal == null) {
                r.run();
                return this;
            }
            if (EventQueue.isDispatchThread()) {
                r.run();
            } else {
                EventQueue.invokeLater(r);
            }
            return this;
        }

        public DialogController<C> onShowOrHideDialog(ShowHideObserver<? super C> obs) {
            this.obs.add(obs);
            return this;
        }

        public <T> T openDialog(Function<C, T> result) {
            if (!EventQueue.isDispatchThread()) {
                throw new IllegalThreadStateException("Must be invoked from the event dispatch thread");
            }
            openDialog();
            if (lastAction.isAffirmitive()) {
                return result.apply(content);
            }
            return null;
        }

        public <T> DialogController<C> openDialog(Function<C, T> func, Consumer<T> consumer) {
            ShowHideObserver<C> ob = new ShowHideObserver<C>() {
                @Override
                public void onTransition(boolean hide, C component, String key1, DialogController ctrllr, JDialog dlg1) {
                    if (hide) {
                        try {
                            if (lastAction.isAffirmitive()) {
                                consumer.accept(func.apply(component));
                            } else {
                                consumer.accept(null);
                            }
                        } finally {
                            obs.remove(this);
                        }
                    }
                }
            };
            obs.add(ob);
            EventQueue.invokeLater(this::openDialog);
            return this;
        }

        public DialogController<C> openDialog() {
            JDialog orig;
            synchronized (this) {
                orig = dlg;
            }
            if (orig != null) {
                throw new IllegalComponentStateException("Already open");
            }
            Runnable r = () -> {
                lastAction = buttons.nonPositiveMeaning();
                JDialog dlgLocal = dialog();
                this.obs.onTransition(false, content, key, this, dlgLocal);
                dlgLocal.setVisible(true);
            };
            if (EventQueue.isDispatchThread()) {
                r.run();
            } else {
                EventQueue.invokeLater(r);
            }
            return this;
        }

        class EscapeAction extends AbstractAction {

            EscapeAction() {
                putValue(NAME, buttons.nonPositiveMeaning().name());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (controlButtons != null) {
                    for (JButton ctrlb : controlButtons) {
                        ButtonMeaning m = ButtonSet.buttonMeaning(ctrlb);
                        if (buttons.nonPositiveMeaning() == m) {
                            if (ctrlb.isEnabled()) {
                                ctrlb.doClick();
                                break;
                            }
                        }
                    }
                }
            }
        }

        private static final String INPUT_KEY_ESC = "_abort";

        private synchronized JDialog dialog() {
            int dist = Utilities.isMac() ? 12 : 5;
            // public FlowLayout(int align, int hgap, int vgap)
            JPanel dialogControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, dist, 0));
            dialogControlPanel.setBorder(new FlexEmptyBorder(FlexEmptyBorder.Side.BOTTOM));
//            dialogControlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, dist, 0));
            ButtonType[] types = buttons.buttons();

            controlButtons = buttons.buttons(this::onButtonAction);
            assert types.length == controlButtons.length;
            for (JButton jb : controlButtons) {
                jb.setBorder(BorderFactory.createCompoundBorder(new FlexEmptyBorder(1, 1, Side.LEFT), jb.getBorder()));
                dialogControlPanel.add(jb);
            }
            if (helpAction != null) {
                JPanel includingHelp = new JPanel(new BorderLayout());
                JPanel helpContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JButton helpButton = new JButton(helpAction);
                helpContainer.add(helpButton);
                includingHelp.add(helpContainer, BorderLayout.WEST);
                includingHelp.add(dialogControlPanel, BorderLayout.EAST);
                dialogControlPanel = includingHelp;
            }
//            dialogControlPanel.setBorder(BorderFactory.createEmptyBorder(dist, dist, dist, dist));

            JPanel contentPane = new JPanel(new BorderLayout());
            contentPane.add(dialogControlPanel, BorderLayout.SOUTH);
            if (content == null) {
                content = contentSupplier.get();
            }
            contentPane.add(content, BorderLayout.CENTER);

            dlg = createOrBorrowDialog();

            JRootPane root = dlg.getRootPane();
            InputMap in = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = root.getActionMap();
            Action a = new EscapeAction();
            in.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0, true), INPUT_KEY_ESC);
            am.put(INPUT_KEY_ESC, a);

            dlg.setModalityType(modal);
            if (title != null) {
                dlg.setTitle(title);
            }
            if (!decorated) {
                dlg.setUndecorated(!decorated);
            }
            if (locationByPlatform) {
                dlg.setLocationByPlatform(locationByPlatform);
            }
            dlg.setContentPane(contentPane);
            updateForValidity(valid);
            Rectangle bds = configureWindow(dlg.getOwner());
            if (bds == null) {
                dlg.pack();
                if (dlg.getOwner() != null) {
                    dlg.setLocationRelativeTo(dlg.getOwner());
                }
            } else {
                dlg.setBounds(bds);
            }
            dlg.addWindowListener(wl);
            return dlg;
        }

        @Override
        public DialogController<C> setValidity(boolean valid) {
            if (valid != this.valid) {
                this.valid = valid;
                updateForValidity(valid);
            }
            return this;
        }

        private void updateForValidity(boolean valid) {
            if (controlButtons != null) {
                ButtonType[] types = buttons.buttons();
                assert controlButtons.length == types.length;

                Map<ButtonMeaning, JButton> all = new HashMap<>();
                for (JButton b : controlButtons) {
                    all.put(ButtonSet.buttonMeaning(b), b);
                }
                JRootPane root = dlg.getRootPane();

                if (!valid) {
                    all.get(buttons.defaultDefaultButtonMeaning()).setEnabled(false);
                    if (root.getDefaultButton() == all.get(buttons.defaultDefaultButtonMeaning())) {
                        root.setDefaultButton(all.get(buttons.nonPositiveMeaning()));
                    }
                } else {
                    JButton btn = all.get(buttons.defaultDefaultButtonMeaning());
                    if (btn == null) {
                        throw new IllegalStateException("No button for " + buttons.defaultDefaultButtonMeaning()
                                + " in " + buttons + " with " + Arrays.asList(buttons.buttons()));
                    }
                    assert btn != null : "No button for " + buttons.defaultDefaultButtonMeaning()
                            + " in " + buttons + " with " + Arrays.asList(buttons.buttons());

                    btn.setEnabled(true);
                    root.setDefaultButton(all.get(defaultButtonMeaning));
                }
            }
        }

        @Override
        public DialogController<C> abort() {
            JDialog dlgLocal;
            synchronized (this) {
                dlgLocal = dlg;
            }
            if (dlgLocal != null) {
                lastAction = buttons.nonPositiveMeaning();
                onCloseAttempt.test(content, lastAction);
                dlgLocal.setVisible(false);
            }
            return this;
        }

        private void onDialogClosed() {
            assert dlg != null;
            try {
                this.obs.onTransition(true, content, key, this, dlg);
            } finally {
                dlg.setVisible(false);
                saveParameters(dlg);
                dlg.dispose();
                JRootPane root = dlg.getRootPane();
                InputMap in = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                ActionMap am = root.getActionMap();
                in.remove(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0, true));
                am.remove(INPUT_KEY_ESC);
                dlg = null;
                controlButtons = null;
                this.buttons = DialogBuilderImpl.this.buttons;
                this.defaultButtonMeaning = DialogBuilderImpl.this.defaultButtonMeaning;
            }
        }

        class WL implements WindowListener {

            @Override
            public void windowOpened(WindowEvent e) {
                EventQueue.invokeLater(() -> {
                    dlg.getContentPane().requestFocus();
                });
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (e.getWindow() == dlg) {
                    if (onCloseAttempt.test(content, buttons.nonPositiveMeaning())) {
                        dlg.setVisible(false);
                    }
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if (e.getWindow() == dlg) {
                    dlg.removeWindowListener(this);
                    onDialogClosed();
                }
            }

            @Override
            public void windowIconified(WindowEvent e) {
                // do nothing
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                // do nothing
            }

            @Override
            public void windowActivated(WindowEvent e) {
                // do nothing
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // do nothing
            }
        }
    }
}
