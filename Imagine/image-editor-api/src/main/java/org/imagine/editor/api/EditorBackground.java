package org.imagine.editor.api;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import javax.swing.Icon;

/**
 *
 * @author Tim Boudreau
 */
public interface EditorBackground {

    Color contrasting();

    void fill(Graphics2D g, Rectangle bounds);

    Paint getPaint();

    Color lowContrasting();

    Color midContrasting();

    Color nonContrasting();

    double meanBrightness();

    default Icon icon() {
        return new EditorBackgroundIcon(this);
    }

    default int getPatternStride() {
        return 16;
    }

    default boolean isBright() {
        return meanBrightness() > 0.75;
    }

    default boolean isDark() {
        return meanBrightness() <= 0.25;
    }

    default boolean isMedium() {
        return !isDark() && !isBright();
    }
}
