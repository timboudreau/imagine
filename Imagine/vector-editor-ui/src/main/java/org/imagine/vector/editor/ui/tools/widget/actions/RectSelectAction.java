/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.LinkedHashSet;
import java.util.Set;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.MutableProxyLookup;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.RectangularSelectDecorator;
import org.netbeans.api.visual.action.RectangularSelectProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
public class RectSelectAction implements RectangularSelectProvider, RectangularSelectDecorator {

    private final MutableProxyLookup lookup;
    private final ShapesCollection shapes;
    private final Scene scene;

    public RectSelectAction(MutableProxyLookup lookup, ShapesCollection shapes, Scene scene) {
        this.lookup = lookup;
        this.shapes = shapes;
        this.scene = scene;
    }

    public static WidgetAction createAction(LayerWidget selectionLayer, MutableProxyLookup lookup, ShapesCollection shapes, Scene scene) {
        RectSelectAction sel = new RectSelectAction(lookup, shapes, scene);
        return ActionFactory.createRectangularSelectAction(sel, selectionLayer, sel);
    }

    @Override
    public void performSelection(Rectangle sceneSelection) {
        Set<Object> selected = new LinkedHashSet<>();
        selected.add(shapes);
        for (ShapeElement el : shapes) {
            if (sceneSelection.contains(el.getBounds())) {
                selected.add(el);
            }
        }
        lookup.updateLookups(Lookups.fixed(selected.toArray()));
    }

    @Override
    public Widget createSelectionWidget() {
        return new SelectionShower(scene);
    }

    static class SelectionShower extends Widget {

        public SelectionShower(Scene scene) {
            super(scene);
        }

        @Override
        protected void paintWidget() {
            Graphics2D g = getGraphics();
            g.setColor(new Color(220, 220, 220, 220));
            Rectangle r = getBounds();
            g.fill(r);
            g.setColor(new Color(80, 80, 80, 220));
            g.draw(r);
        }

    }

}
