/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.spi;

import java.util.function.BiFunction;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.WidgetController;

/**
 *
 * @author Tim Boudreau
 */
public interface WidgetSupplier extends BiFunction<Scene, WidgetController, Widget> {

    default boolean takesOverPaintingScene() {
        return true;
    }
}
