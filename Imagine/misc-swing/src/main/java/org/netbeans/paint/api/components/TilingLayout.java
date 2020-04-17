package org.netbeans.paint.api.components;

import com.mastfrog.function.IntBiFunction;
import com.mastfrog.function.state.Int;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import static java.lang.Math.floor;
import static java.lang.Math.sqrt;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

/**
 *
 * @author Tim Boudreau
 */
public class TilingLayout implements LayoutManager2 {

    private final IntSupplier dimension;
    private final TilingPolicy policy;
    private TilingInfo info;

    public TilingLayout(IntSupplier dimension) {
        this(dimension, TilingPolicy.BEST_FIT);
    }

    public TilingLayout(IntSupplier dimension, TilingPolicy policy) {
        this.dimension = dimension;
        this.policy = policy;
    }

    public enum TilingPolicy {
        FIXED_SIZE,
        BEST_FIT,
        VERTICAL,
        HORIZONTAL;

        boolean isGrow() {
            return this != FIXED_SIZE;
        }
    }

    static int nextPerfectSquare(int count) {
        int nextRoot = (int) Math.floor(Math.sqrt(count)) + 1;
        return nextRoot * nextRoot;
    }

    private PackingStrategy packingStrategy(Container container) {
        int count = container.getComponentCount();
        int dim = dimension.getAsInt();
        if (count == 0 || dim == 0) {
            return new NoPackingStrategy();
        }
        switch (policy) {
            case FIXED_SIZE:
                return new SimplePackingStrategy();
            case VERTICAL:
                return new VerticalPackingStrategy();
            case HORIZONTAL:
                return new HorizontalPackingStrategy();
            case BEST_FIT:
                Rectangle usableBounds = usableBounds(container);
                int fitInWidth = usableBounds.width / dim;
                int fitInHeight = usableBounds.height / dim;
                if (fitInWidth >= count && usableBounds.height < dim * 2) {
                    return new HorizontalPackingStrategy();
                }
                if (fitInHeight >= count && usableBounds.width < dim * 2) {
                    return new VerticalPackingStrategy();
                }
                double sq = Math.sqrt(count);
                if (sq == Math.floor(sq)) {
                    return new SquaredPackingStrategy();
                }
                return new RectangularPackingStrategy();
            default:
                throw new AssertionError(policy);
        }
    }

    private TilingInfo justify(Container container) {
        return packingStrategy(container).pack(container,
                dimension.getAsInt(), usableBounds(container),
                container.getInsets(), container.getComponentCount());
    }

    @Override
    public void layoutContainer(Container parent) {
        TilingInfo info = info(parent);
        info.layoutContainer(parent, policy.isGrow());
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return info(parent).pref(parent);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return info(parent).min(parent);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return info(target).max(target);
    }

    private void discardCache() {
        info = null;
    }

    private TilingInfo info(Container container) {
        if (info == null) {
            return info = justify(container);
        } else if (!info.isUpToDate(container)
                || info.targetDimension != dimension.getAsInt()) {
            return info = justify(container);
        }
        return info;
    }

    static Rectangle usableBounds(Container c) {

        Rectangle r = c.getBounds();
        Insets ins = c.getInsets();
        r.width -= ins.left + ins.right;
        r.height -= ins.top + ins.bottom;
        r.x += ins.left;
        r.y += ins.top;
        if (c.getParent() instanceof JViewport) {
            Rectangle visible = ((JViewport) c.getParent()).getViewRect();
            visible = SwingUtilities.convertRectangle(c.getParent(), visible, c);
            r = visible.intersection(r);
        }
        return r;
    }

    static Dimension withInsets(Container c, Dimension d) {
        Insets ins = c.getInsets();
        int addX = ins.left + ins.right;
        int addY = ins.top + ins.bottom;
        d.width += addX;
        d.height += addY;
        return d;
    }

    interface PackingStrategy {

        TilingInfo pack(Container container, int targetDimension, Rectangle usableBounds, Insets ins, int count);
    }

    static class HorizontalPackingStrategy implements PackingStrategy {

