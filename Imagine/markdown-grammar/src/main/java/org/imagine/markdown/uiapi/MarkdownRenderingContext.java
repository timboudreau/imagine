package org.imagine.markdown.uiapi;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.JComponent;

/**
 * Provides enough information to the rendering engine for it to compute
 * positions and sizes for painting.
 *
 * @author Tim Boudreau
 */
public interface MarkdownRenderingContext {

    void setFont(Font f);

    Font getFont();

    FontMetrics getFontMetrics();

    FontMetrics getFontMetrics(Font font);

    LineMetrics getLineMetrics(String s);

    boolean withGraphics(Consumer<Graphics2D> graphics);

    boolean isPaintingContext();

    default boolean withGraphics(Predicate<Graphics2D> graphics) {
        boolean[] result = new boolean[1];
        return withGraphics(g -> {
            result[0] = graphics.test(g);
        }) && result[0];
    }

    static MarkdownRenderingContext prerenderContext(JComponent comp) {
        return new PrerenderContext(comp);
    }

    static MarkdownRenderingContext renderContext(JComponent comp, Graphics2D g) {
        g.setFont(comp.getFont());
        return renderContext(g);
    }

    static MarkdownRenderingContext renderContext(Graphics2D g) {
        return new RenderContext(g);
    }
}
