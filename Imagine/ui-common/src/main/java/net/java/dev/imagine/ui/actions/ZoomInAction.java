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

import java.util.Arrays;
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
        return canZoom(zoom);
    }

    private boolean canZoom(Zoom zoom) {
        return zoom.getZoom() < MAX_ZOOM;
    }

    static float[] FRACTIONS = new float[]{
        0.05F, 0.10F, 0.15F, 0.20F, 0.25F,
        0.3F, 0.4F, 0.5F, 0.75F, 0.875F,
        1F, 1.25F, 1.5F, 1.75F, 2F, 3F,
        4F, 5F, 6F, 7F, 8F, 9F, 10F,
        20F, 30F, 40F, 50F, 75F, 100F
    };
    static final float MAX_ZOOM = 100F;
    static final float MIN_ZOOM = 0.005F;

    private static int index(float val) {
        int ix = Arrays.binarySearch(FRACTIONS, val);
        if (ix < 0) {
            for (int i = 1; i < FRACTIONS.length; i++) {
                float prev = FRACTIONS[i - 1];
                if (val < prev) {
                    return Integer.MIN_VALUE;
                }
                float curr = FRACTIONS[i];
                if (val > prev && val < curr) {
                    if (Math.abs(prev - val) < Math.abs(curr - val)) {
                        return i - 1;
                    } else {
                        return i;
                    }
                }
            }
        }
        if (ix < 0) {
            return val > FRACTIONS[FRACTIONS.length-1] ? Integer.MAX_VALUE :
                    Integer.MIN_VALUE;
        }
        return ix;
    }

    static float next(float val) {
        int ix = index(val);
        switch (ix) {
            case Integer.MAX_VALUE:
                return Math.min(MAX_ZOOM, val + (val * 0.25F));
            case Integer.MIN_VALUE:
                return FRACTIONS[0];
            default:
                if (ix >= FRACTIONS.length - 1) {
                    return val + (val * 0.25F);
                }
                return FRACTIONS[ix + 1];
        }
    }

    static float prev(float val) {
        int ix = index(val);
        switch (ix) {
            case Integer.MAX_VALUE:
                return val - 0.25F;
            case Integer.MIN_VALUE:
                return Math.max(val - 0.005F, MIN_ZOOM);
            default:
                if (ix <= 0) {
                    return val * 0.95F;
                }
                return FRACTIONS[ix - 1];
        }
    }

    private float nextZoom(float zoom) {
        return next(zoom);
    }

    @Override
    public void performAction(Zoom zoom) {
        assert zoom != null;
        float f = zoom.getZoom();
        zoom.setZoom(nextZoom(f));
    }
}
