package org.netbeans.paint.api.components;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author Tim Boudreau
 */
public class FontComboBoxModel extends DefaultComboBoxModel {

    public FontComboBoxModel() {
        Font times = null;
        Set<String> all = new HashSet<String>();
        for (Font f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            if (all.contains(f.getFamily())) {
                continue;
            }
            all.add(f.getFamily());
            f = f.deriveFont(12F);
            if (times == null && f.getFamily().equals("Times New Roman")) {
                times = f;
            }
            addElement(f);
        }
        setSelectedItem(times);
    }

    @Override
    public void setSelectedItem(Object o) {
        if (o instanceof String) {
            int max = getSize();
            for (int i = 0; i < max; i++) {
                if (o.toString().equals(((Font) getElementAt(i)).getFamily())) {
                    o = getElementAt(i);
                }
            }
        }
        super.setSelectedItem(o);
    }
}
