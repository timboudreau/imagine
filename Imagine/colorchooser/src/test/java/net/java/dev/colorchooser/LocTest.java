package net.java.dev.colorchooser;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class LocTest {

    @Test
    public void testBundleIsOnClasspath() {
//        URL u = ColorChooser.class.getResource("Bundle.properties");
//        assertNotNull(u);
//        System.out.println("U " + u);
        assertNotNull(ColorChooser.class.getResourceAsStream("Bundle.properties"));
    }

    @Test
    public void testLocalizedStringsAreFound() {
        String tip = ColorChooser.getString("basic");
        assertNotNull(tip);
        assertNotEquals("tip", tip);
    }

    @Test
    public void testFallbackLocalizedStringsAreFound() {
        String tip = ColorChooser.getString("basic");
        assertNotNull(tip);
        assertNotEquals("tip", tip);
    }

    @BeforeEach
    public void setup() {
        Locale usEnglish = Locale.forLanguageTag("en-US");
        assertEquals("US", usEnglish.getCountry());
        assertEquals("en", usEnglish.getLanguage());

        Locale.setDefault(usEnglish);
        Locale loc = Locale.getDefault();
        assertEquals("US", loc.getCountry());
        assertEquals("en", loc.getLanguage());
    }

    static class Ctrl extends Control {

        @Override
        public String toBundleName(String baseName, Locale locale) {
            return super.toBundleName(baseName, locale); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean needsReload(String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {
            return super.needsReload(baseName, locale, format, loader, bundle, loadTime); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long getTimeToLive(String baseName, Locale locale) {
            return super.getTimeToLive(baseName, locale); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            return super.newBundle(baseName, locale, format, loader, reload); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            Locale result = super.getFallbackLocale(baseName, locale);
            if (result == null) {
                result = Locale.forLanguageTag("en-US");
            }
            return result;
        }

        @Override
        public List<String> getFormats(String baseName) {
            return super.getFormats(baseName); //To change body of generated methods, choose Tools | Templates.
        }

    }
}
