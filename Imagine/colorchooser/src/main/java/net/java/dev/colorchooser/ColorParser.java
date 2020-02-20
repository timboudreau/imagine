/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.colorchooser;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Tim Boudreau
 */
public enum ColorParser {
    COMMA_DELIMITED_RGBA,
    HEX_RGBA,
    COMMA_DELIMITED_RGB,
    HEX_RGB,
    HEX_RGB_TRUNC;

    static final Pattern COMMA_DELIMITED_RGB_WITH_ALPHA_PATTERN
            = Pattern.compile("^(\\d{1,3})\\s*?,\\s*?(\\d{1,3})\\s*?,\\s*?(\\d{1,3})\\s*?,\\s*?(\\d{1,3})\\s*$");
    static final Pattern HEX_RGBA_PATTERN
            = Pattern.compile("^#?([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})$");
    static final Pattern COMMA_DELIMITED_RGB_PATTERN
            = Pattern.compile("^(\\d{1,3})\\s*?,\\s*?(\\d{1,3})\\s*?,\\s*?(\\d{1,3})\\s*$");
    static final Pattern HEX_RGB_PATTERN
            = Pattern.compile("^#?([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})$");
    static final Pattern HEX_RGB_TRUNC_PATTERN
            = Pattern.compile("^#?([a-fA-F0-9])([a-fA-F0-9])([a-fA-F0-9])$");

    private String toHexString(int val) {
        String result = Integer.toHexString(val);
        if (isShorthand()) {
            result = result.substring(0, 1);
        } else {
            if (result.length() < 2) {
                result = "0" + result;
            }
        }
        return result;
    }

    public String toString(Color color) {
        switch (this) {
            case COMMA_DELIMITED_RGB:
                return color.getRed() + "," + color.getGreen()
                        + "," + color.getBlue();
            case COMMA_DELIMITED_RGBA:
                return color.getRed() + "," + color.getGreen()
                        + "," + color.getBlue()
                        + "," + color.getAlpha();
            case HEX_RGB:
            case HEX_RGB_TRUNC:
                return '#' + toHexString(color.getRed())
                        + toHexString(color.getGreen())
                        + toHexString(color.getBlue());
            case HEX_RGBA:
                return '#' + toHexString(color.getRed())
                        + toHexString(color.getGreen())
                        + toHexString(color.getBlue())
                        + toHexString(color.getAlpha());
            default:
                throw new AssertionError(this);
        }
    }

    static Color parse(String s) {
        for (ColorParser cp : values()) {
            Pattern p = cp.pattern();
            Matcher m = p.matcher(s);
            if (m.find()) {
                Color result = cp.doParse(s, m);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public boolean isHex() {
        switch (this) {
            case HEX_RGB:
            case HEX_RGBA:
            case HEX_RGB_TRUNC:
                return true;
            default:
                return false;
        }
    }

    public boolean containsAlpha() {
        switch (this) {
            case COMMA_DELIMITED_RGBA:
            case HEX_RGBA:
                return true;
            default:
                return false;
        }
    }

    public boolean isShorthand() {
        return this == HEX_RGB_TRUNC;
    }

    private int parseHex(String s) {
        if (isShorthand() && s.length() == 1) {
            s += "0";
        }
        return Integer.parseInt(s, 16);
    }

    Color doParse(String s) {
        Matcher m = pattern().matcher(s);
        if (m.find()) {
            return doParse(s, m);
        }
        return null;
    }

    Color doParse(String s, Matcher m) {
        int r, g, b;
        int a = 255;
        try {
            if (isHex()) {
                r = parseHex(m.group(1));
                g = parseHex(m.group(2));
                b = parseHex(m.group(3));
                if (containsAlpha()) {
                    a = parseHex(m.group(4));
                }
            } else {
                r = Integer.parseInt(m.group(1));
                g = Integer.parseInt(m.group(2));
                b = Integer.parseInt(m.group(3));
                if (containsAlpha()) {
                    a = Integer.parseInt(m.group(4));
                }
            }
            return new Color(r, g, b, a);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean canParse(String s) {
        for (ColorParser cp : values()) {
            Pattern p = cp.pattern();
            Matcher m = p.matcher(s);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    public static String toMinimalString(Color c) {
        if (isShorthandFriendly(c)) {
            return HEX_RGB_TRUNC.toString(c);
        }
        if (c.getAlpha() != 255) {
            return HEX_RGBA.toString(c);
        }
        return HEX_RGB.toString(c);
    }

    private static boolean isShorthandFriendly(Color c) {
        if (c.getAlpha() != 255) {
            return false;
        }
        return isShorthandFriendly(c.getRed())
                && isShorthandFriendly(c.getGreen())
                && isShorthandFriendly(c.getBlue());
    }

    private static boolean isShorthandFriendly(int val) {
        return (val & 0xF0) == val;
    }

    Pattern pattern() {
        switch (this) {
            case COMMA_DELIMITED_RGB:
                return COMMA_DELIMITED_RGB_PATTERN;
            case COMMA_DELIMITED_RGBA:
                return COMMA_DELIMITED_RGB_WITH_ALPHA_PATTERN;
            case HEX_RGB:
                return HEX_RGB_PATTERN;
            case HEX_RGBA:
                return HEX_RGBA_PATTERN;
            case HEX_RGB_TRUNC:
                return HEX_RGB_TRUNC_PATTERN;
            default:
                throw new AssertionError(this);
        }
    }
}