        @Override
        public TilingInfo pack(Container container, int targetDimension,
                Rectangle usableBounds, Insets ins, int count) {
            int sizeByAvailableWidth = count == 0 ? usableBounds.width
                    : usableBounds.width / count;
            int rem = usableBounds.width - (sizeByAvailableWidth * count);
            TilingInfo result = new TilingInfo(targetDimension, count, usableBounds,
                    targetDimension * count, targetDimension * count, targetDimension,
                    targetDimension, count, ins, sizeByAvailableWidth,
                    usableBounds.height);
            if (rem != 0) {
                result.xDistrib = new PixelRedistributorImpl(usableBounds.width, count, sizeByAvailableWidth);
            }
            return result;
        }
    }

    static class RectangularPackingStrategy implements PackingStrategy {

        private TilingInfo findBestDivisors(Rectangle usableBounds, int targetDimension,
                int count, IntBiFunction<TilingInfo> f) {
            if (count < 2 || usableBounds.width == 0 || usableBounds.height == 0) {
                return f.apply(1, 1);
            }
            if (count == 2) {
                if (usableBounds.width < usableBounds.height) {
                    return f.apply(1, 2);
                } else {
                    return f.apply(2, 1);
                }
            }
            double sqrt = sqrt(count);
            if (sqrt == Math.floor(sqrt)) {
                int sq = (int) sqrt;
                return f.apply(sq, sq);
            }
            if (count % 5 == 0) {
                if (usableBounds.width < usableBounds.height) {
                    return f.apply(count / 5, 5);
                } else {
                    return f.apply(5, count / 5);
                }
            }
            int gcd = greatestCommonDivisor(usableBounds.width, usableBounds.height);
            for (int i = 1; gcd == usableBounds.width || gcd == usableBounds.height || gcd == 0 || gcd == 1 /*|| gcd == 2*/; i++) {
                gcd = greatestCommonDivisor(usableBounds.width + i, usableBounds.height);
                if (gcd == usableBounds.width || gcd == usableBounds.height || gcd == 0 || gcd == 1 /*|| gcd == 2*/) {
                    gcd = greatestCommonDivisor(usableBounds.width, usableBounds.height + i);
                }
                if (gcd == usableBounds.width || gcd == usableBounds.height || gcd == 0 || gcd == 1 /* || gcd == 2 */) {
                    gcd = greatestCommonDivisor(usableBounds.width + i, usableBounds.height + i);
                }
            }
            while (gcd > count) {
                gcd /= 2;
            }

            int a = usableBounds.width / gcd;
            int b = usableBounds.height / gcd;
            int countA = count / gcd;
            int countB = count / countA;
            if (count > 3) {
                if (countA == 2) {
                    countA++;
                    countB = count / countA;
                } else if (countB == 2) {
                    countB++;
                    countA = count / countB;
                } else if (countA == 1) {
                    countA += 2;
                    countB = count / countA;
                } else if (countB == 1) {
                    countB += 2;
                    countA = count / countB;
                }
            }
            if (countA * countB < count) {
                countB++;
            }
            if (usableBounds.width > usableBounds.height) {
                return f.apply(Math.max(countA, countB), Math.min(countA, countB));
            } else {
                return f.apply(Math.min(countA, countB), Math.max(countA, countB));
            }
        }

        @Override
        public TilingInfo pack(Container container, int targetDimension,
                Rectangle usableBounds, Insets ins, int count) {
            return findBestDivisors(usableBounds, targetDimension, count, (across, down) -> {

                int diff = (across * down) - count;
                if (diff >= across && down > 1) {
                    down -= diff / across;
                }
                int width = usableBounds.width / across;
                int height = usableBounds.height / down;

                if (down * height < usableBounds.height) {
                    double dist = (usableBounds.height) - (down * height);
                    height += dist / down;
                }
                int remX = usableBounds.width - (width * across);
                int remY = usableBounds.height - (height * down);
                TilingInfo result = new TilingInfo(targetDimension, across, usableBounds,
                        targetDimension * across, targetDimension * across,
                        targetDimension * down, targetDimension * count, count, ins,
                        width, height);
                if (remX != 0) {
                    result.xDistrib = new PixelRedistributorImpl(across, remX);
                }
                if (remY != 0) {
                    result.yDistrib = new PixelRedistributorImpl(down, remY);
                }
                return result;
            });
        }
    }

