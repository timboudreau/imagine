/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api;

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.event.ChangeListener;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
final class ImageEditorBackgroundImpl extends ImageEditorBackground {

    private final WeakChangeSupport supp = new WeakChangeSupport(this);
    CheckerboardBackground style = CheckerboardBackground.LIGHT;

    @SuppressWarnings("rawtype")
    public <T> Map<EditorBackground, T> createCacheMap() {
        Map<CheckerboardBackground, T> result = new EnumMap<>(CheckerboardBackground.class);
        return (Map) result;
    }

    public EditorBackground[] allBackgrounds() {
        return CheckerboardBackground.values();
    }

    public CheckerboardBackground style() {
        return style;
    }

    public void fill(Graphics2D g, Rectangle bounds) {
        style.fill(g, bounds);
    }

    public void setStyle(EditorBackground bg) {
        if (!(notNull("bg", bg) instanceof CheckerboardBackground)) {
            throw new IllegalArgumentException("Not an instance of CheckerboardBackground: " + bg);
        }
        if (style != bg) {
            CheckerboardBackground check = (CheckerboardBackground) bg;
            style = check;
            supp.fire();
            NbPreferences.forModule(ImageEditorBackground.class)
                    .putInt("lastImageEditorBackground", check.ordinal());
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
