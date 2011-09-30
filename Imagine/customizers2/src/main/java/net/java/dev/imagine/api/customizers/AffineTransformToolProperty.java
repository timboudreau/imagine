/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.customizers;

import java.awt.geom.AffineTransform;
import java.util.prefs.Preferences;

/**
 *
 * @author Tim Boudreau
 */
final class AffineTransformToolProperty<R extends Enum> extends AbstractToolProperty<AffineTransform, R> {

    public AffineTransformToolProperty(R name) {
        super(name, AffineTransform.class);
    }

    @Override
    protected AffineTransform load() {
        double[] matrix = new double[6];
        Preferences p = getPreferences();
        for (int i = 0; i < matrix.length; i++) {
            p.getDouble(name().name() + "_" + i, 0);
        }
        return new AffineTransform(matrix);
    }

    @Override
    protected void save(AffineTransform t) {
        if (t == null) {
            t = AffineTransform.getTranslateInstance(0, 0);
        }
        double[] matrix = new double[6];
        t.getMatrix(matrix);
        Preferences p = getPreferences();
        for (int i = 0; i < matrix.length; i++) {
            p.putDouble(name().name() + "_" + i, matrix[i]);
        }
    }
    
}
