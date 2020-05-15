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

import java.util.Collection;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.imagine.editor.api.Zoom;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Timothy Boudreau
 */
public class ZoomInAction extends GenericContextSensitiveAction<Zoom> {

    public ZoomInAction() {
        super(Utilities.actionsGlobalContext(), Zoom.class);
        init();
    }

    public ZoomInAction(Lookup lkp) {
        super(lkp);
        init();
    }

    private void init() {
        setIcon(ImageUtilities.loadImage("net/java/dev/imagine/ui/actions/zoom-in.svg"));//NOI18N
        putValue(NAME, NbBundle.getMessage(ZoomInAction.class, "ACT_ZoomIn")); //NOI18N
    }

    @Override
    protected <T> boolean checkEnabled(Collection<? extends T> coll, Class<T> clazz) {
        if (coll.isEmpty()) {
            return false;
        }
        if (clazz != Zoom.class) {
            return true;
        }
        Zoom zoom = (Zoom) coll.iterator().next();
        return zoom.canZoomIn();
    }

    @Override
    public void performAction(Zoom zoom) {
        if (zoom == null) {
            return;
        }
        zoom.zoomIn();
    }
}
