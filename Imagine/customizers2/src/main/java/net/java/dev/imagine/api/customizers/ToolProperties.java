package net.java.dev.imagine.api.customizers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public enum ToolProperties {

    DEFAULT_FONT,
    DEFAULT_FOREGROUND,
    DEFAULT_BACKGROUND,
    DEFAULT_GRADIENT_0,
    DEFAULT_GRADIENT_1,
    DEFAULT_STROKE,
    DEFAULT_SHOULD_FILL;

    public ToolProperty<?, ?> getProperty() {
        switch (this) {
            case DEFAULT_BACKGROUND:
                return ColorProps.BACKGROUND.create();
            case DEFAULT_FONT:
                return Fonts.FONT.create();
            case DEFAULT_FOREGROUND:
                return ColorProps.FOREGROUND.create();
            case DEFAULT_GRADIENT_0:
                return ColorProps.GRADIENT_0.create();
            case DEFAULT_GRADIENT_1:
                return ColorProps.GRADIENT_1.create();
            case DEFAULT_STROKE:
                return Strokes.STROKE.create();
            case DEFAULT_SHOULD_FILL:
                return ShouldFill.SHOULD_FILL.create();
            default:
                throw new AssertionError(this);
        }
    }

    public static enum ShouldFill implements ToolProperty.Provider<Boolean, ShouldFill> {

        SHOULD_FILL;

        @Override
        public ToolProperty<Boolean, ShouldFill> create() {
            return ToolProperty.createBooleanProperty(this, true);
        }

        public String toString() {
            return NbBundle.getMessage(ToolProperties.class, name());
        }
    }

    public static enum Strokes implements ToolProperty.Provider<BasicStroke, Strokes> {

        STROKE;

        @Override
        public ToolProperty<BasicStroke, Strokes> create() {
            return ToolProperty.createStrokeProperty(this);
        }

        public String toString() {
            return NbBundle.getMessage(ToolProperties.class, name());
        }
    }

    public static enum Fonts implements ToolProperty.Provider<Font, Fonts> {

        FONT;

        @Override
        public ToolProperty<Font, Fonts> create() {
            return ToolProperty.createFontProperty(this);
        }
    }

    public static enum ColorProps implements ToolProperty.Provider<Color, ColorProps> {

        FOREGROUND,
        BACKGROUND,
        GRADIENT_0,
        GRADIENT_1;

        public String toString() {
            return NbBundle.getMessage(ToolProperties.class, name());
        }

        public ToolProperty<Color, ColorProps> create() {
            return ToolProperty.createColorProperty(this);
        }
    }

    public static enum FontProps {

        FONT_FACE,
        FONT_SIZE,
        FONT_STYLE;

        public String toString() {
            return NbBundle.getMessage(ToolProperties.class, name());
        }
    }

    public static enum ColorComponents {

        RED, GREEN, BLUE, ALPHA;

        Color convert(Color old, int newValue) {
            int red = old.getRed();
            int green = old.getGreen();
            int blue = old.getBlue();
            int alpha = old.getAlpha();
            switch (this) {
                case RED:
                    red = newValue;
                    break;
                case GREEN:
                    green = newValue;
                    break;
                case BLUE:
                    blue = newValue;
                    break;
                case ALPHA:
                    alpha = newValue;
                    break;
                default:
                    throw new AssertionError(this);
            }
            return new Color(red, green, blue, alpha);
        }

        int get(Color color) {
            switch (this) {
                case RED:
                    return color.getRed();
                case GREEN:
                    return color.getGreen();
                case BLUE:
                    return color.getBlue();
                case ALPHA:
                    return color.getAlpha();
                default:
                    throw new AssertionError(this);
            }
        }

        public String toString() {
            return NbBundle.getMessage(ToolProperties.class, name());
        }
    }

    public static enum SizeProps {

        STROKE_WIDTH;

        public String toString() {
            return NbBundle.getMessage(ToolProperties.class, name());
        }
    }

    public static enum FontStyles {

        PLAIN, BOLD, ITALIC, BOLD_ITALIC;

        public String toString() {
            return NbBundle.getMessage(ToolProperties.class, name());
        }

        public int getConstant() {
            switch (this) {
                case PLAIN:
                    return Font.PLAIN;
                case BOLD:
                    return Font.BOLD;
                case ITALIC:
                    return Font.ITALIC;
                case BOLD_ITALIC:
                    return Font.BOLD | Font.ITALIC;
                default:
                    throw new AssertionError(this);
            }
        }

        public static FontStyles forConstant(int constant) {
            for (FontStyles s : values()) {
                if (constant == s.getConstant()) {
                    return s;
                }
            }
            return PLAIN;
        }
    }
}
