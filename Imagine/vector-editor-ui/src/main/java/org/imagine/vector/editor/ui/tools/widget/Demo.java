package org.imagine.vector.editor.ui.tools.widget;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.java.dev.imagine.api.vector.elements.CircleWrapper;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.PathText;
import net.java.dev.imagine.api.vector.elements.PolygonWrapper;
import net.java.dev.imagine.api.vector.elements.RhombusWrapper;
import net.java.dev.imagine.api.vector.elements.StringWrapper;
import net.java.dev.imagine.api.vector.elements.Text;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import net.java.dev.imagine.ui.toolbar.GridEditor;
import net.java.dev.imagine.ui.toolbar.SnapEditor;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.grid.Grid;
import org.imagine.editor.api.grid.SnapSettings;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapPointsSupplier;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.Polygon2D;
import com.mastfrog.geometry.Rhombus;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.RepaintProxyShapes;
import org.imagine.vector.editor.ui.Shapes;
import org.imagine.vector.editor.ui.palette.InMemoryPaintPaletteBackend;
import org.imagine.vector.editor.ui.palette.InMemoryShapePaletteBackend;
import org.imagine.vector.editor.ui.palette.PaintPalettes;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.netbeans.api.visual.widget.EventProcessingType;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
public class Demo {

    static ChangeListener cl;

    static Random RND = new Random(3592033264732L);

    static ImageWrapper randomImage() {
        BufferedImage img = GraphicsUtils.newBufferedImage(40, 40, g -> {
            g.setColor(Color.RED);
            g.fillRect(0, 0, 40, 40);
            Circle circ = new Circle(10, 10, 5);
            g.setColor(Color.ORANGE);
            g.fill(circ);
            circ.setCenter(15, 15);
            g.fill(circ);
            circ.setCenter(20, 20);
            g.fill(circ);
        });
        return new ImageWrapper(img, 300, 50);
    }

