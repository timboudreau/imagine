package org.imagine.vector.editor.ui.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 *
 * @author Tim Boudreau
 */
final class ShapeCollectionPanel extends JPanel implements LookupListener {

    private final JList<ShapeElement> list = new JList<>();
    private final Lookup lookup;
    private Lookup.Result<ShapeElement> res;

    ShapeCollectionPanel(ShapesCollection coll, Lookup lookup) {
        this.lookup = lookup;
        list.setCellRenderer(new Ren());
        DefaultListModel<ShapeElement> all = new DefaultListModel<>();
        for (ShapeElement el : coll) {
            all.addElement(el);
        }
        list.setModel(all);
        setLayout(new BorderLayout());
        JScrollPane pane = new JScrollPane(list);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setViewportBorder(BorderFactory.createEmptyBorder());
        add(pane, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        res = lookup.lookupResult(ShapeElement.class);
        res.addLookupListener(this);
        super.addNotify();
        res.allItems();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        res.removeLookupListener(this);
        res = null;
    }

    @Override
    public void resultChanged(LookupEvent le) {
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
    }

    static class Ren extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            ShapeElement el = (ShapeElement) value;
            value = el.getName();
            Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return result;
        }

    }
}
