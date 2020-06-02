package org.imagine.helpimpl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.uirect.MutableRectangle2D;
import org.imagine.markdown.uiapi.Markdown;
import org.imagine.markdown.uiapi.MarkdownComponent;

/**
 *
 * @author Tim Boudreau
 */
public class HelpBubbleComponent extends JComponent {

    private MarkdownComponent comp;
    private final EqPointDouble locusPoint = new EqPointDouble();
    private CaptionBubble bubble;

    public HelpBubbleComponent() {
        setOpaque(false);
        setRequestFocusEnabled(false);
        setFocusable(false);
    }

    public boolean isEventOnBubble(MouseEvent evt) {
        if (bubble != null) {
            Component comp = (Component) evt.getSource();
            Point pt = SwingUtilities.convertPoint(comp, evt.getPoint(), this);
            return bubble.toShape().contains(pt);
        }
        return false;
    }

    public void setMarkdown(Markdown markdown, Point2D locusPoint) {
        this.locusPoint.setLocation(locusPoint);
        if (comp == null) {
            comp = new MarkdownComponent(markdown, false);
            comp.setFocusable(false);
            comp.setRequestFocusEnabled(false);
            comp.setMargin(12);
            comp.setUIProperties(comp.getUIProperties().withFont(new Font("Times New Roman", Font.PLAIN, 20)));
            add(comp);
        } else {
            comp.setMarkdown(markdown);
        }
        invalidate();
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isVisible()) {
            if (bubble != null) {
                Shape shape = bubble.toShape();
                System.out.println("Paint bubble at " + shape.getBounds()
                        + " comp " + comp.getBounds());
                Graphics2D gg = (Graphics2D) g;
                g.setColor(new Color(240, 220, 120, 225));
                gg.fill(shape);
                g.setColor(UIManager.getColor("controlShadow"));
                gg.draw(shape);
            }
        }
    }

    @Override
    public void doLayout() {
        if (comp != null) {
            Container par = getParent();
            if (par != null) {
                Rectangle sz = new Rectangle(0, 0, par.getWidth(), par.getHeight());
//                if (!getBounds().equals(sz)) {
//                    setBounds(sz);
//                }
            }
            Rectangle2D rect = findBestBounds();
            comp.setBounds(rect.getBounds());
            System.out.println("layout with " + rect);
            bubble = new CaptionBubble(new Rectangle(0, 0, getParent().getWidth(), getParent().getHeight()),
                    comp.getBounds(), locusPoint);
        }
    }

    private Rectangle2D findBestBounds() {
        MutableRectangle2D r = new MutableRectangle2D(0, 0, getParent().getWidth(), getParent().getHeight());

        r.width /= 1.5;
        r.height /= 1.5;
        double ang = Circle.angleOf(r.center(), locusPoint);
        Circle.positionOf(Angle.opposite(ang), r.getCenterX(), r.getCenterY(), Math.min(r.width, r.height), (nx, ny) -> {
            r.x = nx;
            r.y = ny;
        });
        if (r.x < 0) {
            r.x = -r.x;
        }
        if (r.y < 0) {
            r.y = -r.y;
        }
        Rectangle2D result = comp.neededBounds(r);
        System.out.println("NB " + r + "  -> " + result);
        return result;

    }
}
