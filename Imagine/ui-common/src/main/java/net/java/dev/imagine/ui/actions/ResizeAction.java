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

import java.awt.Dimension;
import net.java.dev.imagine.ui.actions.spi.Resizable;
import net.java.dev.imagine.ui.components.ImageSizePanel;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Timothy Boudreau
 */
public class ResizeAction extends GenericContextSensitiveAction<Resizable> {

    public ResizeAction() {
        super(Utilities.actionsGlobalContext(), Resizable.class);
        putValue(NAME, NbBundle.getMessage(ResizeAction.class, "ACT_Resize")); //NOI18N
    }

    public ResizeAction(Lookup lookup) {
        super(lookup);
        putValue(NAME, NbBundle.getMessage(ResizeAction.class, "ACT_Resize")); //NOI18N
    }

    @Override
    public void performAction(Resizable target) {
        ImageSizePanel pnl = new ImageSizePanel(false, true);
        DialogDescriptor dd = new DialogDescriptor(pnl,
                NbBundle.getMessage(ResizeAction.class,
                        "TTL_Resize")); //NOI18N

        if (DialogDisplayer.getDefault().notify(dd)
                == DialogDescriptor.OK_OPTION) {

            Dimension d = pnl.getDimension();
            if (d == null || d.width <= 0 || d.height <= 0) {
                return;
            }
            target.resizePicture(d.width, d.height, pnl.isResizeCanvasOnly());
        }
    }
}
