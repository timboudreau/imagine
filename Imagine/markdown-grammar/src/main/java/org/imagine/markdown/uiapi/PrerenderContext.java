package org.imagine.markdown.uiapi;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.util.function.Consumer;
import javax.swing.JComponent;

/**
 * Implementation of MarkdownRenderingContext for computing preferred sizes
 * without actually painting.
 *
 * @author Tim Boudreau
 */
final class PrerenderContext implements MarkdownRenderingContext {

    final JComponent component;
    Font font;

    public PrerenderContext(JComponent component) {
        this.component = component;
        this.font = component.getFont();
    }

    @Override
    public void setFont(Font f) {
        this.font = f;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public FontMetrics getFontMetrics() {
        return component.getFontMetrics(font);
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return component.getFontMetrics(font);
    }

    @Override
    public LineMetrics getLineMetrics(String s) {
        return new FakeLineMetrics(s);
    }

    @Override
    public boolean withGraphics(Consumer<Graphics2D> graphics) {
        return false;
    }

    class FakeLineMetrics extends LineMetrics {

        private final String txt;

        public FakeLineMetrics(String txt) {
            this.txt = txt;
        }

        @Override
        public int getNumChars() {
            return txt.length();
        }

        @Override
        public float getAscent() {
            return getFontMetrics().getAscent();
        }

        @Override
        public float getDescent() {
            return getFontMetrics().getDescent();
        }

        @Override
        public float getLeading() {
            return getFontMetrics().getLeading();
        }

        @Override
        public float getHeight() {
            return getFontMetrics().getHeight();
        }

        @Override
        public int getBaselineIndex() {
            return 0;
        }

        @Override
        public float[] getBaselineOffsets() {
            return new float[0];
        }

        @Override
        public float getStrikethroughOffset() {
            return -(getHeight() / 2F);
        }

        @Override
        public float getStrikethroughThickness() {
            return 1;
        }

        @Override
        public float getUnderlineOffset() {
            return 1;
        }

        @Override
        public float getUnderlineThickness() {
            return 1;
        }
    }
}
