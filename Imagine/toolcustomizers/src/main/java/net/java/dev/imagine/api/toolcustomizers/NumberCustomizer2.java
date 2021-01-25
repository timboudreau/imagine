/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.toolcustomizers;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.function.Function;
import java.util.prefs.Preferences;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.plaf.SliderUI;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizerSupport;
import org.netbeans.paint.api.components.LDPLayout;
import com.mastfrog.swing.slider.PopupSliderUI;
import com.mastfrog.swing.slider.RadialSliderUI;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import com.mastfrog.swing.slider.StringConverter;
import org.openide.awt.Mnemonics;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
final class NumberCustomizer2<T extends Number> extends ListenableCustomizerSupport<T>
        implements Customizer<T>, ListenableCustomizer<T> {

    private final Class<T> type;
    private final String name;
    private final JSlider slider = new JSlider();
    private final JLabel lbl = new JLabel();
    private final NumberConverter<T> conv;
    private final JPanel panel = new SharedLayoutPanel();
    NumberCustomizer2(Class<T> type, String name, T start, T end, T defaultValue, Function<T, String> stringConvert) {
        this(type, name, start, end, defaultValue, converter(type, start, end, stringConvert));
    }

    NumberCustomizer2(Class<T> type, String name, T start, T end, T defaultValue) {
        this(type, name, start, end, defaultValue, converter(type, start, end, null));
    }

    @Override
    public boolean isInUse() {
        return panel.isDisplayable();
    }

    private NumberCustomizer2(Class<T> type, String name, T start, T end, T defaultValue, NumberConverter<T> conv) {
        if (end.doubleValue() < start.doubleValue()) {
            T hold = start;
            start = end;
            end = hold;
        }
        this.type = type;
        this.name = name;
        this.conv = conv;
        Preferences prefs = NbPreferences.forModule(NumberCustomizer2.class);
        String prefsKeyBase = type.getSimpleName() + "_" + name;
        T lastValue = conv.load(prefs, prefsKeyBase, defaultValue, start);
        Mnemonics.setLocalizedText(lbl, name);
        lbl.setLabelFor(slider);
        slider.setOrientation(JSlider.HORIZONTAL);
        RadialSliderUI.setStringConverter(slider, conv);
        slider.setUI((SliderUI) PopupSliderUI.createUI(slider));
        slider.setModel(conv.createModel(start, end, lastValue));
        slider.addChangeListener(ce -> {
            T val = conv.fromInt(slider.getValue());
            conv.save(prefs, prefsKeyBase, val);
            fire();
        });
    }

    public static void main(String[] args) {
        NumberCustomizer2<Integer> ints = new NumberCustomizer2<>(Integer.class, "Ints", 1, 100, 50);
        NumberCustomizer2<Long> longs = new NumberCustomizer2<Long>(Long.class, "Longs", 100L, 1000L, 500L);
        NumberCustomizer2<Double> dbls = new NumberCustomizer2<>(Double.class, "Doubles", 1D, 1000D, 2.75D);
        NumberCustomizer2<Double> dblsScaled = new NumberCustomizer2(Double.class, "Doubles Scaled", 0D, 1D, 0.5D);
        NumberCustomizer2<Float> floats = new NumberCustomizer2<>(Float.class, "Floats", 1F, 1000F, 200F);
        NumberCustomizer2<Float> floatsScaled = new NumberCustomizer2<>(Float.class, "Floats Scaled", 0F, 3F, 1F);
//        JPanel pnl = new JPanel(new LDPLayout(5));
        JPanel pnl = new JPanel(new LDPLayout(5));
        pnl.setMaximumSize(new Dimension(300, 1000));
        for (NumberCustomizer2 n : new NumberCustomizer2[]{ints, longs, dbls, dblsScaled, floats, floatsScaled}) {
            pnl.add(n.getComponent());
        }
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(pnl);
        jf.pack();
        jf.setVisible(true);
    }

    private JPanel pnl;

    @Override
    public JComponent getComponent() {
        if (pnl != null) {
            return pnl;
        }
        panel.add(lbl);
        panel.add(slider);
        return pnl = panel;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T get() {
        return conv.toType(conv.fromInt(slider.getValue()));
    }

    @Override
    public String toString() {
        return name + "(" + type.getSimpleName() + ")";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.type);
        hash = 23 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NumberCustomizer2<?> other = (NumberCustomizer2<?>) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return true;
    }

    static <T extends Number> NumberConverter<T> converter(Class<T> type, T start, T end, Function<T, String> stringConvert) {
        NumberConverter<?> result = null;
        if (!isFloatingPoint(type)) {
            if (type == Integer.class || type == Integer.TYPE) {
                result = new IntConverter();
            } else if (type == Long.class || type == Long.TYPE) {
                result = new LongConverter();
            } else if (type == Short.class || type == Short.TYPE) {
                result = new ShortConverter();
            } else if (type == Byte.class || type == Byte.TYPE) {
                result = new ByteConverter();
            } else {
                throw new AssertionError("Unknown number type " + type);
            }
        } else {
            if (type == Double.class || type == Double.TYPE) {
                double st = start.doubleValue();
                double en = end.doubleValue();
                double factor;
                if (en - st < 10) {
                    factor = 100;
                } else if (en - st < 100) {
                    factor = 1000;
                } else {
                    factor = 1;
                }
                if (factor == 1) {
                    result = new DoubleConverter();
                } else {
                    result = new ScalingDoubleConverter(factor);
                }
            } else if (type == Float.class || type == Float.TYPE) {
                float st = start.floatValue();
                float en = end.floatValue();
                float factor;
                if (en - st <= 1) {
                    factor = 1000;
                } else if (en - st < 10) {
                    factor = 100;
                } else if (en - st < 100) {
                    factor = 10;
                } else {
                    factor = 1;
                }
                if (factor == 1) {
                    result = new FloatConverter();
                } else {
                    result = new ScalingFloatConverter(factor);
                }
            } else {
                throw new AssertionError("Unknown number type " + type);
            }
        }
        NumberConverter<T> res = result.cast(type);
        if (stringConvert != null) {
            res = new ConversionWrapper<>(stringConvert, res);
        }
        return res;
    }

    static boolean isFloatingPoint(Class<? extends Number> type) {
        return type == Float.class || type == Float.TYPE
                || type == Double.class || type == Double.TYPE;
    }

    interface NumberConverter<T extends Number> extends StringConverter {

        T fromInt(int val);

        T toType(Number num);

        default T load(Preferences prefs, String name, T defaultValue, T start) {
            if (defaultValue == null) {
                defaultValue = start;
            }
            int val = prefs.getInt("cust_" + name, toInt(toType(defaultValue)));
            return fromInt(val);
        }

        default void save(Preferences prefs, String name, T value) {
            if (value != null) {
                int val = toInt(toType(value));
                prefs.putInt("cust_" + name, val);
            }
        }

        default BoundedRangeModel createModel(T start, T end, T defaultValue) {
            // int value, int extent, int min, int max
            int val = toInt(toType(defaultValue));
            int ext = 1;
            int startInt = toInt(toType(start));
            int endInt = toInt(toType(end)) + 1;
            try {
                return new DefaultBoundedRangeModel(val, ext,
                        startInt, endInt);
            } catch (IllegalArgumentException ex) {
                String msg = "Start " + start + " (" + startInt + ") end "
                        + end + " (" + endInt + " initial value "
                        + defaultValue + " (" + val + ") extent " + ext;
                Exceptions.printStackTrace(new IllegalArgumentException(msg, ex));
                return new DefaultBoundedRangeModel();
            }
        }

        default <R extends Number> NumberConverter<R> cast(Class<R> r) {
            return (NumberConverter<R>) this;
        }

        default int toInt(T val) {
            return val.intValue();
        }

        @Override
        public default String valueToString(JSlider sl) {
            return valueToString(sl.getValue());
        }

        @Override
        public default int maxChars() {
            return 4;
        }

        @Override
        public default String valueToString(int val) {
            return Integer.toString(val);
        }
    }

    static class IntConverter implements NumberConverter<Integer> {

        @Override
        public Integer fromInt(int val) {
            return val;
        }

        @Override
        public Integer toType(Number num) {
            return num.intValue();
        }
    }

    static class LongConverter implements NumberConverter<Long> {

        @Override
        public Long fromInt(int val) {
            return (long) val;
        }

        @Override
        public Long toType(Number num) {
            return num.longValue();
        }

        @Override
        public int maxChars() {
            return 12;
        }
    }

    static class ShortConverter implements NumberConverter<Short> {

        @Override
        public Short fromInt(int val) {
            return (short) val;
        }

        @Override
        public Short toType(Number num) {
            return num.shortValue();
        }
    }

    static class ByteConverter implements NumberConverter<Byte> {

        @Override
        public Byte fromInt(int val) {
            return (byte) val;
        }

        @Override
        public int toInt(Byte val) {
            return val & 0xFF;
        }

        @Override
        public Byte toType(Number num) {
            return num.byteValue();
        }
    }

    private static final DecimalFormat FMT = new DecimalFormat("##0.0#");

    static class FloatConverter implements NumberConverter<Float> {

        @Override
        public String valueToString(int val) {
            return FMT.format(val);
        }

        @Override
        public Float fromInt(int val) {
            return (float) val;
        }

        @Override
        public int maxChars() {
            return FMT.getMaximumIntegerDigits() + FMT.getMaximumFractionDigits() + 1;
        }

        @Override
        public Float toType(Number num) {
            return num.floatValue();
        }
    }

    static class DoubleConverter implements NumberConverter<Double> {

        private final DecimalFormat fmt = new DecimalFormat("##0.0#");

        @Override
        public String valueToString(int val) {
            return fmt.format(val);
        }

        @Override
        public Double fromInt(int val) {
            return (double) val;
        }

        @Override
        public int maxChars() {
            return FMT.getMaximumIntegerDigits() + FMT.getMaximumFractionDigits() + 1;
        }

        @Override
        public Double toType(Number num) {
            return num.doubleValue();
        }
    }

    static class ScalingDoubleConverter implements NumberConverter<Double> {

        private final double factor;

        public ScalingDoubleConverter(double factor) {
            this.factor = factor;
        }

        @Override
        public Double toType(Number num) {
            return num.doubleValue();
        }

        @Override
        public Double fromInt(int val) {
            return (1D / factor) * val;
        }

        @Override
        public int toInt(Double val) {
            return (int) Math.round(factor * val);
        }

        @Override
        public int maxChars() {
            return FMT.getMaximumIntegerDigits()
                    + FMT.getMaximumFractionDigits() + 1;
        }

        @Override
        public String valueToString(int val) {
            return FMT.format(fromInt(val));
        }
    }

    static class ScalingFloatConverter implements NumberConverter<Float> {

        private final float factor;

        ScalingFloatConverter(float factor) {
            this.factor = factor;
        }

        @Override
        public Float fromInt(int val) {
            return (float) ((1D / factor) * val);
        }

        @Override
        public Float toType(Number num) {
            return num.floatValue();
        }

        @Override
        public int toInt(Float val) {
            return (int) Math.round(factor * val.floatValue());
        }

        @Override
        public int maxChars() {
            return FMT.getMaximumIntegerDigits() + FMT.getMaximumFractionDigits() + 1;
        }

        @Override
        public String valueToString(int val) {
            return FMT.format(fromInt(val));
        }
    }

    static class ConversionWrapper<T extends Number> implements NumberConverter<T> {

        private final Function<T, String> converter;
        private final NumberConverter<T> delegate;

        public ConversionWrapper(Function<T, String> converter, NumberConverter<T> delegate) {
            this.converter = converter;
            this.delegate = delegate;
        }

        @Override
        public String valueToString(int val) {
            return converter.apply(fromInt(val));
        }

        @Override
        public T fromInt(int val) {
            return delegate.fromInt(val);
        }

        @Override
        public T toType(Number num) {
            return delegate.toType(num);
        }

        @Override
        public T load(Preferences prefs, String name, T defaultValue, T start) {
            return delegate.load(prefs, name, defaultValue, start);
        }

        @Override
        public void save(Preferences prefs, String name, T value) {
            delegate.save(prefs, name, value);
        }

        @Override
        public BoundedRangeModel createModel(T start, T end, T defaultValue) {
            return delegate.createModel(start, end, defaultValue);
        }

        @Override
        public int toInt(T val) {
            return delegate.toInt(val);
        }

        @Override
        public String valueToString(JSlider sl) {
            return delegate.valueToString(sl);
        }

        @Override
        public int maxChars() {
            return delegate.maxChars();
        }

    }
}
