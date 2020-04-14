package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapAxis;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Arrow;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPoint;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.PieWedge;
import org.imagine.geometry.Quadrant;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.utils.painting.RepaintHandle;

/**
 *
 * @author Tim Boudreau
 */
class TextPainter {

    private final Rectangle2D.Double lastBounds = new Rectangle2D.Double();
    private double val;
    private static final DecimalFormat FMT = new DecimalFormat("####0.###");
    private static final Font FONT = new Font("Arial", Font.BOLD, 12);
    private boolean degrees;

    void setValue(double val) {
        this.val = val;
        lastBounds.width = 0;
        lastBounds.height = 0;
        degrees = false;
    }

    void setDegrees(double val) {
        this.val = val;
        lastBounds.width = 0;
        lastBounds.height = 0;
        degrees = true;
    }

    void requestRepaint(RepaintHandle handle) {
        handle.repaintArea(lastBounds);
    }

    String text() {
        if (degrees) {
            String result = GeometryStrings.toDegreesStringShort(val);
            if (result.endsWith(".00")) {
                result = result.substring(0, result.length() - 3);
            }
            return result;
        } else {
            return FMT.format(val);
        }
    }

    public void paint(Graphics2D g, Zoom zoom, PieWedge relTo) {
        boolean noFlip = relTo.angle() < 180;
        double opp = noFlip ? relTo.midAngle() : Angle.opposite(relTo.midAngle());
        EqPoint txtPos = new EqPoint(relTo.getCenterX(), relTo.getCenterY());

        Circle.positionOf(opp, txtPos.x, txtPos.y, relTo.radius(), (tx, ty) -> {
            txtPos.setLocation(tx, ty);
        });

        String txt = text();
        g.setFont(FONT.deriveFont(zoom.getInverseTransform()));
        FontMetrics fm = g.getFontMetrics();
        int txtWidth = fm.stringWidth(txt);
        int txtHeight = fm.getMaxAscent() + fm.getMaxDescent();
        txtPos.x -= txtWidth / 2;
        txtPos.y += fm.getMaxAscent();
        final double margin = SnapUISettings.getInstance().captionMargin(zoom);
        txtPos.x += margin;
        txtPos.y += margin;
        lastBounds.setFrame(txtPos.x - margin, txtPos.y - margin - fm.getAscent(), txtWidth + (margin * 2), txtHeight + (margin * 2));
        SnapUISettings settings = SnapUISettings.getInstance();
        g.setPaint(settings.captionFillColor(SnapKind.POSITION));
        g.fill(lastBounds);
        g.setPaint(settings.captionColor(SnapKind.POSITION));
        g.drawString(txt, (float) txtPos.x, (float) txtPos.y);
    }

    public void paint(Graphics2D g, Zoom zoom, Arrow along, boolean direction) {
        EqLine line = along.toLine();
        SnapUISettings set = SnapUISettings.getInstance();
        double offset = set.captionLineOffset(zoom);
        line.shiftPerpendicular(direction ? offset * 2 : -(offset * 2));
        String txt = text();
        g.setFont(FONT.deriveFont(zoom.getInverseTransform()));
        FontMetrics fm = g.getFontMetrics();
        int txtWidth = fm.stringWidth(txt);
        int txtHeight = fm.getMaxAscent() + fm.getMaxDescent();

        EqPointDouble mid = line.midPoint();
        double rotTxt = Angle.perpendicularClockwise(along.angle());
        Quadrant q = Quadrant.forAngle(rotTxt);

        switch (q) {
            case SOUTHWEST:
            case SOUTHEAST:
                rotTxt = Angle.opposite(rotTxt);
        }
        float margin = (float) set.captionMargin(zoom);
        scratchRect.setFrame(mid.x - (txtWidth / 2) - margin, mid.y - margin,
                txtWidth + (margin * 2), txtHeight + (margin * 2));
        double[] pts = new double[]{
            scratchRect.x - margin,
            scratchRect.y - margin,
            scratchRect.x + scratchRect.width + (margin * 2),
            scratchRect.y + scratchRect.height + (margin * 2)
        };
        AffineTransform xform = AffineTransform.getRotateInstance(
                Math.toRadians(rotTxt), mid.x, mid.y);
        xform.transform(pts, 0, pts, 0, 2);
        lastBounds.setFrameFromDiagonal(pts[0], pts[1], pts[2], pts[3]);
        g = (Graphics2D) g.create();
        try {
            g.transform(xform);
            g.setPaint(set.captionFillColor(SnapKind.POSITION));
            g.fill(scratchRect);
            g.setPaint(set.captionColor(SnapKind.POSITION));
            g.drawString(txt, (float) scratchRect.x + margin,
                    (float) scratchRect.y + margin
                    + fm.getMaxAscent());
        } finally {
            g.dispose();
        }
    }
    private final Rectangle2D.Double scratchRect = new Rectangle2D.Double();

    public void paint(Graphics2D g, Zoom zoom, Arrow along, SnapAxis axis,
            Rectangle bds) {
        double textX;
        double textY;
        String txt = text();
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
        float margin = (float) SnapUISettings.getInstance().captionMargin(zoom);
        textX += margin;
        textY += margin;
        lastBounds.setFrame(textX - margin, textY - margin - fm.getAscent(), txtWidth + (margin * 2), txtHeight + (margin * 2));
        SnapUISettings settings = SnapUISettings.getInstance();
        g.setPaint(settings.captionFillColor(SnapKind.POSITION));
        g.fill(lastBounds);
        g.setPaint(settings.captionColor(SnapKind.POSITION));
        g.drawString(txt, (float) textX, (float) textY);
    }

}
