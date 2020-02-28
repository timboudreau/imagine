/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.java.dev.imagine.api.toolcustomizers.AbstractCustomizer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public class EnumCustomizer<T extends Enum<T>> extends AbstractCustomizer<T> implements ListenableCustomizer<T> {

    private final Class<T> type;
    T value;
    T[] values;

    public EnumCustomizer(String name, Class<T> type) {
        super(name);
        assert type != null;
        this.type = type;
        values = type.getEnumConstants();
    }

    private T loadValue() {
        if (values.length == 0) {
            return null;
        }
        String saved = NbPreferences.forModule(EnumCustomizer.class)
                .get(type.getSimpleName(), values[0].name());
        if (saved != null) {
            for (T t : values) {
                if (saved.equals(t.name())) {
                    return t;
                }
            }
        }
        return values[0];
    }

    @Override
    protected T getValue() {
        T result = value;
        if (result == null) {
            result = value = loadValue();
        }
        return result;
    }

    @Override
    protected JComponent[] createComponents() {
        JComponent[] result = new JComponent[values.length];
        ButtonGroup grp = new ButtonGroup();
        T val = getValue();
        for (int i = 0; i < values.length; i++) {
            T v = values[i];
            JRadioButton b = new JRadioButton();
            if (v == val) {
                b.setSelected(true);
            }
            Mnemonics.setLocalizedText(b, v.toString());
            b.addActionListener(ae -> {
                value = v;
                change();
                fire();
            });
            grp.add(b);
            result[i] = b;
        }
        return result;
    }

    @Override
    protected void saveValue(T value) {
        String nm = value == null ? "null" : value.name();
        NbPreferences.forModule(EnumCustomizer.class).put(type.getSimpleName(), nm);
    }

}
