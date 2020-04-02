/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget;

import java.awt.Rectangle;
import java.util.function.Supplier;
import javax.swing.JComponent;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.OnSnap;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.tools.widget.snap.SnapPainter;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class SnapDecorationsLayer2 extends LayerWidget {

    private SnapPainter painter;
    private final Supplier<Zoom> z;

    public SnapDecorationsLayer2(Scene scene, RepaintHandle handle, Lookup selection, Supplier<Zoom> zoomSupplier) {
        super(scene);
        this.z = zoomSupplier;
        painter = new SnapPainter(handle, new SceneBoundsSupplier(scene), selection, zoomSupplier);
    }

    OnSnap<ShapeSnapPointEntry> snapListener() {
        return painter;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    @Override
    protected void paintWidget() {
        painter.paint(getGraphics(), z.get());
    }

    private static class SceneBoundsSupplier implements Supplier<Rectangle> {

        private final Scene scene;

        public SceneBoundsSupplier(Scene scene) {
            this.scene = scene;
        }

        @Override
        public Rectangle get() {
            JComponent view = scene.getView();
            if (view == null) {
                return scene.getBounds();
            }
            Rectangle r = view.getVisibleRect();
            return scene.convertViewToScene(r);
        }
    }
}
