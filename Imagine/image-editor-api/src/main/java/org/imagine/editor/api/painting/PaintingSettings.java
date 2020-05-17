package org.imagine.editor.api.painting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import org.imagine.geometry.Circle;

/**
 *
 * @author Tim Boudreau
 */
public interface PaintingSettings<Purpose extends Enum<Purpose>> {

    BasicStroke stroke(Purpose p);

    Color outline(Purpose p);

    Color interior(Purpose p);

    default Color shadow(Purpose p) {
        return AbstractPaintingSettings.alphaReduced(outline(p));
    }

    default boolean hasShadow(Purpose p) {
        return false;
    }

    default Shape shape(Purpose p, double x, double y) {
        return new Circle(x, y, size(p));
    }

    default Font font(Purpose p) {
        return new Font("Sans Serif", Font.PLAIN, 12);
    }

    default double size(Purpose p) {
        return 7;
    }
}
