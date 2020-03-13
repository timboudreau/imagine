/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.spi;

import java.awt.Point;
import java.util.function.Consumer;
import org.imagine.editor.api.Zoom;
import org.netbeans.paintui.widgetlayers.SetterResult;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public interface LayerRenderingWidget {

    SetterResult setOpacity(double opacity);

    SetterResult setLocation(Point location);

    void setZoom(Zoom zoom);

    default void setLookupConsumer(Consumer<Lookup[]> additionaLookupConsumer) {
        // do nothing
    }
}
