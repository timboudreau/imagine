package org.netbeans.paint.api.components.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.netbeans.paint.api.components.FlexEmptyBorder;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.Parameters;
import static org.openide.util.Parameters.notNull;

/**
 * Because NotifyDescriptor/DialogDescriptor/DialogDisplayer are horrifying '90s
 * monstrosities and this should be simple.
 *
 * @author Tim Boudreau
 */
public abstract class DialogBuilder {

    final String key;
    String title;
    ButtonSet buttons = ButtonSet.OK_CANCEL;
    ModalityType modal = ModalityType.APPLICATION_MODAL;
    boolean decorated = true;
    boolean locationByPlatform = false;
    Action helpAction;
    ButtonMeaning defaultButtonMeaning = ButtonMeaning.AFFIRM;
    Window owner;
    ShowHideObserver<? super JComponent> observer;

    DialogBuilder(String key) {
        this.key = key;
    }

    public static DialogBuilder forName(String name) {
        return new DialogBuilderImpl(name);
    }

    /**
     * Call this method to set up the builder to allow the OS to position the
     * window via Dialog.setLocationByPlatform().
     *
     * @return this
     */
    public DialogBuilder letOperatingSystemPositionWindow() {
        locationByPlatform = true;
        return this;
    }

    /**
     * Provide a specific window you want to be the owner of created dialogs. If
     * omitted, the focus owner at the time of invocation, or an offscreen
     * window if none can be found, is used.
     *
     * @param owner The owner
     * @return this
     */
    public DialogBuilder ownedBy(Window owner) {
        notNull("owner", owner);
        this.owner = owner;
        return this;
    }

