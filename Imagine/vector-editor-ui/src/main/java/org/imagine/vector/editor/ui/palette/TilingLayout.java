package org.imagine.vector.editor.ui.palette;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JViewport;

/**
 * A layout manager which lays out components as square tiles, and handles
 * scroll panes correctly.
 *
 * @author Tim Boudreau
 */
class TilingLayout implements LayoutManager {

    private final Supplier<Dimension> tileDimensions;

    public TilingLayout(Supplier<Dimension> tileDimensions) {
        this.tileDimensions = tileDimensions;
    }

    public TilingLayout(Dimension targetDimension) {
        this(() -> new Dimension(targetDimension));
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return computePreferredSize(parent);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Dimension dim = tileDimensions.get();
        int count = parent.getComponentCount();
        if (count <= 1) {
            return dim;
        }
        int rootCeil = (int) Math.max(1, Math.ceil(Math.sqrt(count)));
        dim.width *= rootCeil;
        dim.height *= rootCeil;
        return dim;
    }

    private Rectangle boundsRectFor(Container parent) {
        if (parent.getParent() instanceof JViewport) {
            JViewport vp = (JViewport) parent.getParent();
            return vp.getViewRect();
        }
        return new Rectangle(new Point(0, 0), parent.getSize());
    }

    private Rectangle getParentUsableBounds(Container parent) {
        Rectangle r = boundsRectFor(parent);
        Insets ins = parent.getInsets();
        if (parent instanceof JViewport) {
            if (r.x == 0) {
                r.x = ins.left;
            }
            if (r.y == 0) {
                r.y += ins.top;
            }
        } else {
            r.x = ins.left;
            r.y += ins.top;
        }
        r.width = Math.max(0, r.width - (ins.left + ins.right));
        r.height = Math.max(0, r.height - (ins.top + ins.bottom));
        return r;
    }

    private Dimension computePreferredSize(Container parent) {
        Rectangle r = getParentUsableBounds(parent);
        Dimension dim = tileDimensions.get();
        if (r.width == 0 || r.height == 0) {
            return minimumLayoutSize(parent);
        }
        Component[] comps = parent.getComponents();
        Arrays.sort(comps, PaletteItemsPanel::compareComponents);
        boolean singleRow = r.height >= dim.height && (dim.width * comps.length) <= r.width;
        if (singleRow) {
            int x = r.x;
            int maxX = x;
            for (int i = 0; i < comps.length; i++) {
                maxX = Math.max(x + dim.width, maxX);
                Insets ci = insets(comps[i]);
                x += dim.width - ci.right;
            }
            return new Dimension(maxX, r.y + dim.height);
        }
        boolean singleColumn = r.width >= dim.width && (dim.height * comps.length) <= r.height;
        if (singleColumn) {
            int y = r.y;
            int maxY = y;
            for (int i = 0; i < comps.length; i++) {
                maxY = Math.max(maxY, y + dim.height);
                Insets ci = insets(comps[i]);
                y += dim.height - ci.bottom;
            }
            return new Dimension(r.x + dim.width, maxY);
        }
        int oneTileArea = dim.width * dim.height;
        int areaNeeded = oneTileArea * comps.length;
        double areaAvailable = r.width * r.height;
        if (areaAvailable < areaNeeded + oneTileArea) {
            double size = Math.sqrt(areaAvailable) / Math.sqrt(comps.length);
            dim.width = (int) Math.floor(size);
            dim.height = dim.width;
        }
        int y = r.y;
        int x = r.x;
        Insets compInsets;
        int maxX = x;
        int maxY = y;
        for (int i = 0; i < comps.length; i++) {
            maxY = Math.max(maxY, y + dim.height);
            maxX = Math.max(x + dim.width, maxX);
            compInsets = insets(comps[i]);
            x += dim.width - compInsets.right;
            if (x + dim.width > r.width) {
                x = r.x;
                y += dim.height - compInsets.bottom;
                if (y + dim.height > r.height) {
                    dim.height -= (y + dim.height) - r.height;
                }
            }
        }
        return new Dimension(maxX, maxY);
    }

    @Override
    public void layoutContainer(Container parent) {
        Rectangle r = getParentUsableBounds(parent);
        if (r.width == 0 || r.height == 0) {
            return;
        }
        Dimension dim = tileDimensions.get();
        Component[] comps = parent.getComponents();
        Arrays.sort(comps, PaletteItemsPanel::compareComponents);
        boolean singleRow = r.height >= dim.height && (dim.width * comps.length) <= r.width;
        if (singleRow) {
            int x = r.x;
            for (int i = 0; i < comps.length; i++) {
                comps[i].setBounds(x, r.y, dim.width, dim.height);
                Insets ci = insets(comps[i]);
                x += dim.width - ci.right;
            }
            return;
        }
        boolean singleColumn = r.width >= dim.width && (dim.height * comps.length) <= r.height;
        if (singleColumn) {
            int y = r.y;
            for (int i = 0; i < comps.length; i++) {
                comps[i].setBounds(r.x, y, dim.width, dim.height);
                Insets ci = insets(comps[i]);
                y += dim.height - ci.bottom;
            }
            return;
        }
        int oneTileArea = dim.width * dim.height;
        int areaNeeded = oneTileArea * comps.length;
        double areaAvailable = r.width * r.height;
        if (areaAvailable < areaNeeded + oneTileArea) {
            double size = Math.sqrt(areaAvailable) / Math.sqrt(comps.length);
            dim.width = (int) Math.floor(size);
            dim.height = dim.width;
        }
        int y = r.y;
        int x = r.x;
        Insets compInsets;
        for (int i = 0; i < comps.length; i++) {
            comps[i].setBounds(x, y, dim.width, dim.height);
            compInsets = insets(comps[i]);
            x += dim.width - compInsets.right;
            if (x + dim.width > r.width) {
                x = r.x;
                y += dim.height - compInsets.bottom;
                if (y + dim.height > r.height) {
                    dim.height -= (y + dim.height) - r.height;
                }
            }
        }
    }
    private static final Insets scratchInsets = new Insets(0, 0, 0, 0);

    private static Insets insets(Component c) {
        if (c instanceof JComponent) {
            ((JComponent) c).getInsets(scratchInsets);
        } else {
            scratchInsets.top
                    = scratchInsets.left
                    = scratchInsets.bottom
                    = scratchInsets.right
                    = 0;
        }
        return scratchInsets;
    }
}
