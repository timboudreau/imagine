package net.java.dev.imagine.api.toolcustomizers;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.prefs.Preferences;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.toolcustomizers.BooleanCustomizer;
import net.java.dev.imagine.toolcustomizers.ColorCustomizer;
import net.java.dev.imagine.toolcustomizers.EnumCustomizer;
import net.java.dev.imagine.toolcustomizers.FontCustomizer;
import net.java.dev.imagine.toolcustomizers.TextCustomizer;
import org.netbeans.paint.api.components.LDPLayout;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 * General handler for getting customizers for specific types. Basic supported
 * types are Boolean, String, Font, all Number types. Enums are supported in a
 * generic way by providing a customizer with a combobox containing all of the
 * enum instances (FontStyles is an example; any such enum type should override
 * <code>toString()</code> to return a localized name.
 * <p/>
 * <b>General usage:</b>
 * <br/>
 * Getting a customizer for a number type:
 * <pre>
 * Customizer&lt;Float&gt; strokeCustomizer = Customizers.getCustomizer(Float.class, Constants.STROKE, 1F, 24F);
 * </pre> Getting a customizer for a boolean:
 * <pre>
 * Customizer&lt;Boolean&gt; fillCustoizer = Customizers.getCustomizer(Boolean.class, Constants.FILL);
 * </pre> For types other than the ones mentioned above, modules can register
 * instances of CustomizerFactory in the default lookup.
 *
 * @author Tim Boudreau
 */
public final class Customizers {

    private Customizers() {
    }

    private static final Map<Class, Map<String, Customizer>> map = new HashMap<Class, Map<String, Customizer>>();

    /**
     * Get a customizer for a number type. The type may be anything that extends
     * java.lang.Number, e.g. Integer, Long, Short, Byte, Float, Double
     *
     * @param type The number type
     * @param name The name of the customizer - this is used both for title
     * (i.e. it should be a localized string) and to look up the last known
     * value for this name/type combination in NbPreferences.
     * @param start The beginning of the range of numbers this customizer should
     * allow as values
     * @param end The end of the range of values this customizer should allow as
     * values
     * @return A customizer that can supply a component for editing the value of
     * this name+type combination.
     */
    public synchronized static <T extends Number> Customizer<T> getCustomizer(Class<T> type, String name, T start, T end) {
        return getCustomizer(type, name, start, end, start, null);
    }

    /**
     * Get a customizer for a number type. The type may be anything that extends
     * java.lang.Number, e.g. Integer, Long, Short, Byte, Float, Double
     *
     * @param type The number type
     * @param name The name of the customizer - this is used both for title
     * (i.e. it should be a localized string) and to look up the last known
     * value for this name/type combination in NbPreferences.
     * @param start The beginning of the range of numbers this customizer should
     * allow as values
     * @param end The end of the range of values this customizer should allow as
     * values
     * @param defaultValue The default value to use if none is known
     * @return A customizer that can supply a component for editing the value of
     * this name+type combination.
     */
    public synchronized static <T extends Number> Customizer<T> getCustomizer(Class<T> type, String name, T start, T end, T defaultValue) {
        return getCustomizer(type, name, start, end, defaultValue, null);
    }

    /**
     * Get a customizer for a number type. The type may be anything that extends
     * java.lang.Number, e.g. Integer, Long, Short, Byte, Float, Double
     *
     * @param type The number type
     * @param name The name of the customizer - this is used both for title
     * (i.e. it should be a localized string) and to look up the last known
     * value for this name/type combination in NbPreferences.
     * @param start The beginning of the range of numbers this customizer should
     * allow as values
     * @param end The end of the range of values this customizer should allow as
     * values
     * @param stringConvert Custom conversion of values to strings
     * @return A customizer that can supply a component for editing the value of
     * this name+type combination.
     */
    public synchronized static <T extends Number> Customizer<T> getCustomizer(Class<T> type, String name, T start, T end, Function<T, String> stringConvert) {
        return getCustomizer(type, name, start, end, start, stringConvert);
    }

    /**
     * Get a customizer for a number type. The type may be anything that extends
     * java.lang.Number, e.g. Integer, Long, Short, Byte, Float, Double
     *
     * @param type The number type
     * @param name The name of the customizer - this is used both for title
     * (i.e. it should be a localized string) and to look up the last known
     * value for this name/type combination in NbPreferences.
     * @param start The beginning of the range of numbers this customizer should
     * allow as values
     * @param end The end of the range of values this customizer should allow as
     * values
     * @param defaultValue The value to use if no saved value is present
     * @param stringConvert Custom conversion of values to strings
     * @return A customizer that can supply a component for editing the value of
     * this name+type combination.
     */
    public synchronized static <T extends Number> Customizer<T> getCustomizer(Class<T> type, String name, T start, T end, T defaultValue, Function<T, String> stringConvert) {
        assert EventQueue.isDispatchThread();
        assert type == Integer.class || type == Short.class || type == Long.class
                || type == Byte.class || type == Double.class || type == Float.class;
        if (start.doubleValue() > end.doubleValue()) {
            T hold = start;
            start = end;
            end = hold;
        }
        Map<String, Customizer> m = map.get(type);
        Customizer<T> result = null;
        if (m != null) {
            result = m.get(name);
        } else {
            m = new TimedExpirationMap<>();
            map.put(type, m);
        }
        if (result == null) {
            result = new NumberCustomizer2<>(type, name, start, end, defaultValue, stringConvert);
            m.put(name, result);
        }
        return result;
    }

    /**
     * Get a customizer for a particular type.
     *
     * @param type The type of object the customizer customizes
     * @param name The name of the customizer. This should be a <i>localized</i>
     * name that can be shown to the user in a window titlebar or similar.
     * @return A customizer, if the type is known, or if there is a
     * CustomizerFactory in the default lookup that can create customizers for
     * this type.
     */
    public synchronized static <T> Customizer<T> getCustomizer(Class<T> type, String name) {
        if (Number.class.isAssignableFrom(type)) {
            if (type == Float.class) {
                return (Customizer<T>) getCustomizer(Float.class, name, Float.MIN_VALUE, Float.MAX_VALUE - 1);
            } else if (type == Double.class) {
                return (Customizer<T>) getCustomizer(Double.class, name, Double.MIN_VALUE, Double.MAX_VALUE - 1);
            } else if (type == Integer.class) {
                return (Customizer<T>) getCustomizer(Integer.class, name, Integer.MIN_VALUE, Integer.MAX_VALUE - 1);
            } else if (type == Long.class) {
                return (Customizer<T>) getCustomizer(Long.class, name, Long.MIN_VALUE, Long.MAX_VALUE - 1);
            } else if (type == Short.class) {
                return (Customizer<T>) getCustomizer(Short.class, name, Short.MIN_VALUE, (short) (Short.MAX_VALUE - 1));
            } else if (type == Byte.class) {
                return (Customizer<T>) getCustomizer(Byte.class, name, Byte.MIN_VALUE, (byte) (Byte.MAX_VALUE - 1));
            }
        }
        if (Enum.class.isAssignableFrom(type)) {
            return (Customizer<T>) new OldEnumCustomizer(name, type);
        }
        Map<String, Customizer> m = map.get(type);
        boolean created = false;
        if (m == null) {
            m = new HashMap<String, Customizer>();
            map.put(type, m);
            created = true;
        }
//        Customizer<T> result = m.get(name);
        Customizer<T> result = null; //stealing componnents from other customizers
        if (result == null) {
            result = createCustomizer(type, name);
            created = true;
            if (result != null) {
                m.put(name, result);
            }
        }
        if (created) {
            map.put(type, m);
        }
        return result;
    }

    private static <T> Customizer<T> createCustomizer(Class<T> type, String name) {
        Customizer<T> result = null;
        if (Enum.class.isAssignableFrom(type)) {
            result = new EnumCustomizer(name, type);
        } else if (type == String.class) {
            result = (Customizer<T>) new TextCustomizer(name);
        } else if (type == Boolean.class) {
            result = (Customizer<T>) new BooleanCustomizer(name);
        } else if (type == Font.class) {
            result = (Customizer<T>) new FontCustomizer(name);
        } else if (type == Color.class) {
            result = (Customizer<T>) new ColorCustomizer(name);
        } else {
            CustomizerFactory fac = findFactory(type);
            if (fac != null) {
                result = fac.getCustomizer(type, name, new Object[0]);
            }
        }
        return result;
    }

    private static CustomizerFactory findFactory(Class<?> type) {
        Collection<? extends CustomizerFactory> facs = Lookup.getDefault().lookupAll(CustomizerFactory.class);
        for (CustomizerFactory f : facs) {
            if (f.supportsType(type)) {
                return f;
            }
        }
        return null;
    }

    private static final class OldEnumCustomizer<T extends Enum> implements Customizer<T>, ActionListener {

        private final Class<T> type;
        private final String name;
        private final ComboBoxModel mdl;

        OldEnumCustomizer(Class<T> type, String name) {
            this.type = type;
            this.name = name;
            mdl = new DefaultComboBoxModel(type.getEnumConstants());
        }

        OldEnumCustomizer(String name, Class<T> type) {
            this.type = type;
            this.name = name;
            mdl = new DefaultComboBoxModel(type.getEnumConstants());
        }

        public JComponent getComponent() {
            JPanel pnl = new JPanel(new LDPLayout());
            pnl.add(new JLabel(name));
            JComboBox box = new JComboBox();
            box.setModel(mdl);
            pnl.add(box);
            String selectionName = getPreferences().get(getKey(), null);
            if (selectionName != null) {
                for (T t : type.getEnumConstants()) {
                    if (selectionName.equals(t.name())) {
                        box.setSelectedItem(t);
                        break;
                    }
                }
            }
            box.addActionListener(this);
            return pnl;
        }

        public String getName() {
            return name;
        }

        public T get() {
            return (T) mdl.getSelectedItem();
        }

        public void actionPerformed(ActionEvent e) {
            JComboBox box = (JComboBox) e.getSource();
            T t = (T) box.getSelectedItem();
            String selectionName = t.name();
            getPreferences().put(getKey(), selectionName);
        }

        private String getKey() {
            String key = name + "." + type.getName();
            return key;
        }

        private Preferences getPreferences() {
            Preferences prefs = NbPreferences.forModule(OldEnumCustomizer.class);
            return prefs;
        }
    }
}
