package org.netbeans.paint.api.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerListener;
import java.awt.event.HierarchyListener;
import java.awt.geom.AffineTransform;
import static java.awt.geom.AffineTransform.getScaleInstance;
import java.awt.image.BufferedImage;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import org.imagine.geometry.util.PooledTransform;

/**
 *
 * @author Tim Boudreau
 */
public final class FontCellRenderer implements ListCellRenderer {

    private static Reference<FontCellRenderer> REF;
    private final FontView view = new FontView();

    private FontCellRenderer() {

    }

    public static FontCellRenderer instance() {
        FontCellRenderer result = REF == null ? null : REF.get();
        if (result == null) {
            result = new FontCellRenderer();
            REF = new SoftReference<>(result);
        }
        return result;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        view.setTargetFont((Font) value);
        view.setPaintFocusIndicator(cellHasFocus && index >= 0);
        if (isSelected) {
            view.setSelectionColor(list.getSelectionBackground());
        } else {
            view.setSelectionColor(null);
        }
        return view;
    }

    static class FontImages {

        private final int gap = 5;
        private final String[] names;
        private final int[] positions;
        private final int[] heights;
        private final BufferedImage image;
        private final int maxWidth;
        private final int maxHeight;

        BufferedImage imageFor(Font font) {
            int ix = Arrays.binarySearch(names, font.getName(),
                    (a, b) -> {
                        return a.compareToIgnoreCase(b);
                    });
            if (ix < 0) {
                return null; // XXX
            }
            int top = positions[ix];
            int height = max(1, heights[ix]);
            return image.getSubimage(0, top,
                    max(1, maxWidth), height);
        }

        static Font[] allFonts() {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Font[] result = new Font[names.length];
            for (int i = 0; i < names.length; i++) {
                Font f = new Font(names[i], Font.PLAIN, 14);
                result[i] = f;
            }
            return result;
        }

        FontImages() {
            names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Arrays.sort(names, (a, b) -> {
                return a.compareToIgnoreCase(b);
            });
            Font[] fonts = new Font[names.length];
            heights = new int[fonts.length];
            positions = new int[fonts.length];
            int[] widths = new int[fonts.length];
            int[] ascents = new int[fonts.length];
            int minHeight, minWidth, maxHeight, maxWidth;
            minHeight = minWidth = Integer.MAX_VALUE;
            maxHeight = maxWidth = Integer.MIN_VALUE;
            int maxAscent = Integer.MIN_VALUE;
            BufferedImage img = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(1, 1, Transparency.OPAQUE);
            Graphics2D g = img.createGraphics();
            try {
                for (int i = 0; i < fonts.length; i++) {
                    Font f = new Font(names[i], Font.PLAIN, 16);
                    fonts[i] = f;
                    g.setFont(f);
                    FontMetrics fm = g.getFontMetrics(f);
                    ascents[i] = fm.getAscent();
                    heights[i] = fm.getHeight();
                    widths[i] = fm.stringWidth(names[i]);
                    maxAscent = max(maxAscent, ascents[i]);
                    minWidth = min(widths[i], minWidth);
                    maxWidth = max(widths[i], maxWidth);
                    minHeight = min(heights[i], minHeight);
                    maxHeight = max(heights[i], maxHeight);
                }
            } finally {
                g.dispose();
                img.flush();
            }
            this.maxHeight = maxHeight;
            this.maxWidth = maxWidth;
            int imageHeight = (maxHeight + gap) * fonts.length;
            img = image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(2 + maxWidth + 2, imageHeight, Transparency.TRANSLUCENT);
            g = img.createGraphics();
            int y = 0;
            try {
//                g.setColor(Color.WHITE);
//                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g.setColor(UIManager.getColor("textText"));
                for (int i = 0; i < fonts.length; i++) {
                    positions[i] = y;
                    boolean problematic = widths[i] < 5;
                    for (int j = 0; j < names[i].length(); j++) {
                        char c = names[i].charAt(j);
                        if (!fonts[i].canDisplay(c)) {
                            problematic = true;
                            break;
                        }
                    }
                    if (problematic) {
                        g.setFont(new Font("Times New Roman", Font.ITALIC, 14));
                        FontMetrics fm = g.getFontMetrics();
                        widths[i] = fm.stringWidth(names[i]);
                        heights[i] = fm.getHeight();
                        ascents[i] = fm.getAscent();
                        minWidth = min(minWidth, widths[i]);
                        minHeight = min(minHeight, heights[i]);
                        maxWidth = max(maxWidth, widths[i]);
                        maxHeight = max(maxHeight, heights[i]);
                    } else {
                        g.setFont(fonts[i]);
                    }
                    if (heights[i] < maxHeight) {
                        Font f = g.getFont();
                        double scale = (double) maxHeight / (double) heights[i];
                        f = f.deriveFont(getScaleInstance(scale, scale));
                        g.setFont(f);
                        ascents[i] = g.getFontMetrics().getAscent();
                        heights[i] = g.getFontMetrics().getHeight();
                    }
                    int fy = y + ascents[i] + 1;
                    g.drawString(names[i], gap, fy);
                    y += heights[i] + gap;
                }
            } finally {
                g.dispose();
            }
        }
    }

