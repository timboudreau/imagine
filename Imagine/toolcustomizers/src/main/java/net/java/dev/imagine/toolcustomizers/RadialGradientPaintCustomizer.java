package net.java.dev.imagine.toolcustomizers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizerSupport;
import static net.java.dev.imagine.toolcustomizers.LinearGradientPaintCustomizer.listenForAspectRatio;
import org.imagine.editor.api.AspectRatio;
import com.mastfrog.geometry.util.PooledTransform;
import org.imagine.help.api.annotations.Help;
import org.imagine.help.api.annotations.Help.HelpText;
import com.mastfrog.swing.slider.PopupSliderUI;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.SharedLayoutRootPanel;
import org.netbeans.paint.api.components.TitledPanel2;
import org.netbeans.paint.api.components.dialog.ButtonMeaning;
import org.netbeans.paint.api.components.dialog.DialogBuilder;
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
@Help(id = "RadialGradient", content = @HelpText(value = "# Radial Gradient Customizer\n\n"
        + "A radial gradient is a sequence of multiple colors which is (optionally) repeated in concentric"
        + " circles radianting out from a central point, with a defined radius and focus point.\n\n"
        + "The customizer shows point-selector control which mirrors the aspect ratio of "
        + "the picture being edited.  Press and drag with the mouse to draw the initial point and"
        + "focus point (creating an effect as if you were viewing the colored circles at an angle).\n\n"
        + "Radial gradients can be used to produce complex, interesting fill patters simply and in "
        + "a way that is well supported by SVG rendering engines.\n\n## Adding Points\n\n"
        + "Double click in the gradient designer to add a new color-stop, or drag existing stops "
        + "to move them; each has a color-chooser below it which can be used to change the color."
        + "\n\nInternally, a radial gradient is defined by a collection of colors and frational"
        + "values between zero and one - percentages of the spread between the start and end  "
        + "points of the gradient at which colors change.\n\nThe *Adjust Colors* button allows you "
        + "to change the palette of all colors in the gradient at once, adjusting all of their "
        + "hue, saturation or brightness at once.", keywords = {"gradient", "radial", "fill", "color"}))
