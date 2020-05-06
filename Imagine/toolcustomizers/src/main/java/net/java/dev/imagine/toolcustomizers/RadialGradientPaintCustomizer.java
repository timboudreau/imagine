package net.java.dev.imagine.toolcustomizers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MultipleGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizerSupport;
import org.imagine.editor.api.AspectRatio;
import org.imagine.geometry.util.PooledTransform;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.netbeans.paint.api.components.fractions.FractionsAndColorsEditor;
import org.netbeans.paint.api.components.points.PointSelector;
import org.netbeans.paint.api.components.points.PointSelectorBackgroundPainter;
import org.netbeans.paint.api.components.points.PointSelectorMode;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class RadialGradientPaintCustomizer extends ListenableCustomizerSupport<RadialGradientPaint>
        implements Customizer<RadialGradientPaint>, PointSelectorBackgroundPainter,
        ListenableCustomizer<RadialGradientPaint> {

    private final String name;
    private final PaintParams params = new PaintParams();

    public RadialGradientPaintCustomizer() {
        this(null, null);
    }

    public RadialGradientPaintCustomizer(String name) {
        this(name, null);
    }

    public RadialGradientPaintCustomizer(String name, RadialGradientPaint paint) {
        this.name = name;
        if (paint != null) {
            params.colors = paint.getColors();
            params.fractions = paint.getFractions();
            params.focusPoint.setLocation(paint.getFocusPoint());
            params.targetPoint.setLocation(paint.getCenterPoint());
            params.radius = paint.getRadius();
            params.cycleMethod = paint.getCycleMethod();
            params.colorSpaceType = paint.getColorSpace();
        } else {
            load();
        }
    }

    private void load() {
        Preferences prefs = NbPreferences.forModule(RadialGradientPaintCustomizer.class);
        String nm = name == null ? "rgpDefault" : name;
        params.load(nm, prefs);
    }

    private void store() {
        Preferences prefs = NbPreferences.forModule(RadialGradientPaintCustomizer.class);
        String nm = name == null ? "rgpDefault" : name;
        params.store(nm, prefs);
    }

    @Override
    protected void onAfterFire() {
        store();
    }

    private void changed() {
        fire();
    }

    @Override
    public JComponent getComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        FractionsAndColorsEditor fAndC = new FractionsAndColorsEditor(params.fractions, params.colors);
        AspectRatio ratio = Utilities.actionsGlobalContext().lookup(AspectRatio.class);
        if (ratio == null) {
            ratio = AspectRatio.create(() -> new Dimension(400, 300));
        }

        Insets titleInsets = new Insets(12, 0, 5, 0);
        Insets emptyInsets = new Insets(0, 0, 0, 0);

        JLabel psLabel = new JLabel();
        Mnemonics.setLocalizedText(psLabel, NbBundle.getMessage(
                RadialGradientPaintCustomizer.class, "TARGET_AND_FOCUS"));

        Rectangle2D.Double dbl = new Rectangle2D.Double(0, 0,
                ratio.width(), ratio.height());
        PointSelector ps = new PointSelector(dbl);
        ps.setMode(PointSelectorMode.POINT_AND_LINE);

        ps.setTargetPoint(params.targetPoint);
        ps.setFocusPoint(params.focusPoint);

        JLabel cycleLabel = new JLabel();
        Mnemonics.setLocalizedText(cycleLabel,
                NbBundle.getMessage(RadialGradientPaintCustomizer.class, "CYCLE_MODE"));

        JLabel colorSpaceLabel = new JLabel();
        Mnemonics.setLocalizedText(colorSpaceLabel,
                NbBundle.getMessage(RadialGradientPaintCustomizer.class, "COLOR_SPACE_TYPE"));

        JLabel radiusLabel = new JLabel();
        Mnemonics.setLocalizedText(radiusLabel,
                NbBundle.getMessage(RadialGradientPaintCustomizer.class, "RADIUS"));

        EnumCellRenderer ren = new EnumCellRenderer();
        DefaultComboBoxModel<CycleMethod> cycleMethods
                = new DefaultComboBoxModel<>(CycleMethod.values());
        cycleMethods.setSelectedItem(params.cycleMethod);
        JComboBox<CycleMethod> cycleCombo = new JComboBox<>(cycleMethods);
        cycleCombo.setRenderer(ren);
        cycleLabel.setLabelFor(cycleCombo);

        DefaultComboBoxModel<ColorSpaceType> colorSpaceTypes
                = new DefaultComboBoxModel<>(ColorSpaceType.values());
        colorSpaceTypes.setSelectedItem(params.colorSpaceType);
        JComboBox<ColorSpaceType> colorSpaceCombo = new JComboBox<>(colorSpaceTypes);
        colorSpaceCombo.setRenderer(ren);
        colorSpaceLabel.setLabelFor(colorSpaceCombo);

        System.out.println("1, DIAG " + ratio.diagonal() + " RAD " + params.radius);
        int max = Math.max(2, (int) ratio.diagonal());
        int val = Math.max(1, Math.min((int) params.radius, 1));
        JSlider radius = new JSlider(1, max,
                val);
        radius.setUI(PopupSliderUI.createUI(radius));
        radiusLabel.setLabelFor(radius);

        JLabel fcLabel = new JLabel();
        Mnemonics.setLocalizedText(fcLabel,
                NbBundle.getMessage(RadialGradientPaintCustomizer.class, "COLORS_AND_FRACTIONS"));

        fcLabel.setLabelFor(fAndC);

        Insets compInsets = new Insets(0, 0, 0, 5);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 3;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;

        c.anchor = GridBagConstraints.LAST_LINE_START;
        c.insets = titleInsets;
        c.gridwidth = 1;
        c.gridy++;

        c.fill = GridBagConstraints.NONE;
        panel.add(cycleLabel, c);
        c.gridx++;
        panel.add(colorSpaceLabel, c);
        c.gridx++;
        panel.add(radiusLabel, c);

        c.insets = compInsets;
        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(cycleCombo, c);
        c.gridx++;
        panel.add(colorSpaceCombo, c);
        c.gridx++;
        panel.add(radius, c);

        c.gridx = 0;
        c.gridy++;
        c.insets = titleInsets;
        c.anchor = GridBagConstraints.LAST_LINE_START;
        panel.add(psLabel, c);

        c.weighty = 0.75;
        c.gridwidth = 3;
        c.gridy++;
        c.insets = emptyInsets;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(ps, c);

        c.weightx = 0;
        c.weighty = 0;
        c.insets = titleInsets;
        c.anchor = GridBagConstraints.LAST_LINE_START;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        panel.add(fcLabel, c);

        c.weightx = 1;
        c.weighty = 0.25;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        panel.add(fAndC, c);

        ps.setBackgroundPainter(this);

        ps.addPropertyChangeListener("targetPoint", evt -> {
            Point2D p = (Point2D) evt.getNewValue();
            params.targetPoint.setLocation(p);
            ps.repaint();
            rev++;
            changed();
        });
        ps.addPropertyChangeListener("focusPoint", evt -> {
            Point2D p = (Point2D) evt.getNewValue();
            params.focusPoint.setLocation(p);
            ps.repaint();
            rev++;
            changed();
        });

        colorSpaceCombo.addActionListener(ae -> {
            params.colorSpaceType = (ColorSpaceType) colorSpaceCombo.getSelectedItem();
            ps.repaint();
            rev++;
            changed();
        });

        cycleCombo.addActionListener(ae -> {
            params.cycleMethod = (CycleMethod) cycleCombo.getSelectedItem();
            ps.repaint();
            rev++;
            changed();
        });

        radius.addChangeListener(e -> {
            Number n = (Number) radius.getValue();
            params.radius = n.floatValue();
            ps.repaint();
            rev++;
            changed();
        });

        fAndC.addChangeListener(e -> {
            rev++;
            fAndC.colorsAndFractions((fracs, cols) -> {
                params.colors = cols;
                params.fractions = fracs;
                ps.repaint();
                changed();
            });
        });

        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return panel;
    }

    private int rev = 0;

    @Override
    public String getName() {
        return name != null ? name
                : NbBundle.getMessage(RadialGradientPaintCustomizer.class,
                        "RADIAL_GRADIENT_PAINT");
    }

    private RadialGradientPaint lastResult;
    private int revAtLastResult = -1;

    @Override
    public RadialGradientPaint get() {
        if (revAtLastResult != rev) {
            revAtLastResult = rev;
            return lastResult = PooledTransform.lazyTranslate(0, 0, (xform, ownerConsumer) -> {
                RadialGradientPaint gp = new RadialGradientPaint(params.targetPoint,
                        params.radius, params.focusPoint, params.fractions, params.colors,
                        params.cycleMethod, params.colorSpaceType,
                        xform);
                ownerConsumer.accept(gp);
                return gp;
            });
        }
        return lastResult;
    }

    @Override
    public void paintBackground(Graphics2D g, Point2D target, Rectangle2D frame, double angle, PointSelectorMode mode, PointSelector sel) {
        g.setPaint(get());
        g.fill(frame);
    }

    static class PaintParams {

        Color[] colors = new Color[]{new Color(255, 128, 128),
            new Color(255, 220, 180),
            Color.BLUE,
            new Color(180, 180, 255),
            new Color(200, 150, 20),
            Color.ORANGE};
        float[] fractions = new float[]{0, 0.1825F, 0.25F, 0.625F, 0.875F, 1};
        float radius = 120;
        Point2D.Double focusPoint = new Point2D.Double(100, 100);
        Point2D.Double targetPoint = new Point2D.Double(120, 125);
        MultipleGradientPaint.CycleMethod cycleMethod
                = MultipleGradientPaint.CycleMethod.REFLECT;

        MultipleGradientPaint.ColorSpaceType colorSpaceType
                = MultipleGradientPaint.ColorSpaceType.LINEAR_RGB;

        void load(String pfx, Preferences prefs) {
            int ct = prefs.getInt(pfx + "_count", -1);
            if (ct < 0) {
                return;
            }
            float[] frac = new float[ct];
            for (int i = 0; i < ct; i++) {
                frac[i] = prefs.getFloat(pfx + "_frac_" + i, 1F / ct + 1);
            }
            Color[] c = colorsFromByteArray(prefs.getByteArray(pfx + "_colors", new byte[ct]));
            float r = prefs.getFloat(pfx + "_radius", 10F);
            double focusX = prefs.getFloat(pfx + "_focus_x", 100);
            double focusY = prefs.getFloat(pfx + "_focus_y", 100);
            double targetX = prefs.getFloat(pfx + "_target_x", 104);
            double targetY = prefs.getFloat(pfx + "_target_y", 107);
            int cycle = prefs.getInt(pfx + "_cycle", 0);
            int cs = prefs.getInt(pfx + "_cs", 0);
            focusPoint.setLocation(focusX, focusY);
            targetPoint.setLocation(targetX, targetY);

            cycleMethod = CycleMethod.values()[cycle];
            colorSpaceType = ColorSpaceType.values()[cs];
            this.radius = r;
            this.fractions = frac;
            this.colors = c;
        }

        void store(String pfx, Preferences prefs) {
            prefs.putInt(pfx + "_count", fractions.length);
            prefs.putByteArray(pfx + "_colors", colorsByteArray());
            prefs.putFloat(pfx + "_radius", radius);
            prefs.putDouble(pfx + "_focus_x", focusPoint.x);
            prefs.putDouble(pfx + "_focus_y", focusPoint.y);
            prefs.putDouble(pfx + "_target_x", targetPoint.x);
            prefs.putDouble(pfx + "_target_y", targetPoint.y);
            prefs.putInt(pfx + "_cycle", cycleMethod.ordinal());
            prefs.putInt(pfx + "_cs", colorSpaceType.ordinal());
            for (int i = 0; i < fractions.length; i++) {
                prefs.putFloat(pfx + "_frac_" + i, fractions[i]);
            }
        }

        private Color[] colorsFromByteArray(byte[] bts) {
            Color[] result = new Color[bts.length / 4];
            for (int i = 0; i < bts.length; i += 4) {
                int red = bts[i] & 0xFF;
                int green = bts[i + 1] & 0xFF;
                int blue = bts[i + 2] & 0xFF;
                int alpha = bts[i + 3] & 0xFF;
                result[i / 4] = new Color(red, green, blue, alpha);
            }
            return result;
        }

        private byte[] colorsByteArray() {
            byte[] result = new byte[4 * colors.length];
            for (int i = 0; i < colors.length; i++) {
                int byteOffset = i * 4;
                Color c = colors[i];
                result[byteOffset] = (byte) (c.getRed());
                result[byteOffset + 1] = (byte) (c.getGreen());
                result[byteOffset + 2] = (byte) (c.getBlue());
                result[byteOffset + 3] = (byte) (c.getAlpha());
            }
            return result;
        }
    }

    public static void main(String[] args) {

        RadialGradientPaintCustomizer c = new RadialGradientPaintCustomizer("Wookie");

        JComponent comp = c.getComponent();

        JFrame jf = new JFrame(c.getName());
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(comp);
        jf.pack();
        jf.setVisible(true);
    }
}
