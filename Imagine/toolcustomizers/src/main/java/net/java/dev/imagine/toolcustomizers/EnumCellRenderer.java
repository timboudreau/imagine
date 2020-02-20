/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.Component;
import java.util.MissingResourceException;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
final class EnumCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof Enum<?>) {
            Enum<?> en = (Enum<?>) value;
            String name = en.name();
            try {
                name = NbBundle.getMessage(EnumCellRenderer.class, en.name());
            } catch (MissingResourceException mre) {
                // do nothing
            }
            value = name;
        }
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); //To change body of generated methods, choose Tools | Templates.
    }

}
