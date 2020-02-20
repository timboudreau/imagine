/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.spi;

import java.awt.Point;
import java.util.function.Consumer;
import org.imagine.editor.api.Zoom;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.SetterResult;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class LayerRenderingWidget extends Widget {

    protected LayerRenderingWidget(Scene scene) {
        super(scene);
    }

    public abstract SetterResult setOpacity(double opacity);

    public abstract SetterResult setLocation(Point location);

    public abstract void setZoom(Zoom zoom);

    public void setLookupConsumer(Consumer<Lookup[]> additionaLookupConsumer) {
        // do nothing
    }
}