    static class SquaredPackingStrategy implements PackingStrategy {

        @Override
        public TilingInfo pack(Container container, int targetDimension, Rectangle usableBounds, Insets ins, int count) {
            if (count == 0) {
                return new TilingInfo(0, 0, usableBounds, 0, 0, 0, 0, 0, ins, 0, 0);
            }
            double sq = sqrt(count);
            int columns = (int) floor(sq);
            int rows = columns;
            if (sq != floor(sq)) { // not a perfect square
                rows = count / columns;
                if (count % columns != 0) {
                    rows++;
                }
            }
            int targetWidth = targetDimension * columns;
            int targetHeight = targetDimension * rows;

            int realWidth = targetDimension;
            int remX = 0;
            if (usableBounds.width / targetDimension != columns) {
                realWidth = usableBounds.width / columns;
                remX = usableBounds.width - (realWidth * columns);
            }
            int realHeight = targetDimension;
            int remY = 0;
            if (usableBounds.height / targetDimension != rows) {
                realHeight = usableBounds.height / rows;
                remY = usableBounds.height - (realHeight * rows);
            }
            TilingInfo result = new TilingInfo(targetDimension, columns, usableBounds,
                    targetWidth, targetWidth, targetHeight, targetHeight, count, ins,
                    realWidth, realHeight);

            if (remX != 0) {
                result.xDistrib = new PixelRedistributorImpl(columns, remX);
            }
            if (remY != 0) {
                result.yDistrib = new PixelRedistributorImpl(rows, remY);
            }
            return result;
        }
    }

    static class VerticalPackingStrategy implements PackingStrategy {

        @Override
        public TilingInfo pack(Container container, int targetDimension,
                Rectangle usableBounds, Insets ins, int count) {
            int sizeByAvailableHeight = count == 0 ? usableBounds.height
                    : usableBounds.height / count;
            int rem = usableBounds.height - (sizeByAvailableHeight * count);
            TilingInfo result = new TilingInfo(targetDimension, 1,
                    usableBounds, targetDimension, targetDimension,
                    targetDimension * count, targetDimension * count, count, ins,
                    usableBounds.width, sizeByAvailableHeight);
            if (rem != 0) {
                result.yDistrib = new PixelRedistributorImpl(usableBounds.height,
                        count, sizeByAvailableHeight);
            }
            return result;
        }
    }

    static class NoPackingStrategy implements PackingStrategy {

        @Override
        public TilingInfo pack(Container container, int targetDimension, Rectangle usableBounds, Insets ins, int count) {
            return new TilingInfo(0, 0, usableBounds, 0, 0, 0, 0, 0, ins);
        }
    }

    static class SimplePackingStrategy implements PackingStrategy {

        @Override
        public TilingInfo pack(Container container, int targetDimension, Rectangle usableBounds, Insets ins, int count) {
            double sq = sqrt(count);
            int targetRows = (int) floor(sq);
            if (count % targetRows != 0) {
                targetRows++;
            }
            int columns = Math.max(1, usableBounds.width / targetDimension);
            int rows = Math.max(1, count / columns);
            return new TilingInfo(targetDimension, columns, usableBounds, targetDimension * count,
                    targetDimension * columns,
                    targetDimension * targetRows, targetDimension * rows, count, ins);
        }

    }

    private interface PixelRedistributor {

        int additionalSize(int pos);
    }

//    private static class NextToLastRowPackingPixelRedistributor implements PixelRedistributor {
//
//        private int nextToLastRow;
//
//
//        @Override
//        public int additionalSize(int pos) {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//    }
//
    private static class PixelRedistributorImpl implements PixelRedistributor {

        private final int remainder;
        private final int count;

        public PixelRedistributorImpl(int count, int remainder) {
            this.count = count;
            this.remainder = remainder;
        }

        public PixelRedistributorImpl(int pxPerRowOrColumn, int itemsPerRowOrColumn,
                int itemSize) {
            this.count = itemsPerRowOrColumn;
            int widthAlreadyUsed = itemSize * itemsPerRowOrColumn;
            remainder = pxPerRowOrColumn - widthAlreadyUsed;
        }

