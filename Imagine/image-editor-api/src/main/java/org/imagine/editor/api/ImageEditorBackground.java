package org.imagine.editor.api;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ChangeListener;
import org.openide.util.NbPreferences;

/**
 * Provides the default image editor background (currently an instance of
 * CheckerboxBackground, but this is not guaranteed in the future and code
 * should not assume it is one). The default implementation weakly references
 * listeners which are attached, so code that wants to listen should retain a
 * reference to the listener (i.e. don't use method references - they do not
 * have the same lifetime as the object they reference).
 *
 * @author Tim Boudreau
 */
public abstract class ImageEditorBackground {

    private static final ImageEditorBackground INSTANCE;

    static {
        ImageEditorBackgroundImpl impl = new ImageEditorBackgroundImpl();
        Preferences prefs = NbPreferences.forModule(ImageEditorBackground.class);
        int ord = prefs.getInt("lastImageEditorBackground", CheckerboardBackground.LIGHT.ordinal());
        CheckerboardBackground[] all = CheckerboardBackground.values();
        if (ord < all.length && ord >= 0) {
            impl.style = all[ord];
        }
        INSTANCE = impl;
    }

    /**
     * Get the default global instance of the editor background (which may or
     * may not be the same as that of any given editor, though currently they
     * are the same).
     *
     * @return The default instance
     */
    public static ImageEditorBackground getDefault() {
        return INSTANCE;
    }

    /**
     * Create a map for caching objects related to image editor backgrounds;
     * this allows foreign code to take advantage of the performance of EnumMap
     * if the editor background type is an enum (for example,
     * CheckerboxBackground) without assuming the background type will be an
     * enum forever.
     *
     * @param <T> The value type
     * @return A map
     */
    public abstract <T> Map<EditorBackground, T> createCacheMap();

    /**
     * All of the backgrounds this instance can display, for use in selection
     * UIs.
     *
     * @return An array of editor backgrounds
     */
    public abstract EditorBackground[] allBackgrounds();

    /**
     * Get the current editor background.
     *
     * @return An editor background
     */
    public abstract EditorBackground style();

    /**
     * Fill the passed rectangle in the passed graphics context with the
     * background pattern or color.
     *
     * @param g A graphics
     * @param bounds A rectangle
     */
    public abstract void fill(Graphics2D g, Rectangle bounds);

    /**
     * Set the current style.
     *
     * @throws IllegalArgumentException if the passed editor background is not
     * one this instance supports
     * @param bg An editor background
     */
    public abstract void setStyle(EditorBackground bg);

    /**
     * Add a change listener; the listener will be weakly referenced, so the
     * caller must retain a reference to it for it to be called on changes.
     *
     * @param listener A listener
     * @return this
     */
    public abstract ImageEditorBackground addChangeListener(ChangeListener listener);

    /**
     * Remove a change listener.
     *
     * @param listener The listener
     * @return this
     */
    public abstract ImageEditorBackground removeChangeListeners(ChangeListener listener);

    public ComboBoxModel<? extends EditorBackground> createSelectorComboBoxModel() {
        DefaultComboBoxModel<EditorBackground> mdl = new DefaultComboBoxModel<>(allBackgrounds());
        return mdl;
    }

    public JComboBox<? extends EditorBackground> createSelectorComboBox() {
        ComboBoxModel<? extends EditorBackground> mdl = createSelectorComboBoxModel();
        JComboBox<? extends EditorBackground> box = new JComboBox<>(mdl);
        box.addActionListener(ae -> {
            EditorBackground eb = (EditorBackground) box.getSelectedItem();
            setStyle(eb);
        });
        box.setRenderer(new EditorBackgroundIcon.Renderer());
        return box;
    }
}
