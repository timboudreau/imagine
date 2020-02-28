/*
 * BooleanCustomizer.java
 *
 * Created on September 30, 2006, 12:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.java.dev.imagine.api.toolcustomizers.AbstractCustomizer;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public class BooleanCustomizer extends AbstractCustomizer<Boolean> implements ActionListener, ListenableCustomizer<Boolean> {

    private boolean value;

    public BooleanCustomizer(String name, Boolean existingValue) {
        super(name);
        this.value = existingValue == null ? false : existingValue;
    }

    public BooleanCustomizer(String name) {
        this(name, null);
    }

    @Override
    public Boolean getValue() {
        return Boolean.valueOf(((JCheckBox) getComponents()[0]).isSelected());
    }

    public boolean isValue() {
        return getValue().booleanValue();
    }

    protected JComponent[] createComponents() {
        JCheckBox cbox = new JCheckBox(getName());
        cbox.setSelected(NbPreferences.forModule(getClass()).getBoolean(getName() + "-bool", true));
        cbox.addActionListener(this);
        return new JComponent[]{cbox};
    }

    @Override
    protected void onAfterFire() {
        saveValue(value);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JCheckBox box = (JCheckBox) e.getSource();
        boolean sel = box.isSelected();
        if (sel != value) {
            value = sel;
            fire();
        }
    }

    @Override
    protected void saveValue(Boolean value) {
        NbPreferences.forModule(getClass()).putBoolean(getName(), value);
    }
}
