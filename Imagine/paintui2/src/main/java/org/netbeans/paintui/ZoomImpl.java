package org.netbeans.paintui;

import java.awt.geom.AffineTransform;
import javax.swing.event.ChangeListener;
import org.imagine.editor.api.Zoom;
import org.imagine.geometry.util.PooledTransform;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
class ZoomImpl implements Zoom {

    private final ChangeSupport supp = new ChangeSupport(this);
    private final AffineTransform xform = PooledTransform.get(this);
    private final AffineTransform inverse = PooledTransform.get(this);
    private final PictureScene scene;

    public ZoomImpl(PictureScene scene) {
        this.scene = scene;
    }

    public float getZoom() {
        return (float) scene.getZoomFactor();
    }

    @Override
    public AffineTransform getZoomTransform() {
        double z = scene.getZoomFactor();
        xform.setToScale(z, z);
        return xform;
    }

    @Override
    public AffineTransform getInverseTransform() {
        double zz = scene.getZoomFactor();
        double z = zz == 0 ? 0.00000000000000000000001 : 1 / zz;
        inverse.setToScale(z, z);
        return inverse;
    }

    public void setZoom(float val) {
        if (val != getZoom()) {
            scene.setZoomFactor(val);
            supp.fireChange();
            scene.validate();
        }
    }

    public void addChangeListener(ChangeListener cl) {
        supp.addChangeListener(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        supp.removeChangeListener(cl);
    }

}
