package org.netbeans.paintui;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import org.imagine.editor.api.Zoom;
import org.netbeans.api.visual.widget.Scene;

/**
 *
 * @author Tim Boudreau
 */
final class InnerPanel extends JComponent implements Scene.SceneListener {

    private final JComponent inner;
    private Dimension prefSize;
    private final PictureScene scene;
    private final Zoom zoom;

    InnerPanel(JComponent inner, PictureScene scene, Zoom zoom) {
        this.inner = inner;
        add(inner);
        setBorder(BorderFactory.createEmptyBorder());
        this.scene = scene;
        this.zoom = zoom;
    }

    @Override
    public Dimension getPreferredSize() {
        return prefSize == null ? inner.getPreferredSize() : new Dimension(prefSize);
        //            return inner.getPreferredSize();
    }
    private boolean pristine = true;

    private void ensureZoom() {
        if (pristine) {
            Rectangle r = scene.getBounds();
            if (r == null) {
                scene.validate();
                r = scene.getBounds();
            }
            if (r == null) {
                return;
            }
            // If we are opening an extremely small or large image,
            // adjust the zoom level to fit the image within the visible area
            Dimension pictureSize = r.getSize();
            Dimension currentSize = getParent().getSize();
            System.out.println("\n\nPristine validate " + currentSize + " and " + pictureSize);
            if (currentSize.width != 0 && currentSize.height != 0 && pictureSize.width != 0 && pictureSize.height != 0) {
                pristine = false;
                int myArea = currentSize.width * currentSize.height;
                int pictureArea = pictureSize.width * pictureSize.height;
                if (pictureArea < myArea / 4) {
                    if (pictureSize.width > pictureSize.height) {
                        double widthFactor = (double) currentSize.width / pictureSize.width;
                        zoom.setZoom((float) widthFactor);
                        System.out.println("\n\nSCALE TO WIDTH " + widthFactor + "\n\n");
                    } else {
                        double heightFactor = (double) currentSize.height / pictureSize.height;
                        zoom.setZoom((float) heightFactor);
                        System.out.println("\n\nSCALE TO HEIGHT " + heightFactor + "\n\n");
                    }
                }
            }
        }
    }

    @Override
    public void doLayout() {
        ensureZoom();
        Dimension d = inner.getPreferredSize();
        int offX = 0;
        int offY = 0;
        if (d.width < getWidth()) {
            offX = (getWidth() - d.width) / 2;
        }
        if (d.height < getHeight()) {
            offY = (getHeight() - d.height) / 2;
        }
        inner.setBounds(offX, offY, d.width, d.height);
    }

    @Override
    public void sceneRepaint() {
    }

    @Override
    public void sceneValidating() {
    }

    @Override
    public void sceneValidated() {
        prefSize = inner.getPreferredSize();
    }

}
