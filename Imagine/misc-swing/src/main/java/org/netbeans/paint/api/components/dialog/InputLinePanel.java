package org.netbeans.paint.api.components.dialog;

import com.mastfrog.swing.FlexEmptyBorder;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
final class InputLinePanel extends JPanel implements DocumentListener, FocusListener, ShowHideObserver<InputLinePanel>, BiPredicate<InputLinePanel, ButtonMeaning> {

    private final JTextComponent field;
    private final Predicate<String> tester;
    private final Border fieldOrigBorder;
    private final Border errorBorder;
    private boolean inputValid = true;
    private final Consumer<String> consumer;
    private final JComponent errorBorderTarget;

    @SuppressWarnings("LeakingThisInConstructor")
    InputLinePanel(int columns, String initialText, DialogBuilder bldr, Predicate<String> tester, Consumer<String> consumer, JTextComponent comp) {
        setLayout(new GridBagLayout());
        boolean isMultiline = comp instanceof JTextArea;
        GridBagConstraints n = new GridBagConstraints();
        n.anchor = GridBagConstraints.BASELINE_LEADING;
        n.gridx = 0;
        n.gridy = 0;
        n.gridwidth = 1;
        n.fill = GridBagConstraints.BOTH;
        n.ipadx = 5;
        n.weightx = 1;
        this.tester = tester;
        setBorder(new FlexEmptyBorder());
        comp.setText(initialText);
        JLabel lbl = new JLabel(bldr.title());
        lbl.setBorder(new FlexEmptyBorder(isMultiline ? FlexEmptyBorder.Side.BOTTOM : FlexEmptyBorder.Side.LEFT));
        field = comp;
        lbl.setLabelFor(field);
        Border origBorder = field.getBorder();
        if (origBorder == null) {
            origBorder = BorderFactory.createLineBorder(field.getForeground(), 1);
        }
        this.fieldOrigBorder = origBorder;
        Color errorHighlightColor = UIManager.getColor("nb.errorForeground");
        if (errorHighlightColor == null) {
            errorHighlightColor = Color.RED.darker();
        }
        Insets ins = origBorder.getBorderInsets(field);
        if (ins.bottom > 1 || ins.top > 1 || ins.left > 1 || ins.right > 1) {
            errorBorder = BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(Math.max(0, ins.top - 1),
                    Math.max(0, ins.left - 1), Math.max(0, ins.bottom - 1), Math.max(0, ins.right - 1), getBackground()), BorderFactory.createMatteBorder(1, 1, 1, 1, errorHighlightColor));
        } else {
            errorBorder = BorderFactory.createMatteBorder(ins.top, ins.left, ins.bottom, ins.right, errorHighlightColor);
        }
        this.consumer = consumer;
        if (isMultiline) {
            add(lbl, n);
            JScrollPane pane = new JScrollPane(field);
            Border empty = BorderFactory.createEmptyBorder();
//            pane.setBorder(empty);
            pane.setViewportBorder(empty);
            n.gridy++;
            add(pane, n);
            errorBorderTarget = pane;
        } else {
            add(lbl, n);
            n.gridx++;
            add(field, n);
            errorBorderTarget = field;
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

    private boolean setInputValid(boolean inputValid) {
        if (inputValid != this.inputValid) {
            this.inputValid = inputValid;
            if (inputValid) {
                errorBorderTarget.setBorder(fieldOrigBorder);
            } else {
                errorBorderTarget.setBorder(errorBorder);
            }
            errorBorderTarget.repaint();
            if (ctrllr != null) {
                ctrllr.setValidity(inputValid);
            }
            return true;
        }
        return false;
    }

    private void onChange(DocumentEvent e) {
        try {
            String txt = e.getDocument().getText(0, e.getDocument().getLength());
            boolean valid = tester.test(txt.trim());
            setInputValid(valid);
            System.out.println("onChange set input valid " + valid);
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
    public void removeNotify() {
        super.removeNotify();
        ctrllr = null;
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
        System.out.println("focus gained");
        field.selectAll();
        field.requestFocus();
    }

    @Override
    public void focusLost(FocusEvent e) {
        // do nothing
    }

    @Override
    public void onTransition(boolean hide, InputLinePanel component, String key, DialogController ctrllr, JDialog dlg) {
        this.ctrllr = ctrllr;
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
