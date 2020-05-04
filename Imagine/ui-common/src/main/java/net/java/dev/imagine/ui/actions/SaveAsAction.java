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
 * Microsystems, Inc. All Rights Reserved. */
package net.java.dev.imagine.ui.actions;

import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.netbeans.paint.api.editor.IO;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

public class SaveAsAction extends GenericContextSensitiveAction<IO> {

    public SaveAsAction() {
        super("ACT_SaveAs", IO.class);
        setIcon(
                ImageUtilities.loadImage(
                        "net/java/dev/imagine/ui/actions/save24.png")); //NOI18N
    }

    public SaveAsAction(Lookup lookup) {
        super(lookup);
    }

    public void performAction(IO io) {
        io.saveAs((thrown, path) -> {
            if (thrown != null) {
                Exceptions.printStackTrace(thrown);
            }
        });
    }
}
