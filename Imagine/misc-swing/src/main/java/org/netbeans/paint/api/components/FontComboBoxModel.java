package org.netbeans.paint.api.components;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ListDataListener;

/**
 *
 * @author Tim Boudreau
 */
public class FontComboBoxModel implements ComboBoxModel<Font> {

    private static final List<String> fontNames = new ArrayList<>();
    private static String lastSelected;
    private String selectedName;

    public FontComboBoxModel() {
        selectedName = lastSelected;
        if (selectedName == null) {
            selectedName = fontNames().get(0);
        }
    }

    public static JComboBox<Font> newFontComboBox() {
        JComboBox result = new JComboBox(new FontComboBoxModel());
        result.setRenderer(FontCellRenderer.instance());
        return result;
    }

    private static List<String> fontNames() {
        if (fontNames.isEmpty()) {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Arrays.sort(names, (a, b) -> {
                return a.compareToIgnoreCase(b);
            });
            fontNames.addAll(Arrays.asList(names));
        }
        return fontNames;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (anItem instanceof String) {
            selectedName = (String) anItem;
            lastSelected = selectedName;
            if (!fontNames.isEmpty()) {
                fontNames.remove(selectedName);
                fontNames.add(0, selectedName);
            }
        } else if (anItem instanceof Font) {
            selectedName = ((Font) anItem).getFamily();
            lastSelected = selectedName;
            if (!fontNames.isEmpty()) {
                fontNames.remove(selectedName);
                fontNames.add(0, selectedName);
            }
        } else if (anItem == null) {
            selectedName = null;
        }
    }

    @Override
    public Object getSelectedItem() {
        if (selectedName == null) {
            return null;
        }
        return new Font(selectedName, Font.PLAIN, 14);
    }

    @Override
    public int getSize() {
        return fontNames().size();
    }

    @Override
    public Font getElementAt(int index) {
        String name = fontNames().get(index);
        return new Font(name, Font.PLAIN, 14);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        // do nothing
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        // do nothing
    }
}
