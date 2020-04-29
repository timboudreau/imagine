package org.imagine.editor.api;

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeListener;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public final class ImageEditorBackground {

    private static final ImageEditorBackground INSTANCE = new ImageEditorBackground();
    private final WeakChangeSupport supp = new WeakChangeSupport(this);
    private CheckerboardBackground style = CheckerboardBackground.LIGHT;

    static {
        Preferences prefs = NbPreferences.forModule(ImageEditorBackground.class);
        int ord = prefs.getInt("lastImageEditorBackground", 0);
        CheckerboardBackground[] all = CheckerboardBackground.values();
        if (ord < all.length && ord >= 0) {
            INSTANCE.style = all[ord];
        }
    }

    public static ImageEditorBackground getDefault() {
        return INSTANCE;
    }

    public CheckerboardBackground style() {
        return style;
    }

    public void fill(Graphics2D g, Rectangle bounds) {
        style.fill(g, bounds);
    }

    public void setStyle(CheckerboardBackground bg) {
        if (style != notNull("bg", bg)) {
            style = bg;
            supp.fire();
            NbPreferences.forModule(ImageEditorBackground.class)
                    .putInt("lastImageEditorBackground", bg.ordinal());
        }
    }

    public ImageEditorBackground addChangeListener(ChangeListener listener) {
        supp.addChangeListener(listener);
        return this;
    }

    public ImageEditorBackground removeChangeListeners(ChangeListener listener) {
        supp.removeChangeListener(listener);
        return this;
    }
}
