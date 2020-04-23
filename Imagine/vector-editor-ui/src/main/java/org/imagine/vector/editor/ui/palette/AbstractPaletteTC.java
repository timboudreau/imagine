package org.imagine.vector.editor.ui.palette;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
abstract class AbstractPaletteTC extends TopComponent {

    private HBL hbl;

    @Override
    public void addNotify() {
        super.addNotify();
        HBL hbl = this.hbl == null ? this.hbl = new HBL(this) : this.hbl;
        addHierarchyBoundsListener(hbl);
    }

    protected abstract String preferredID();

    @Override
    public void removeNotify() {
        assert hbl != null : "removeNotify called asymmetrically";
        removeHierarchyBoundsListener(hbl);
        super.removeNotify();
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_ALWAYS;
    }

    @Override
    public Dimension getPreferredSize() {
        return preferredSize(this);
    }

    @Override
    public Dimension getMaximumSize() {
        return preferredSize(this);
    }

    @Override
    public Dimension getMinimumSize() {
        return preferredSize(this);
    }

    private static Dimension preferredSize(JComponent of) {
        Container c = of.getTopLevelAncestor();
        Dimension size = c.getSize();
        if (c != null) {
            if (size.width > 0 && size.height > 0) {
                return adjustDimension(size);
            }
        }
        Frame f = WindowManager.getDefault().getMainWindow();
        if (f != null && f.isDisplayable()) {
            size = ((JFrame) f).getContentPane().getSize();
            if (size.width > 0 && size.height > 0) {
                return adjustDimension(size);
            }
        }
        f = Frame.getFrames()[0];
        if (f != null && f.isDisplayable()) {
            size = ((JFrame) f).getContentPane().getSize();
            if (size.width > 0 && size.height > 0) {
                return adjustDimension(size);
            }
        }
        Dimension d = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds().getSize();
        return adjustDimension(d);
    }

    private static Dimension adjustDimension(Dimension size) {
        size.width /= 4.5;
        size.height /= 2;
        return size;
    }

    static class HBL implements HierarchyBoundsListener {

        private final Dimension size;
        private final JComponent comp;

        HBL(JComponent c) {
            comp = c;
            size = c.getParent().getSize();
        }

        @Override
        public void ancestorMoved(HierarchyEvent e) {
            // do nothing
        }

        @Override
        public void ancestorResized(HierarchyEvent e) {
            Dimension newSize = comp.getParent().getSize();
            if (!newSize.equals(size)) {
                size.setSize(newSize);
                comp.invalidate();
            }
        }
    }
}
