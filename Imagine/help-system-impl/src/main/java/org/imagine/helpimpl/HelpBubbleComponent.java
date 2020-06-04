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
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EnhRectangle2D;
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
//            comp.setMargin(12);
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
            EnhRectangle2D rect = findBestBounds();
            double h = comp.getFontMetrics(comp.getUIProperties().getFont()).getHeight() ;
//            rect.translate(0, -h);
            comp.setBounds(rect.getBounds());
            if (rect.x < 0) {
                rect.x = 10;
            }
            if (rect.y < 0) {
                rect.y = 10;
            }
            if (rect.getMaxY() > getHeight()) {
                rect.y -= (rect.getMaxY() - getHeight()) + 10;
            }
            if (rect.getMaxX() > getWidth()) {
                rect.y -= (rect.getMaxX() - getWidth()) + 10;
            }
            rect.grow(h);

//            rect.translate(0, h);
            bubble = new CaptionBubble(new Rectangle(0, 0, getParent().getWidth(), getParent().getHeight()),
                    rect.getBounds(), locusPoint);
        }
    }

    private EnhRectangle2D findBestBounds() {
        double w = getParent().getWidth();
        double h = getParent().getHeight();
        MutableRectangle2D overallBounds = MutableRectangle2D.of(0, 0, w, h);
        MutableRectangle2D r = MutableRectangle2D.of(0, 0, w * 0.5, h * 0.5);

        System.out.println("OB " + overallBounds + " base " + r);

        r.setCenter(locusPoint.getX(), locusPoint.getY());
        System.out.println("Base r " + r);
        EnhRectangle2D needed = EnhRectangle2D.of(comp.neededBounds(r));
        System.out.println("orig needed " + needed);
        needed.width = Math.max(250, needed.width);
        System.out.println("  adj to " + needed);
        boolean xAdjusted = false;
        boolean yAdjusted = false;
        for (double mul : new double[]{2.25, 2, 1.875, 1.75, 1.625, 1.5, 1.375, 1.25, 1.125}) {
            if (!yAdjusted) {
                if (locusPoint.y - (needed.height * mul) >= 0) {
                    System.out.println("A");
                    yAdjusted = true;
                    needed.translate(0, -needed.height * mul);
                } else if (overallBounds.getMaxY() >= locusPoint.y + (needed.height * mul)) {
                    System.out.println("B");
                    yAdjusted = true;
                    needed.translate(0, needed.height * mul);
                }
            }
            if (!xAdjusted) {
                if (locusPoint.x - (needed.width * mul) >= 0) {
                    System.out.println("C");
                    xAdjusted = true;
                    needed.translate(-needed.width * mul, 0);
                } else if (overallBounds.getMaxY() >= locusPoint.x + (needed.width * mul)) {
                    System.out.println("D");
                    xAdjusted = true;
                    needed.translate(needed.width * mul, 0);
                }
            }
            if (xAdjusted && yAdjusted) {
                break;
            }
        }
        System.out.println("preconstrain " + needed);
        constrain(needed, overallBounds);
        System.out.println("bestBounds constrained to " + needed);
        Rectangle2D redone = comp.neededBounds(needed);
        System.out.println("   reconstrain would get " + new EnhRectangle2D(redone));
        needed.height = Math.min(redone.getHeight(), needed.height);
        needed.width = Math.max (300, Math.min(redone.getWidth(), needed.width));
        double randomAngle = ThreadLocalRandom.current().nextDouble() * 360;
        Circle.positionOf(randomAngle, needed.x, needed.y, needed.width /2, (x, y) -> {
            needed.translate(x, y);
        });
        constrain(needed, overallBounds);
        return needed;
    }


    private void constrain(EnhRectangle2D rect, MutableRectangle2D within) {
        if (rect.getMaxY() > within.getMaxY()) {
            rect.y -= rect.getMaxY() - within.getMaxY();
        }
        if (rect.getMinY() < within.getMinY()) {
            rect.y += within.getMinY() - rect.getMinY();
        }
        if (rect.getMaxX() > within.getMaxX()) {
            rect.x -= rect.getMaxX() - within.getMaxX();
        }
        if (rect.getMinX() < within.getMinX()) {
            rect.x += within.getMinX() - rect.getMinX();
        }
        if (rect.contains(locusPoint)) {
            
        }
    }
}
