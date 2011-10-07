package net.java.dev.imagine.api.customizers;

import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.properties.ComposedProperty;
import net.java.dev.imagine.api.properties.EnumPropertyID;
import net.java.dev.imagine.api.properties.Mutable;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.api.properties.PropertyID;
import net.java.dev.imagine.api.properties.Value;
import net.java.dev.imagine.api.properties.preferences.PreferencesFactory;

/**
 *
 * @author Tim Boudreau
 */
final class AffineTransformToolProperty<R extends Enum<R>> implements Property<AffineTransform, EnumPropertyID<AffineTransform, R>> {
    private final EnumPropertyID<AffineTransform, R> id;
    private final Map<Matrix, Property<Double, ?>> map = new EnumMap<Matrix, Property<Double,?>>(Matrix.class);

    public AffineTransformToolProperty(R name) {
        id = new EnumPropertyID<AffineTransform, R>(name, AffineTransform.class);
        for (Matrix m : Matrix.values()) {
            PropertyID<Double> p = id.subId(name, Double.class);
            PreferencesFactory<Double> f = PreferencesFactory.createPreferencesFactory(p);
            Value<Double> v = f.createGetter(0D);
            Mutable<Double> s = f.createSetter();
            Property<Double, ?> prop = ComposedProperty.create(v, s, p);
            map.put (m, prop);
        }
    }

    @Override
    public EnumPropertyID<AffineTransform, R> id() {
        return id;
    }

    @Override
    public Class<AffineTransform> type() {
        return AffineTransform.class;
    }

    @Override
    public String getDisplayName() {
        return id.getDisplayName();
    }

    @Override
    public AffineTransform get() {
        double[] d = new double[Matrix.values().length];
        for (int i = 0; i < d.length; i++) {
            Matrix m = Matrix.values()[i];
            Property<Double,?> p = map.get(m);
            d[i] = p.get();
        }
        return new AffineTransform(d);
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        //do nothing
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        //do nothing
    }

    @Override
    public boolean set(AffineTransform value) {
        //XXX check and fire changes
        double[] d = new double[6];
        value.getMatrix(d);
        for (int i = 0; i < d.length; i++) {
            Matrix m = Matrix.values()[i];
            Property<Double,?> p = map.get(m);
            p.set(d[i]);
        }
        return true;
    }

    @Override
    public <R> R get(Class<R> type) {
        return null;
    }

    @Override
    public <R> Collection<? extends R> getAll(Class<R> type) {
        if (Property.class == type) {
            return (Collection<? extends R>) map.values();
        }
        return Collections.<R>emptySet();
    }

//    @Override
//    protected AffineTransform load() {
//        double[] matrix = new double[6];
//        Preferences p = getPreferences();
//        for (int i = 0; i < matrix.length; i++) {
//            p.getDouble(name().name() + "_" + i, 0);
//        }
//        return new AffineTransform(matrix);
//    }
//
//    @Override
//    protected void save(AffineTransform t) {
//        if (t == null) {
//            t = AffineTransform.getTranslateInstance(0, 0);
//        }
//        double[] matrix = new double[6];
//        t.getMatrix(matrix);
//        Preferences p = getPreferences();
//        for (int i = 0; i < matrix.length; i++) {
//            p.putDouble(name().name() + "_" + i, matrix[i]);
//        }
//    }
    
    static enum Matrix {
        ZERO, ONE, TWO, THREE, FOUR, FIVE
    }
    
}
