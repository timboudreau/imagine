package net.java.dev.imagine.api.customizers;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.customizers.ToolProperties.FontStyles;
import net.java.dev.imagine.api.properties.ComposedProperty;
import net.java.dev.imagine.api.properties.EnumPropertyID;
import net.java.dev.imagine.api.properties.Explicit;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.api.properties.preferences.PreferencesFactory;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
final class FontToolProperty<R extends Enum<R>> implements Property<Font>, Explicit<Font> {
    
    private final EnumPropertyID<Font, R> id;
    private final Property<String> faceProp;
    private final Property<ToolProperties.FontStyles> styleProp;
    private final Property<Integer> sizeProp;

    public FontToolProperty(R name) {
        this.id = new EnumPropertyID(name, Font.class);
        faceProp = PreferencesFactory.createPreferencesFactory(id.subId(ToolProperties.FontProps.FONT_FACE, String.class)).createProperty("Times New Roman");
        styleProp = PreferencesFactory.createPreferencesFactory(id.subId(ToolProperties.FontProps.FONT_STYLE, FontStyles.class)).createProperty(FontStyles.PLAIN);
        sizeProp = ComposedProperty.createBounded(PreferencesFactory.createPreferencesFactory(id.subId(ToolProperties.FontProps.FONT_SIZE, Integer.TYPE)).createProperty(12), 4, 156);
    }

    @Override
    public EnumPropertyID<Font, R> id() {
        return id;
    }

    @Override
    public Class<Font> type() {
        return id.type();
    }

    @Override
    public String getDisplayName() {
        return id.getDisplayName();
    }

    @Override
    public Font get() {
        FontStyles style = styleProp.get();
        if (style == null) {
            style = FontStyles.PLAIN;
        }
        int size = sizeProp.get();
        String face = faceProp.get();
        return new Font(face, style.getConstant(), size);
    }

    @Override
    public boolean set(Font value) {
        FontStyles style = FontStyles.forConstant(value.getStyle());
        if (style == null) {
            style = FontStyles.PLAIN;
        }
        String face = value.getName();
        int size = value.getSize();
        boolean result = styleProp.set(style);
        result |= sizeProp.set(size);
        result |= faceProp.set(face);
        return result;
    }
    
    private final ChangeSupport supp = new ChangeSupport(this);
    
    @Override
    public void addChangeListener(ChangeListener cl) {
        supp.addChangeListener(cl);
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        supp.removeChangeListener(cl);
    }

    @Override
    public <R> R get(Class<R> type) {
        return null;
    }

    @Override
    public <R> Collection<? extends R> getAll(Class<R> type) {
        if (Property.class == type) {
            return (Collection<? extends R>) Arrays.asList(faceProp, sizeProp, styleProp);
        }
        return Collections.<R>emptySet();
    }

    @Override
    public Collection<Font> getValues() {
        return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
    }
}