    Rectangle configureWindow(Window prospectiveParent) {
        if (locationByPlatform) {
            prospectiveParent.setLocationByPlatform(true);
        }
        if (isSavable()) {
            GraphicsConfiguration config = prospectiveParent.getGraphicsConfiguration();
            if (config == null) {
                return null;
            }
            String base = keyBase(config);
            System.out.println("Save config for " + base);
            Preferences prefs = NbPreferences.forModule(DialogBuilder.class);
            int w = prefs.getInt(base + "-w", -1);
            int h = prefs.getInt(base + "-h", -1);
            if (w <= 10 || h <= 10) {
                return null;
            }
            int relX = prefs.getInt(base + "-relx", Integer.MIN_VALUE);
            int relY = prefs.getInt(base + "-rely", Integer.MIN_VALUE);
            Rectangle configBounds = config.getBounds();
            Rectangle prospectiveBounds = null;
            if (relX != Integer.MIN_VALUE && relY != Integer.MIN_VALUE) {
                Rectangle parentBounds = prospectiveParent.getBounds();
                if (!parentBounds.isEmpty()) {
                    prospectiveBounds = new Rectangle(parentBounds.x + relX,
                            parentBounds.y + relY, w, h);
                    if (!configBounds.contains(prospectiveBounds)) {
                        prospectiveBounds = null;
                    }
                }
            }
            if (prospectiveBounds == null) {
                int x = prefs.getInt(base + "-x", Integer.MIN_VALUE);
                int y = prefs.getInt(base + "-y", Integer.MIN_VALUE);
                if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
                    prospectiveBounds = new Rectangle(x, y, w, h);
                    if (!configBounds.contains(prospectiveBounds)) {
                        prospectiveBounds = null;
                    }
                }
            }
            return prospectiveBounds;
        }
        return null;
    }

    boolean saveParameters(Window from) {
        if (isSavable()) {
            Rectangle bds = from.getBounds();
            if (bds.isEmpty()
                    || !from.getGraphicsConfiguration().getBounds().contains(bds)) {
                return false;
            }
            String base = keyBase(from.getGraphicsConfiguration());
            Preferences prefs = NbPreferences.forModule(DialogBuilder.class);
            prefs.putInt(base + "-x", bds.x);
            prefs.putInt(base + "-y", bds.y);
            prefs.putInt(base + "-w", bds.width);
            prefs.putInt(base + "-h", bds.height);

            Window owner = from.getOwner();
            if (owner != null) {
                Rectangle ownerBounds = owner.getBounds();
                if (!ownerBounds.isEmpty()) {
                    int relX = bds.x - ownerBounds.x;
                    int relY = bds.y - ownerBounds.y;
                    prefs.putInt(base + "-relx", relX);
                    prefs.putInt(base + "-rely", relY);
                }
            }
            return true;
        }
        return false;
    }

    private boolean isSavable() {
        return !locationByPlatform;
    }

    private String keyBase(GraphicsConfiguration config) {
        String gid = config.getDevice().getIDstring();
        DisplayMode mode = config.getDevice().getDisplayMode();
        return gid + "-" + mode.getWidth() + "-" + mode.getHeight();
    }

    /**
     * Set this builder to create dialogs which do not have the system default
     * window manager's frame decorations, title bar, etc.
     *
     * @return this
     */
    public DialogBuilder undecorated() {
        decorated = false;
        return this;
    }

    /**
     * Set the dialog title.
     *
     * @param title The title, non-null
     * @return this
     */
    public DialogBuilder setTitle(String title) {
        notNull("title", title);
        this.title = title;
        return this;
    }

    /**
     * Generate OK cancel dialogs.
     *
     * @param defaultOption The options which should be the default button if
     * the user presses enter in the open dialog when focus is not on a
     * component that consumes that keystroke
     * @return this
     */
    public DialogBuilder okCancel(ButtonMeaning defaultOption) {
        notNull("defaultOption", defaultOption);
        return setButtons(ButtonSet.OK_CANCEL);
    }

    /**
     * Generate OK cancel dialogs.
     *
     * @return this
     */
    public DialogBuilder okCancel() {
        return setButtons(ButtonSet.OK_CANCEL);
    }

    /**
     * Generate dialogs which have only a Close button.
     *
     * @return this
     */
    public DialogBuilder closeOnly() {
        return setButtons(ButtonSet.CLOSE);
    }

    /**
     * Generate Yes/No dialogs.
     *
     * @param defaultOption The options which should be the default button if
     * the user presses enter in the open dialog when focus is not on a
     * component that consumes that keystroke
     * @return this
     */
    public DialogBuilder yesNo(ButtonMeaning defaultOption) {
        notNull("defaultOption", defaultOption);
        return setButtons(ButtonSet.YES_NO, defaultOption);
    }

    /**
     * Generate Yes/No dialogs.
     *
     * @return this
     */
    public DialogBuilder yesNo() {
        return setButtons(ButtonSet.YES_NO);
    }

    /**
     * Generate Yes/No/Cancel dialogs.
     *
     * @param defaultOption The options which should be the default button if
     * the user presses enter in the open dialog when focus is not on a
     * component that consumes that keystroke
     * @return this
     */
    public DialogBuilder yesNoCancel(ButtonMeaning defaultOption) {
        notNull("defaultOption", defaultOption);
        return setButtons(ButtonSet.YES_NO_CANCEL, defaultOption);
    }

    /**
     * Generate Yes/No/Cancel dialogs.
     *
     * @return this
     */
    public DialogBuilder yesNoCancel() {
        return setButtons(ButtonSet.YES_NO_CANCEL);
    }

    private DialogBuilder setButtons(ButtonSet set) {
        return setButtons(set, set.defaultDefaultButtonMeaning());
    }

    private DialogBuilder setButtons(ButtonSet set, ButtonMeaning defaultOption) {
        assert consistent(set, defaultOption);
        defaultButtonMeaning = defaultOption;
        buttons = set;
        return this;
    }

    private boolean consistent(ButtonSet set, ButtonMeaning meaning) {
        for (ButtonType b : set.buttons()) {
            ButtonMeaning m = b.meaning(set);
            if (m == meaning) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set this builder to create ModalityType.DOCUMENT_MODAL dialogs.
     *
     * @return this
     */
    public DialogBuilder modal() {
        this.modal = ModalityType.DOCUMENT_MODAL;
        return this;
    }

    /**
     * Set this builder to create ModalityType.MODELESS dialogs.
     *
     * @return this
     */
    public DialogBuilder nonModal() {
        this.modal = ModalityType.MODELESS;
        return this;
    }

    /**
     * Provide a help button at the bottom of dialogs.
     *
     * @param help A help button
     * @return this
     */
    public DialogBuilder withHelpAction(Action help) {
        Parameters.notNull("help", help);
        this.helpAction = help;
        return this;
    }

    /**
     * Provide a callback which will be invoked on show and hide of any dialog
     * generated by this builder.
     *
     * @param observer The observer
     * @return this
     */
    public DialogBuilder onShowAndHideDialog(ShowHideObserver<? super JComponent> observer) {
        if (this.observer == null) {
            this.observer = observer;
        } else {
            if (this.observer instanceof ObserverDispatcher<?>) {
                ((ObserverDispatcher<?>) this.observer).add(observer);
            } else {
                ShowHideObserver<? super JComponent> old = this.observer;
                ObserverDispatcher<JComponent> nue;
                this.observer = nue = new ObserverDispatcher<JComponent>();
                nue.add(old);
                nue.add(observer);
            }
        }
        return this;
    }

//    public abstract <C extends JComponent> DialogController<C> showDialog(C content, BiPredicate<? super C, ? super ButtonMeaning> onCloseAttempt);
    /**
     * Create the type-specific dialog controller that is used to manage one
     * dialog; the returned controller can be reused, but only after the
     * previously opened dialog is closed.
     *
     * @param <C>
     * @param contentSupplier The factory that will create the inner component
     * @param onCloseAttempt Called when the user attempts to close the dialog,
     * and can veto its closing, update buttons, etc.
     * @return this
     */
    public abstract <C extends JComponent> DialogController<C> forComponent(Supplier<C> contentSupplier, BiPredicate<? super C, ? super ButtonMeaning> onCloseAttempt);

    public final <C extends JComponent> DialogController<C> forComponent(Supplier<C> content) {
        return forComponent(content, (ignored1, ignored2) -> true);
    }

    public final <C extends JComponent> DialogController<C> forContent(C content) {
        return forComponent((Supplier<C>) () -> content, (ignored1, ignored2) -> true);
    }

    public final <C extends JComponent> DialogController<C> forContent(C content, BiPredicate<? super C, ? super ButtonMeaning> onCloseAttempt) {
        return forComponent((Supplier<C>) () -> content, onCloseAttempt);
    }

    public void showTextLineDialog(String initialText, int minLength, int maxLength, Consumer<String> consumer) {
        showTextLineDialog(Math.max(minLength, maxLength), initialText, s -> {
            return !(s.length() < minLength || s.length() > maxLength);
        }, consumer);
    }

    public void showTextLineDialog(Predicate<String> validator, Consumer<String> consumer) {
        showTextLineDialog("", validator, consumer);
    }

    public void showTextLineDialog(String initialText, Predicate<String> validator, Consumer<String> consumer) {
        okCancel().showTextLineDialog(80, initialText, validator, consumer);
    }

    public void showTextLineDialog(int columns, String initialText, Predicate<String> validator, Consumer<String> consumer) {
        JTextField jtf = new JTextField();
        jtf.setColumns(Math.max(1, columns));
        InputLinePanel pnl = new InputLinePanel(columns, initialText, this, validator, consumer, jtf);
        okCancel().forComponent(() -> pnl, pnl).onShowOrHideDialog(pnl).openDialog();
    }

    public String showTextLineDialog(int columns, String initialText, Predicate<String> validator) {
        assert EventQueue.isDispatchThread();
        InputLinePanel pnl = new InputLinePanel(columns, initialText, this, validator, null, new JTextField());
        return forComponent(() -> pnl, pnl).onShowOrHideDialog(pnl).openDialog(InputLinePanel::getText);
    }

    public void showMultiLineTextLineDialog(String initialText, int minLength, int maxLength, Consumer<String> consumer) {
        showMultiLineTextLineDialog(80, 5, initialText, s -> {
            return !(s.length() < minLength || s.length() > maxLength);
        }, consumer);
    }

    public void showMultiLineTextLineDialog(Predicate<String> validator, Consumer<String> consumer) {
        showMultiLineTextLineDialog("", validator, consumer);
    }

    public void showMultiLineTextLineDialog(String initialText, Predicate<String> validator, Consumer<String> consumer) {
        okCancel().showMultiLineTextLineDialog(80, 5, initialText, validator, consumer);
    }

    public void showMultiLineTextLineDialog(int columns, int rows, String initialText, Predicate<String> validator, Consumer<String> consumer) {
        JTextArea area = new JTextArea();
        area.setColumns(Math.max(1, columns));
        area.setRows(Math.max(1, rows));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        InputLinePanel pnl = new InputLinePanel(columns, initialText, this, validator, consumer, area);
        okCancel().forComponent(() -> pnl, pnl).onShowOrHideDialog(pnl).openDialog();
    }

    public String showMultiLineTextLineDialog(int columns, int rows, String initialText, Predicate<String> validator) {
        assert EventQueue.isDispatchThread();
        JTextArea area = new JTextArea();
        area.setColumns(Math.max(1, columns));
        area.setRows(Math.max(1, rows));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setColumns(columns);
        InputLinePanel pnl = new InputLinePanel(columns, initialText, this, validator, null, area);
        return forComponent(() -> pnl, pnl)
                .onShowOrHideDialog(pnl)
                .openDialog(InputLinePanel::getText);
    }

    static final class InputLinePanel extends JPanel implements DocumentListener, FocusListener, ShowHideObserver<InputLinePanel>, BiPredicate<InputLinePanel, ButtonMeaning> {

        private final JTextComponent field;
        private final Predicate<String> tester;
        private final Border fieldOrigBorder;
        private final Border errorBorder;
        private boolean inputValid = true;
        private final Consumer<String> consumer;

        InputLinePanel(int columns, String initialText, DialogBuilder bldr, Predicate<String> tester, Consumer<String> consumer, JTextComponent comp) {
            setLayout(new BorderLayout());
            this.tester = tester;
            setBorder(new FlexEmptyBorder());
            comp.setText(initialText);
            JLabel lbl = new JLabel(bldr.title);
            field = comp;
            lbl.setLabelFor(field);
            Border origBorder = field.getBorder();
            if (origBorder == null) {
                origBorder = BorderFactory.createLineBorder(field.getForeground(), 1);
            }
            this.fieldOrigBorder = origBorder;
            Color c = UIManager.getColor("nb.errorForeground");
            if (c == null) {
                c = Color.RED.darker();
            }
            Insets ins = origBorder.getBorderInsets(field);
            errorBorder = BorderFactory.createMatteBorder(ins.top, ins.left, ins.bottom, ins.right, c);
            this.consumer = consumer;
            if (field instanceof JTextArea) {
                add(lbl, BorderLayout.NORTH);
                JScrollPane pane = new JScrollPane(field);
                add(pane, BorderLayout.CENTER);
            } else {
                add(field, BorderLayout.LINE_START);
                add(lbl, BorderLayout.CENTER);
            }
            field.getDocument().addDocumentListener(this);
            field.addFocusListener(this);
        }

        String getText() {
            return field.getText();
        }

        private DialogController<InputLinePanel> ctrllr;

        void setDialogController(DialogController<InputLinePanel> ctrllr) {
            if (!inputValid) {
                ctrllr.setValidity(false);
            }
        }

        private void setInputValid(boolean inputValid) {
            if (inputValid != this.inputValid) {
                this.inputValid = inputValid;
                if (inputValid) {
                    field.setBorder(fieldOrigBorder);
                } else {
                    field.setBorder(errorBorder);
                }
                field.repaint();
                if (ctrllr != null) {
                    ctrllr.setValidity(inputValid);
                }
            }
        }

        private void onChange(DocumentEvent e) {
            try {
                String txt = e.getDocument().getText(0, e.getDocument().getLength());
                setInputValid(tester.test(txt.trim()));
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public void requestFocus() {
            field.requestFocus();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            EventQueue.invokeLater(this::requestFocus);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            onChange(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onChange(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onChange(e);
        }

        @Override
        public void focusGained(FocusEvent e) {
            field.selectAll();
        }

        @Override
        public void focusLost(FocusEvent e) {
            // do nothing
        }

        @Override
        public void onTransition(boolean hide, InputLinePanel component, String key, DialogController ctrllr, JDialog dlg) {
            System.out.println("transition " + (hide ? "HIDE " : "SHOW ") + last + " valid " + inputValid + " txt + " + getText());
            if (hide) {
                if (last.isAffirmitive() && inputValid) {
                    if (consumer != null) {
                        consumer.accept(field.getText().trim());
                    }
                }
            } else {
                setInputValid(tester.test(field.getText()));
                ctrllr.setValidity(inputValid);
                setDialogController(ctrllr);
            }
        }

        private ButtonMeaning last = ButtonMeaning.IGNORE;

        @Override
        public boolean test(InputLinePanel t, ButtonMeaning u) {
            System.out.println("TEST " + u);
            last = u;
            assert t == this;
            return u.isAffirmitive() ? inputValid : true;
        }
    }
}
