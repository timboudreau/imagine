package org.netbeans.paintui.widgetlayers;

import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapPointsSupplier;

/**
 *
 * @author Tim Boudreau
 */
public interface WidgetController {

    Zoom getZoom();

    SnapPointsSupplier snapPoints();
}
