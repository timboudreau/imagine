package net.java.dev.imagine.api.customizers;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.customizers.ToolProperties.FontProps;
import net.java.dev.imagine.api.customizers.ToolProperties.FontStyles;
import org.netbeans.paint.api.components.Fonts;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
final class FontToolProperty<R extends Enum> extends AbstractToolProperty<Font, R> implements Explicit<Font> {

    public FontToolProperty(R name) {
        super(name, Font.class);
    }

    @Override
    protected Font load() {
        return Fonts.getDefault().get(name().name());
    }

    @Override
    protected void save(Font f) {
        Fonts.getDefault().set(name().name(), f);
    }

    @Override
    public ToolProperty<?, ?>[] getSubProperties() {
        return new ToolProperty<?, ?>[]{
                    new FaceProp(), new StyleProp(), new SizeProp()
                };
    }

    @Override
    public Collection<Font> getValues() {
        List<Font> result = new ArrayList<Font>();
        for (String s : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            Font f = new Font(s, Font.PLAIN, 14);
            result.add(f);
        }
        return result;
    }

    class FaceProp extends ToolProperty<String, ToolProperties.FontProps> implements ChangeListener, Explicit<String> {

        @Override
        public String get() {
            return FontToolProperty.this.getName();
        }

        public FontProps name() {
            return ToolProperties.FontProps.FONT_FACE;
        }

        @Override
        public void set(String value) {
            Font f = FontToolProperty.this.get();
            int size = f.getSize();
            int style = f.getStyle();
            f = new Font(value, style, size);
            FontToolProperty.this.set(f);
        }

        @Override
        public Class<String> type() {
            return String.class;
        }

        @Override
        public Collection<String> getValues() {
            return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        }
        private final ChangeSupport supp = new ChangeSupport(this);

        @Override
        public void addChangeListener(ChangeListener listener) {
            boolean hadListeners = supp.hasListeners();
            supp.addChangeListener(listener);
            if (!hadListeners) {
                FontToolProperty.this.addChangeListener(this);
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            supp.removeChangeListener(listener);
            if (!supp.hasListeners()) {
                FontToolProperty.this.removeChangeListener(this);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            supp.fireChange();
        }
    }

    class StyleProp extends ToolProperty<ToolProperties.FontStyles, ToolProperties.FontProps> implements ChangeListener {

        private final ChangeSupport supp = new ChangeSupport(this);

        @Override
        public void addChangeListener(ChangeListener listener) {
            boolean hadListeners = supp.hasListeners();
            supp.addChangeListener(listener);
            if (!hadListeners) {
                FontToolProperty.this.addChangeListener(this);
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            supp.removeChangeListener(listener);
            if (!supp.hasListeners()) {
                FontToolProperty.this.removeChangeListener(this);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            supp.fireChange();
        }

        @Override
        public FontStyles get() {
            int constant = FontToolProperty.this.get().getStyle();
            return FontStyles.forConstant(constant);
        }

        @Override
        public FontProps name() {
            return FontProps.FONT_STYLE;
        }

        @Override
        public void set(FontStyles value) {
            Font f = FontToolProperty.this.get();
            int style = f.getStyle();
            int nue = value.getConstant();
            if (style != nue) {
                f = f.deriveFont(nue);
                FontToolProperty.this.set(f);
            }
        }

        @Override
        public Class<FontStyles> type() {
            return FontStyles.class;
        }
    }

    class SizeProp extends ToolProperty<Integer, ToolProperties.FontProps> implements Bounded<Integer>, ChangeListener {

        private final ChangeSupport supp = new ChangeSupport(this);

        @Override
        public void addChangeListener(ChangeListener listener) {
            boolean hadListeners = supp.hasListeners();
            supp.addChangeListener(listener);
            if (!hadListeners) {
                FontToolProperty.this.addChangeListener(this);
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            supp.removeChangeListener(listener);
            if (!supp.hasListeners()) {
                FontToolProperty.this.removeChangeListener(this);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            supp.fireChange();
        }

        @Override
        public Integer get() {
            return FontToolProperty.this.get().getSize();
        }

        @Override
        public FontProps name() {
            return ToolProperties.FontProps.FONT_SIZE;
        }

        @Override
        public void set(Integer value) {
            Font f = FontToolProperty.this.get();
            int sz = f.getSize();
            if (sz != value) {
                f = f.deriveFont(value.floatValue());
                FontToolProperty.this.set(f);
            }
        }

        @Override
        public Class<Integer> type() {
            return Integer.TYPE;
        }

        @Override
        public Integer getMinimum() {
            return 4;
        }

        @Override
        public Integer getMaximum() {
            return 120;
        }
    }
}
