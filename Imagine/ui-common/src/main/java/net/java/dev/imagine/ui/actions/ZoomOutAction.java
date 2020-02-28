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
import javax.swing.ImageIcon;
import static net.java.dev.imagine.ui.actions.ZoomInAction.MAX_ZOOM;
import static net.java.dev.imagine.ui.actions.ZoomInAction.prev;
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
            = "net/java/dev/imagine/ui/actions/zoomOut.png";

    public ZoomOutAction() {
        super(Utilities.actionsGlobalContext(), Zoom.class); //NOI18N
        setIcon(new ImageIcon(
                ImageUtilities.loadImage(ICON_BASE)));
        putValue(Action.NAME, NbBundle.getMessage(ZoomOutAction.class,
                "ACT_ZoomOut"));
    }

    public ZoomOutAction(Lookup lookup) {
        super(lookup);
    }

    private float nextZoom(float zoom) {
        return prev(zoom);
    }

    @Override
    public void performAction(Zoom zoom) {
        assert zoom != null;
        float f = zoom.getZoom();
        zoom.setZoom(nextZoom(f));
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
        return canZoom(zoom);
    }

    private boolean canZoom(Zoom zoom) {
        return zoom.getZoom() < MAX_ZOOM;
    }

}
