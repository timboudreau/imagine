package org.imagine.inspectors.spi;

import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
public final class Inspectors {

    public static TopComponent openUI(boolean ensureVisible) {
        TopComponent tc = findComponent();
        if (tc != null) {
            if (!tc.isOpened()) {
                tc.open();
            }
            if (ensureVisible) {
                tc.requestVisible();
            }
        }
        return tc;
    }

    public static void closeUI() {
        TopComponent tc = findComponent();
        if (tc != null && tc.isOpened()) {
            tc.close();
        }
    }

    private static TopComponent findComponent() {
        TopComponent tc = WindowManager.getDefault().findTopComponent("inspectors");
        return tc;
    }
}
