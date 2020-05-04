/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.toolcustomizers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
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
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paint.api.components.RadialSliderUI;
import org.netbeans.paint.api.components.StringConverter;
import org.openide.awt.Mnemonics;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
public class AffineTransformCustomizer implements Customizer<AffineTransform>,
        ListenableCustomizer<AffineTransform> {

    private final AffineTransform value;
    private final String name;

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
    public AffineTransform get() {
        return new AffineTransform(value);
    }

    private String typeString(int type) {
        switch (type) {
            case AffineTransform.TYPE_FLIP:
                return "Flip";
            case AffineTransform.TYPE_GENERAL_ROTATION:
                return "General Rotation";
            case AffineTransform.TYPE_GENERAL_SCALE:
                return "General Scale";
            case AffineTransform.TYPE_GENERAL_TRANSFORM:
                return "General Transform";
            case AffineTransform.TYPE_IDENTITY:
                return "Identity";
            case AffineTransform.TYPE_QUADRANT_ROTATION:
                return "Quadrant Rotation";
            case AffineTransform.TYPE_TRANSLATION:
                return "Translation";
            case AffineTransform.TYPE_UNIFORM_SCALE:
                return "Uniform Scale";
            case AffineTransform.TYPE_MASK_ROTATION:
                return "Rotation";
            case AffineTransform.TYPE_MASK_SCALE:
                return "Scale";
            default:
                return "Unknown";
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

    private JPanel panel;

    @Override
    public JComponent getComponent() {
        if (panel != null) {
            return panel;
        }
        JPanel pnl = new JPanel(new BorderLayout());

        JLabel type = new JLabel(typeString());
        pnl.add(type, BorderLayout.NORTH);

        MatrixPreview pre = new MatrixPreview();
        pnl.add(pre, BorderLayout.CENTER);
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
        MatrixComponent mx = new MatrixComponent(() -> {
            double[] d = new double[6];
            value.getMatrix(d);
            return d;
        }, updater, () -> {
            value.setToIdentity();
            change();
        });
        JPanel inner = new JPanel(new GridBagLayout());
        pnl.add(inner, BorderLayout.EAST);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = (new Insets(5, 5, 5, 5));
        c.gridy = 0;
        inner.add(mx, c);
        c.gridy++;

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
        slider.addChangeListener(ch);
        inner.add(slider, c);
        JButton reset = new JButton();
        Mnemonics.setLocalizedText(reset,
                NbBundle.getMessage(AffineTransformCustomizer.class, "RESET"));

        c.gridy++;
        inner.add(reset, c);
        reset.addActionListener(ae -> {
            System.out.println("reset ");
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

    private static SpinnerModel model(double val) {
        return new SpinnerDoubles(val);
    }

    private static JSpinner spinner(double val) {
        return new JSpinner(model(val));
    }

    @Override
    public String getName() {
        return name;
    }

    private final Shape shape() {
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
            result.width = Math.max(180, result.width + bds.x);
            result.height = Math.max(180, result.height + bds.y);
            return result;
        }

        public void paintComponent(Graphics g) {
            paintC((Graphics2D) g);
        }

        private void paintC(Graphics2D g) {
            GraphicsUtils.setHighQualityRenderingHints(g);
            g.setColor(UIManager.getColor("text"));
            Shape shape = value.createTransformedShape(orig);
            Rectangle bds = shape.getBounds();
            shape = AffineTransform.getTranslateInstance(-bds.x, -bds.y).createTransformedShape(shape);
            g.draw(shape);
            g.setColor(UIManager.getColor("controlShadow"));
            g.fill(shape);
        }
    }

    private static final class MatrixComponent extends JComponent implements ChangeListener {

        private final Supplier<double[]> doubles;
        private final JSpinner[] lbls;
        private final DecimalFormat fmt = new DecimalFormat("####00.000###");
        private final Consumer<double[]> consumer;
        private final Runnable onHide;

        public MatrixComponent(Supplier<double[]> doubles, Consumer<double[]> consumer, Runnable onHide) {
            this.doubles = doubles;
            this.consumer = consumer;
            this.onHide = onHide;
//            setLayout(new GridLayout(3, 2));
            setLayout(new GridBagLayout());
            double[] vals = doubles.get();
            lbls = new JSpinner[vals.length];
            setBackground(UIManager.getColor("controlShadow"));
            setForeground(UIManager.getColor("text"));
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 1;
            setMinimumSize(new Dimension(200, 200));
            for (int i = 0; i < vals.length; i++) {
                JSpinner l = spinner(vals[i]);
                l.setMinimumSize(new Dimension(60, 12));
                l.addChangeListener(this);
                l.setToolTipText(name(i));
                l.setOpaque(true);
                l.setBackground(UIManager.getColor("control"));
                l.setForeground(UIManager.getColor("text"));
                l.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, UIManager.getColor("controlShadow")));
                add(l, c);
                lbls[i] = l;
                if (i % 2 == 1) {
                    c.gridy++;
                    c.gridx = 0;
                } else {
                    c.gridx++;
                }
            }
            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            EventQueue.invokeLater(onHide);
        }

        private boolean refreshing;

        public void update() {
            if (refreshing) {
                return;
            }
            double[] nue = new double[lbls.length];
            for (int i = 0; i < lbls.length; i++) {
                nue[i] = (double) lbls[i].getValue();
            }
            consumer.accept(nue);
        }

        public void refresh() {
            refreshing = true;
            try {
                double[] vals = doubles.get();
                for (int i = 0; i < vals.length; i++) {
                    try {
                        lbls[i].setValue(vals[i]);
                    } catch (IllegalArgumentException ex) {
                        new IllegalArgumentException("Illegal " + vals[i]
                                + " min " + lbls[i].getModel(),
                                ex).printStackTrace();
                    }
                }
            } finally {
                refreshing = false;
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            update();
        }
    }

    static class SpinnerDoubles implements SpinnerModel {

        private double value;
        private final ChangeSupport supp = new ChangeSupport(this);

        private SpinnerDoubles(double val) {
            value = val;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object val) {
            double newValue;
            if (val == null) {
                newValue = 0;
            } else if (val instanceof Number) {
                newValue = ((Number) val).doubleValue();
            } else if (val instanceof String) {
                newValue = Double.parseDouble(val.toString().trim());
            } else {
                throw new IllegalArgumentException("Wrong type " + val.getClass());
            }
            if (this.value != newValue) {
                this.value = newValue;
                supp.fireChange();
            }
        }

        @Override
        public Object getNextValue() {
            return value + 0.1;
        }

        @Override
        public Object getPreviousValue() {
            return value - 0.1;
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            supp.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            supp.removeChangeListener(l);
        }
    }
}
