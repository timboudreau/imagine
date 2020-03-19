package org.imagine.awt;

import java.awt.AlphaComposite;
import static java.awt.AlphaComposite.SRC_OVER;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.Timer;
import org.imagine.awt.counters.Ring;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Circle;

/**
 *
 * @author Tim Boudreau
 */
public class Demo {

    public static class RandomGradientComponent extends JComponent implements ActionListener {

        private final GradientManager manager;
        private static final Random rnd = new Random(2302039023L);
        int changes;
        private long tick;

        private Circle[] circles;
        private double[] angles;

        {
            int circCount = 25;
            int maxRadius = 150;
            int minRadius = 10;
            circles = new Circle[circCount];
            angles = new double[circCount];
            for (int i = 0; i < circles.length; i++) {
                int sz = rnd.nextInt(maxRadius - minRadius);
                Point2D p = randomPoint();
                circles[i] = new Circle(p.getX(), p.getY(), sz);
            }
            for (int i = 0; i < angles.length; i++) {
                angles[i] = randomAngle();
            }
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!e.isPopupTrigger() && e.getClickCount() == 2) {
                        for (int i = 0; i < 5; i++) {
                            System.gc();
                            System.runFinalization();
                        }
                    }
                }

            });
        }

        private Paint paint = new GradientPaint(160, 160, Color.BLUE, 620, 620, Color.ORANGE, true);

        private static double randomAngle() {
            return rnd.nextDouble() * 360;
        }

        private void updatePaint() {
            int rn = rnd.nextInt(3);
            switch (rn) {
                case 0:
                    paint = newGradientPaint();
                    break;
                case 1:
                    paint = newRadialPaint();
                    break;
                case 2:
                    paint = newLinearPaint();
            }
            if (onChange != null) {
                onChange.accept(paint);
            }
            DefaultGradientManager.getDefault().findPaint(paint, getWidth(), getHeight());
            changes++;
        }

        private Consumer<Paint> onChange;

        private Point2D randomPoint() {
            float x = rnd.nextFloat() * getWidth();
            float y = rnd.nextFloat() * getHeight();
            return new Point2D.Double(x, y);
        }

        private Color randomColor() {
            int r = rnd.nextInt(255);
            int g = rnd.nextInt(255);
            int b = rnd.nextInt(255);
//            int a = rnd.nextInt(255);
            return new Color(r, g, b);
        }

        private float[] randomFloats() {
            float[] result = new float[rnd.nextInt(20) + 2];
            result[result.length - 1] = 1;
            for (int i = 1; i < result.length - 1; i++) {
                do {
                    result[i] = 0.1F + rnd.nextFloat() * 0.8F;
                } while (result[i - 1] == result[i]);
            }
            Arrays.sort(result);
            return result;
        }

        private Color[] randomColors(int count) {
            Color[] result = new Color[count];
            for (int i = 0; i < count; i++) {
                result[i] = randomColor();
            }
            return result;
        }

        private Paint newGradientPaint() {
            return new GradientPaint(randomPoint(), randomColor(),
                    randomPoint(), randomColor(), rnd.nextBoolean());
        }

        private <T extends Enum<T>> T randomEnum(Class<T> type) {
            T[] consts = type.getEnumConstants();
            return consts[rnd.nextInt(consts.length)];
        }

        private Paint newRadialPaint() {
            int sz = Math.min(getWidth(), getHeight());
            float rad = rnd.nextFloat() * (float) sz;
            float[] fractions = randomFloats();
            Color[] colors = randomColors(fractions.length);
            return new RadialGradientPaint(randomPoint(), rad,
                    randomPoint(), fractions, colors, randomEnum(CycleMethod.class), randomEnum(ColorSpaceType.class
            ), AffineTransform.getTranslateInstance(0, 0));
        }

        private Paint newLinearPaint() {
            float[] fractions = randomFloats();
            Color[] colors = randomColors(fractions.length);
            return new LinearGradientPaint(randomPoint(),
                    randomPoint(), fractions, colors, randomEnum(CycleMethod.class), randomEnum(ColorSpaceType.class
            ), AffineTransform.getTranslateInstance(0, 0));
        }

        private final Timer timer = new Timer(20, this);

        {
            timer.setRepeats(true);
            setDoubleBuffered(false);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        Rectangle adj = new Rectangle();

        private void adjust() {
            tick++;
            Rectangle bds = new Rectangle(0, 0, getWidth(), getHeight());

            for (int i = 0; i < circles.length; i++) {
                Circle c = circles[i];
                c.getBounds(adj);
                if (adj.x < 0 || adj.y < 0 || adj.x + adj.width > bds.width || adj.y + adj.height > bds.height) {
                    angles[i] = newAngle(c, angles[i]);
                }
                double[] pos = c.positionOf(angles[i], 2);
                c.setCenter(pos[0], pos[1]);
            }
            if (tick % 200 == 0) {
                updatePaint();
            }
        }

        private double newAngle(Circle circ, double curr) {
            double opp = Angle.opposite(curr);
            opp += rnd.nextDouble() * 120;
            if (opp > 360) {
                opp -= 360D;
            }
            return opp;
        }

        public RandomGradientComponent(GradientManager manager) {
            this.manager = manager;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(640, 480);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public void reshape(int x, int y, int w, int h) {
            boolean change = w != getWidth() || h != getHeight();

            super.reshape(x, y, w, h);
            if (change) {
                int cx = w / 2;
                int cy = h / 2;
                for (Circle c : circles) {
                    c.setCenter(cx, cy);
                }
            }
        }

        @Override
        public void paint(Graphics g) {
//            Graphics g1 = manager.wrapGraphics((Graphics2D) g);
//            super.paint(g1);
            super.paint(g);
        }

        @Override
        protected void paintComponent(Graphics gr) {
            long now = System.nanoTime();
            Graphics2D g = (Graphics2D) gr;
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            adjust();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

            g.setPaint(GradientManager.getDefault().findPaint(paint, getWidth(), getHeight()));
//            g.setPaint(paint);

            g.setComposite(AlphaComposite.getInstance(SRC_OVER, 0.25F));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setComposite(AlphaComposite.getInstance(SRC_OVER, 1));
            for (Circle c : circles) {
                g.fill(c);
            }
            g.setPaint(Color.BLACK);
            for (Circle c : circles) {
                g.draw(c);
            }
            long elapsed = (System.nanoTime() - now);

            long memory
                    = Runtime.getRuntime().totalMemory()
                    - Runtime.getRuntime().freeMemory();

            recordMemory(max);
            int top = 20;
            g.drawString("Change: " + changes, 0, (top += 20));
            g.drawString("Repaint: " + tick, 0, (top += 20));
            g.drawString("Mem Use: " + memory, 0, (top += 20));
            long accel = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getAvailableAcceleratedMemory();

            g.drawString("Accel : " + accel, 0, (top += 20));

            g.drawString("Elapsed : " + elapsed,
                    0, (top += 20));

            max = Math.max(max, elapsed);

            if (tick > 1500) {
                double rel = (double) elapsed / max;
                g.drawString("Max : " + max, 0, (top += 20));
                g.drawString("PCT : " + fmt.format(rel), 0, (top += 20));

                double w = max * factor;
                r.setFrame(0, 0, w, 18);
                g.draw(r);
                w = elapsed * factor;
                r.setFrame(0, 0, w, 18);
                g.fill(r);
            }
            paintMemoryUsage(g);
        }
        Rectangle2D.Double r = new Rectangle2D.Double();
        long max = 0;
        double factor = 0.0000035;
        DecimalFormat fmt = new DecimalFormat("####0.00000");
        private final Ring<Long> memorySizes = new Ring<>(50);
        private Color bars = new Color(0, 0, 0, 70);
        private long historicMinMemory = Long.MAX_VALUE;
        private long historicMaxMemory = Long.MIN_VALUE;

        private final Ring<Long> cumulation = new Ring<>(1200);

        private void recordMemory(long mem) {
            historicMaxMemory = Math.max(historicMaxMemory, mem);
            historicMinMemory = Math.min(historicMinMemory, mem);
            cumulation.put(mem);
            if (tick % cumulation.size() == 0) {
                long avg = 0;
                int ct = 0;
                for (Long l : cumulation) {
                    avg += l;
                    ct++;
                }
                avg /= ct;
                memorySizes.put(avg);
            }

        }

        private void paintMemoryUsage(Graphics2D g) {
            List<Long> sizes = memorySizes.copy();
            if (sizes.size() < 3) {
                return;
            }
            double bottom = getHeight();
            double height = bottom / 4;
            double range = (historicMaxMemory - historicMinMemory);
            double w = getWidth();
            double barWidth = (w / (double) sizes.size()) - 2D;
            r.setFrame(w - barWidth, bottom, barWidth, 0);
            g.setColor(bars);
            for (int i = sizes.size() - 1; i >= 0; i--) {
                long l = sizes.get(i) - historicMinMemory;
                double ht = height * (l / range);
                r.height = ht;
                r.y = bottom - ht;
                if (r.x + r.width < 0) {
                    break;
                }
                g.fill(r);
                r.x -= 2 + barWidth;
            }
        }

        public void actionPerformed(ActionEvent e) {
//            paint(getGraphics());
//            paintImmediately(0, 0, getWidth(), getHeight());
            repaint();
        }

        class RasterCachingPaint implements Paint {

            private final Paint orig;

            public RasterCachingPaint(Paint orig) {
                this.orig = orig;
            }

            @Override
            public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
//                System.out.println("CTX " + deviceBounds.x + "," + deviceBounds.y + "," + deviceBounds.width + "," + deviceBounds.height
//                        + " " + userBounds.getX() + "," + userBounds.getY() + "," + userBounds.getWidth() + ", " + userBounds.getHeight()
//                );
                return new WrapContext(orig.createContext(cm, deviceBounds, userBounds, xform, hints));
            }

            @Override
            public int getTransparency() {
                return orig.getTransparency();
            }

            class WrapContext implements PaintContext {

                private final PaintContext ctx;

                public WrapContext(PaintContext ctx) {
                    this.ctx = ctx;
                }

                @Override
                public void dispose() {
                    ctx.dispose();
                }

                @Override
                public ColorModel getColorModel() {
                    return ctx.getColorModel();
                }

                @Override
                public Raster getRaster(int x, int y, int w, int h) {
                    Raster result = ctx.getRaster(x, y, w, h);
//                    System.out.println("  rast " + x + "," + y + "," + w + "," + h + " id " + System.identityHashCode(result)
//                        + " bytes " + (result.getWidth() * result.getHeight() * 3 * 4));
                    return result;
                }

            }

        }

    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");

        CachingGradientManager mgr = new CachingGradientManager();
        RandomGradientComponent comp = new RandomGradientComponent(mgr);
        JFrame jf = new JFrame("Demo");
        JPanel pnl = new JPanel(new BorderLayout());
        JToolBar tools = new JToolBar();
        pnl.add(tools, BorderLayout.NORTH);
        pnl.add(comp, BorderLayout.CENTER);
        JCheckBox box = new JCheckBox("Enabled");
        box.setSelected(mgr.isEnabled());
        box.addActionListener(ae -> {
            mgr.setEnabled(box.isSelected());
        });
        tools.add(box);
        JLabel type = new JLabel("GradientPaint");
        tools.add(type);
        comp.onChange = pt -> {
            type.setText(pt.getClass().getSimpleName());
        };
        jf.setDefaultCloseOperation(EXIT_ON_CLOSE);
        jf.setContentPane(pnl);
        jf.pack();
        jf.setVisible(true);
    }

    public Demo() {
    }
}