    static PathIteratorWrapper randomShape() {
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_NON_ZERO);
        int count = RND.nextInt(5) + 4;
        DoubleSupplier dbl = () -> {
            return 100 + (RND.nextDouble() * 200);
        };
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                path.moveTo(dbl.getAsDouble(), dbl.getAsDouble());
            } else if (i != count - 1) {
                int t = RND.nextInt(3);
                switch (t) {
                    case 0:
                        path.lineTo(dbl.getAsDouble(), dbl.getAsDouble());
                        break;
                    case 1:
                        path.curveTo(dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble());
                    case 2:
                        path.quadTo(dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble());
                }
            } else {
                path.closePath();
            }
        }
        return new PathIteratorWrapper(path);
    }

    static Color randomColor() {
        return new Color(RND.nextInt(255), RND.nextInt(255), RND.nextInt(255));
    }

    static {
//        System.setProperty("ArrayIntMap.debug", "true");
    }

    public static void main(String[] args) {
        Grid.getInstance().setEnabled(false);
//        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("swing.aatext", "true");
        System.setProperty("awt.useSystemAAFontSettings", "lcd_hrgb");

        InMemoryPaintPaletteBackend paintPaletteBackend
                = new InMemoryPaintPaletteBackend().setAsDefault();
        for (int i = 0; i < 8; i++) {
            paintPaletteBackend.addRandom();
        }

        InMemoryShapePaletteBackend shapePaletteBackend
                = new InMemoryShapePaletteBackend().setAsDefault();
        for (int i = 0; i < 8; i++) {
            shapePaletteBackend.addRandom();
        }

        JComponent paintPalette = PaintPalettes.createPaintPaletteComponent();
        JComponent shapePalette = PaintPalettes.createShapesPaletteComponent();

        Scene scene = new Scene();
        SceneRepainter rep = new SceneRepainter(scene);
        Shapes shapes = new Shapes();
        RepaintProxyShapes proxy = new RepaintProxyShapes(shapes, rep);
        BasicStroke strk = new BasicStroke(3);

        Path2D.Double p2 = new Path2D.Double();
        p2.moveTo(10, 100);
        p2.curveTo(200, 600, 200, 0, 400, 500);
        p2.curveTo(500, 800, 500, 600, 10, 700);
        p2.curveTo(-100, 1200, 400, 800, 400, 1000);

        Shape s2 = AffineTransform.getScaleInstance(0.25, 0.25).createTransformedShape(p2);

        PathText txt = new PathText(
                //                new CircleWrapper(200, 200, 200),
                //                new CircleWrapper(200, 200, 200),
                new PathIteratorWrapper(s2),
                new StringWrapper("Hello World, how about path text?", 0, 0), FontWrapper.create(
                        "Times New Roman", 30F, Font.BOLD));

//        proxy.add(txt, new Color(140, 140, 240), Color.RED,
//                new BasicStroke(0.5f), PaintingStyle.FILL);
//        proxy.add(new PathIteratorWrapper(new PieWedge(200, 200, 200, 0, 110)), new Color(140, 140, 240), Color.RED,
//                strk, true, true);
//        proxy.add(randomImage(), Color.BLACK, Color.RED,
//                strk, true, true);
        proxy.add(new Text("Hello", new Font("Times New Roman", Font.BOLD, 32), 5, 5), randomColor(), randomColor(), strk, PaintingStyle.OUTLINE_AND_FILL);
//        proxy.add(randomShape(), new Color(128, 128, 255), new Color(0, 0, 128),
//                strk, true, true);

        proxy.add(new net.java.dev.imagine.api.vector.elements.Rectangle(350, 10, 30, 30, true), randomColor(), randomColor(), strk, true, true);

        double[] pts = new double[]{
            200, 20.5,
            180, 50,
            200.232, 200,
            150, 150.77773,
            12, 200,
            50, 50,
            60, 45,
            50, 40,
            3, 80,
            10, 60,
            10, 10,
            100, 10,
            100, 100,};
        Polygon2D p2d = new Polygon2D(pts);
        System.out.println("POLY NORM " + p2d.isNormalized());
        p2d.reverse();
        p2d.normalize();

        Rhombus rhom = new Rhombus(100, 100, 60, 30, 15);
        proxy.add(new RhombusWrapper(rhom), Color.YELLOW, Color.GRAY, strk, PaintingStyle.OUTLINE_AND_FILL);
//        System.out.println("POLY NORM NOW " + p2d.isNormalized());

//        proxy.add(new PolygonWrapper(p2d), Color.ORANGE, Color.BLUE, strk, PaintingStyle.OUTLINE_AND_FILL);
//        PathIteratorWrapper piw = new PathIteratorWrapper(p2d);
        PolygonWrapper piw = new PolygonWrapper(p2d);
        piw.translate(250, 250);
        proxy.add(piw, Color.ORANGE, Color.BLUE, strk, PaintingStyle.OUTLINE_AND_FILL);

        proxy.add(new CircleWrapper(30, 330, 22), Color.BLUE, Color.YELLOW,
                strk, true, true);


        proxy.add(new TriangleWrapper(140, 140, 169, 160, 180, 140), Color.ORANGE,
                Color.BLUE, strk, true, true);


        proxy.add(new PathIteratorWrapper(new Rhombus(80, 80, 30, 40, 45)),
                Color.GREEN.darker(), Color.GRAY, strk, true, true);

        JLabel lbl = new JLabel("Status here");
        CtrllrImpl c = new CtrllrImpl(shapes, lbl, scene);

        SnapSettings.getGlobal().setSnapKinds(EnumSet.noneOf(SnapKind.class));
        EventQueue.invokeLater(() -> {
            try {
                DesignWidgetManager man = new DesignWidgetManager(scene, shapes);
                Widget w = man.getMainWidget();

                scene.setKeyEventProcessingType(EventProcessingType.FOCUSED_WIDGET_AND_ITS_PARENTS);

                scene.addChild(w);
                scene.addChild(new GridWidget(scene));

                cl = ce -> {
                    scene.repaint();
                };

                Grid.getInstance().addChangeListener(cl);
                SnapSettings.getGlobal().addChangeListener(cl);

                JToolBar bar = new JToolBar();

                JButton add = new JButton("Add");
                bar.add(add);
                add.addActionListener(ae -> {
                    proxy.add(randomShape(), randomColor(), randomColor(), strk, true, true);
                    man.shapeMayBeAdded();
                });
                JButton del = new JButton("Rem");
                bar.add(del);
                del.addActionListener(ae -> {
                    if (shapes.size() > 0) {
                        int ix = ThreadLocalRandom.current().nextInt(shapes.size());
                        ShapeElement el = shapes.get(ix);
                        shapes.deleteShape(el);
                        man.shapesMayBeDeleted();
                    } else {
                        System.err.println("No more shapes");
                    }
                });

                float[] fractions = new float[]{0.0625F, 0.125F, 0.25F, 0.5F, 0.75F, 1F,
                    1.25F, 1.5F, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                    16, 17, 18, 19, 20, 21, 22, 23, 24, 25};
                int[] zoomCursor = new int[]{7};
                scene.setZoomFactor(fractions[zoomCursor[0]]);
                JButton in = new JButton("Zoom In");
                in.addActionListener(ae -> {
                    if (zoomCursor[0] < fractions.length - 1) {
                        c.z.setZoom(fractions[++zoomCursor[0]]);
                        scene.validate();
                        man.centerSelected();
                        scene.repaint();
                    }
                });
                JButton out = new JButton("Zoom Out");
                out.addActionListener(ae -> {
                    if (zoomCursor[0] > 0) {
                        c.z.setZoom(fractions[--zoomCursor[0]]);
                        scene.validate();
                        man.centerSelected();
                        scene.repaint();
                    }
                });
                bar.add(in);
                bar.add(out);
                bar.add(new JSeparator());
                bar.add(new GridEditor());
                bar.add(new JSeparator());
                bar.add(new SnapEditor());

                JFrame jf = new JFrame();
                jf.setTitle("Designer Demo");
                jf.setLayout(new BorderLayout());
                jf.add(bar, BorderLayout.NORTH);
                jf.add(lbl, BorderLayout.SOUTH);
                JComponent view = scene.createView();

                JScrollPane scroll = scrollPane(view);
                JViewport viewport = scroll.getViewport();
                viewport.setScrollMode(JViewport.BLIT_SCROLL_MODE);
                viewport.setDoubleBuffered(false);

//                jf.add(scrollPane(paintPalette), BorderLayout.EAST);
//                jf.add(scrollPane(shapePalette), BorderLayout.WEST);
                jf.add(paintPalette, BorderLayout.EAST);
                jf.add(shapePalette, BorderLayout.WEST);

                jf.add(scroll, BorderLayout.CENTER);
                jf.pack();
                jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                jf.setVisible(true);
                scene.getView().requestFocus();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        });
    }

    private static JScrollPane scrollPane(JComponent comp) {
        JScrollPane result = new JScrollPane(comp);
        result.setBorder(BorderFactory.createEmptyBorder());
        result.setViewportBorder(BorderFactory.createEmptyBorder());
        return result;

    }

    static class SceneRepainter implements Repainter, RepaintHandle {

        private final Scene scene;

        public SceneRepainter(Scene scene) {
            this.scene = scene;
        }

        private void withView(Consumer<JComponent> c) {
            JComponent comp = scene.getView();
            if (comp != null) {
                c.accept(comp);
            }
        }

        @Override
        public void requestRepaint() {
            withView(JComponent::repaint);
            scene.repaint();
        }

        @Override
        public void requestRepaint(Rectangle bounds) {
            withView(comp -> {
                comp.repaint(bounds);
            });
            scene.repaint();
        }

        @Override
        public void setCursor(Cursor cursor) {
            withView(comp -> {
                comp.setCursor(cursor);
            });
        }

        @Override
        public void requestCommit() {
            // do nothing
            scene.repaint();
        }

        @Override
        public Component getDialogParent() {
            return scene.getView();
        }

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            withView(c -> {
                c.repaint(x, y, w, h);
            });
        }
    }

    static class CtrllrImpl implements WidgetController {

        Z z = new Z();
        private final Shapes shapes;
        private final JLabel lbl;
        private final Scene scene;

        public CtrllrImpl(Shapes shapes, JLabel lbl, Scene scene) {
            this.shapes = shapes;
            this.lbl = lbl;
            this.scene = scene;
        }

        @Override
        public Zoom getZoom() {
            return z;
        }

        @Override
        public SnapPointsSupplier snapPoints() {
            return shapes.snapPoints(11, (xp, yp) -> {
                lbl.setText(xp + " / " + yp);
                return true;
            })::get;
        }

        class Z implements Zoom {

            private double zm = 1;
            private final ChangeSupport supp = new ChangeSupport(this);

            @Override
            public double getZoom() {
                return zm;
            }

            @Override
            public void setZoom(double val) {
                zm = val;
                scene.setZoomFactor(zm);
                supp.fireChange();
                scene.validate();
                scene.repaint();
            }

            @Override
            public void addChangeListener(ChangeListener cl) {
                supp.addChangeListener(cl);
            }

            @Override
            public void removeChangeListener(ChangeListener cl) {
                supp.removeChangeListener(cl);
            }
        }
    }
}
