/*
 * ColorCustomizer.java
 *
 * Created on September 30, 2006, 2:08 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizerSupport;
import net.java.dev.colorchooser.ColorChooser;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public class ColorCustomizer extends ListenableCustomizerSupport<Color>
        implements ActionListener, ListenableCustomizer<Color> {

    private int red;
    private int green;
    private int blue;
    private int alpha;
    private final String name;

    public ColorCustomizer(String name, Color existingValue) {
        this.name = name;
        Preferences p = NbPreferences.forModule(ColorCustomizer.class);
        if (existingValue == null) {
            red = p.getInt(name + ".red", 128); //NOI18N
            green = p.getInt(name + ".green", 128); //NOI18N
            blue = p.getInt(name + ".blue", 230); //NOI18N
            alpha = p.getInt(name + ".alpha", 255); //NOI18N
        } else {
            red = existingValue.getRed();
            green = existingValue.getGreen();
            blue = existingValue.getBlue();
            alpha = existingValue.getAlpha();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JComponent getComponent() {
        JLabel lbl = new JLabel(getName());
        Color color = new Color(red, green, blue, alpha);
        ColorChooser chooser = new ColorChooser(color);
        lbl.setLabelFor(chooser);
        JPanel slp = new SharedLayoutPanel(lbl, chooser);
        chooser.addActionListener(this);
        return slp;
    }

    @Override
    protected void onAfterFire() {
        saveValue(get());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ColorChooser ch = (ColorChooser) e.getSource();
        Color c = ch.getColor();
        red = c.getRed();
        green = c.getGreen();
        blue = c.getBlue();
        alpha = c.getAlpha();
        fire();
    }

    @Override
    public Color get() {
        return new Color(red, green, blue, alpha);
    }

    protected void saveValue(Color value) {
        int r = value.getRed();
        int g = value.getGreen();
        int b = value.getBlue();
        int a = value.getAlpha();
        Preferences p = NbPreferences.forModule(ColorCustomizer.class);
        p.putInt(getName() + ".red", r); //NOI18N
        p.putInt(getName() + ".green", g); //NOI18N
        p.putInt(getName() + ".blue", b); //NOI18N
        p.putInt(getName() + ".alpha", a); //NOI18N
    }

}
