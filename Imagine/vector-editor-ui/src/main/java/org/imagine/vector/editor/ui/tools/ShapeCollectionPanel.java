package org.imagine.vector.editor.ui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.java.dev.imagine.api.vector.Textual;
import net.java.dev.imagine.spi.image.LayerImplementation;
import com.mastfrog.geometry.util.GeometryStrings;
import com.mastfrog.geometry.util.PooledTransform;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.netbeans.paint.api.components.dialog.DialogBuilder;
import org.openide.awt.Mnemonics;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
final class ShapeCollectionPanel extends JPanel implements LookupListener, ListSelectionListener {

    private final JList<ShapeElement> list = new JList<>();
    private final Lookup lookup;
    private Lookup.Result<ShapeElement> res;
    private boolean updating;
    private final Consumer<ShapeElement> onChangeSelection;
    private final ShapesCollection coll;
    private final ML ml;
    private final Lookup layerLookup;

    @Messages({
        "ensureOnscreen=&Bring All Shapes Into View",
        "# {0} - shapeCount",
        "opEnsure=Reposition {0} On-Screen",
        "# {0} - oldName",
        "renameShape=Rename {0}"
    })
    ShapeCollectionPanel(ShapesCollection coll, Lookup lookup, Consumer<ShapeElement> onChangeSelection,
            Function<ShapeElement, JPopupMenu> popupProvider, Lookup layerLookup) {
        this.lookup = lookup;
        this.coll = coll;
        this.ml = new ML(popupProvider);
        list.setCellRenderer(new Ren());
        this.onChangeSelection = onChangeSelection;
        setLayout(new BorderLayout());
        JScrollPane pane = new JScrollPane(list);
        pane.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
        pane.setViewportBorder(BorderFactory.createEmptyBorder());
        add(pane, BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        JButton ensureOnscreen = new JButton();
        bottom.add(ensureOnscreen);
        add(bottom, BorderLayout.NORTH);
        Mnemonics.setLocalizedText(ensureOnscreen, Bundle.ensureOnscreen());
        ensureOnscreen.addActionListener(ae -> {
            LayerImplementation layer = layerLookup.lookup(LayerImplementation.class);
            Rectangle2D r = layer.getBounds();
            for (ShapeElement el : coll) {
                Shape s = el.shape();
                Rectangle2D b = s.getBounds2D();
                if (!r.intersects(b)) {
                    double offX = b.getX() - r.getX();
                    double offY = b.getY() - r.getY();
                    coll.edit(Bundle.opEnsure(el.getName()), el, () -> {
                        PooledTransform.withTranslateInstance(-offX, -offY, xf -> {
                            el.applyTransform(xf);
                        });
                    });
                }
            }
        });
        list.addMouseListener(ml);
        this.layerLookup = layerLookup;
    }

    final class ML extends MouseAdapter {

        private final Function<ShapeElement, JPopupMenu> popupProvider;

        public ML(Function<ShapeElement, JPopupMenu> popupProvider) {
            this.popupProvider = popupProvider;
        }

        private ShapeElement itemForEvent(MouseEvent evt) {
            int ix = list.locationToIndex(evt.getPoint());
            if (ix >= 0 && ix < list.getModel().getSize()) {
                return list.getModel().getElementAt(ix);
            }
            return null;
        }

        private boolean maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                ShapeElement target = itemForEvent(e);
                if (target != null) {
                    JPopupMenu menu = popupProvider.apply(target);
                    if (menu != null) {
                        menu.show(ShapeCollectionPanel.this, e.getX(), e.getY());
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!maybeShowPopup(e) && e.getClickCount() == 2) {
                ShapeElement target = itemForEvent(e);
                if (target != null) {
                    String oldName = target.getName();
                    DialogBuilder.forName("shapeName").setTitle(Bundle.renameShape(oldName))
                            .modal().ownedBy((Window) getTopLevelAncestor())
                            .showTextLineDialog(oldName, 1, 96, newName -> {
                                if (!Objects.equals(oldName, newName)) {
                                    target.setName(newName);
                                }
                            });
                }
            }
        }
    }

    @Override
    public void addNotify() {
        res = lookup.lookupResult(ShapeElement.class);
        res.addLookupListener(this);
        sync();
        super.addNotify();
        res.allInstances();
        list.addListSelectionListener(this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        res.removeLookupListener(this);
        res = null;
        list.removeListSelectionListener(this);
        list.setModel(new DefaultListModel<>());
    }

    private void sync() {
        updating = true;
        try {
            Collection<? extends ShapeElement> selection = res.allInstances();
            ShapeElement toSelect = null;
            if (!selection.isEmpty()) {
                toSelect = selection.iterator().next();
            }
            int index = -1;
            DefaultListModel<ShapeElement> all = new DefaultListModel<>();
            int i = 0;
            for (ShapeElement el : coll) {
                if (toSelect != null && toSelect.equals(el)) {
                    index = i;
                }
                all.addElement(el);
                i++;
            }
            list.setModel(all);
            if (index >= 0) {
                list.setSelectedIndex(index);
            }
        } finally {
            updating = false;
        }
    }

    private void checkSync() {
        if (list.getModel().getSize() != coll.size()) {
            sync();
        }
    }

    @Override
    public void paint(Graphics g) {
        checkSync();
        super.paint(g);
    }

    @Override
    public void resultChanged(LookupEvent le) {
        updating = true;
        try {
            ShapeElement sel = lookup.lookup(ShapeElement.class);
            if (sel == null) {
                list.setSelectedIndex(-1);
            } else {
                int ix = -1;
                for (int i = 0; i < list.getModel().getSize(); i++) {
                    ShapeElement item = list.getModel().getElementAt(i);
                    if (item == sel || item.equals(sel)) {
                        ix = i;
                        break;
                    }
                }
                if (ix != -1) {
                    list.setSelectedIndex(ix);
                }
            }
        } finally {
            updating = false;
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!isDisplayable()) {
            return;
        }
        if (!updating) {
            ShapeElement el = list.getSelectedValue();
            onChangeSelection.accept(el);
        }
    }

    static class Ren extends DefaultListCellRenderer {

        private final Map<ShapeElement, ShapeElementIcon> icons = new WeakHashMap<>();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            ShapeElement el = (ShapeElement) value;
            if (el.item().is(Textual.class)) {
                value = el.getName() + "\"" + el.item().as(Textual.class).getText() + "\"";
            } else {
                value = el.getName();
            }
            JLabel result = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            result.setIcon(iconFor(el));
            return result;
        }

        private Icon iconFor(ShapeElement el) {
            ShapeElementIcon icon = icons.get(el);
            if (icon == null) {
                icon = new ShapeElementIcon(el);
                icons.put(el, icon);
            }
            return icon;
        }

        static class ShapeElementIcon implements Icon {

            private final ShapeElement el;

            public ShapeElementIcon(ShapeElement el) {
                this.el = el;
            }

            @Override
            public void paintIcon(Component c, Graphics gg, int x, int y) {
                Graphics2D g = (Graphics2D) gg.create();
                GraphicsUtils.setHighQualityRenderingHints(g);
                try {
                    Color bg = c.getBackground();
                    g.setColor(bg);
                    g.fillRect(x, y, getIconWidth(), getIconHeight());
                    Shape shape = el.shape();
                    Rectangle2D r = shape.getBounds2D();
                    PooledTransform.withTranslateInstance(-r.getX(), -r.getY(), xl -> {
                        double scaleX = getIconWidth() / r.getWidth();
                        double scaleY = getIconHeight() / r.getHeight();
                        PooledTransform.withScaleInstance(scaleX, scaleY, xs -> {
                            xl.preConcatenate(xs);
                            Rectangle2D transf = xl.createTransformedShape(r).getBounds2D();
                            Shape xf = xl.createTransformedShape(shape);
                            g.setColor(c.getForeground());
//                            g.transform(xl);
                            g.draw(xf);
                        });
                    });
                } finally {
                    g.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return 32;
            }

            @Override
            public int getIconHeight() {
                return 32;
            }
        }
    }
}
