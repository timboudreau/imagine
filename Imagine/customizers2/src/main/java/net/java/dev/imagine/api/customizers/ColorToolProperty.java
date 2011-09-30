package net.java.dev.imagine.api.customizers;

import java.awt.Color;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.customizers.ToolProperties.ColorComponents;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
final class ColorToolProperty<R extends Enum> extends AbstractToolProperty<Color, R> {
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
        public void set(Integer value) {
            comp.convert(ColorToolProperty.this.get(), value);
        }

        @Override
        public Class<Integer> type() {
            return Integer.TYPE;
        }
    }
}
