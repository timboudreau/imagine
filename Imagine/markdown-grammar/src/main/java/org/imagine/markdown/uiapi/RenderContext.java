package org.imagine.markdown.uiapi;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.util.function.Consumer;

/**
 * Implementation of MarkdownRenderingContext for actual painting.
 *
 * @author Tim Boudreau
 */
final class RenderContext implements MarkdownRenderingContext {

    final Graphics2D graphics;

    public RenderContext(Graphics2D graphics) {
        this.graphics = graphics;
    }

    @Override
    public void setFont(Font f) {
        graphics.setFont(f);
    }

    @Override
    public Font getFont() {
        return graphics.getFont();
    }

    @Override
    public FontMetrics getFontMetrics() {
        return graphics.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return graphics.getFontMetrics(font);
    }

    @Override
    public boolean withGraphics(Consumer<Graphics2D> c) {
        c.accept(graphics);
        return true;
    }

    @Override
    public LineMetrics getLineMetrics(String s) {
        return getFontMetrics().getLineMetrics(s, graphics);
    }

}
