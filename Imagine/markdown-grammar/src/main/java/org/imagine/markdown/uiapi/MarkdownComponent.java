package org.imagine.markdown.uiapi;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleEditableText;
import javax.accessibility.AccessibleIcon;
import javax.accessibility.AccessibleRelationSet;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleTable;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

/**
 *
 * @author Tim Boudreau
 */
public final class MarkdownComponent extends JComponent {

    private Markdown md;
    private MarkdownUIProperties props = MarkdownUIProperties.forComponent(this);
    private int margin = 0;
    private Consumer<String> linkListener;
    private final Consumer<RegionOfInterest> addLink = this::addLink;
    private final List<RegionOfInterest> links = new ArrayList<>();
    private static int fallbackSize;
    private EmbeddedImageLoader imageLoader;

    public MarkdownComponent(Markdown md) {
        this(md, true);
    }

    public MarkdownComponent(Markdown md, boolean opaque) {
        this.md = md;
        setOpaque(opaque);
        setBackground(uiColor("text", Color.WHITE));
        setForeground(uiColor("textText", Color.BLACK));
        setFont(uiFont("TextArea.font", () -> new Font("Times New Roman", Font.PLAIN, fontSize())));
        addMouseWheelListener(MouseWheel.INSTANCE);
    }

    public void setUIProperties(MarkdownUIProperties props) {
        this.props = props;
        invalidate();
        revalidate();
        repaint();
    }

    private EmbeddedImageLoader imageLoader() {
        if (imageLoader == null) {
            return EmbeddedImageLoader.classRelative(Markdown.class);
        }
        return imageLoader;
    }

    public MarkdownUIProperties getUIProperties() {
        return props;
    }

