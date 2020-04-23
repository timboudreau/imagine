/*
 *                 Sun Public License Notice
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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileFilter;
import net.java.dev.imagine.ui.common.ImageEditorFactory;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Timothy Boudreau
 */
public class OpenFileAction extends AbstractAction {

    private static final String ICON_BASE
            = "net/java/dev/imagine/ui/actions/openFile24.png"; //NOI18N

    public OpenFileAction() {
        putValue(Action.NAME, NbBundle.getMessage(OpenFileAction.class,
                "ACT_Open")); //NOI18N
        Icon ic = new ImageIcon(ImageUtilities.loadImage(ICON_BASE));
        putValue(Action.SMALL_ICON, ic);
    }

    @Override
    public boolean isEnabled() {
        return Lookup.getDefault()
                .lookupItem(new Lookup.Template<>(ImageEditorFactory.class)) != null;
    }

    public void actionPerformed(ActionEvent e) {
        File[] f = new FileChooserBuilder(OpenFileAction.class)
                .setFileFilter(new FF())
                .setFilesOnly(true)
                .setFileHiding(true)
                .setTitle(NbBundle.getMessage(OpenFileAction.class,
                        "TTL_OpenDlg"))
                .showMultiOpenDialog();
        if (f != null && f.length > 0) {
            if (f.length == 1) {
                for (ImageEditorFactory factory : Lookup.getDefault().lookupAll(ImageEditorFactory.class)) {
                    if (factory.canOpen(f[0])) {
                        factory.openExisting(f[0]);
                    }
                }
            } else {
                Map<ImageEditorFactory, List<File>> m = new HashMap<>();
                for (File file : f) {
                    for (ImageEditorFactory factory : Lookup.getDefault().lookupAll(ImageEditorFactory.class)) {
                        if (factory.canOpen(file)) {
                            List<File> files = m.get(factory);
                            if (files == null) {
                                files = new ArrayList<>(f.length);
                                m.put(factory, files);
                            }
                            files.add(file);
                        }
                    }
                }
                for (Map.Entry<ImageEditorFactory, List<File>> en : m.entrySet()) {
                    en.getKey().openMany(f, (unopened) -> {
                        StringBuilder sb = new StringBuilder();
                        for (File un : unopened) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(un.getName());
                        }
                        StatusDisplayer.getDefault().setStatusText(
                                NbBundle.getMessage(OpenFileAction.class, "UNABLE_TO_OPEN", sb));
                    });
                }
            }
        }
    }

    private static class FF extends FileFilter {

        final Collection<? extends ImageEditorFactory> all
                = Lookup.getDefault().lookupAll(ImageEditorFactory.class);

        FF() {
        }

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            for (ImageEditorFactory factory : all) {
                if (factory.canOpen(f)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return NbBundle.getMessage(FF.class, "LBL_ImageFileFormats"); //NOI18N
        }
    }

}
