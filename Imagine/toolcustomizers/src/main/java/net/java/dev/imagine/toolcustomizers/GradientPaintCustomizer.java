/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizerSupport;
import net.java.dev.colorchooser.ColorChooser;
import org.imagine.editor.api.AspectRatio;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.netbeans.paint.api.components.points.PointSelector;
import org.netbeans.paint.api.components.points.PointSelectorMode;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class GradientPaintCustomizer extends ListenableCustomizerSupport<GradientPaint>
        implements ListenableCustomizer<GradientPaint> {

    private final String name;
    private GradientPaint value;

    public GradientPaintCustomizer() {
        this(defaultName());
    }

    public GradientPaintCustomizer(String name) {
        this(name, null);
    }

    public GradientPaintCustomizer(String name, GradientPaint initial) {
        this.name = name == null ? defaultName() : name;
        if (initial != null) {
            value = initial;
        } else {
            value = load();
        }
    }

    private GradientPaint load() {
        Preferences prefs = NbPreferences.forModule(GradientPaintCustomizer.class);
        Color c1 = loadColor("c1", prefs, new Color(180, 180, 255));
        Color c2 = loadColor("c2", prefs, Color.ORANGE);
        boolean cyclic = prefs.getBoolean(key("cyc"), true);
        AspectRatio aspect = currentAspectRatio();
        double thirdW = (aspect.width() / 3);
        double thirdH = (aspect.height() / 3F);
        Point2D.Float p1 = loadPoint("p1", prefs, thirdW, thirdH);
        Point2D.Float p2 = loadPoint("p2", prefs, aspect.width() - thirdW,
                aspect.height() - thirdH);
        return new GradientPaint(p1, c1, p2, c2, cyclic);
    }

    private AspectRatio currentAspectRatio() {
        AspectRatio ratio = Utilities.actionsGlobalContext().lookup(AspectRatio.class);
        if (ratio == null) {
            ratio = AspectRatio.create(() -> new Dimension(640, 480));
        }
        return ratio;
    }

    @Override
    public JComponent getComponent() {
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        ColorChooser color1Chooser = new ColorChooser(value.getColor1());

        JLabel color1Label = new JLabel();
        Mnemonics.setLocalizedText(color1Label,
                NbBundle.getMessage(GradientPaintCustomizer.class, "COLOR_1"));
        color1Chooser.setToolTipText(color1Label.getText());
        color1Label.setLabelFor(color1Chooser);

        ColorChooser color2Chooser = new ColorChooser(value.getColor2());
        JLabel color2Label = new JLabel();
        Mnemonics.setLocalizedText(color2Label,
                NbBundle.getMessage(GradientPaintCustomizer.class, "COLOR_2"));
        color2Chooser.setToolTipText(color2Label.getText());
        color2Label.setLabelFor(color2Chooser);

        JCheckBox cyclicBox = new JCheckBox();
        Mnemonics.setLocalizedText(cyclicBox, NbBundle.getMessage(GradientPaintCustomizer.class, "CYCLIC"));
        cyclicBox.setSelected(value.isCyclic());

        JPanel sub = new SharedLayoutPanel();
        sub.add(color1Label);
        sub.add(color1Chooser);
        pnl.add(sub);

        JPanel sub2 = new SharedLayoutPanel();
        sub2.add(color2Label);
        sub2.add(color2Chooser);
        sub2.add(cyclicBox);
        pnl.add(sub2);

        AspectRatio ratio = currentAspectRatio();
        PointSelector sel = new PointSelector(ratio.rectangle());
        sel.setMode(PointSelectorMode.POINT_AND_LINE);
        sel.setTargetPoint(value.getPoint1());
        sel.setFocusPoint(value.getPoint2());
        sel.setBackgroundPainter((Graphics2D g, Point2D target, Rectangle2D frame, double angle, PointSelectorMode mode, PointSelector sel1) -> {
            g.setPaint(get());
            g.fill(frame);
        });
        sel.addPropertyChangeListener("targetPoint", evt -> {
//            first.setLocation((Point2D) evt.getNewValue());
            Point2D pt = (Point2D) evt.getNewValue();
            value = new GradientPaint(pt, value.getColor1(),
                    value.getPoint2(), value.getColor2(), value.isCyclic());
            fire();
        });
        sel.addPropertyChangeListener("focusPoint", evt -> {
            Point2D pt = (Point2D) evt.getNewValue();
            value = new GradientPaint(value.getPoint1(), value.getColor1(),
                    pt, value.getColor2(), value.isCyclic());
            fire();
        });
        color1Chooser.addActionListener(ae -> {
            value = new GradientPaint(value.getPoint1(), color1Chooser.getColor(),
                    value.getPoint2(), value.getColor2(), value.isCyclic());
            fire();
        });
        color1Chooser.addActionListener(ae -> {
            value = new GradientPaint(value.getPoint1(), value.getColor1(),
                    value.getPoint2(), color2Chooser.getColor(), value.isCyclic());
            fire();
        });
        pnl.add(sel);

        return pnl;
    }

    private void save() {
        Preferences prefs = NbPreferences.forModule(GradientPaintCustomizer.class);
        saveColor("c1", prefs, value.getColor1());
        saveColor("c2", prefs, value.getColor2());
        savePoint("p1", prefs, value.getPoint1());
        savePoint("p2", prefs, value.getPoint2());
        prefs.putBoolean(key("cyc"), value.isCyclic());
    }

    private void saveColor(String pfx, Preferences prefs, Color save) {
        saveInt(pfx + "-red", prefs, save.getRed());
        saveInt(pfx + "-green", prefs, save.getGreen());
        saveInt(pfx + "-blue", prefs, save.getBlue());
        saveInt(pfx + "-alpha", prefs, save.getAlpha());
    }

    private Color loadColor(String pfx, Preferences prefs, Color def) {
        int r = loadInt(pfx + "-red", prefs, def.getRed());
        int g = loadInt(pfx + "-green", prefs, def.getGreen());
        int b = loadInt(pfx + "-blue", prefs, def.getBlue());
        int a = loadInt(pfx + "-blue", prefs, def.getAlpha());
        return new Color(r, g, b, a);
    }

    private void saveInt(String suffix, Preferences prefs, int value) {
        prefs.putInt(key(suffix), value);
    }

    private int loadInt(String suffix, Preferences prefs, int def) {
        return prefs.getInt(key(suffix), def);
    }

    private void savePoint(String suffix, Preferences prefs, Point2D p) {
        saveFloat(suffix + "-x", prefs, p.getX());
        saveFloat(suffix + "-y", prefs, p.getY());
    }

    private Point2D.Float loadPoint(String suffix, Preferences prefs, double defX, double defY) {
        float x = loadFloat(suffix + "-x", prefs, (float) defX);
        float y = loadFloat(suffix + "-y", prefs, (float) defY);
        return new Point2D.Float(x, y);
    }

    private void saveFloat(String suffix, Preferences prefs, double val) {
        prefs.putFloat(key(suffix), (float) val);
    }

    private float loadFloat(String suffix, Preferences prefs, float def) {
        return prefs.getFloat(key(suffix), def);
    }

    private String key(String suffix) {
        return name.toLowerCase().replace(' ', '-') + "-" + suffix;
    }

    private static String defaultName() {
        return NbBundle.getMessage(GradientPaintCustomizer.class, "GRADIENT_PAINT");
    }

    @Override
    protected void onAfterFire() {
        save();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GradientPaint get() {
        return value;
    }

}