        private boolean shouldAdd(int pos) {
            if (remainder == 0) {
                return false;
            }
            int absRem = Math.abs(remainder);
            if (count > absRem / 2) {
                int every = (count / absRem);
                if (pos % every == 0) {
                    return true;
                }
                if (count % absRem != 0) {
                    if (pos < absRem * (count / absRem)) {
                        return true;
                    }
                }
            }
            return true;
        }

        public int additionalSize(int pos) {
            if (pos < Math.abs(remainder)) {
//            if (shouldAdd(pos)) {
                int result = remainder > 0 ? 1 : -1;
                return result;
            }
            return 0;
        }
    }

    static class TilingInfo {

        final int targetDimension;
        final int columns;
        final Rectangle usableBounds;
        final int minWidth;
        final int prefWidth;
        final int minHeight;
        final int prefHeight;
        final int tileCount;
        final Insets ins;
        final int dimX;
        final int dimY;
        PixelRedistributor xDistrib;
        PixelRedistributor yDistrib;

        void size(Dimension into, int x, int y) {
            int width = dimX;
            int height = dimY;
            if (xDistrib != null) {
                int newWidth = width + xDistrib.additionalSize(x);
                if (newWidth > 0) {
                    width = newWidth;
                }
            }
            if (yDistrib != null) {
                int newHeight = height + yDistrib.additionalSize(y);
                if (newHeight > 0) {
                    height = newHeight;
                }
            }
            into.width = width;
            into.height = height;
        }

        boolean isUpToDate(Container c) {
            Rectangle ub = usableBounds(c);
            if (!usableBounds.equals(ub)) {
                return false;
            }
            if (c.getComponentCount() != tileCount) {
                return false;
            }
            return true;
        }

        Dimension pref(Container c) {
            return withInsets(new Dimension(prefWidth, prefHeight));
        }

        Dimension min(Container c) {
            return withInsets(new Dimension(minWidth, minHeight));
        }

        Dimension max(Container c) {
            if (tileCount == 0) {
                return withInsets(new Dimension(0, 0));
            }
            if (tileCount == 1) {
                return withInsets(new Dimension(targetDimension, targetDimension));
            }
            double sqrt = Math.sqrt(tileCount);
            int s = (int) Math.ceil(sqrt);
            return withInsets(new Dimension(s * targetDimension, s * targetDimension));
        }

        Dimension withInsets(Dimension d) {
            int addX = ins.left + ins.right;
            int addY = ins.top + ins.bottom;
            d.width += addX;
            d.height += addY;
            return d;
        }

        TilingInfo(int dimension, int columns, Rectangle usableBounds,
                int minWidth, int prefWidth, int minHeight, int prefHeight,
                int tileCount, Insets ins) {
            this(dimension, columns, usableBounds, minWidth, prefWidth,
                    minHeight, prefHeight, tileCount, ins,
                    dimension, dimension);
        }

        public void layoutContainer(Container parent, boolean grow) {
            if (columns == 0 || dimX == 0 || dimY == 0) {
                return;
            }
            int x = ins.left;
            int y = ins.top - dimY;
            int rowCount = Math.max(1, tileCount / columns);
            int row = -1;
            int bottom = ins.top + usableBounds.height;
            int right = ins.left + usableBounds.width;
            Dimension dim = new Dimension(dimX, dimY);
            int column;
            for (int i = 0; i < tileCount; i++) {
                column = columns == 1 ? 0 : i % columns;
                boolean newRow = column == 0;
                if (newRow) {
                    x = ins.left;
                    y += dim.height;
                    row++;
                }
                dim.width = dimX;
                dim.height = dimY;
                Component c = parent.getComponent(i);
                size(dim, column, row);

                boolean lastColumn = (i + 1) % columns == 0;
//            boolean lastRow = row == rowCount - 1;
//            if (lastRow && grow) {
//                if (y + dim.height != bottom) {
//                    System.out.println("Last row " + row + " change height from " + dim.height + " to " + (bottom - y)
//                            + " for " + i + " with y " + y + " and bottom " + bottom);
//                    dim.height = bottom - y;
//                }
//            }
                if (lastColumn && grow) {
                    if (x + dim.height != right) {
                        dim.width += right - (x + dim.width);
                    }
                }
                c.setBounds(x, y, dim.width, dim.height);
                x += dim.width;
            }

        }

