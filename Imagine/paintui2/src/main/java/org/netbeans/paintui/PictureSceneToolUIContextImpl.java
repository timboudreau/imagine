/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paintui;

import java.awt.Rectangle;
import javax.swing.JComponent;
import net.dev.java.imagine.api.tool.ToolUIContextImplementation;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.Zoom;

/**
 *
 * @author Tim Boudreau
 */
class PictureSceneToolUIContextImpl implements ToolUIContextImplementation {

    private final PictureScene scene;

    public PictureSceneToolUIContextImpl(PictureScene scene) {
        this.scene = scene;
    }

    @Override
    public Zoom zoom() {
        return scene.zoom();
    }

    @Override
    public AspectRatio aspectRatio() {
        return scene.aspectRatio();
    }

    @Override
    public void fetchVisibleBounds(Rectangle into) {
        JComponent view = scene.getView();
        if (view == null) {
            into.x = into.y = into.width = into.height = 0;
            return;
        }
        Rectangle r = view.getVisibleRect();
        into.setFrame(scene.convertViewToScene(r));
    }

}