    static class FontView extends JComponent {

        private final FontImages images = new FontImages();

        @Override
        public boolean isOpaque() {
            return true;
        }

        @Override
        public String getToolTipText() {
            return targetFont == null ? null : targetFont.getFamily();
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(UIManager.getColor("control"));
            g.fillRect(0, 0, getWidth(), getHeight());
            Font f = targetFont;
            if (f != null) {
                BufferedImage image = images.imageFor(f);
                if (image != null) {
                    AffineTransform xform = null;
                    if (image.getHeight() < getHeight()) {
                        xform = PooledTransform.getTranslateInstance(
                                0, (getHeight() - image.getHeight()) / 2, null);
//                        xform = AffineTransform.getTranslateInstance(
//                                0, (getHeight() - image.getHeight()) / 2);
                    }
                    if (selectionBackground != null) {
                        g.setColor(selectionBackground);
                        g.fillRect(0, 0, images.maxWidth + 5, images.maxHeight + 5);
                        if (paintFocus) {
//                            g.drawRect(1, 1, images.maxWidth - 2, images.maxHeight - 2);
                        }
                    }
                    ((Graphics2D) g).drawRenderedImage(image, xform);
                    if (xform != null) {
                        PooledTransform.returnToPool(xform);
                    }
                } else {
                    System.err.println("No image for font: " + f.getName());
                }
            }
        }
        private Color selectionBackground;
        private boolean paintFocus;

        private void setSelectionColor(Color selectionBackground) {
            this.selectionBackground = selectionBackground;
        }

        private void setPaintFocusIndicator(boolean val) {
            paintFocus = val;
        }
        private Font targetFont;

        private void setTargetFont(Font font) {
            targetFont = font;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(images.maxWidth, images.maxHeight);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public void layout() {
            // do nothing
        }

        @Override
        public void invalidate() {
            // do nothing
        }

        @Override
        public void revalidate() {
            // do nothing
        }

        @Override
        public void repaint() {
            // do nothing
        }

        @Override
        public void repaint(long tm) {
            // do nothing
        }

        @Override
        public void repaint(int x, int y, int width, int height) {
            // do nothing
        }

        @Override
        public void repaint(Rectangle r) {
            // do nothing
        }

        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
            // do nothing
        }

        @Override
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            // do nothing
        }

        @Override
        public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
            // do nothing
        }

        @Override
        public void firePropertyChange(String propertyName, char oldValue, char newValue) {
            // do nothing
        }

        @Override
        public void firePropertyChange(String propertyName, short oldValue, short newValue) {
            // do nothing
        }

        @Override
        public void firePropertyChange(String propertyName, int oldValue, int newValue) {
            // do nothing
        }

        @Override
        public void firePropertyChange(String propertyName, long oldValue, long newValue) {
            // do nothing
        }

        @Override
        public void firePropertyChange(String propertyName, float oldValue, float newValue) {
            // do nothing
        }

        @Override
        public void firePropertyChange(String propertyName, double oldValue, double newValue) {
            // do nothing
        }

        @Override
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
            // do nothing
        }

        @Override
        public void addHierarchyListener(HierarchyListener l) {
            // do nothing
        }

        @Override
        public void addContainerListener(ContainerListener l) {
            // do nothing
        }

        @Override
        public synchronized void addComponentListener(ComponentListener l) {
            // do nothing
        }
    }
}