    class LinkAdapter extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1 && !e.isPopupTrigger()) {
                Point2D pt = e.getPoint();
                for (RegionOfInterest link : links) {
                    switch (link.kind()) {
                        case LINK:
                            if (link.region().contains(pt)) {
                                linkListener.accept(link.content());
                            }
                    }
                }
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point2D pt = e.getPoint();
            for (RegionOfInterest link : links) {
                switch (link.kind()) {
                    case LINK:
                        if (link.region().contains(pt)) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            return;
                        }
                }
            }
            mouseExited(e);
        }
    }

    static class MouseWheel extends MouseAdapter implements MouseWheelListener {

        // JComponents by default use a 1-pixel wheel-scroll increment, which is
        // wrong for text displaying comonents.
        static final MouseWheel INSTANCE = new MouseWheel();

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            MarkdownComponent comp = (MarkdownComponent) e.getComponent();
            if (SwingUtilities.getAncestorOfClass(JViewport.class, comp) != null) {
                int units;
                switch (e.getScrollType()) {
                    case MouseWheelEvent.WHEEL_UNIT_SCROLL:
                        units = e.getUnitsToScroll();
                        break;
                    case MouseWheelEvent.WHEEL_BLOCK_SCROLL:
                        units = e.getWheelRotation();
                        break;
                    default:
                        return;
                }
                FontMetrics fm = comp.getFontMetrics(comp.props.getFont());
                int lineHeight = fm.getHeight() + fm.getMaxDescent() + fm.getLeading();
                Rectangle visBounds = comp.getVisibleRect();
                Rectangle fullBounds = comp.getBounds();
                int amount = units * lineHeight;
                if (units < 0 && visBounds.y > 0) {
                    Rectangle scrollTo = new Rectangle(visBounds.x, visBounds.y + amount, visBounds.width, amount);
                    comp.scrollRectToVisible(scrollTo);
                    e.consume();
                } else if (units > 0 && visBounds.y + visBounds.height < fullBounds.y + fullBounds.height) {
                    Rectangle scrollTo = new Rectangle(visBounds.x, visBounds.y + visBounds.height + lineHeight, visBounds.width, lineHeight);
                    comp.scrollRectToVisible(scrollTo);
                    e.consume();
                }
            }
        }
    }

    public void setLinkListener(Consumer<String> linkListener) {
        boolean wasNull = this.linkListener == null;
        this.linkListener = linkListener;
        if (!wasNull) {
            if (linkListener == null) {
                LinkAdapter adap = null;
                for (MouseListener ml : getMouseListeners()) {
                    if (ml instanceof LinkAdapter) {
                        adap = (LinkAdapter) ml;
                        break;
                    }
                }
                if (adap != null) {
                    removeMouseListener(adap);
                    removeMouseMotionListener(adap);
                }
            }
        } else {
            if (linkListener != null) {
                LinkAdapter adap = new LinkAdapter();
                addMouseListener(adap);
                addMouseMotionListener(adap);
            }
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        md.addLinkListener(addLink);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public void removeNotify() {
        links.clear();
        md.removeLinkListener(addLink);
        ToolTipManager.sharedInstance().unregisterComponent(this);
        super.removeNotify();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Point2D pt = event.getPoint();
        for (RegionOfInterest link : links) {
            if (link.region().contains(pt)) {
                return link.content();
            }
        }
        return super.getToolTipText(event);
    }

    public void setMarkdown(Markdown md) {
        if (md == null) {
            throw new IllegalArgumentException("Null markdown");
        }
        if (md != this.md) {
            this.md.removeLinkListener(addLink);
            links.clear();
            this.md = md;
            md.addLinkListener(addLink);
            invalidate();
            revalidate();
            repaint();
        }
    }

    public Markdown getMarkdown() {
        return md;
    }

    private void addLink(RegionOfInterest link) {
        links.add(link);
    }

    private static int fontSize() {
        String prop = System.getProperty("uiFontSize");
        Integer result = null;
        if (prop != null) {
            try {
                result = Integer.parseInt(prop);
            } catch (NumberFormatException nfe) {
                // ok
            }
        }
        if (result == null) {
            Font f = UIManager.getFont("controlFont");
            if (f != null) {
                return f.getSize();
            }
            f = UIManager.getFont("Label.font");
            if (f != null) {
                return f.getSize();
            }
        }
        return fallbackSize > 0 ? fallbackSize
                : (fallbackSize = new JLabel().getFont().getSize());
    }

    private static Color uiColor(String key, Color fallback) {
        Color result = UIManager.getColor(key);
        return result == null ? fallback : result;
    }

    private static Font uiFont(String key, Supplier<Font> fallback) {
        Font result = UIManager.getFont(key);
        return result == null ? fallback.get() : result;
    }

    public void setMargin(int margin) {
        if (this.margin != margin) {
            this.margin = margin;
            invalidate();
            revalidate();
            repaint();
        }
    }

    public Rectangle2D neededBounds(Rectangle2D within) {
        MarkdownRenderingContext ctx = MarkdownRenderingContext.prerenderContext(this);
        Rectangle2D r = md.render(ctx, props, new Rectangle(0, 0, (int) Math.floor(within.getWidth()), (int) Math.floor(within.getHeight())), imageLoader());
        r.setRect(within.getX(), within.getY(), r.getWidth(), r.getHeight());
        Insets ins = getInsets();
        r.setFrame(r.getX(), r.getY(),
                r.getWidth() + ins.left + ins.right + (margin * 2),
                r.getHeight() + ins.top + ins.bottom + (margin * 2));
        return r;
    }

    private Rectangle2D doRender(MarkdownRenderingContext ctx, Rectangle bds) {
        links.clear();
        return md.render(ctx, props, bds, imageLoader());
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Rectangle bds = null;
        if (getParent() instanceof JViewport && getParent().isShowing()) {
            JViewport vp = (JViewport) getParent();
            bds = vp.getVisibleRect();
        } else {
            bds = getBounds();
            if (bds.isEmpty()) {
                bds = getGraphicsConfiguration().getBounds();
            }
        }
        Insets ins = getInsets();
        bds.x += ins.left + margin;
        bds.y += ins.top + margin;
        bds.height -= (ins.top + ins.bottom) + (margin * 2);
        bds.width -= (ins.left + ins.right) + (margin * 2);

        MarkdownRenderingContext ctx = MarkdownRenderingContext.prerenderContext(this);
        Rectangle2D r2d = doRender(ctx, bds);
        Dimension dim = r2d.getBounds().getSize();
        dim.width += margin + ins.left + ins.right;
        dim.height += margin + ins.top + ins.bottom;
        return dim;
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintComponent((Graphics2D) g);
    }

    private void paintComponent(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, 200);
        Insets ins = getInsets();
        Rectangle r = new Rectangle(ins.left + margin, ins.top + margin, getWidth() - ((margin * 2) + ins.left + ins.right), getHeight() - ((margin * 2) + ins.top + ins.bottom));
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.setFont(getFont());
        g.setColor(getForeground());
        MarkdownRenderingContext ctx = MarkdownRenderingContext.renderContext(g);
        //            g.setClip(r);
        Rectangle2D rect = doRender(ctx, r);

//        g.setColor(Color.BLUE);
//                    g.setColor(new Color(255, 220, 0, 90));
        //            g.fill(rect);
//        for (RegionOfInterest l : links) {
//            g.fill(l.region());
//        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        AccessibleContext result = super.getAccessibleContext();
        return new AD(result);
    }

    class AD extends AccessibleContext {
        // Since getting the text involves I/O, and accessibility methods
        // are frequently uncalled, lazily fetch this.

        private final AccessibleContext delegate;

        private AD(AccessibleContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getAccessibleName() {
            return delegate.getAccessibleName();
        }

        @Override
        public void setAccessibleName(String s) {
            delegate.setAccessibleName(s);
        }

        @Override
        public String getAccessibleDescription() {
            return md.extractPlainText();
        }

        @Override
        public void setAccessibleDescription(String s) {
            delegate.setAccessibleDescription(s);
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return delegate.getAccessibleRole();
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            return delegate.getAccessibleStateSet();
        }

        @Override
        public Accessible getAccessibleParent() {
            return delegate.getAccessibleParent();
        }

        @Override
        public void setAccessibleParent(Accessible a) {
            delegate.setAccessibleParent(a);
        }

        @Override
        public int getAccessibleIndexInParent() {
            return delegate.getAccessibleIndexInParent();
        }

        @Override
        public int getAccessibleChildrenCount() {
            return delegate.getAccessibleChildrenCount();
        }

        @Override
        public Accessible getAccessibleChild(int i) {
            return delegate.getAccessibleChild(i);
        }

        @Override
        public Locale getLocale() throws IllegalComponentStateException {
            return delegate.getLocale();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            delegate.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            delegate.removePropertyChangeListener(listener);
        }

        @Override
        public AccessibleAction getAccessibleAction() {
            return delegate.getAccessibleAction();
        }

        @Override
        public AccessibleComponent getAccessibleComponent() {
            return delegate.getAccessibleComponent();
        }

        @Override
        public AccessibleSelection getAccessibleSelection() {
            return delegate.getAccessibleSelection();
        }

        @Override
        public AccessibleText getAccessibleText() {
            return delegate.getAccessibleText();
        }

        @Override
        public AccessibleEditableText getAccessibleEditableText() {
            return delegate.getAccessibleEditableText();
        }

        @Override
        public AccessibleValue getAccessibleValue() {
            return delegate.getAccessibleValue();
        }

        @Override
        public AccessibleIcon[] getAccessibleIcon() {
            return delegate.getAccessibleIcon();
        }

        @Override
        public AccessibleRelationSet getAccessibleRelationSet() {
            return delegate.getAccessibleRelationSet();
        }

        @Override
        public AccessibleTable getAccessibleTable() {
            return delegate.getAccessibleTable();
        }

        @Override
        public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            delegate.firePropertyChange(propertyName, oldValue, newValue);
        }
    }
}
