package org.imagine.awt.dnd;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import org.imagine.awt.GradientManager;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
public class Demo {

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JToolBar bar = new JToolBar();
        for (int i = 0; i < 36; i++) {
            bar.add(paintButton());
        }
        Target t = new Target();
        jf.setLayout(new BorderLayout());
        jf.add(bar, BorderLayout.NORTH);
        jf.add(t, BorderLayout.CENTER);
        jf.pack();
        jf.setVisible(true);
    }

    static final class Target extends JComponent {

        private PaintKey<?> key;

        Target() {
            setDropTarget(dt);
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    requestFocus();
                    repaint();
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    System.out.println("got key");
                    if (e.getKeyCode() == KeyEvent.VK_V && (e.getModifiersEx() & CTRL_DOWN_MASK) != 0) {
                        Clipboard cb = getToolkit().getSystemClipboard();
                        Transferable xfer = cb.getContents(this);
                        PaintKey<?> k = PaintKeyDropSupport.extractPaintKey(xfer);
                        System.out.println("  pasted " + k);
                        Target.this.key = k;
                        repaint();
                    }
                }
            });
            addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(300, 300);
        }

        public void paint(Graphics gr) {
            Graphics2D g = (Graphics2D) gr;
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (key != null) {
                Paint p = GradientManager.getDefault().findPaint(key);
                System.out.println(p);
                g.setPaint(p);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            if (hasFocus()) {
                g.setColor(Color.BLACK);
                g.drawRect(5, 5, getWidth() - 10, getHeight() - 10);
            }
        }

        DropTarget dt = new DropTarget(this, PaintKeyDropSupport.createDropTargetListener(key -> {
            System.out.println("DROPPING " + key);
            this.key = key;
            repaint();
        }));

    }

    private static JButton paintButton() {
        Paint p = randomPaint();
        PaintKey<?> key = PaintKey.forPaint(p);
        JButton btn = new JButton() {
            DragSource src = new DragSource();

            {
                src.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, new DragGestureListener() {
                    @Override
                    public void dragGestureRecognized(DragGestureEvent dge) {
                        Transferable t = PaintKeyDropSupport.createTransferrable(key);
                        src.startDrag(dge, DragSource.DefaultCopyDrop, t, new DragSourceListener() {
                            @Override
                            public void dragEnter(DragSourceDragEvent dsde) {
                            }

                            @Override
                            public void dragOver(DragSourceDragEvent dsde) {
                            }

                            @Override
                            public void dropActionChanged(DragSourceDragEvent dsde) {
                            }

                            @Override
                            public void dragExit(DragSourceEvent dse) {
                            }

                            @Override
                            public void dragDropEnd(DragSourceDropEvent dsde) {
                            }
                        });
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g1 = (Graphics2D) g;
                g1.setPaint(p);
                g.fillRect(2, 2, getWidth() - 4, getHeight() - 4);
            }
        };
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Clipboard cb = btn.getToolkit().getSystemClipboard();
                Transferable xfer = PaintKeyDropSupport.createTransferrable(key);
                System.out.println("on clipboard: " + key);
                cb.setContents(xfer, new ClipboardOwner() {
                    @Override
                    public void lostOwnership(Clipboard clipboard, Transferable contents) {

                    }
                });
            }

        });

        btn.setMargin(new Insets(5, 5, 5, 5));
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setMinimumSize(new Dimension(36, 36));
        btn.setMaximumSize(new Dimension(36, 36));
        btn.setBorderPainted(true);
        btn.setText("X");
        btn.setToolTipText(p.getClass().getSimpleName());
        btn.setContentAreaFilled(false);
        btn.setTransferHandler(PaintKeyDropSupport.createTransferHandler(comp -> {
            return key;
        }));
        return btn;
    }

    private static final Random rnd = new Random(7302539023L);

    private static Paint randomPaint() {
        Paint paint;
        int rn = rnd.nextInt(4);
        switch (rn) {
            case 0:
                paint = newGradientPaint();
                break;
            case 1:
                paint = newRadialPaint();
                break;
            case 2:
                paint = newLinearPaint();
                break;
            case 3:
                paint = new Color(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255));
                break;
            default:
                throw new AssertionError();
        }
        return paint;
    }

    private static Point2D randomPoint() {
        float x = rnd.nextFloat() * 200;
        float y = rnd.nextFloat() * 200;
        return new Point2D.Double(x, y);
    }

    private static Color randomColor() {
        int r = rnd.nextInt(255);
        int g = rnd.nextInt(255);
        int b = rnd.nextInt(255);
//            int a = rnd.nextInt(255);
        return new Color(r, g, b);
    }

    private static float[] randomFloats() {
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

    private static Color[] randomColors(int count) {
        Color[] result = new Color[count];
        for (int i = 0; i < count; i++) {
            result[i] = randomColor();
        }
        return result;
    }

    private static Paint newGradientPaint() {
        return new GradientPaint(randomPoint(), randomColor(),
                randomPoint(), randomColor(), rnd.nextBoolean());
    }

    private static <T extends Enum<T>> T randomEnum(Class<T> type) {
        T[] consts = type.getEnumConstants();
        return consts[rnd.nextInt(consts.length)];
    }

    private static Paint newRadialPaint() {
        int sz = 30;
        float rad = rnd.nextFloat() * (float) sz;
        float[] fractions = randomFloats();
        Color[] colors = randomColors(fractions.length);
        return new RadialGradientPaint(randomPoint(), rad,
                randomPoint(), fractions, colors, randomEnum(MultipleGradientPaint.CycleMethod.class), randomEnum(MultipleGradientPaint.ColorSpaceType.class
        ), AffineTransform.getTranslateInstance(0, 0));
    }

    private static Paint newLinearPaint() {
        float[] fractions = randomFloats();
        Color[] colors = randomColors(fractions.length);
        return new LinearGradientPaint(randomPoint(),
                randomPoint(), fractions, colors, randomEnum(MultipleGradientPaint.CycleMethod.class), randomEnum(MultipleGradientPaint.ColorSpaceType.class
        ), AffineTransform.getTranslateInstance(0, 0));
    }

}
