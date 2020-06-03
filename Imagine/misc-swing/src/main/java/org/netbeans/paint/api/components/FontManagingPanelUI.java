package org.netbeans.paint.api.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.CellRendererPane;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicPanelUI;
import javax.swing.plaf.metal.MetalComboBoxButton;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellRenderer;
import net.java.dev.colorchooser.ColorChooser;
import org.netbeans.paint.api.components.fractions.FractionsEditor;
import org.netbeans.paint.api.components.points.PointSelector;

/**
 * A panel UI that forces the font of all descendant components to a specific
 * size. Not pretty, but does the job.
 *
 * @author Tim Boudreau
 */
public class FontManagingPanelUI extends BasicPanelUI {

    private static final AffineTransform DEFAULT_TRANSFORM
            = AffineTransform.getScaleInstance(0.8, 0.8);
    private static final String KEY = "cfaKey";
    private final Font font;
    private final AffineTransform xform;

    public FontManagingPanelUI() {
        this(smallFont(), DEFAULT_TRANSFORM);
    }

    public FontManagingPanelUI(AffineTransform xform) {
        this(xform.isIdentity() ? defaultFont() : defaultFont().deriveFont(xform), xform);
    }

    public FontManagingPanelUI(Font font, AffineTransform xform) {
        this.font = font;
        this.xform = xform;
    }

    static Font smallFont() {
        return defaultFont().deriveFont(DEFAULT_TRANSFORM);
    }

    static Font defaultFont() {
        Font font = UIManager.getFont("controlFont");
        if (font == null) {
            String fontName = System.getProperty("uiFont", "SansSerif");
            Integer val = UIManager.getString("uiFontSize") != null
                    ? UIManager.getInt("uiFontSize") : null;
            if (val == null) {
                val = 14;
            }
            font = new Font(fontName, Font.PLAIN, val);
        }
        return font;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ContainerFontAdjuster listener = (ContainerFontAdjuster) c.getClientProperty(KEY);
        if (listener == null) {
            listener = new ContainerFontAdjuster(c, font, xform);
            c.putClientProperty(KEY, listener);
            listener.attach(c);
        }
        c.addMouseWheelListener(MouseWheel.INSTANCE);
    }

    @Override
    public void uninstallUI(JComponent c) {
        c.removeMouseWheelListener(MouseWheel.INSTANCE);
        ContainerFontAdjuster cfa = (ContainerFontAdjuster) c.getClientProperty(KEY);
        if (cfa != null) {
            cfa.detach(c);
        }
        super.uninstallUI(c);
    }

    /**
     * JPanels by default get a 1-pixel scroll, which is not desirable; this
     * method attaches a listener which translates wheel scroll events into
     * a scroll by a number of pixels matching the component's current font's
     * line-height.
     *
     * @param comp A component
     * @return A runnable which detaches the listener
     */
    public static Runnable manageWheelScrollEvents(JComponent comp) {
        comp.addMouseWheelListener(MouseWheel.INSTANCE);
        return () -> {
            comp.removeMouseWheelListener(MouseWheel.INSTANCE);
        };
    }

    private static class MouseWheel extends MouseAdapter implements MouseWheelListener {

