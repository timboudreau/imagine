package net.java.dev.imagine.api.properties.preferences;

import java.util.prefs.Preferences;
import net.java.dev.imagine.api.properties.EnumPropertyID;
import org.openide.util.NbPreferences;

/**
 *
 * @author tim
 */
public abstract class PreferencesFactory {
    public abstract Preferences getPreferences();
    
    private static class EnumPreferencesFactory<T extends Enum> extends PreferencesFactory {
        private final EnumPropertyID<?, T> id;

        public EnumPreferencesFactory(EnumPropertyID<?, T> id) {
            this.id = id;
        }

        @Override
        public Preferences getPreferences() {
            return NbPreferences.forModule(id.constant().getClass());
        }
    }
}
