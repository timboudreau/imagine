package net.java.dev.imagine.api.customizers;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.properties.ComposedProperty;
import net.java.dev.imagine.api.properties.EnumPropertyID;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.api.properties.PropertyID;
import net.java.dev.imagine.api.properties.preferences.PreferencesFactory;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
final class ColorToolProperty<R extends Enum<R>> implements Property<Color, EnumPropertyID<Color, R>> {

    private final ChangeSupport supp = new ChangeSupport(this);
    private final EnumPropertyID<Color, R> id;
    private final Property<Integer, ?> red;
    private final Property<Integer, ?> green;
    private final Property<Integer, ?> blue;
    private final Property<Integer, ?> alpha;

    public ColorToolProperty(R name) {
        this(name, null);
    }

    public ColorToolProperty(R name, Color defaultValue) {
        if (defaultValue == null) {
            defaultValue = Color.BLACK;
        }
        this.id = new EnumPropertyID<Color, R>(name, Color.class);
        PropertyID<Integer> redId = id.subId(Components.RED, Integer.class);
        PropertyID<Integer> greenId = id.subId(Components.GREEN, Integer.class);
        PropertyID<Integer> blueId = id.subId(Components.BLUE, Integer.class);
        PropertyID<Integer> alphaId = id.subId(Components.ALPHA, Integer.class);
        PreferencesFactory<Integer> redFactory = PreferencesFactory.createPreferencesFactory(redId);
        PreferencesFactory<Integer> greenFactory = PreferencesFactory.createPreferencesFactory(greenId);
        PreferencesFactory<Integer> blueFactory = PreferencesFactory.createPreferencesFactory(blueId);
        PreferencesFactory<Integer> alphaFactory = PreferencesFactory.createPreferencesFactory(alphaId);
        red = ComposedProperty.createBounded(redFactory.createGetter(defaultValue.getRed()), redFactory.createSetter(), redId, 0, 255);
        green = ComposedProperty.createBounded(greenFactory.createGetter(defaultValue.getGreen()), greenFactory.createSetter(), greenId, 0, 255);
        blue = ComposedProperty.createBounded(blueFactory.createGetter(defaultValue.getBlue()), blueFactory.createSetter(), blueId, 0, 255);
        alpha = ComposedProperty.createBounded(alphaFactory.createGetter(defaultValue.getAlpha()), alphaFactory.createSetter(), alphaId, 0, 255);
    }

    @Override
    public EnumPropertyID<Color, R> id() {
        return id;
    }

    @Override
    public Class<Color> type() {
        return id.type();
    }

    @Override
    public String getDisplayName() {
        return id.getDisplayName();
    }

    @Override
    public Color get() {
        int r = red.get();
        int g = green.get();
        int b = blue.get();
        int a = alpha.get();
        return new Color(r, g, b, a);
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        boolean had = supp.hasListeners();
        supp.addChangeListener(cl);
        if (!had) {
            addNotify();
        }
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        supp.removeChangeListener(cl);
        if (!supp.hasListeners()) {
            removeNotify();
        }
    }

    @Override
    public boolean set(Color value) {
        red.set(value.getRed());
        green.set(value.getGreen());
        blue.set(value.getBlue());
        alpha.set(value.getAlpha());
        return true;
    }

    @Override
    public <R> R get(Class<R> type) {
        return null;
    }

    @Override
    public <R> Collection<? extends R> getAll(Class<R> type) {
        if (Property.class == type) {
            return (Collection<? extends R>) Arrays.asList(red, green, blue, alpha);
        }
        return Collections.<R>emptySet();
    }

    private void removeNotify() {
    }

    private void addNotify() {
    }

    static enum Components {

        RED, GREEN, BLUE, ALPHA
    }
    /*
    private final Color initialValue;
    
    public ColorToolProperty(R name, Color initialValue) {
    super(name, Color.class);
    this.initialValue = initialValue == null ? Color.BLUE : initialValue;
    }
    
    @Override
    protected Color load() {
    String nm = name().name();
    Preferences p = getPreferences();
    int rgb = p.getInt(nm, initialValue.getRGB());
    //        int r = p.getInt(nm + "_red", initialValue.getRed());
    //        int g = p.getInt(nm + "_green", initialValue.getGreen());
    //        int b = p.getInt(nm + "_blue", initialValue.getBlue());
    //        int a = p.getInt(nm + "_alpha", initialValue.getAlpha());
    return new Color(rgb);
    }
    
    @Override
    protected void save(Color t) {
    String nm = name().name();
    Preferences p = getPreferences();
    
    //        p.putInt(nm + "_red", t.getRed());
    //        p.putInt(nm + "_green", t.getGreen());
    //        p.putInt(nm + "_blue", t.getBlue());
    //        p.putInt(nm + "_alpha", t.getAlpha());
    p.putInt(nm, t.getRGB());
    }
    
    class ComponentSubProp extends ToolProperty<Integer, ToolProperties.ColorComponents> implements ChangeListener {
    private final ColorComponents comp;
    ComponentSubProp(ColorComponents comp) {
    this.comp = comp;
    }
    
    private final ChangeSupport supp = new ChangeSupport(this);
    
    @Override
    public void addChangeListener(ChangeListener listener) {
    boolean hadListeners = supp.hasListeners();
    supp.addChangeListener(listener);
    if (!hadListeners) {
    ColorToolProperty.this.addChangeListener(this);
    }
    }
    
    @Override
    public void removeChangeListener(ChangeListener listener) {
    supp.removeChangeListener(listener);
    if (!supp.hasListeners()) {
    ColorToolProperty.this.removeChangeListener(this);
    }
    }
    
    @Override
    public void stateChanged(ChangeEvent e) {
    supp.fireChange();
    }
    
    @Override
    public Integer get() {
    return comp.get(ColorToolProperty.this.get());
    }
    
    @Override
    public ColorComponents name() {
    return comp;
    }
    
    @Override
    public boolean set(Integer value) {
    Color nue = comp.convert(ColorToolProperty.this.get(), value);
    return ColorToolProperty.this.set(nue);
    }
    
    @Override
    public Class<Integer> type() {
    return Integer.TYPE;
    }
    }
     */
}
