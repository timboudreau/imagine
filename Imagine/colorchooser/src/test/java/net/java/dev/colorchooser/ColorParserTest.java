/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.colorchooser;

import java.awt.Color;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ColorParserTest {

    @Test
    public void testColorParsing() {
        for (ColorParser cp : ColorParser.values()) {
            testOne(cp);
        }
    }

    @Test
    public void testBlack() {
        String str = ColorParser.HEX_RGB_TRUNC.toString(Color.BLACK);
        assertEquals("#000", str);
    }

    private void testOne(ColorParser p) {
        for (int r = 0; r <= 255; r += 15) {
            for (int g = 0; g <= 255; g += 15) {
                for (int b = 0; b <= 255; b += 15) {
                    if (p.containsAlpha()) {
                        for (int a = 0; a <= 255; a += 15) {
                            testOneColorAndParser(p, new Color(r, g, b, a));
                        }
                    } else {
                        testOneColorAndParser(p, new Color(r, g, b));
                        String min = ColorParser.toMinimalString(new Color(r, g, b));
                        Color got = ColorParser.parse(min);
                        assertColorsEqual(new Color(r, g, b), got, "Minimal string '" + min + "'");
                    }
                }
            }
        }
    }

    private void testOneColorAndParser(ColorParser p, Color c) {
        String s = p.toString(c);
        assertNotNull(s);
        if (p.isHex()) {
            assertTrue(s.charAt(0) == '#');
        }
        Color got = p.doParse(s);
        assertNotNull(got, p + " created '" + s + "' from " + c
                + " but could not parse it");
        Color expected = c;
        if (p.isShorthand()) {
            expected = new Color(c.getRed() & 0xF0,
                    c.getGreen() & 0xF0,
                    c.getBlue() & 0xF0, 255);
            return;
        }
        assertColorsEqual(expected, got, "Got different result from " + p + " for '" + s + "'");
    }

    private void assertColorsEqual(Color a, Color b, String msg) {
        int ar = a.getRed();
        int ag = a.getGreen();
        int ab = a.getBlue();
        int aa = a.getAlpha();

        int br = b.getRed();
        int bg = b.getGreen();
        int bb = b.getBlue();
        int ba = b.getAlpha();

        StringBuilder sb = new StringBuilder(msg).append(".  Expected ")
                .append(ar).append(',').append(ag).append(',').append(ab)
                .append(',').append(aa).append(" but got ")
                .append(br).append(',').append(bg).append(',').append(bb)
                .append(',').append(ba).append('.');
        boolean fail = false;
        if (ar != br) {
            sb.append(" Red values differ: ").append(ar).append(" vs. ").append(br);
            fail = true;
        }
        if (ag != bg) {
            sb.append(" Green values differ: ").append(ag).append(" vs. ").append(bg);
            fail = true;
        }
        if (ab != bb) {
            sb.append(" Blue values differ: ").append(ab).append(" vs. ").append(bb);
            fail = true;
        }
        if (aa != ba) {
            sb.append(" Alpha values differ: ").append(aa).append(" vs. ").append(ba);
            fail = true;
        }
        if (fail) {
            fail(sb.toString());
        }
    }

}
