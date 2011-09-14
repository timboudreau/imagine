package org.netbeans.paint.api.util;

import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;
import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public class Fonts {
    private static final String DEFAULT = "default";
    private final Map<String, Font> cache = new HashMap<String,Font>();
    private static Fonts INSTANCE = new Fonts();
    public static Fonts getDefault() {
        return INSTANCE;
    }
    
    public Font get() {
        return get(DEFAULT);
    }
    
    public Font get(String name) {
        Parameters.notNull("name", name);
        return load(name);
    }
    
    public void set(String name, Font f) {
        Parameters.notNull("f", f);
        Parameters.notNull("name", name);
        Font old = cache.get(name);
        if (old == null || (old != null && !old.equals(f))) {
            cache.put(name, f);
            save(f, name);
        }
    }
    
    public void set(Font f) {
        set(DEFAULT, f);
    }
    
    protected void save(Font value, String name) {
        if (value == null) {
            return;
        }
        String family = value.getFamily();
        float size = value.getSize2D();
        int style = value.getStyle();
        AffineTransform xform = value.getTransform();
        double[] matrix = new double[6];
        xform.getMatrix(matrix);
        Preferences p = NbPreferences.forModule(Fonts.class);
        p.put(name + ".family", family);
        p.putFloat (name + ".size", size);
        p.putInt(name + ".style", style);
        for (int i=0; i < matrix.length; i++) {
            p.putDouble(name + ".xform." + i, matrix[i]);
        }
    }
    
    private Font load(String name) {
        Preferences p = NbPreferences.forModule(Fonts.class);
        String family = p.get(name + ".family", "Serif");
        float size = p.getFloat(name + ".size", 24);
        int style = p.getInt (name + ".style", Font.PLAIN);
        double[] matrix = new double[6];
        boolean hasXform = false;
        for (int i=0; i < matrix.length; i++) {
            matrix[i] = p.getDouble(name + ".xform." + i, 0);
            hasXform |= matrix[i] != 0D;
        }
        AffineTransform xform = new AffineTransform (matrix);
        Font result = new Font (family, style, (int) size);
        if (hasXform) {
            result = result.deriveFont(xform);
        }
        return result;
    }
    
}
