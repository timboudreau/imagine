package org.netbeans.paint.api.components;

import java.awt.Component;
import java.awt.Font;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 *
 * @author Tim Boudreau
 */
public class FontCellRenderer implements ListCellRenderer {
    private static final DefaultListCellRenderer r = new DefaultListCellRenderer();

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        DefaultListCellRenderer result = (DefaultListCellRenderer) r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Font f = (Font) value;
        if (f == null) {
            result.setText("(no font)");
            result.setFont(new Font("Monospaced", Font.PLAIN, 12));
        } else {
            result.setText(f.getFamily());
            result.setFont(f.deriveFont((float)list.getFont().getSize()));
        }
        return result;
    }
}
