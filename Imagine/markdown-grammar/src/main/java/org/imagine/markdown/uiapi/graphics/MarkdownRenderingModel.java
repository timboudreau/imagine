package org.imagine.markdown.uiapi.graphics;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A model that can recapitulate the painting operations performed when painting
 * some markdown, without needing to reparse the document (as long as the
 * bounds have not changed, or the component is at its preferred size and
 * does not need a re-layout).
 *
 * @author Tim Boudreau
 */
public class MarkdownRenderingModel implements Iterable<PaintItem> {

    protected final List<PaintItem> items;
    private final Rectangle2D.Float renderedBounds;

    public MarkdownRenderingModel(List<PaintItem> items, Rectangle2D.Float renderedBounds) {
        this.items = items;
        this.renderedBounds = renderedBounds;
    }

    public Rectangle2D bounds() {
        return renderedBounds;
    }

    public void paint(Graphics2D g) {
        Graphics2D sub = (Graphics2D) g.create();
        try {
            for (PaintItem p : items) {
                p.paint(sub);
            }
        } finally {
            sub.dispose();
        }
    }

    @Override
    public Iterator<PaintItem> iterator() {
        return Collections.unmodifiableList(items).iterator();
    }
}
