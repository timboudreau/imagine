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
import java.util.Collection;
import javax.swing.JDialog;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.ui.actions.spi.Resizable;
import net.java.dev.imagine.ui.components.ResizePicturePanel;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.netbeans.paint.api.components.dialog.DialogBuilder;
import org.netbeans.paint.api.components.dialog.DialogController;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author Timothy Boudreau
 */
public class ResizeAction extends GenericContextSensitiveAction<Picture> {

    public ResizeAction() {
        super(Utilities.actionsGlobalContext(), Picture.class);
        putValue(NAME, NbBundle.getMessage(ResizeAction.class, "ACT_Resize")); //NOI18N
    }

    public ResizeAction(Lookup lookup) {
        super(lookup);
        putValue(NAME, NbBundle.getMessage(ResizeAction.class, "ACT_Resize")); //NOI18N
    }

    @Override
    protected <T> boolean checkEnabled(Collection<? extends T> coll, Class<T> clazz) {
        boolean result = super.checkEnabled(coll, clazz);
        if (result) {
            result = !lookup.lookupResult(Resizable.class).allItems().isEmpty();
        }
        return result;
    }


    @Override
    public void performAction(Picture target) {
        boolean[] resizeCanvas = new boolean[1];
        Dimension newSize = DialogBuilder.forName(NbBundle.getMessage(ResizeAction.class,
                "TTL_Resize")) //NOI18N
                .okCancel()
                .ownedBy(WindowManager.getDefault().getMainWindow())
                .forComponent(() -> new ResizePicturePanel(target))
                .onShowOrHideDialog((boolean hide, ResizePicturePanel component, String key, DialogController ctrllr, JDialog dlg) -> {
                    if (!hide) {
                        component.addChangeListener(evt -> {
                            ctrllr.setValidity(!component.getDimension().equals(target.getSize()));
                        });
                    } else {
                        ctrllr.setValidity(!component.getDimension().equals(target.getSize()));
                    }
                })
                .openDialog(p -> {
                    resizeCanvas[0] = p.isResizeCanvasOnly();
                    return p.getDimension();
                });
        System.out.println("Resize to " + newSize);
        if (newSize != null) {
            for (Resizable resz : lookup.lookupAll(Resizable.class)) {
                System.out.println("Resize " + resz + " to " + newSize);
                resz.resizePicture(newSize.width, newSize.height, resizeCanvas[0]);
            }
        } else {
            System.out.println("Dlog must have been cancelled");
        }
    }
}
