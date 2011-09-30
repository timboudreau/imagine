package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.TwoStateHoverProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.State;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.layout.LayoutFactory.SerialAlignment;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.widget.general.IconNodeWidget;
import org.openide.util.ImageUtilities;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class ExpandableWidget extends Widget {

    private boolean expanded;
    private final Expandable expandable;
    private final Widget innerPanel;
    private static final int GAP = Utilities.isMac() ? 13 : 5;
    private final ExpansionWidget expander;
    private final Layout vertical = LayoutFactory.createVerticalFlowLayout(SerialAlignment.JUSTIFY, GAP);
    private final Layout horizontal;

    public ExpandableWidget(ColumnDataScene scene, Expandable expandable) {
        this(scene, expandable, false);
    }

    public ExpandableWidget(ColumnDataScene scene, Expandable expandable, boolean initialState) {
        super(scene);
        horizontal = scene.getColumns().createLayout();
        this.expandable = expandable;
        this.expanded = !initialState;
        expander = new ExpansionWidget(scene);
        expander.setFont(scene.getFont());
//        innerPanel = new Widget(scene);
        innerPanel = new IP();
        innerPanel.setLayout(vertical);
        innerPanel.setOpaque(false);
        setExpanded(initialState);
    }

    private void setExpanded(boolean ex) {
        if (ex != expanded) {
            expanded = ex;
            populate();
            expander.update();
            getScene().validate();
            innerPanel.setBorder(BorderFactory.createEmptyBorder(depth() * 4));
        }
    }
    
    private int depth() {
        int depth = 0;
        Widget w = this;
        do {
            w = w.getParentWidget();
            if (w instanceof ExpandableWidget) {
                depth++;
            }
        } while (w != null);
        return depth;
    }

    private Color depthColor() {
        int red = 10;
        int green = 10;
        int blue = 54;
        int depth = 0;
        Widget w = this;
        do {
            w = w.getParentWidget();
            if (w instanceof ExpandableWidget) {
                depth++;
            }
        } while (w != null);
        return new Color(adjustComponent(red, depth), adjustComponent(green, depth),
                adjustComponent(blue, depth));
    }
    
    protected void paintWidget() {
        if (expanded) {
            Graphics2D g = getGraphics();
            g.setPaint(gp());
            GeneralPath pth = new GeneralPath();
            Rectangle r = getBounds();
            Rectangle r1 = expander.getBounds();
            Insets i = expander.getBorder().getInsets();
            r1.x += i.left + depth() * 2;
            r.x = r1.x;
            pth.moveTo(r.x , r.y + GAP);
            pth.lineTo(r.x , r.y + r.height - (GAP + 1));
            pth.quadTo(r.x, r.y + r.height - 1, r.x + GAP, r.y + (r.height - 1));
            pth.lineTo(r.x + r.width - GAP, r.y + (r.height - 1));
//            g.setStroke(new BasicStroke(2));
            System.out.println("pb");
//            g.draw(pth);
            pth.quadTo(r.x + r.width - GAP, r.y + (r.height - (1)), r.x + r.width, r.y + (r.height - (GAP + 1)));
            pth.lineTo(r.x + r.width, r.y + GAP);
            pth.closePath();
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15F));
            g.fill(pth);
            g.setComposite(old);
        }
        super.paintWidget();
    }

    class IP extends Widget {

        IP() {
            super(ExpandableWidget.this.getScene());
        }

        @Override
        protected void paintWidget() {
//            Graphics2D g = getGraphics();
//            g.setPaint(depthColor());
//            GeneralPath pth = new GeneralPath();
//            Rectangle r = getBounds();
//            Rectangle r1 = expander.getBounds();
//            Insets i = expander.getBorder().getInsets();
//            r1.x += i.left;
//            r.x = r1.x;
//            pth.moveTo(r.x , r.y + GAP);
//            pth.lineTo(r.x , r.y + r.height - (GAP + 1));
//            pth.quadTo(r.x, r.y + r.height - 1, r.x + GAP, r.y + (r.height - 1));
//            pth.lineTo(r.x + (r.width / 2), r.y + (r.height - 1));
//            g.setStroke(new BasicStroke(2));
//            System.out.println("pb");
//            g.draw(pth);
            super.paintWidget();
        }
    }

    private int adjustComponent(int val, int depth) {
        if (depth == 0) {
            return val;
        }
        int off = 255 - val;
        val += (off / 4) * depth;
        val = Math.max(0, Math.min(255, val));
        return val;
    }

    private Columns columns() {
        return ((ColumnDataScene) getScene()).getColumns();
    }
    
    private GradientPaint gp() {
        Color color = depthColor();
        return new GradientPaint(00, 0, color, getScene().getBounds().width, 0, Color.WHITE);
    }

    private void populate() {
        columns().clear(this);
        removeChildren();
        addChild(expander);
        if (expanded) {
            setLayout(vertical);
            addChild(innerPanel);
            expander.clearInner();
            for (Widget w : expandable.getWidgets(expanded)) {
                columns().add(w, this);
                w.setLayout(horizontal);
                innerPanel.addChild(w);
                w.setOpaque(false);
            }
        } else {
            innerPanel.removeChildren();
            columns().add(this, this);
            setLayout(horizontal);
            for (Widget w : expandable.getWidgets(expanded)) {
                expander.addInner(w);
                w.setOpaque(false);
            }
        }
        revalidate();
    }

    public interface Expandable {

        String getTitle();

        List<Widget> getWidgets(boolean expanded);
    }
    private static final Image RIGHT =
            ImageUtilities.loadImage("net/java/dev/imagine/customizers2/visualizer/turner-right.png");
    private static final Image RIGHT_HOVER =
            ImageUtilities.loadImage("net/java/dev/imagine/customizers2/visualizer/turner-right-lit.png");
    private static final Image DOWN =
            ImageUtilities.loadImage("net/java/dev/imagine/customizers2/visualizer/turner-down.png");
    private static final Image DOWN_HOVER =
            ImageUtilities.loadImage("net/java/dev/imagine/customizers2/visualizer/turner-down-lit.png");

    final class ExpansionWidget extends IconNodeWidget implements TwoStateHoverProvider {

        private boolean hovered;
        private final Widget innerPanel;

        public ExpansionWidget(Scene scene) {
            super(scene, TextOrientation.RIGHT_CENTER);
            innerPanel = new Widget(scene);
            innerPanel.setLayout(LayoutFactory.createHorizontalFlowLayout());
            setLabel(expandable.getTitle());
            getActions().addAction(ActionFactory.createHoverAction(this));
            getActions().addAction(new WidgetAction.Adapter() {

                @Override
                public State mouseClicked(Widget widget, WidgetMouseEvent event) {
                    setExpanded(!expanded);
                    update();
                    return State.CONSUMED;
                }
            });
            setOpaque(false);
            update();
            addChild(innerPanel);
            innerPanel.setBorder(BorderFactory.createEmptyBorder(0, GAP, 0, GAP));
            setBorder(new B());
            setCheckClipping(true);
            getLabelWidget().setFont(scene.getFont().deriveFont(Font.BOLD));
            getLabelWidget().setForeground(Color.WHITE);
        }

        @Override
        protected void paintBackground() {
//            Rectangle r = getBounds();
//            RoundRectangle2D rr = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 15, 15);
//            Graphics2D g = getGraphics();
//            Shape old = g.getClip();
//            g.setClip(rr);
//            g.setPaint(new GradientPaint(00, 0, depthColor(), getScene().getBounds().width, 0, Color.WHITE));
//            g.setClip(old);
        }

        void addInner(Widget w) {
            innerPanel.addChild(w);
            innerPanel.revalidate();
        }

        void clearInner() {
            innerPanel.removeChildren();
            innerPanel.revalidate();
        }

        void setInnerComponent(Widget w) {
            innerPanel.removeChildren();
            if (w != null) {
                innerPanel.addChild(w);
            }
            innerPanel.revalidate();
        }

        @Override
        protected Rectangle calculateClientArea() {
//            EventQueue.invokeLater(new Runnable() {
//
//                @Override
//                public void run() {
//                    Rectangle r = expander.getBounds();
//                    if (r != null) {
//                        expander.setBackground(new GradientPaint(0, 0, depthColor(), r.width, 0, Color.WHITE));
//                    }
//                    expander.repaint();
//                }
//            });
            Rectangle r = super.calculateClientArea();
            r.width = Math.max(r.width, 350);
            if (expanded) {
                r.height += GAP * 5;
            }
//            Insets i = getBorder().getInsets();
//            r.height += i.top + i.bottom;
//            r.width += i.left + i.right;
            return r;
        }

        class B implements Border {

            @Override
            public Insets getInsets() {
                return new Insets(GAP, GAP, GAP, GAP * 2);
            }

            private Shape shape(Rectangle r) {
                Shape result = new RoundRectangle2D.Double(r.x, r.y, r.width - 1, r.height + (expanded ? GAP * 2 : 0) - 1, 15, 15);
                if (!expanded) {
                    return result;
                }
                Area a = new Area(result);
                int half = r.y + (r.height / 2);
                a.add(new Area(new Rectangle(r.x, half, r.width - 1, r.height - (half + 1))));
                Shape cutout = new RoundRectangle2D.Double(r.x + GAP, GAP + r.y + r.height - (GAP * 2),
                        r.width - (GAP * 2), GAP * 3, 15, 15);
                a.subtract(new Area(cutout));
                a.subtract(new Area(new Rectangle(r.x, r.y + r.height - 1, r.width, 5)));
                result = a;
                return result;
            }

            @Override
            public void paint(Graphics2D gd, Rectangle r) {
//                int depth = depth();
//                r.x += depth * 2;
//                r.width -= depth * 2;
                Color color = depthColor();
                gd.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                gd.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
                gd.setPaint(gp());
                Shape shape = shape(r);
                gd.fill(shape);
                gd.setPaint(color);
                gd.setStroke(new BasicStroke(1));
                gd.draw(shape);
            }

            @Override
            public boolean isOpaque() {
                return true;
            }
        }

        void update() {
            if (hovered) {
                if (expanded) {
                    setImage(DOWN_HOVER);
                } else {
                    setImage(RIGHT_HOVER);
                }
            } else {
                if (expanded) {
                    setImage(DOWN);
                } else {
                    setImage(RIGHT);
                }
            }
        }

        @Override
        public void unsetHovering(Widget widget) {
            hovered = false;
            update();
        }

        @Override
        public void setHovering(Widget widget) {
            hovered = true;
            update();
        }
    }
}
