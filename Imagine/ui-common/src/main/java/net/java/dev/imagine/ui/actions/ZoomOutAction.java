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
import javax.swing.Action;
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
public class ZoomOutAction extends GenericContextSensitiveAction<Zoom> {

    private static final String ICON_BASE
            = "net/java/dev/imagine/ui/actions/zoom-out.svg";

    public ZoomOutAction() {
        super(Utilities.actionsGlobalContext(), Zoom.class); //NOI18N
        init();
    }

    public ZoomOutAction(Lookup lookup) {
        super(lookup);
        init();
    }

    private void init() {
        setIcon(
                ImageUtilities.loadImageIcon(ICON_BASE, false));
        putValue(Action.NAME, NbBundle.getMessage(ZoomOutAction.class,
                "ACT_ZoomOut"));
    }

    @Override
    public void performAction(Zoom zoom) {
        if (zoom == null) {
            return;
        }
        zoom.zoomOut();
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
        return zoom.canZoomOut();
    }

}
