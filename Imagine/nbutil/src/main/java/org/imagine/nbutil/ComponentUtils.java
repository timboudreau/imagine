package org.imagine.nbutil;

import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

/**
 *
 * @author Tim Boudreau
 */
public final class ComponentUtils {

    private ComponentUtils() {
        throw new AssertionError();
    }

    public static Font getFont(Component c) {
        Font f = c.getFont();
        if (f == null) {
            Component fo = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (fo != null) {
                f = fo.getFont();
            }
            if (f == null) {
                f = UIManager.getFont("controlFont");
                if (f == null) {
                    f = UIManager.getFont("Label.font");
                }
                if (f == null) {
                    f = new Font("Arial", Font.PLAIN, 12);
                    String uf = System.getProperty("uiFontSize");
                    if (uf != null) {
                        try {
                            int size = Integer.parseInt(uf);
                            f = f.deriveFont((float) size);
                        } catch (Exception ex) {
                            Logger.getLogger(ComponentUtils.class.getName()).log(Level.INFO, "Bad uiFontSize " + uf, ex);
                        }
                    }
                }
            }
        }
        return f;
    }
}
