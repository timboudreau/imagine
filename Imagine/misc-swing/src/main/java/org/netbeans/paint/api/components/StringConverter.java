package org.netbeans.paint.api.components;

import javax.swing.JSlider;

/**
 *
 * @author Tim Boudreau
 */
public interface StringConverter {
    public static final String CLIENT_PROP_CONVERTER = "converter";

    public String valueToString(JSlider sl);

    public int maxChars();

    public String valueToString(int val);

}
