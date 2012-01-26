package net.java.dev.imagine.api.customizers;

import java.awt.BasicStroke;
import java.util.Collection;
import java.util.Collections;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.properties.EnumPropertyID;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.api.properties.PropertyID;
import net.java.dev.imagine.api.properties.preferences.PreferencesFactory;

/**
 *
 * @author Tim Boudreau
 */

final class StrokeToolProperty<R extends Enum<R>> implements Property<BasicStroke> {
    private final EnumPropertyID<BasicStroke, R> id;
    private final Property<Float> widthProp;
    public StrokeToolProperty(R name) {
        id = new EnumPropertyID<BasicStroke, R>(name, BasicStroke.class);
        PropertyID<Float> subId = id.subId(StrokeValues.WIDTH, Float.TYPE);
        widthProp = PreferencesFactory.createPreferencesFactory(subId).createProperty(1.0F);
    }

    @Override
    public EnumPropertyID<BasicStroke, R> id() {
        return id;
    }

    @Override
    public Class<BasicStroke> type() {
        return id.type();
    }

    @Override
    public String getDisplayName() {
        return id.getDisplayName();
    }

    @Override
    public BasicStroke get() {
        float w = widthProp.get();
        return new BasicStroke(w);
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        widthProp.addChangeListener(cl);
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        widthProp.removeChangeListener(cl);
    }

    @Override
    public boolean set(BasicStroke value) {
        float w = value.getLineWidth();
        return widthProp.set(w);
    }



//    @Override
//    public BasicStroke get() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public boolean set(BasicStroke value) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    public StrokeToolProperty(PropertyID<BasicStroke> id, R getter, Object... contents) {
//        super(id, getter, contents);
//    }
//
//    @Override
//    public BasicStroke get() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public boolean set(BasicStroke value) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    public StrokeToolProperty(R name) {
//        super(name, BasicStroke.class);
//    }
//
//    @Override
//    protected BasicStroke load() {
//        float f = getPreferences().getFloat(name().name() + "_width", 1.0F);
//        return new BasicStroke(f);
//    }
//
//    @Override
//    protected void save(BasicStroke t) {
//        getPreferences().putFloat(name().name() + "_width", t.getLineWidth());
//    }
    
    static enum StrokeValues {
        WIDTH
    }
    
}
