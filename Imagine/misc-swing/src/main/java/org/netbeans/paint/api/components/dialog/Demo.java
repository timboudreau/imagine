/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.dialog;

import com.mastfrog.function.BooleanConsumer;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import org.netbeans.paint.api.components.EnumComboBoxModel;

/**
 *
 * @author Tim Boudreau
 */
public class Demo {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            createFrame().setVisible(true);
            createFrame().setVisible(true);
        });
    }

    static int fix = 1;

    private static JFrame createFrame() {
        JFrame frm = new JFrame("Frame " + fix++);
        frm.setLocationByPlatform(true);
        frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frm.setContentPane(new DemoPanel());
        frm.pack();
        return frm;
    }

    static class DemoPanel extends JPanel implements ActionListener {

        static int ixs = 1;
        private final JComboBox buttonSets = EnumComboBoxModel.newComboBox(ButtonSet.OK_CANCEL);
        int ix = ixs++;
        private final Set<ButtonMeaning> allowCloseFor = EnumSet.allOf(ButtonMeaning.class);
        private JCheckBox defaultButtonIsNonOk = new JCheckBox("Negative default button");

        DemoPanel() {
            setLayout(new FlowLayout(FlowLayout.LEADING, 12, 12));
            setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            JLabel name = new JLabel("Dialogs Demo " + ix);
            JButton non = new JButton("Non-modal dialog");
            non.setActionCommand("non");
            JButton modal = new JButton("Modal dialog");
            modal.setActionCommand("modal");
            JButton undecorated = new JButton("Undecorated");
            undecorated.setActionCommand("und");

            add(name);
            add(non);
            add(undecorated);
            add(modal);
            add(defaultButtonIsNonOk);
            add(buttonSets);
            for (ButtonMeaning meaning : ButtonMeaning.values()) {
                JCheckBox mbox = new JCheckBox("Close on " + meaning.name());
                mbox.setSelected(true);
                mbox.addActionListener(ae -> {
                    if (mbox.isSelected()) {
                        allowCloseFor.add(meaning);
                    } else {
                        allowCloseFor.remove(meaning);
                    }
                });
                add(mbox);
            }

            non.addActionListener(this);
            modal.addActionListener(this);
            undecorated.addActionListener(this);
        }

        void setValidUpdater(BooleanConsumer c) {
            JCheckBox box = new JCheckBox("Valid");
            box.addActionListener(ae -> {
                c.accept(box.isSelected());
            });
            add(box);
            invalidate();
            revalidate();
            repaint();
        }

        boolean shouldClose(DemoPanel key, ButtonMeaning meaning) {
            System.out.println("should close " + key + " for " + meaning);
            return allowCloseFor.contains(meaning);
        }

        public void addNotify() {
            super.addNotify();
            buttonSets.requestFocus();
        }

        int ttls = 1;

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean undecorated = "und".equals(e.getActionCommand());
            boolean modal = "modal".equals(e.getActionCommand());
            ButtonSet buttons = (ButtonSet) buttonSets.getSelectedItem();
            System.out.println("SELECTED BUTTO " + buttons);
            String ttl = "Child " + ttls++ + " of " + ix;
            DialogBuilder b = new DialogBuilderImpl(e.getActionCommand() + "-" + ix)
                    .setTitle(ttl);
            if (modal) {
                b.modal();
            } else {
                b.nonModal();
            }
            if (undecorated) {
                b.undecorated();
            }
            switch (buttons) {
                case CLOSE:
                    b.closeOnly();
                    break;
                case OK_CANCEL:
                    if (defaultButtonIsNonOk.isSelected()) {
                        b.okCancel(buttons.negativeMeaning());
                    } else {
                        b.okCancel();
                    }
                    break;
                case YES_NO:
                    if (defaultButtonIsNonOk.isSelected()) {
                        b.yesNo(buttons.nonPositiveMeaning());
                    } else {
                        b.yesNo();
                    }
                    break;
                case YES_NO_CANCEL:
                    if (defaultButtonIsNonOk.isSelected()) {
                        b.yesNoCancel(buttons.nonPositiveMeaning());
                    } else {
                        b.yesNoCancel();
                    }
                    break;
                default:
                    throw new AssertionError(buttons);
            }

            DemoPanel newPanel = new DemoPanel();

            DialogController<DemoPanel> ctrlr = b.forContent(newPanel, this::shouldClose);
            ctrlr.onShowOrHideDialog((hid, comp, str, ctrl, dlg) -> {
                if (!hid) {
                    JButton aborter = new JButton("Abort");
                    aborter.addActionListener(ae -> {
                        ctrl.abort();
                    });
                    newPanel.add(aborter);
                    newPanel.setValidUpdater(ctrl::setValidity);
                }
            });
            Boolean val = ctrlr.openDialog(foo -> {
                return true;
            });
            System.out.println("RETURN FROM SHOW DIALOG WITH " + val);
        }
    }
}
