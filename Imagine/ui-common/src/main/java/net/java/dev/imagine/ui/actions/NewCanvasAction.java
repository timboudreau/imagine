/*
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package net.java.dev.imagine.ui.actions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import net.java.dev.imagine.ui.components.ImageSizePanel;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import net.java.dev.imagine.ui.common.ImageEditorFactory;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

public final class NewCanvasAction extends org.openide.util.actions.CallableSystemAction {

    public NewCanvasAction() {
        setIcon(ImageUtilities.loadImageIcon("net/java/dev/imagine/ui/actions/newFile.png", true));//NOI18N
    }

    private ImageEditorFactory findFactory() {
        Collection<? extends ImageEditorFactory> all = new LinkedHashSet<>(Lookup.getDefault().lookupAll(ImageEditorFactory.class));
        // Filter out ones that simply delegate to whatever else is in the
        // lookup and should not be offered as a choice to the user - these
        // exist just to supply additional file filters
        for (Iterator<? extends ImageEditorFactory> it = all.iterator(); it.hasNext();) {
            if (!it.next().isUserVisible()) {
                it.remove();
            }
        }

        if (all.isEmpty()) {
            return new Dummy();
        } else if (all.size() == 1) {
            return all.iterator().next();
        } else {
            JPanel pnl = new JPanel(new GridBagLayout());
            int ins = Utilities.isMac() ? 12 : 5;
            pnl.setBorder(BorderFactory.createEmptyBorder(ins, ins, ins, ins));
            JLabel lbl = new JLabel();
            Mnemonics.setLocalizedText(lbl,
                    NbBundle.getMessage(NewCanvasAction.class,
                            "CHOOSE_EDITOR"));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 1;
            c.anchor = GridBagConstraints.FIRST_LINE_START;
            c.insets = new Insets(0, 0, 12, 0);
            Color col = UIManager.getColor("controlShadow");
            if (col == null) {
                col = Color.GRAY;
            }
            lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, col));
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            pnl.add(lbl, c);
            c.insets.left = 12;
            c.insets.bottom = 5;
            boolean first = true;
            ImageEditorFactory[] result = new ImageEditorFactory[1];
            DialogDescriptor[] dlg = new DialogDescriptor[1];
            ActionListener al = ae -> {
                if (!(result[0] instanceof Dummy)) {
                    dlg[0].setValid(true);
                }
            };
            for (ImageEditorFactory e : all) {
                JRadioButton btn = new JRadioButton(e.name());
                if (first) {
                    first = false;
                    lbl.setLabelFor(btn);
                }
                c.gridy++;
                pnl.add(btn, c);
                btn.addActionListener(evt -> {
                    result[0] = e;
                    al.actionPerformed(evt);
                });
            }
            DialogDescriptor desc = new DialogDescriptor(pnl, "EDITOR_CHOICE", true, NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.CANCEL_OPTION, evt -> {
            });
            dlg[0] = desc;
            desc.setValid(false);
            if (NotifyDescriptor.OK_OPTION.equals(DialogDisplayer.getDefault().notify(desc)) && result[0] != null) {
                return result[0];
            }
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return !Lookup.getDefault().lookupAll(
                ImageEditorFactory.class).isEmpty();
    }

    public void performAction() {
        ImageEditorFactory f = findFactory();
        if (f == null) {
            return;
        }
        Dimension last = loadLastSize();
        final ImageSizePanel pnl = new ImageSizePanel(f.supportsBackgroundStyles(), false, last);
        String ttl = NbBundle.getMessage(ResizeAction.class, "TTL_NewImage");
        //This code really should use DialogDisplayer, but is not due
        //to a bug in the window system
        int result = JOptionPane.showOptionDialog(Frame.getFrames()[0], pnl,
                ttl, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, null, null);
        if (result == JOptionPane.OK_OPTION) {
            Dimension d = pnl.getDimension();
            if (d.width == 0 || d.height == 0) {
                return;
            }
            f.openNew(d, pnl.getBackgroundStyle());
            if (d.width != last.width || d.height != last.height) {
                saveLastSize(d);
            }
        }
    }

    private Dimension loadLastSize() {
        Preferences prefs = NbPreferences.forModule(NewCanvasAction.class);
        int w = prefs.getInt("editor-width", 1280);
        int h = prefs.getInt("editor-height", 1024);
        return new Dimension(w, h);
    }

    private void saveLastSize(Dimension dim) {
        Preferences prefs = NbPreferences.forModule(NewCanvasAction.class);
        prefs.putInt("editor-width", Math.max(8, dim.width));
        prefs.putInt("editor-height", Math.max(8, dim.height));
    }

    public String getName() {
        return NbBundle.getMessage(NewCanvasAction.class, "ACT_NewImage");
    }

    public String iconResource() {
        return null;
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    protected boolean asynchronous() {
        return false;
    }

    static class Dummy extends ImageEditorFactory {

        public Dummy() {
            super("No image editor factories installed");
        }

        @Override
        public void openNew(Dimension dim, BackgroundStyle bg) {
        }

        @Override
        public boolean openExisting(File file) {
            return false;
        }

        @Override
        public boolean canOpen(File file) {
            return false;
        }
    }
}
