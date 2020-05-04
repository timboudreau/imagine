/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.actions;

import java.awt.geom.Dimension2D;
import javax.swing.Action;
import org.imagine.editor.api.ImageEditor;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class ZoomToFitAction extends GenericContextSensitiveAction<ImageEditor> {

    private static final String ICON_BASE
            = "net/java/dev/imagine/ui/actions/zoom-box.svg"; //NOI18N

    public ZoomToFitAction() {
        super(Utilities.actionsGlobalContext(), ImageEditor.class); //NOI18N
        init();
    }

    public ZoomToFitAction(Lookup lkp) {
        super(lkp, ImageEditor.class);
        init();
    }

    private void init() {
        setIcon(ImageUtilities.loadImageIcon(ICON_BASE, false));
        putValue(Action.NAME, NbBundle.getMessage(ZoomOutAction.class,
                "ZOOM_TO_FIT")); //NOI18N
    }

    @Override
    protected void performAction(ImageEditor t) {
        if (t == null) {
            return;
        }
        Dimension2D pictureSize = t.getPictureAspectRatio().toDimension();
        Dimension2D currentSize = t.getAvailableSize();
        if (currentSize.getWidth() != 0 && currentSize.getHeight() != 0
                && pictureSize.getWidth() != 0 && pictureSize.getHeight() != 0) {
            double myArea = currentSize.getWidth() * currentSize.getHeight();
            double pictureArea = pictureSize.getWidth() * pictureSize.getHeight();
            if (myArea == pictureArea) {
                return;
            }
            if (pictureSize.getWidth() > pictureSize.getHeight()) {
                double widthFactor = (double) currentSize.getWidth() / pictureSize.getWidth();
                t.getZoom().setZoom((float) widthFactor);
            } else {
                double heightFactor = (double) currentSize.getHeight() / pictureSize.getHeight();
                t.getZoom().setZoom((float) heightFactor);
            }
        }
    }
}
