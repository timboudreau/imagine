package org.netbeans.paint.api.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ListDataListener;

/**
 * Factory for combo boxen over enums.
 *
 * @author Tim Boudreau
 */
public class EnumComboBoxModel<T extends Enum<T>> implements ComboBoxModel<T> {

    private final List<T> values = new ArrayList<>();
    private T selectedItem;
    private final Class<T> type;

    EnumComboBoxModel(T selectedItem) {
        this(selectedItem.getDeclaringClass(), selectedItem);
    }

    EnumComboBoxModel(Class<T> type) {
        this(type, null);
    }

    EnumComboBoxModel(Class<T> type, T selectedItem) {
        this.type = type;
        values.addAll(Arrays.asList(type.getEnumConstants()));
        this.selectedItem = selectedItem == null ? values.isEmpty() ? null
                : values.get(0) : selectedItem;
    }

    public static <T extends Enum<T>> ComboBoxModel<T> newModel(Class<T> type) {
        return new EnumComboBoxModel<>(type);
    }

    public static <T extends Enum<T>> ComboBoxModel<T> newModel(Class<T> type, T selected) {
        assert selected == null || type.isInstance(selected);
        return new EnumComboBoxModel<>(type, selected);
    }

    public static <T extends Enum<T>> ComboBoxModel<T> newModel(T selected) {
        return new EnumComboBoxModel<>(selected);
    }

    public static <T extends Enum<T>> JComboBox<T> newComboBox(Class<T> type) {
        return new JComboBox<>(new EnumComboBoxModel<>(type));
    }

    public static <T extends Enum<T>> JComboBox<T> newComboBox(Class<T> type, T item) {
        assert item == null || type.isInstance(item);
        return new JComboBox<>(new EnumComboBoxModel<>(type, item));
    }

    public static <T extends Enum<T>> JComboBox<T> newComboBox(T item) {
        assert item != null : "Null item - cannot determine type";
        return new JComboBox<>(new EnumComboBoxModel<>(item));
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (anItem instanceof String) {
            String s = (String) anItem;
            for (T val : values) {
                if (s.equals(val.name())) {
                    doSetSelectedItem(val);
                    break;
                } else if (s.equals(val.toString())) {
                    doSetSelectedItem(val);
                    break;
                }
            }
        } else if (type.isInstance(anItem)) {
            doSetSelectedItem(type.cast(anItem));
        } else if (anItem == null) {
            selectedItem = null;
        }
    }

    private void doSetSelectedItem(T item) {
        values.remove(item);
        values.add(0, item);
        selectedItem = item;
    }

    @Override
    public Object getSelectedItem() {
        return selectedItem;
    }

    @Override
    public int getSize() {
        return values.size();
    }

    @Override
    public T getElementAt(int index) {
        return values.get(index);
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
