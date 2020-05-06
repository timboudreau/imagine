/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.key;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import org.imagine.awt.util.IdPathBuilder;
import org.imagine.io.KeyWriter;
import org.imagine.io.KeyReader;

/**
 *
 * @author Tim Boudreau
 */
public class ColorKey extends PaintKey<Color> {

    public static String ID_BASE = "color";
    private final int rgb;

    public ColorKey(Color color) {
        assert color != null : "color";
        this.rgb = color.getRGB();
    }

    public ColorKey(int val) {
        this.rgb = val;
    }

    @Override
    public PaintKey<Color> createTransformedCopy(AffineTransform xform) {
        return this;
    }

    @Override
    public PaintKeyKind kind() {
        return StandardPaintKeyKinds.COLOR;
    }

    public static ColorKey read(KeyReader reader) throws IOException {
        int rgb = reader.readInt();
        return new ColorKey(rgb);
    }

    @Override
    public void writeTo(KeyWriter writer) {
        writer.writeInt(rgb);
    }

    @Override
    protected Class<Color> type() {
        return Color.class;
    }

    @Override
    public Color toPaint() {
        return new Color(rgb, true);
    }

    @Override
    protected int computeHashCode() {
        return rgb * 74167;
    }

    @Override
    public String idBase() {
        return ID_BASE;
    }

    @Override
    protected void buildId(IdPathBuilder bldr) {
        Color color = toPaint();
        bldr.add(color.getRed()).add(color.getGreen())
                .add(color.getBlue());
        if (color.getAlpha() != 255) {
            bldr.add(color.getAlpha());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof ColorKey) {
            return rgb == ((ColorKey) o).rgb;
        }
        return false;
    }

    public String toString() {
        return "ColorKey(" + rgb(toPaint()) + ")";
    }
}
