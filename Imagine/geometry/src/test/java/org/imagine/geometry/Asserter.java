package org.imagine.geometry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import org.junit.jupiter.api.Assertions;

/**
 *
 * @author Tim Boudreau
 */
final class Asserter {

    private final boolean visualAssert;
    private final List<AssertionError> failures = new ArrayList<>();
    private final Map<AssertionError, Painter> painterForError = new IdentityHashMap<>();

    public Asserter(boolean visual) {
        this.visualAssert = visual;
    }

    public void coalesce(Asserter other) {
        failures.addAll(other.failures);
        painterForError.putAll(other.painterForError);
    }

    public void add(String s, Painter p) {
        painterForError.put(new AssertionError("OK: " + s), p);
    }

    public void assertTrue(boolean val, String msg) {
        assertTrue(val, msg, null);
    }

    public void assertTrue(boolean val, String msg, Painter painter) {
        try {
            Assertions.assertTrue(val, msg);
        } catch (AssertionError err) {
            err.printStackTrace();
            failures.add(err);
            if (painter != null) {
                painterForError.put(err, painter);
            }
        }
    }

    public void assertEquals(Object a, Object b, String msg) {
        assertEquals(a, b, msg, null);
    }

    public void assertEquals(Object a, Object b, String msg, Painter painter) {
        try {
            Assertions.assertEquals(a, b, msg);
        } catch (AssertionError err) {
            failures.add(err);
            err.printStackTrace();
            if (painter != null) {
                painterForError.put(err, painter);
            }
        }
    }

    private void showUI() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        assert !painterForError.isEmpty();
        CountDownLatch latch = new CountDownLatch(1);
        Map<AssertionError, Painter> m = Collections.unmodifiableMap(new HashMap<>(painterForError));
        painterForError.clear();
        EventQueue.invokeLater(() -> {
            JFrame jf = new JFrame();
            jf.setContentPane(new JScrollPane(new VisComp(new HashMap<>(m))));
            jf.pack();
            jf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            jf.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    latch.countDown();
                }
            });
            jf.setVisible(true);
        });
        try {
            latch.await(60000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Asserter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void rethrow() {
        if (visualAssert && !painterForError.isEmpty()) {
            showUI();
        }
        AssertionError first = null;
        if (!failures.isEmpty()) {
            Iterator<AssertionError> it = failures.iterator();
            first = it.next();
            while (it.hasNext()) {
                first.addSuppressed(it.next());
            }
        }
        if (first != null) {
            throw first;
        }
    }

    public interface Painter {

        void paint(Graphics2D g, double scale);

        Rectangle requiredBounds();
    }

    static class VisComp extends JPanel implements DoubleSupplier {

        public VisComp(Map<AssertionError, Painter> items) {
            super(new GridBagLayout());
            ZoomInAction in = new ZoomInAction();
            ZoomOutAction out = new ZoomOutAction();
            InputMap inMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap actions = getActionMap();
            inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "in");
            inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "out");
            actions.put("in", in);
            actions.put("out", out);
            int ix = 0;
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            Set<Painter> seen = new HashSet<>();
            for (Map.Entry<AssertionError, Painter> e : items.entrySet()) {
                if (seen.contains(e.getValue())) {
                    continue;
                }
                seen.add(e.getValue());
                PComp comp = new PComp(e.getValue(), this);
                String msg = e.getKey().getMessage();
                add(comp, gbc);
                gbc.gridy++;
                JTextArea area = new JTextArea(msg);
                area.setEditable(false);
                area.setBackground(Color.LIGHT_GRAY);
                area.setWrapStyleWord(true);
                area.setLineWrap(true);
                area.setColumns(60);
                add(area, gbc);
                gbc.gridy--;
                gbc.gridx++;
                if (ix++ % 2 == 0 && ix != 0) {
                    gbc.gridx = 0;
                    gbc.gridy += 2;
                }
            }
        }

        public double getAsDouble() {
            return sizes[zoomIndex];
        }

        int zoomIndex = 5;
        private double[] sizes = {0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75,
            2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

        class ZoomInAction extends AbstractAction {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (zoomIndex + 1 < sizes.length) {
                    zoomIndex++;
                    invalidate();
                    revalidate();
                    repaint();
                }
            }
        }

        class ZoomOutAction extends AbstractAction {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (zoomIndex - 1 < sizes.length) {
                    zoomIndex--;
                    invalidate();
                    revalidate();
                    repaint();
                }
            }

        }

        private static final class PComp extends JComponent {

            private final Painter painter;
            private final DoubleSupplier zoom;

            public PComp(Painter painter, DoubleSupplier zoom) {
                this.painter = painter;
                this.zoom = zoom;
            }

            @Override
            public Dimension getPreferredSize() {
                Rectangle r = painter.requiredBounds();
                double z = zoom.getAsDouble();
                double w = r.width * z;
                double h = r.height * z;
                return new Dimension((int) Math.ceil(w),
                        (int) Math.ceil(h));
            }

            @Override
            public void paintComponent(Graphics gg) {
                Graphics2D g = (Graphics2D) gg;
                g.setColor(Color.LIGHT_GRAY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g = (Graphics2D) g.create();
                double z = zoom.getAsDouble();
                g.scale(z, z);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                painter.paint(g, z);
                g.dispose();
            }
        }
    }
}
