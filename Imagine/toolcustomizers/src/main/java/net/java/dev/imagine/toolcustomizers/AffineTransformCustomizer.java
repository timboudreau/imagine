/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.dev.java.imagine.api.tool.aspects.ShapePreview;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paint.api.components.FlexEmptyBorder;
import org.netbeans.paint.api.components.FlexEmptyBorder.Side;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.netbeans.paint.api.components.RadialSliderUI;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.StringConverter;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
public class AffineTransformCustomizer implements Customizer<AffineTransform>,
        ListenableCustomizer<AffineTransform>, ShapePreview {

    private final AffineTransform value;
    private final String name;
    private Shape shape;

    public AffineTransformCustomizer(String name, AffineTransform initial) {
        this.name = name;
        this.value = initial == null ? AffineTransform.getTranslateInstance(0, 0) : initial;
    }

    public static void main(String[] args) {
        AffineTransformCustomizer xc = new AffineTransformCustomizer("Stuff", AffineTransform.getQuadrantRotateInstance(2));
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setContentPane(xc.getComponent());
        jf.pack();
        jf.setVisible(true);
    }

    @Override
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    @Override
    public AffineTransform get() {
        return new AffineTransform(value);
    }

    @Messages({
        "flip=Flip",
        "generalRotation=General Rotation",
        "generalScale=General Scale",
        "generalTransform=General Transform",
        "identity=Identity",
        "quadrantRotation=Quadrant Rotation",
        "translation=Translation",
        "uniformScale=Uniform Scale",
        "scale=Scale",
        "unknown=Unknown",})
    private String typeString(int type) {
        switch (type) {
            case AffineTransform.TYPE_FLIP:
                return Bundle.flip();
            case AffineTransform.TYPE_GENERAL_ROTATION:
                return Bundle.generalRotation();
            case AffineTransform.TYPE_GENERAL_SCALE:
                return Bundle.generalScale();
            case AffineTransform.TYPE_GENERAL_TRANSFORM:
                return Bundle.generalTransform();
            case AffineTransform.TYPE_IDENTITY:
                return Bundle.identity();
            case AffineTransform.TYPE_QUADRANT_ROTATION:
                return Bundle.quadrantRotation();
            case AffineTransform.TYPE_TRANSLATION:
                return Bundle.translation();
            case AffineTransform.TYPE_UNIFORM_SCALE:
                return Bundle.uniformScale();
            case AffineTransform.TYPE_MASK_ROTATION:
                return Bundle.rotation();
            case AffineTransform.TYPE_MASK_SCALE:
                return Bundle.scale();
            default:
                return Bundle.unknown();
        }
    }

    private String typeString() {
        StringBuilder sb = new StringBuilder();
        int[] flags = new int[]{AffineTransform.TYPE_FLIP, AffineTransform.TYPE_GENERAL_ROTATION,
            AffineTransform.TYPE_GENERAL_SCALE, AffineTransform.TYPE_GENERAL_TRANSFORM,
            AffineTransform.TYPE_QUADRANT_ROTATION, AffineTransform.TYPE_TRANSLATION, AffineTransform.TYPE_UNIFORM_SCALE};
        int type = value.getType();
        for (int i = 0; i < flags.length; i++) {
            int flg = flags[i];
            if ((type & flg) != 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(typeString(type & flg));
            }
        }
        return sb.toString();
    }

    private final WeakSet<Consumer<? super AffineTransform>> listeners = new WeakSet<>();

    private void change() {
        Set<Consumer<? super AffineTransform>> l = new HashSet<>(listeners);
        if (!l.isEmpty()) {
            AffineTransform xform = new AffineTransform(get());
            for (Consumer<? super AffineTransform> c : l) {
                c.accept(xform);
            }
        }
    }

    @Override
    public Runnable listen(Consumer<? super AffineTransform> consumer) {
        listeners.add(consumer);
        return () -> {
            listeners.remove(consumer);
        };
    }

    @Override
    public boolean isInUse() {
        return panel != null && panel.isDisplayable();
    }

    private JPanel panel;

    @Messages({"matrix=Transform Matrix", "rotation=Rotation"})
    @Override
    public JComponent getComponent() {
        if (panel != null && !panel.isDisplayable()) {
            return panel;
        }
//        setShape(new Triangle2D(100, 100, 0, 200, 200, 200));
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBorder(SharedLayoutPanel.createIndentBorder());

        JLabel type = new JLabel(typeString());
        type.setHorizontalTextPosition(SwingConstants.CENTER);
        type.setFont(type.getFont().deriveFont(Font.ITALIC));
        type.setMinimumSize(new Dimension(24, 24));
//        pnl.add(type, BorderLayout.SOUTH);

        MatrixPreview pre = new MatrixPreview();
        JPanel margin = new JPanel(new BorderLayout());
        margin.setBorder(new FlexEmptyBorder());
        margin.add(new JScrollPane(pre), BorderLayout.CENTER);
        margin.add(type, BorderLayout.SOUTH);
        pnl.add(margin, BorderLayout.CENTER);
        JSlider slider = new JSlider(0, 360, 0);

        boolean[] updating = new boolean[1];
        Consumer<double[]> updater = dbls -> {
            updating[0] = true;
            value.setTransform(dbls[0], dbls[1],
                    dbls[2], dbls[3], dbls[4], dbls[5]);
            type.setText(typeString());
            pre.refresh();
            updating[0] = false;
            change();
        };
        MxComponent mx = new MxComponent(() -> {
            double[] d = new double[6];
            value.getMatrix(d);
            return d;
        }, updater, () -> {
            value.setToIdentity();
            change();
        }, -10, 10);
        JPanel inner = new JPanel(new VerticalFlowLayout());
        inner.setBorder(new FlexEmptyBorder(1f, 1f, Side.TOP, Side.RIGHT, Side.BOTTOM));
        pnl.add(inner, BorderLayout.NORTH);
        JLabel mxLabel = new JLabel(Bundle.matrix());
        mxLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("controlDkShadow")));
        inner.add(mxLabel);
        inner.add(mx);

        RadialSliderUI.attach(slider);
        RadialSliderUI.setStringConverter(slider, new StringConverter() {
            @Override
            public String valueToString(JSlider sl) {
                return valueToString(sl.getValue());
            }

            @Override
            public int maxChars() {
                return 4;
            }

            @Override
            public String valueToString(int val) {
                return val + "\u00B0";
            }

        });
        ChangeListener ch = ce -> {
            if (updating[0]) {
                return;
            }
            double[] oldMx = new double[6];
            value.getMatrix(oldMx);
            double theta = Math.toRadians(slider.getValue());
            AffineTransform nue = AffineTransform.getRotateInstance(theta);
            double[] newMx = new double[6];
            nue.getMatrix(newMx);
            newMx[4] = oldMx[4];
            newMx[5] = oldMx[5];
            value.setTransform(newMx[0], newMx[1], newMx[2],
                    newMx[3], newMx[4], newMx[5]);
            mx.refresh();
            pre.refresh();
            type.setText(typeString());
            change();
        };
        JLabel rotLabel = new JLabel(Bundle.rotation());
        rotLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("controlDkShadow")));
        inner.add(rotLabel);
        slider.addChangeListener(ch);
        inner.add(slider);
        JButton reset = new JButton();
        Mnemonics.setLocalizedText(reset,
                NbBundle.getMessage(AffineTransformCustomizer.class, "RESET"));

        inner.add(reset);
        reset.addActionListener(ae -> {
            double[] d = new double[6];
            AffineTransform.getTranslateInstance(0, 0).getMatrix(d);
            updater.accept(d);
            value.setToIdentity();
//            ch.stateChanged(null);
            mx.refresh();
            pre.repaint();
            change();
        });

        return panel = pnl;
    }

    static String[] props = new String[]{"scaleX", "shearY", "shearX", "scaleY", "translateX", "translateY"};

    static String name(int ix) {
        return props[ix];
    }

    @Override
    public String getName() {
        return name;
    }

    private final Shape shape() {
        if (this.shape != null) {
            return this.shape;
        }
        Shape[] result = new Shape[1];
        GraphicsUtils.newBufferedImage(1, 1, g -> {
            Font font = new Font("Times New Roman", Font.BOLD, 12);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            String txt = "Q";
            int ht = fm.getMaxAscent() + fm.getMaxDescent();
            int wid = fm.stringWidth(txt);
            AffineTransform xf = GraphicsUtils.scalingTransform(wid, ht, 180, 180);
            font = font.deriveFont(xf);
            GlyphVector v = font.createGlyphVector(g.getFontRenderContext(), txt);
            result[0] = v.getOutline(0, 0);
            Rectangle bds = result[0].getBounds();
            // Glyph vectors are always -x ending at 0
            result[0] = AffineTransform.getTranslateInstance(-bds.x, -bds.y).createTransformedShape(result[0]);
        }).flush();
        return result[0];
    }

    private final class MatrixPreview extends JComponent {

        private final Shape orig;

        public MatrixPreview() {
            this(shape());
        }

        public MatrixPreview(Shape orig) {
            this.orig = orig;
        }

        void refresh() {
            invalidate();
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Shape xf = value.createTransformedShape(orig);
            Rectangle bds = xf.getBounds();
            Dimension result = bds.getSize();
            result.width = Math.min(680, Math.max(360, result.width + bds.x));
            result.height = Math.min(680, Math.max(360, result.height + bds.y));
            return result;
        }

        public void paintComponent(Graphics g) {
            paintC((Graphics2D) g);
        }

        private void paintC(Graphics2D g) {
            GraphicsUtils.setHighQualityRenderingHints(g);
            Shape shape = value.createTransformedShape(orig);
            Rectangle bds = shape.getBounds();
            shape = AffineTransform.getTranslateInstance(-bds.x, -bds.y).createTransformedShape(shape);
            g.setColor(new Color(128, 128, 255));
            g.fill(shape);
            g.setStroke(new BasicStroke(2));
            g.setColor(Color.BLACK);
            g.draw(shape);
            g.setColor(UIManager.getColor("controlDkShadow"));
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    static final class SimpleUI extends JPanel implements ChangeListener {

        SimpleUI() {
            super(new GridBagLayout());
            AffineTransform xform = new AffineTransform();
            xform.setToShear(WIDTH, WIDTH);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    static final class MxComponent extends JPanel implements ChangeListener, StringConverter {

        private final double[] matrix = new double[6];
        private final double rangeStart;
        private final double rangeEnd;
        private final Supplier<double[]> doubles;
        private final Consumer<double[]> consumer;
        private final Runnable onHide;
        private static final int MULTIPLIER = 1000;
        private final int min;
        private final int max;

        MxComponent(Supplier<double[]> doubles, Consumer<double[]> consumer, Runnable onHide, double rangeStart, double rangeEnd) {
            setLayout(new GridLayout(3, 2));
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.doubles = doubles;
            this.consumer = consumer;
            this.onHide = onHide;
            min = (int) Math.floor(rangeStart * MULTIPLIER);
            max = (int) Math.floor(rangeEnd * MULTIPLIER);
            System.arraycopy(doubles.get(), 0, matrix, 0, 6);
            for (int i = 0; i < 4; i++) {
                // Don't need the translate parameters
                JSlider slider = createNumberModel(i);
                add(slider);
            }
        }

        private boolean refreshing;

        void refresh() {
            refreshing = true;
            System.arraycopy(doubles.get(), 0, matrix, 0, 6);
            for (Component c : getComponents()) {
                if (c instanceof JSlider) {
                    JSlider slider = (JSlider) c;
                    Integer index = (Integer) slider.getClientProperty("ix");
                    updateSlider(index, slider);
                }
            }
            refreshing = false;
        }

        private void updateSlider(int index, JSlider slider) {
            int val = valueForSlider(index);
            slider.setValue(val);
        }

        private double valueFromSlider(int sliderValue) {
            double factor = (sliderValue - min) / (double) (max - min);
            double scaled = (rangeEnd - rangeStart) * factor;
            return Math.max(rangeStart, Math.min(rangeEnd, rangeStart + scaled));
        }

        private int valueForSlider(int index) {
            double val = matrix[index];
            double scaled = (val - rangeStart) * MULTIPLIER;
            int result = (int) Math.round(scaled) + min;
            return Math.max(min, Math.min(max, result));
        }

        private JSlider createNumberModel(int index) {
            JSlider slider = new JSlider(min, max, valueForSlider(index));
            PopupSliderUI.attach(slider);
            RadialSliderUI.setStringConverter(slider, this);
            slider.putClientProperty("ix", index);
            slider.addChangeListener(this);
            return slider;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (refreshing) {
                return;
            }
            JSlider slider = (JSlider) e.getSource();
            double val = valueFromSlider(slider.getValue());
            Integer index = (Integer) slider.getClientProperty("ix");
            matrix[index] = val;
            consumer.accept(Arrays.copyOf(matrix, matrix.length));
        }

        private final DecimalFormat FMT = new DecimalFormat("##0.0###");

        @Override
        public String valueToString(JSlider sl) {
            double real = valueFromSlider(sl.getValue());
            return FMT.format(real);
        }

        @Override
        public int maxChars() {
            return 8;
        }

        @Override
        public String valueToString(int val) {
            return FMT.format(valueFromSlider(val));
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            EventQueue.invokeLater(onHide);
        }
    }
}