        static MouseWheel INSTANCE = new MouseWheel();

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (!(e.getComponent() instanceof JComponent)) {
                return;
            }
            JComponent comp = (JComponent) e.getComponent();
            Container vp = SwingUtilities.getAncestorOfClass(JViewport.class, comp);
            if (vp != null) {
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
                FontMetrics fm = comp.getFontMetrics(comp.getFont());
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

    private static class ContainerFontAdjuster implements ContainerListener {

        private final JComponent root;
        private final Set<Component> listeningTo = new LinkedHashSet<>();
        private final Font font;
        private final AffineTransform xform;

        ContainerFontAdjuster(JComponent root, Font font, AffineTransform xform) {
            this.root = root;
            this.font = font;
            this.xform = xform;
        }

        Container canAttach(Component comp) {
            if (listeningTo.contains(comp)) {
                return null;
            }
            if (comp instanceof JComponent) {
                if (((JComponent) comp).getClientProperty(KEY) != null) {
                    return null;
                }
            }
            if (comp instanceof JScrollPane) {
                return ((JScrollPane) comp).getViewport();
            }
            if (comp instanceof JLabel
                    || comp instanceof JComboBox
                    || comp instanceof JTree
                    || comp instanceof JList
                    || comp instanceof CellRendererPane
                    || comp instanceof BasicArrowButton
                    || comp instanceof MetalComboBoxButton
                    || comp instanceof ListCellRenderer
                    || comp instanceof TreeCellRenderer
                    || comp instanceof AbstractButton
                    || comp instanceof JSlider
                    || comp instanceof JScrollBar
                    || comp instanceof ColorChooser
                    || comp instanceof JTextComponent) {
                return null;
            }
            if (comp instanceof Container) {
                return (Container) comp;
            }
            return null;
        }

        private void adjustFont(Component comp) {
            if (xform.isIdentity()) {
                return;
            }
            if (comp == null) {
                return;
            }
            if (comp instanceof JTextComponent) {
                JTextComponent jtc = (JTextComponent) comp;
                if (jtc.getClientProperty("font.adjusted") == null) {
                    jtc.putClientProperty("font.adjusted", true);
                    Font f = jtc.getFont();
                    if (f != null) {
                        jtc.setFont(f.deriveFont(xform));
                    }
                }
            } else {
                Font f = comp.getFont();
                if (f != null) {
                    if (f.getStyle() != font.getStyle()) {
                        comp.setFont(font.deriveFont(f.getStyle()));
                    } else {
                        comp.setFont(this.font);
                    }
                }
            }
        }

        private boolean canAdjustFont(Component comp) {
            if (comp instanceof JScrollPane
                    || comp instanceof ColorChooser
                    || comp instanceof JPanel
                    || comp instanceof BasicArrowButton
                    || comp instanceof JViewport
                    || comp instanceof JScrollBar
                    || comp instanceof JSplitPane
                    || comp instanceof CellRendererPane
                    || comp instanceof ListCellRenderer
                    || comp instanceof TreeCellRenderer
                    || comp instanceof ColorChooser
                    || comp instanceof FractionsEditor
                    || comp instanceof PointSelector
                    || comp instanceof JRootPane) {
                return false;
            }
            return true;
        }

        void walkTree(Component comp) {
            if (comp instanceof Container && !(comp instanceof JLabel)) {
                for (Component c : ((Container) comp).getComponents()) {
                    attach(c);
                }
            }
        }

        void attach(Component comp) {
            if (canAdjustFont(comp)) {
                adjustFont(comp);
            }
            if (listeningTo.contains(comp)) {
                return;
            }
            if (comp != root) {
                Container attachTo = canAttach(comp);
                if (attachTo != null) {
                    listeningTo.add(attachTo);
                    attachTo.addContainerListener(this);
                    if (comp instanceof JComponent) {
                        ((JComponent) comp).putClientProperty(KEY, this);
                    }
                    walkTree(attachTo);
                }
            } else {
                ((Container) comp).addContainerListener(this);
            }
        }

        void detach(Component comp) {
            if (comp == root) {
                root.removeContainerListener(this);
                for (Component c : new LinkedHashSet<>(listeningTo)) {
                    detach(c);
                }
                return;
            }
            if (comp instanceof Container) {
                Container maybeAttachedTo = (Container) comp;
                if (maybeAttachedTo instanceof JComponent) {
                    JComponent jc = (JComponent) maybeAttachedTo;
                    Object existing = jc.getClientProperty(KEY);
                    if (existing != this) {
                        return;
                    }
                    jc.putClientProperty(KEY, null);
                }
                maybeAttachedTo.removeContainerListener(this);
                listeningTo.remove(comp);
            }
        }

        @Override
        public void componentAdded(ContainerEvent e) {
            attach(e.getChild());
        }

        @Override
        public void componentRemoved(ContainerEvent e) {
            detach(e.getChild());
        }
    }

    /*
    public static void main(String[] args) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setName("BASE");

        pnl.setUI(new FontManagingPanelUI(
                AffineTransform.getScaleInstance(0.7, 0.7)));

        JPanel sub1 = new JPanel(new VerticalFlowLayout());
        sub1.setName("SUB 1");
        JPanel sub2 = new JPanel(new VerticalFlowLayout());
        sub2.setName("SUB 2");
        JPanel sub3 = new JPanel(new VerticalFlowLayout());
        sub3.setName("SUB 3");
        JPanel sub4 = new JPanel(new VerticalFlowLayout());
        sub4.setName("SUB 4");
        JPanel sub5 = new JPanel(new VerticalFlowLayout());
        sub4.setName("SUB 5");

        System.out.println("\nadd 1\n");
        pnl.add(sub1, BorderLayout.CENTER);

        System.out.println("\nadd 2\n");
        pnl.add(sub2, BorderLayout.EAST);

        System.out.println("\nadd 3\n");
        pnl.add(sub3, BorderLayout.WEST);

        System.out.println("\nadd 3\n");
        pnl.add(sub4, BorderLayout.SOUTH);
        System.out.println("\nadd 3\n");
        pnl.add(sub5, BorderLayout.NORTH);

        IntFunction<Font> f = sz -> {
            return new Font("Arial", Font.PLAIN, sz);
        };

        BiFunction<Integer, String, JLabel> labels = (sz, txt) -> {
            JLabel result = new JLabel(txt);
            result.setFont(f.apply(sz));
            return result;
        };

        BiFunction<Integer, String, JComboBox> boxen = (sz, txt) -> {
            JComboBox result = new JComboBox(new String[]{txt, "foo", "bar"});
            result.setSelectedItem(txt);
            result.setFont(f.apply(sz));
            return result;
        };

        BiFunction<Integer, String, JScrollPane> lists = (sz, txt) -> {
            JList list = new JList(new String[]{txt, "foo", "bar"});
            JScrollPane result = new JScrollPane(list);
            list.setFont(f.apply(sz));
            return result;
        };

        BiFunction<Integer, String, JScrollPane> eds = (sz, txt) -> {
            JEditorPane ed = new JEditorPane("text/plain",
                    "Wookie wookie " + sz + "\n" + txt);
            JScrollPane result = new JScrollPane(ed);
            ed.setFont(new Font("Monospaced", Font.PLAIN, sz + 5));
            return result;
        };

        for (int i = 12; i < 17; i++) {
            JLabel lbl = labels.apply(i, "Size " + i);
            sub1.add(lbl);
            lbl = labels.apply(i * 2, "Size " + i);
            sub2.add(lbl);
            JComboBox box = boxen.apply(i, "Box Size " + i);
            sub3.add(box);
            JScrollPane pn = lists.apply(i, "List Size " + i);
            sub4.add(pn);
            pn = eds.apply(i, "Editor size " + i);
            sub5.add(pn);
        }

        JButton doit = new JButton("Do It");
        sub5.add(doit);
        doit.addActionListener(ae -> {
            pnl.setUI(new BasicPanelUI());
            JLabel lbl = labels.apply(20, "I am big!");
            sub5.add(lbl);
            sub5.invalidate();
            sub5.revalidate();
            sub5.repaint();
        });

        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(pnl);
        jf.pack();
        jf.setVisible(true);
    }
     */
}
