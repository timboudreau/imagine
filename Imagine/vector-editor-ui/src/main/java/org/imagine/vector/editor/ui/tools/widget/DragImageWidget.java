package org.imagine.vector.editor.ui.tools.widget;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class DragImageWidget extends Widget {

    private final Image dragImage;

    DragImageWidget(Scene scene, Image img) {
        super(scene);
        this.dragImage = img;
    }

    @Override
    protected Rectangle calculateClientArea() {
        return new Rectangle(0, 0, dragImage.getWidth(null), dragImage.getHeight(null));
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();
        g.drawImage(dragImage, 0, 0, null);
    }

}
