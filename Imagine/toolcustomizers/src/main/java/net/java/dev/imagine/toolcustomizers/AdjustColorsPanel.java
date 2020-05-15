/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.colorchooser.ColorChooser;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.SharedLayoutRootPanel;
import org.netbeans.paint.api.components.TilingLayout;
import org.netbeans.paint.api.components.TilingLayout.TilingPolicy;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
final class AdjustColorsPanel extends SharedLayoutRootPanel implements ActionListener, ChangeListener {

    private Color[] colors;
    private final Color[] originals;
    private final ColorChooser[] choosers;
    private double hue, sat, val;
    private final JSlider hueSlider = new JSlider(-100, 100, 0);
    private final JSlider satSlider = new JSlider(-100, 100, 0);
    private final JSlider valSlider = new JSlider(-100, 100, 0);
    private final JPanel subpanel = new JPanel(new TilingLayout(() -> 24, TilingPolicy.FIXED_SIZE));
    private final JLabel hueLabel = new JLabel();
    private final JLabel satLabel = new JLabel();
    private final JLabel valLabel = new JLabel();
    private final JLabel hueValueLabel = new JLabel("0.0");
    private final JLabel satValueLabel = new JLabel("0.0");
    private final JLabel valValueLabel = new JLabel("0.0");
    private final DecimalFormat fmt = new DecimalFormat("#0.0###");

    @Messages({"hue=&Hue", "sat=&Saturation", "val=&Value"})
    @SuppressWarnings("LeakingThisInConstructor")
    public AdjustColorsPanel(Color[] colors) {
        setLayout(new VerticalFlowLayout());
        choosers = new ColorChooser[colors.length];
        this.colors = Arrays.copyOf(colors, colors.length);
        this.originals = Arrays.copyOf(colors, colors.length);
        for (int i = 0; i < colors.length; i++) {
            ColorChooser chooser = new ColorChooser(colors[i]);
            chooser.putClientProperty("ix", i);
            subpanel.add(chooser);
            chooser.addActionListener(this);
            choosers[i] = chooser;
        }
        hueSlider.setName("hue");
        satSlider.setName("sat");
        valSlider.setName("val");
        hueSlider.addChangeListener(this);
        satSlider.addChangeListener(this);
        valSlider.addChangeListener(this);

        Mnemonics.setLocalizedText(hueLabel, Bundle.hue());
        Mnemonics.setLocalizedText(satLabel, Bundle.sat());
        Mnemonics.setLocalizedText(valLabel, Bundle.val());
        hueLabel.setLabelFor(hueSlider);
        satLabel.setLabelFor(satSlider);
        valLabel.setLabelFor(valSlider);
        add(new SharedLayoutPanel(hueLabel, hueSlider, hueValueLabel));
        add(new SharedLayoutPanel(satLabel, satSlider, satValueLabel));
        add(new SharedLayoutPanel(valLabel, valSlider, valValueLabel));

        JButton left = new JButton("<<<");
        left.addActionListener(ae -> {
            Color hold = originals[0];
            System.arraycopy(originals, 1, originals, 0, originals.length - 1);
            originals[originals.length - 1] = hold;
            update();
        });
        JButton right = new JButton(">>>");
        right.addActionListener(ae -> {
            Color hold = originals[originals.length - 1];
            System.arraycopy(originals, 0, originals, 1, originals.length - 1);
            originals[0] = hold;
            update();
        });

        add(new SharedLayoutPanel(left, subpanel, right));
    }

    public Color[] colors() {
        return Arrays.copyOf(colors, colors.length);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ColorChooser ch = (ColorChooser) e.getSource();
        int index = (Integer) ch.getClientProperty("ix");
        colors[index] = ch.getColor();
        originals[index] = ch.getColor();
        update();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JSlider slider = (JSlider) e.getSource();
        switch (slider.getName()) {
            case "hue":
                hue = sliderValue(slider.getValue());
                hueValueLabel.setText(fmt.format(hue));
                break;
            case "sat":
                sat = sliderValue(slider.getValue());
                satValueLabel.setText(fmt.format(sat));
                break;
            case "val":
                val = sliderValue(slider.getValue());
                valValueLabel.setText(fmt.format(val));
                break;
        }
        update();
    }

    private double sliderValue(double sliderValue) {
        return sliderValue / 100;
    }

    void update() {
        for (int i = 0; i < originals.length; i++) {
            Color curr = originals[i];
            colors[i] = adjustColor(curr);
            choosers[i].setColor(colors[i]);
        }
    }

    private final float[] scratch = new float[3];

    private Color adjustColor(Color orig) {
        Color.RGBtoHSB(orig.getRed(), orig.getGreen(), orig.getBlue(), scratch);
        scratch[0] += hue;
        scratch[1] += sat;
        scratch[2] += val;
        Color result = new Color(Color.HSBtoRGB(loopAroundZero(scratch[0]), clamp(scratch[1]), clamp(scratch[2])));
        if (orig.getAlpha() != 255) {
            result = new Color(result.getRed(), result.getGreen(), result.getBlue(), orig.getAlpha());
        }
        return result;
    }

    private float clamp(float val) {
        return Math.max(0, Math.min(1, val));
    }

    private float loopAroundZero(float val) {
        if (val < 1 && val > 0) {
            return val;
        }
        if (val > 1) {
            int ival = (int) val;
            val -= ival;
        } else if (val < 0) {
            val += 1;
        }
        return val;
    }
}
