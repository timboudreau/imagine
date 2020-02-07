/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.fills;

import java.awt.Color;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public class RadialGradientFill extends GradientFill {

    JSlider slider;

    @Override
    public JComponent getCustomizer() {
        JComponent comp = super.getCustomizer();
        if (slider == null) {
            JLabel radiusLabel = new JLabel(NbBundle.getMessage(RadialGradientFill.class, "LBL_Radius"));
            slider = new JSlider();
            slider.setUI(new PopupSliderUI());
            slider.setMinimum(4);
            slider.setMaximum(1024);
            int lastSliderValue = NbPreferences.forModule(RadialGradientFill.class)
                    .getInt("radialGpRadius", 100);
            slider.setValue(Math.max(4, Math.min(1024, lastSliderValue)));
            slider.addChangeListener(e -> {
                int val = slider.getValue();
                if (val != lastSliderValue) {
                    NbPreferences.forModule(RadialGradientFill.class)
                            .putInt("radialGpRadius", val);
                }
            });
            comp.add(radiusLabel);
            comp.add(slider);
        }
        return comp;
    }

    @Override
    public Paint getPaint() {
        Color a = super.baseColor();
        Color b = ch.getColor();
        int rad = slider == null ? 100 : slider.getValue();
        RadialGradientPaint gp
                = new RadialGradientPaint(500, 500, rad,
                        new float[]{0, 1}, new Color[]{a, b});
        return gp;
    }

    @Override
    protected String getChooserCaption() {
        return NbBundle.getMessage(RadialGradientFill.class,
                "LBL_RadialFirst");
    }

    @Override
    protected String secondChooserCaption() {
        return NbBundle.getMessage(RadialGradientFill.class,
                "LBL_RadialSecond");
    }
}
