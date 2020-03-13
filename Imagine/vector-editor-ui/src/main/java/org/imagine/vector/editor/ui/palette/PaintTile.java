package org.imagine.vector.editor.ui.palette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import javax.swing.BorderFactory;
import org.imagine.awt.GradientManager;
import org.imagine.awt.key.ColorKey;
import org.imagine.awt.key.PaintKey;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
final class PaintTile extends Tile<PaintKey<?>> {

    private final PaletteBackend<? extends PaintKey<?>> storage;

    public PaintTile(String name, PaletteBackend<? extends PaintKey<?>> storage) {
        super(name);
        this.storage = storage;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
    }

    @Override
    protected PaletteBackend<? extends PaintKey<?>> storage() {
        return storage;
    }

    static float[] scratchFloats = new float[4];

    @Override
    protected void paintContent(PaintKey<?> item, Graphics2D g, int x, int y, int w, int h) {
        g.setPaint(GradientManager.getDefault().findPaint(item));
        try {
            AffineTransform xform = transform().createInverse();
            float[] flts = scratchFloats;
            flts[0] = x;
            flts[1] = y;
            flts[2] = x + w;
            flts[3] = y + h;
            xform.transform(flts, 0, flts, 0, 2);
            g.fillRect((int) Math.floor(flts[0]),
                    (int) Math.floor(flts[1]),
                    (int) Math.ceil(flts[2] - flts[0]),
                    (int) Math.ceil(flts[3] - flts[1]));
        } catch (NoninvertibleTransformException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    @Override
    protected AffineTransform recomputeTransform(PaintKey<?> item, double x, double y, double w, double h) {
        if (item instanceof ColorKey) {
            return null;
        }
        // XXX could get the AspectRatio instance from the current or last
        // editor and use that to scale the paint as it would look in context
        double baseWidth = 640;
        double baseHeight = 480;
        AffineTransform result = AffineTransform.getScaleInstance(w / baseWidth, h / baseHeight);
        result.concatenate(AffineTransform.getTranslateInstance(x, y));
        return result;
    }
}
