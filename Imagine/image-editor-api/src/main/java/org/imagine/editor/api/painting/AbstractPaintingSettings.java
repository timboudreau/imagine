/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api.painting;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.UIManager;
import org.imagine.editor.api.Zoom;
import com.mastfrog.geometry.Circle;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AbstractPaintingSettings<Purpose extends Enum<Purpose>> implements PaintingSettings<Purpose> {

    private final DoubleMap<BasicStroke> strokes = DoubleMap.create(6);
    private final DoubleMap<Map<Purpose, Font>> fonts = DoubleMap.create(6);
    private final Class<Purpose> type;
    private Font baseFont;
    private final Circle circle = new Circle();

    protected AbstractPaintingSettings(Class<Purpose> type) {
        this.type = type;
    }

    protected abstract Zoom zoom();

    @Override
    public Font font(Purpose p) {
        return getOrCreateFont(p);
    }

    @Override
    public BasicStroke stroke(Purpose p) {
        return getOrCreateLineStroke(size(p), p);
    }

    @Override
    public Shape shape(Purpose p, double x, double y) {
        circle.setCenterAndRadius(x, y, size(p));
        return circle;
    }

    @Override
    public double size(Purpose p) {
        double base = baseSize(p);
        return zoom().inverseScale(base);
    }

    protected double strokeSize(Purpose p) {
        double base = baseStrokeSize(p);
        return zoom().inverseScale(base);
    }

    protected abstract double baseStrokeSize(Purpose p);

    protected abstract double baseSize(Purpose p);

    protected static final Color alphaReduced(Color c) {
        int a = c.getAlpha();
        int alpha = a - (a / 4);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    protected static final Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private Map<Purpose, Font> fontMap() {
        double z = zoom().getZoom();
        Map<Purpose, Font> map = fonts.get(z);
        if (map == null) {
            map = new EnumMap<>(type);
            fonts.put(z, map);
        }
        return map;
    }

    private Font getOrCreateFont(Purpose p) {
        Map<Purpose, Font> map = fontMap();
        Font result = map.get(p);
        if (result == null) {
            result = createFont(p);
            map.put(p, result);
        }
        return result;
    }

    protected Font createFont(Purpose p) {
        Font result = baseFont();
        Zoom z = zoom();
        if (!z.isOneToOne()) {
            result = result.deriveFont(z.getInverseTransform());
        }
        return result;
    }

    protected BasicStroke getOrCreateLineStroke(double size, Purpose p) {
        BasicStroke result = strokes.get(size);
        if (result == null) {
            result = new BasicStroke((float) size);
            strokes.put(size, result);
        }
        return result;
    }

    private Font baseFont() {
        if (baseFont == null) {
            baseFont = createBaseFont();
        }
        return baseFont;
    }

    protected Font createBaseFont() {
        Font f = UIManager.getFont("controlFont");
        if (f == null) {
            f = UIManager.getFont("Label.font");
        }
        if (f == null) {
            f = new JLabel().getFont();
        }
        if (f == null) {
            return new Font("Sans Serif", Font.PLAIN, 12);
        }
        return f;
    }
}
