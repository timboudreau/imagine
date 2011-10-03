package net.java.dev.imagine.customizers.impl;

import javax.swing.JPanel;
import net.java.dev.imagine.spi.customizers.Customizes;

/**
 *
 * @author Tim Boudreau
 */
@Customizes(value = Integer.class)
public class X extends JPanel {
    public X(Integer val) {
    }
}