public class RadialGradientPaintCustomizer extends ListenableCustomizerSupport<RadialGradientPaint>
        implements Customizer<RadialGradientPaint>, PointSelectorBackgroundPainter,
        ListenableCustomizer<RadialGradientPaint> {

    private final String name;
    private final PaintParams params = new PaintParams();

    @Help(id = "FractionsAndColors", related = {"ColorChooser"}, content = {
        @Help.HelpText(language = "en", country = "US",
                value = "# Fractions and Colors Editor\n\nLinear and radial gradients are composed of _color stops_ - points "
                + "at which the gradient shifts from transitioning smoothly between one "
                + "pair of colors and a new pair formed from the previous destination color "
                + "and the color assigned to the next _color stop_.\n\n"
                + "These stops are defined as _percentages_ of the visible space the "
                + "gradient takes up in a picture, rather than as concrete positions.\n\n"
                + "The Fractions and Colors Editor lets you add, remove and adjust these "
                + "color stops:  Double click to create a new one; drag them to change their "
                + "relative position;  select and use the DELETE key to delete; use the "
                + "Color Chooser below each stop-line to adjust that stop's color.",
                keywords = {"gradient", "fraction", "stops", "color"})})
    public RadialGradientPaintCustomizer() {
        this(null, null);
    }

    @Help(id = "FractionsAndColorsPopup", related = {"FractionsAndColors"},
            content = {
                @Help.HelpText(language = "en", country = "US",
                        value = "# Color Stops Editor\n\n "
                        + "Linear and radial gradients are composed of *color stops* which "
                        + "can be positioned relative to each other; the gradient then scales "
                        + "those colors across the region it occupies."
                        + "\n\n* Drag lines to position color-stops within the gradient\n"
                        + " * Select a line by clicking and press _DELETE_ to delete a color stop "
                        + "(other than the first and last which are mandatory)\n"
                        + " * Double-click to create a new color stop\n"
                        + " * Use the color chooser for each stop to fine-tune the color\n"
                        + " * Drag stops (or create additional ones) closer together to create more"
                        + " abrupt color transitions, or further apart to create more gradiual ones.\n\n",
                        keywords = {"gradient", "fraction"})})
    public RadialGradientPaintCustomizer(String name) {
        this(name, null);
    }

    @Help(id = "PointSelector", related = {"FractionsAndColors"}, content
            = {
                @Help.HelpText(language = "en", country = "US", value = "# Points Selector\n\n"
                        + "The Points Selector allows you to choose the start and end points "
                        + "of a radial or linear gradient by drawing them (click-and-drag) on "
                        + "the preview image (which shows a preview of the gradient you're designing,"
                        + "scaled to the aspect ratio of the currently edited picture)."
                        + "\n\nAs displayed, it has the same aspect ratio as the currently edited "
                        + "picture, and locations within it represent their equivalent in the picture."
                        + "\n\nThe arrow keys can also be used to fine tune the points (hold down ALT to "
                        + "adjust the focus point instead of the initial point).",
                        keywords = {"focus", "initial", "aspect", "ratio"})})
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

    @Help(id = "PointSelectorPopup", related = {"FractionsAndColors"}, noIndex = true,
            content = {
                @Help.HelpText(language = "en", country = "US", value = "Click and drag to define the "
                        + "the distance between the start and end points of the gradient.  These are "
                        + "defined here as percentages of the aspect ratio of the picture being edited;"
                        + "the control's preview will update to show approximately how the gradient would "
                        + "appear if applied to a shape spanning the entire picture.")})
    private void load() {
        Preferences prefs = NbPreferences.forModule(RadialGradientPaintCustomizer.class);
        String nm = name == null ? "rgpDefault" : name;
        params.load(nm, prefs);
    }

    @Help(id = "AdjustColorsPopup", related = {"FractionsAndColors"}, noIndex = true,
            content = {
                @Help.HelpText(language = "en", country = "US",
                        value = "Click here to open a dialog that lets you adjust the hue/saturation/brightness "
                        + "of the _entire palette of colors_ in this gradient at once.\n\n")})
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
        NestingPanel panel = new NestingPanel();
        FractionsAndColorsEditor fAndC = new FractionsAndColorsEditor(params.fractions, params.colors);
        AspectRatio ratio = Utilities.actionsGlobalContext().lookup(AspectRatio.class);
        if (ratio == null) {
            ratio = AspectRatio.create(() -> new Dimension(400, 300));
        }

        JLabel psLabel = panel.addHeadingLabel(NbBundle.getMessage(
                RadialGradientPaintCustomizer.class, "TARGET_AND_FOCUS"));

        Rectangle2D.Double dbl = new Rectangle2D.Double(0, 0,
                ratio.width(), ratio.height());
        PointSelector ps = new PointSelector(dbl);
        listenForAspectRatio(ps);
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

        int max = Math.max(2, (int) ratio.diagonal());
        int val = Math.max(1, Math.min((int) params.radius, 1));
        JSlider radius = new JSlider(1, max,
                val);
        radius.setUI(PopupSliderUI.createUI(radius));
        radiusLabel.setLabelFor(radius);

        JLabel fcLabel = panel.addHeadingLabel(NbBundle.getMessage(RadialGradientPaintCustomizer.class, "COLORS_AND_FRACTIONS"));

        fcLabel.setLabelFor(fAndC);
        ps.setBorder(SharedLayoutPanel.createIndentBorder());
        JButton adjustButton = new JButton();
        Mnemonics.setLocalizedText(adjustButton, Bundle.adjustColors());
        adjustButton.addActionListener(ae -> {
            DialogBuilder.forName("adjustColors")
                    .forComponent(() -> {
                        return new AdjustColorsPanel(fAndC.getColors());
                    }, (AdjustColorsPanel pnl, ButtonMeaning update) -> {
                        if (update.isAffirmitive()) {
                            fAndC.setColors(pnl.colors());
                        }
                        return true;
                    }).openDialog();
        });
        panel.add(new SharedLayoutPanel(radiusLabel, radius, adjustButton));
        panel.add(new SharedLayoutPanel(colorSpaceLabel, colorSpaceCombo, cycleLabel, cycleCombo));
        panel.add(psLabel);
        panel.add(ps);
        panel.add(fcLabel);
        panel.add(fAndC);
        fAndC.setBorder(SharedLayoutPanel.createIndentBorder());

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

        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.onShow(() -> {
            HelpItems.FractionsAndColorsPopup.enqueueInSeries(fAndC);
            HelpItems.PointSelectorPopup.enqueueInSeries(ps);
            HelpItems.AdjustColorsPopup.enqueueInSeries(adjustButton);
        });

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

        TitledPanel2 tp2 = new TitledPanel2("Whoopie", false, expanded -> {
            return expanded ? comp : new JComboBox();
        });

        SharedLayoutRootPanel root = new SharedLayoutRootPanel(1, tp2);
//        JPanel root = new JPanel();
//        root.add(tp2);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(root);
        jf.pack();
        jf.setVisible(true);
    }
}
