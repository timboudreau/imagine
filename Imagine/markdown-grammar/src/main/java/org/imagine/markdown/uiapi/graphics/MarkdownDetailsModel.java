package org.imagine.markdown.uiapi.graphics;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.BitSet;
import java.util.List;
import org.imagine.markdown.util.RectangleCollection;

/**
 * A variant of MarkdownRenderingModel which can
 *
 * @author Tim Boudreau
 */
public class MarkdownDetailsModel extends MarkdownRenderingModel {

    private final BitSet textItemPositions;

    public MarkdownDetailsModel(List<PaintItem> items, BitSet textItemPositions, Rectangle2D.Float renderedBounds) {
        super(items, renderedBounds);
        this.textItemPositions = textItemPositions;
    }

    public int documentCharacterOffsetAt(float x, float y) {
        for (int bit = textItemPositions.nextSetBit(0); bit >= 0; bit = textItemPositions.nextSetBit(bit + 1)) {
            TextDetails td = (TextDetails) this.items.get(bit);
            System.out.println("pos " + bit + " " + td);
            if (td.contains(x, y)) {
                System.out.println("   match " + td.text());
                return td.documentOffset(x, y);
            }
        }
        return -1;
    }

    public int[] lineAndLineOffsetAt(float x, float y) {
        for (int bit = textItemPositions.nextSetBit(0); bit >= 0; bit = textItemPositions.nextSetBit(bit + 1)) {
            TextDetails td = (TextDetails) this.items.get(bit);
            if (td.contains(x, y)) {
                int line = td.line();
                int lineOffset = td.charPositionInLine(x, y);
                return new int[]{line, lineOffset};
            }
        }
        return null;
    }

    public Shape highlightOutline(float x1, float y1, float x2, float y2) {
        BitSet span = new BitSet(64);
        boolean haveFirst = false;
        for (int bit = textItemPositions.nextSetBit(0); bit >= 0; bit = textItemPositions.nextSetBit(bit + 1)) {
            TextDetails td = (TextDetails) this.items.get(bit);
            if (!haveFirst && td.contains(x1, y1)) {
                span.set(bit);
                haveFirst = true;
            } else if (haveFirst && td.contains(x2, y2)) {
                span.set(bit);
                break;
            } else if (haveFirst) {
                span.set(bit);
            }
        }
        int first = span.nextSetBit(0);
        if (first < 0) {
            return new Rectangle();
        }
        Rectangle2D.Float bds = new Rectangle2D.Float();
        RectangleCollection rc = new RectangleCollection();
        boolean isFirst = true;
        for (int bit = span.nextSetBit(0); bit >= 0; bit = span.nextSetBit(bit + 1)) {
            TextDetails td = (TextDetails) this.items.get(bit);
            td.fetchBounds(bds);
            boolean isLast = bds.contains(x2, y2);
            if (isFirst) {
                int co = td.charOffsetAt(x1, y1);
                float start = td.charXStart(co);
                if (start > bds.x) {
                    bds.width -= start - bds.x;
                }
                bds.x = start;
                isFirst = false;
                if (isLast) {
                    co = td.charOffsetAt(x2, y2);
                    float end = td.charXEnd(co);
                    if (end < bds.getMaxX()) {
                        bds.width -= bds.getMaxX() - end;
                    }
                }
                rc.addCopy(bds);
                if (isLast) {
                    break;
                }
            } else if (isLast) {
                int co = td.charOffsetAt(x2, y2);
                float end = td.charXEnd(co);
                if (end < bds.getMaxX()) {
                    bds.width -= bds.getMaxX() - end;
                }
                rc.addCopy(bds);
                break;
            } else {
                rc.addCopy(bds);
            }
        }
        return rc.toShape();
    }

}
