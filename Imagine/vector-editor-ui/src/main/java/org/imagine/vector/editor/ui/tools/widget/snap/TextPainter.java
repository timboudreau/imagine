package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapAxis;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.geometry.Arrow;
import org.imagine.utils.painting.RepaintHandle;

/**
 *
 * @author Tim Boudreau
 */
class TextPainter {

    private final Rectangle2D.Double lastBounds = new Rectangle2D.Double();
    private double val;
    private static final DecimalFormat FMT = new DecimalFormat("####0.###");
    private static final Font FONT = new Font("Arial", Font.PLAIN, 12);

    void setValue(double val) {
        this.val = val;
        lastBounds.width = 0;
        lastBounds.height = 0;
    }

    void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(lastBounds);
    }

    public void paint(Graphics2D g, Zoom zoom, Arrow along, SnapAxis axis, Rectangle bds) {
        double textX;
        double textY;
        String txt = FMT.format(val);
        g.setFont(FONT.deriveFont(zoom.getInverseTransform()));
        FontMetrics fm = g.getFontMetrics();
        int txtWidth = fm.stringWidth(txt);
        int txtHeight = fm.getMaxAscent() + fm.getMaxDescent();
        switch (axis) {
            case X:
                textX = (along.x1 + ((along.x2 - along.x1) / 2)) - (txtWidth / 2D);
                textY = along.y1 + txtHeight + zoom.inverseScale(2);
                break;
            case Y:
                textX = along.x1 + zoom.inverseScale(5);
                textY = along.y1 + ((along.y2 - along.y1) / 2);
                break;
            default:
                throw new AssertionError(axis);
        }
        final double margin = zoom.inverseScale(2);
        lastBounds.setFrame(textX - margin, textY - margin - fm.getAscent(), txtWidth + (margin * 2), txtHeight + (margin * 2));
        SnapUISettings settings = SnapUISettings.getInstance();
        g.setPaint(settings.captionFillColor(SnapKind.MATCH));
        g.fill(lastBounds);
        g.setPaint(settings.captionColor(SnapKind.MATCH));
        g.drawString(txt, (float) textX, (float) textY);
    }

}
