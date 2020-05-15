/*
 * ZoomFullAction.java
 *
 * Created on November 20, 2006, 8:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.actions;

import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.imagine.editor.api.Zoom;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Tim
 */
public class ZoomFullAction extends GenericContextSensitiveAction<Zoom> {

    /**
     * Creates a new instance of ZoomFullAction
     */
    public ZoomFullAction() {
        super(Utilities.actionsGlobalContext(), Zoom.class);
        init();
    }

    public ZoomFullAction(Lookup lkp) {
        super(lkp, Zoom.class);
        init();
    }

    private void init() {
        setIcon(ImageUtilities.loadImageIcon(
                "net/java/dev/imagine/ui/actions/zoom-one-to-one.svg", false)); //NOI18N
        putValue(NAME, NbBundle.getMessage(ZoomInAction.class, "ACT_ZoomFull")); //NOI18N
    }

    protected void performAction(Zoom zoom) {
        if (zoom != null) {
            zoom.zoomOneToOne();
        }
    }
}