        TilingInfo(int dimension, int columns, Rectangle usableBounds,
                int minWidth, int prefWidth, int minHeight, int prefHeight,
                int tileCount, Insets ins, int dimX, int dimY) {
            this.targetDimension = dimension;
            this.columns = columns;
            this.usableBounds = usableBounds;
            this.minWidth = minWidth;
            this.prefWidth = prefWidth;
            this.minHeight = minHeight;
            this.prefHeight = prefHeight;
            this.tileCount = tileCount;
            this.ins = ins;
            this.dimX = dimX;
            this.dimY = dimY;
        }
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        discardCache();
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0;
    }

    @Override
    public void invalidateLayout(Container target) {
        discardCache();
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        discardCache();
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        discardCache();
    }

    public static void main(String[] args) {
        Int sz = Int.of(36);
        JPanel pnl = new JPanel(new TilingLayout(sz));
        pnl.setBorder(BorderFactory.createLineBorder(Color.GRAY, 13));
        JFrame jf = new JFrame();
        jf.setLayout(new BorderLayout());

        JPanel count = new JPanel();
        JSlider countSlider = new JSlider(0, 100, 0);
        JLabel countLabel = new JLabel();
        JLabel countTitle = new JLabel("Count");
        countSlider.addChangeListener(ce -> {
            int nue = countSlider.getValue();
            int old = pnl.getComponentCount();
            if (nue > old) {
                while (pnl.getComponentCount() < nue) {
                    pnl.add(new Tile(pnl.getComponentCount() + 1));
                }
            } else if (nue < old) {
                while (pnl.getComponentCount() > nue) {
                    pnl.remove(pnl.getComponentCount() - 1);
                }
            }
            countLabel.setText(Integer.toString(pnl.getComponentCount()));
            pnl.invalidate();
            pnl.revalidate();
            pnl.repaint();
        });
        countSlider.setValue(15);
        count.add(countTitle);
        count.add(countSlider);
        count.add(countLabel);

        JPanel dimPanel = new JPanel();
        JSlider slider = new JSlider(10, 50, 36);
        JLabel dimLabel = new JLabel("36");
        JLabel dimTitle = new JLabel("Dimension");
        dimPanel.add(dimTitle);
        slider.addChangeListener(ce -> {
            sz.set(slider.getValue());
            dimLabel.setText(Integer.toString(slider.getValue()));
            pnl.invalidate();
            pnl.revalidate();
            pnl.repaint();
        });
        dimPanel.add(slider);
        dimPanel.add(dimLabel);
        JButton b = new JButton("Repack");
        b.addActionListener(ae -> jf.pack());
        dimPanel.add(b);

        jf.add(pnl, BorderLayout.CENTER);
        jf.add(count, BorderLayout.SOUTH);
        jf.add(dimPanel, BorderLayout.NORTH);

        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.pack();
        jf.setVisible(true);
    }

    static class Tile extends JComponent {

        private final Color color;
        private static final Border LINE = BorderFactory.createLineBorder(Color.BLACK, 1);
        private final int index;

        Tile(int ix) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            color = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
            setBorder(LINE);
            this.index = ix;
        }

        private Color txt;

        private Color textColor() {
            if (txt == null) {
                float[] f = new float[3];
                Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), f);
                f[0] = 1F - f[0];
//                f[1] = Math.max(0.4F, 1F-f[1]);
                f[1] = 1F - Math.max(0F, Math.min(1F, Math.abs((f[1] - 0.5F) * 2F)));
                f[2] = 1F - Math.max(0F, Math.min(1F, Math.abs((f[2] - 0.5F) * 2F)));
                return txt = new Color(Color.HSBtoRGB(f[0], f[1], f[2]));
            }
            return txt;
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            g.setColor(color);
            g.fillRect(0, 0, w, h);
            g.setColor(textColor());
            String txt = Integer.toString(index);
            FontMetrics fm = g.getFontMetrics();
            int txtW = fm.stringWidth(txt);
            int y = ((h / 2) - ((fm.getMaxAscent() + fm.getMaxDescent()) / 2))
                    + fm.getAscent();
            int x = (w / 2) - (txtW / 2);
            g.drawString(txt, x, y);
        }
    }
}
