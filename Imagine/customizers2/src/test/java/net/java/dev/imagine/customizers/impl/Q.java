package net.java.dev.imagine.customizers.impl;

import net.java.dev.imagine.spi.customizers.Customizes;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
@Customizes(value = Integer.class)
public class Q extends Widget {
    public Q(Scene scene, Integer val) {
        super(scene);
    }
}
