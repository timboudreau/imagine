package net.java.dev.imagine.toolcustomizers;

import com.mastfrog.function.state.Bool;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.java.dev.imagine.api.toolcustomizers.AbstractCustomizer;
import org.imagine.editor.api.Cap;
import org.imagine.editor.api.Join;
import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.Triangle2D;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paint.api.components.EnumComboBoxModel;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.netbeans.paint.api.components.RadialSliderUI;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.SharedLayoutRootPanel;
import org.netbeans.paint.api.components.StringConverter;
import org.netbeans.paint.api.components.TitledPanel2;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.netbeans.paint.api.components.fractions.Fraction;
import org.netbeans.paint.api.components.fractions.FractionsEditor;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public class BasicStrokeCustomizer extends AbstractCustomizer<BasicStroke> implements ListenableCustomizer<BasicStroke> {

    private static int WIDTH_MULTIPLIER = 100;
    private static int MITER_MULTIPLIER = 10;
    private static final String DEFAULT_NAME = "Stroke";
    private static final int CYCLE_MAX = 96;
    private float width = 1;
    private Cap cap = Cap.ROUND;
    private Join join = Join.ROUND;
    private float miterLimit = 5;
    private float[] dashArray = new float[]{8, 4};
    private float phase = 0;
    private boolean dashed = true;
    private int rev;
    private int revAtLastGet = -1;
    private BasicStroke last;

    public BasicStrokeCustomizer() {
        this(DEFAULT_NAME);
    }

    public BasicStrokeCustomizer(String name) {
        super(name);
    }

    public BasicStrokeCustomizer(String name, BasicStroke stroke) {
        super(name == null ? DEFAULT_NAME : name);
        if (stroke != null) {
            configureFrom(stroke);
            last = stroke;
            revAtLastGet = 0;
        }
    }

    public BasicStrokeCustomizer(BasicStroke stroke) {
        this(DEFAULT_NAME, stroke);
    }

    public void set(BasicStroke stroke) {
        configureFrom(stroke);
        last = stroke;
        rev++;
        revAtLastGet = rev;
    }

    public static void main(String[] args) {
        Font f = new Font("Arial", Font.PLAIN, 24);
        UIManager.put("controlFont", f);
        UIManager.put("Label.font", f);
        UIManager.put("Panel.font", f);
        UIManager.put("Slider.font", f);
        UIManager.put("ComboBox.font", f);
        UIManager.put("Tree.font", f);
        UIManager.put("Button.font", f);
        UIManager.put("TextField.font", f);
        UIManager.put("List.font", f);

//        TitledPanel2.debugLayout(true);
        BasicStrokeCustomizer cus = new BasicStrokeCustomizer("wurgle");
        JFrame frm = new JFrame("Demo");
        JComponent comp = cus.getComponent();
        System.out.println("COMPONENT " + comp);

        frm.setContentPane(comp);
        frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frm.pack();
        frm.setVisible(true);
    }

    @Override
    protected JComponent[] createComponents() {
        JSlider widthSlider = createWidthSlider();

//        JPanel pnl = new JPanel(new VerticalFlowLayout());
        JPanel pnl = new SharedLayoutRootPanel();
        pnl.add(createWidthPanel());
        pnl.add(createCombos());
        pnl.add(createMiterPanel());
        pnl.add(createDashedBox());
        pnl.add(createDashesEditor());
        pnl.add(createPreview());
        TitledPanel2 tp = new TitledPanel2("STROKE", BasicStrokeCustomizer.class, false,
                expanded -> {
                    return expanded ? pnl : widthSlider;
                });
        return new JComponent[]{
            tp
        };
    }

    private JComponent createDashedBox() {
        JCheckBox dashed = new JCheckBox();
        Mnemonics.setLocalizedText(dashed, NbBundle.getMessage(BasicStrokeCustomizer.class, "DASHED"));
        dashed.addChangeListener(ce -> {
            this.dashed = dashed.isSelected();
            change();
        });
        dashed.setSelected(this.dashed);
        return new SharedLayoutPanel(dashed);
    }

    private JComponent createCombos() {
        JLabel joinLabel = new JLabel();
        Mnemonics.setLocalizedText(joinLabel, NbBundle.getMessage(BasicStrokeCustomizer.class, "JOIN"));
        JComboBox<Join> joinBox = EnumComboBoxModel.newComboBox(join);
        joinLabel.setLabelFor(joinBox);
        SharedLayoutPanel pnl1 = new SharedLayoutPanel();
//        SharedLayoutPanel pnl2 = new SharedLayoutPanel();
        pnl1.add(joinLabel);
        pnl1.add(joinBox);

        joinBox.addItemListener(ie -> {
            this.join = (Join) joinBox.getSelectedItem();
            change();
        });

        JLabel capLabel = new JLabel();
        Mnemonics.setLocalizedText(capLabel, NbBundle.getMessage(BasicStrokeCustomizer.class, "CAP"));
        JComboBox<Cap> capBox = EnumComboBoxModel.newComboBox(cap);
        capBox.addItemListener(ie -> {
            this.cap = (Cap) capBox.getSelectedItem();
            change();
        });
        capLabel.setLabelFor(capBox);
        pnl1.add(capLabel);
        pnl1.add(capBox);
//        JPanel pnl = new JPanel(new VerticalFlowLayout());
//        pnl.add(pnl1);
//        pnl.add(pnl2);
        return pnl1;
    }

    private static final int PHASE_MULTIPLIER = 100;

    private JComponent createDashesEditor() {
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        SharedLayoutPanel phasePanel = new SharedLayoutPanel();

        JSlider phaseSlider = new JSlider(0, CYCLE_MAX * PHASE_MULTIPLIER, 0);
        PopupSliderUI.attach(phaseSlider);
        RadialSliderUI.setStringConverter(phaseSlider, new WidthSC());
        phaseSlider.addChangeListener(ce -> {
            float val = phaseSlider.getValue();
            val /= PHASE_MULTIPLIER;
            phase = val;
            change();
        });
        JLabel phaseLabel = new JLabel();
        Mnemonics.setLocalizedText(phaseLabel, NbBundle.getMessage(BasicStrokeCustomizer.class, "PHASE"));
        phaseLabel.setLabelFor(phaseSlider);
        phasePanel.add(phaseLabel);
        phasePanel.add(phaseSlider);

        pnl.add(phasePanel);

        SharedLayoutPanel cyclePanel = new SharedLayoutPanel();
        pnl.add(cyclePanel);
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, NbBundle.getMessage(BasicStrokeCustomizer.class, "CYCLE_LENGTH"));
        JSlider cycle = new JSlider(1, CYCLE_MAX, 10);
        PopupSliderUI.attach(cycle);
        lbl.setLabelFor(cycle);
        cyclePanel.add(lbl);
        cyclePanel.add(cycle);
        float max = 0;
        for (int i = 0; i < dashArray.length; i++) {
            max += dashArray[i];
        }
        float[] fracs = new float[dashArray.length];
        float curr = 0;
        for (int i = 0; i < fracs.length; i++) {
            curr += dashArray[i];
            fracs[i] = curr / max;
        }
        FractionsEditor frac = new FractionsEditor(fracs);
        frac.setFont(phaseLabel.getFont().deriveFont(AffineTransform.getScaleInstance(0.8, 0.8)));
        frac.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));

        ChangeListener cl = ce -> {
            int sz = frac.getModel().size();
            float[] nue = new float[sz - 2];
            double mx = cycle.getValue();
            for (int i = 1; i < sz - 1; i++) {
                Fraction fr = frac.getModel().get(i);
                nue[i - 1] = (float) (fr.getValue() * mx);
            }
            dashArray = nue;
            change();
        };
        frac.getModel().addChangeListener(cl);
        cycle.addChangeListener(cl);
        addChangeListener(ce -> {
            if (dashed != pnl.isVisible()) {
                pnl.setVisible(dashed);
                JComponent parent = (JComponent) pnl.getParent();
                parent.invalidate();
                parent.revalidate();
                parent.repaint();
            }
        });
        pnl.add(frac);
        pnl.setVisible(dashed);
        return pnl;
    }

    private JComponent createPreview() {
        StrokePreview pre = new StrokePreview();
        pre.setStroke(get());
        addChangeListener(ce -> {
            pre.setStroke(get());
        });
        return pre;
    }

    private JComponent createMiterPanel() {
        SharedLayoutPanel result = new SharedLayoutPanel();

        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, NbBundle.getMessage(BasicStrokeCustomizer.class, "MITER_LIMIT"));

        JSlider slider = new JSlider(10, 48 * MITER_MULTIPLIER, MITER_MULTIPLIER);
        PopupSliderUI.attach(slider);
        RadialSliderUI.setStringConverter(slider, new MiterSC());

        slider.addChangeListener(ce -> {
            float val = slider.getValue();
            val /= MITER_MULTIPLIER;
            miterLimit = val;
            change();
        });
        lbl.setLabelFor(slider);
        result.add(lbl);
        result.add(slider);
        result.setVisible(join == Join.MITER);
        addChangeListener(ce -> {
            boolean isMiter = join == Join.MITER;
            if (isMiter != result.isVisible()) {
                result.setVisible(isMiter);
                JComponent par = (JComponent) result.getParent();
                par.invalidate();
                par.revalidate();
                par.repaint();
            }
        });
        return result;
    }

    private JComponent createWidthPanel() {
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, NbBundle.getMessage(BasicStrokeCustomizer.class, "LINE_WIDTH"));
        JSlider slider = createWidthSlider();
        lbl.setLabelFor(slider);
        SharedLayoutPanel result = new SharedLayoutPanel();
        result.add(lbl);
        result.add(slider);
        return result;
    }

    private JSlider createWidthSlider() {
        JSlider slider = new JSlider(1, 24 * WIDTH_MULTIPLIER, WIDTH_MULTIPLIER);
        PopupSliderUI.attach(slider);
        RadialSliderUI.setStringConverter(slider, new WidthSC());
        Bool changing = Bool.create();
        slider.addChangeListener(ce -> {
            changing.set();
            try {
                float val = slider.getValue();
                val /= WIDTH_MULTIPLIER;
                width = val;
                change();
            } finally {
                changing.set(false);
            }
        });
        addChangeListener(ce -> {
            if (changing.get()) {
                return;
            }
            int val = (int) (width * WIDTH_MULTIPLIER);
            if (val != slider.getValue()) {
                slider.setValue(val);
            }
        });
        return slider;
    }

    private static class WidthSC implements StringConverter {

        private final DecimalFormat fmt = new DecimalFormat("###0.##");

        @Override
        public String valueToString(JSlider sl) {
            float val = sl.getValue();
            val /= WIDTH_MULTIPLIER;
            return fmt.format(val);
        }

        @Override
        public int maxChars() {
            return 5;
        }

        @Override
        public String valueToString(int val) {
            float f = val;
            f /= WIDTH_MULTIPLIER;
            return fmt.format(f);
        }
    }

    private static class MiterSC implements StringConverter {

        private final DecimalFormat fmt = new DecimalFormat("###0.##");

        @Override
        public String valueToString(JSlider sl) {
            float val = sl.getValue();
            val /= MITER_MULTIPLIER;
            return fmt.format(val);
        }

        @Override
        public int maxChars() {
            return 5;
        }

        @Override
        public String valueToString(int val) {
            float f = val;
            f /= MITER_MULTIPLIER;
            return fmt.format(f);
        }
    }

    @Override
    protected void change() {
        rev++;
        super.change();
    }

    private String name(String base, int ix) {
        return name(base) + "-" + ix;
    }

    private String name(String base) {
        return getName() + "-" + base;
    }

    private void saveToPrefs(Preferences prefs) {
        prefs.putFloat(name("width"), phase);
        prefs.putFloat(name("miter"), miterLimit);
        prefs.putFloat(name("phase"), phase);
        prefs.putInt(name("cap"), cap.ordinal());
        prefs.putInt(name("join"), join.ordinal());
        prefs.putBoolean(name("dashed"), dashed);
        prefs.putInt(name("dashes"), dashArray.length);
        for (int i = 0; i < dashArray.length; i++) {
            prefs.putFloat(name("dash", i), dashArray[i]);
        }
    }

    private void configureFrom(BasicStroke value) {
        width = value.getLineWidth();
        cap = Cap.forStroke(value);
        join = Join.forStroke(value);
        miterLimit = value.getMiterLimit();
        phase = value.getDashPhase();
        float[] dash = value.getDashArray();
        revAtLastGet = -1;
        last = value;
        if (dash == null) {
            dashed = false;
            dashArray = new float[]{1};
        } else {
            dashArray = dash;
        }
    }

    @Override
    protected void saveValue(BasicStroke value) {
        configureFrom(value);
        if (DEFAULT_NAME.equals(getName())) {
            Preferences prefs = NbPreferences.forModule(BasicStrokeCustomizer.class);
            saveToPrefs(prefs);
        }
    }

    private void save() {
        Preferences prefs = NbPreferences.forModule(BasicStrokeCustomizer.class);
        saveToPrefs(prefs);
    }

    @Override
    public BasicStroke getValue() {
        if (last != null && rev == revAtLastGet) {
            return last;
        }
        return last = create();
    }

    private BasicStroke create() {
        if (!dashed || dashArray == null || dashArray.length == 0) {
            return new BasicStroke(width, cap.constant(), join.constant(),
                    miterLimit, null, 0);
        }
        return new BasicStroke(width, cap.constant(), join.constant(),
                miterLimit, dashArray, phase);
    }

    static class StrokePreview extends JComponent {

        private BasicStroke stroke = new BasicStroke(1);
        private final Triangle2D tri = new Triangle2D();

        void setStroke(BasicStroke stroke) {
            setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
            this.stroke = stroke;
            repaint();
        }

        public Dimension getPreferredSize() {
            return new Dimension(200, 200);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public void paintComponent(Graphics g) {
            paintComponent((Graphics2D) g);
        }

        private double margin = 10;

        private final EqLine line = new EqLine();

        public void paintComponent(Graphics2D g) {
            int h = getHeight();
            int w = getWidth();
            Color bg = UIManager.getColor("text");
            g.setColor(bg);
            g.fillRect(0, 0, w, h);

            Color fg = UIManager.getColor("textText");
            g.setColor(fg);
            GraphicsUtils.setHighQualityRenderingHints(g);

            Insets ins = getInsets();
            int workingHeight = h - (ins.top + ins.bottom);
            int workingWidth = w - (ins.left + ins.right);

            double center = ins.left + margin + (workingWidth / 2) - (stroke.getLineWidth() / 2);
            double top = ins.top + margin + stroke.getLineWidth();

            double bottom = ins.top + workingHeight - (stroke.getLineWidth() + margin);
            double left = ins.left + (stroke.getLineWidth() + margin);
            double right = ins.left + workingWidth - (stroke.getLineWidth() + margin);

            tri.setPoints(center, top, right, bottom, left, bottom);
            g.setStroke(stroke);
            g.draw(tri);

            line.setLine(center, top + (stroke.getLineWidth() * 2) + margin + 2, center, bottom - ((stroke.getLineWidth() * 0.5) + margin + 2));
            g.draw(line);
        }
    }
}
