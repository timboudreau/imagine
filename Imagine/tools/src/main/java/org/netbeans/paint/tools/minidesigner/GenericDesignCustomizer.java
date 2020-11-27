package org.netbeans.paint.tools.minidesigner;

import com.mastfrog.function.state.Obj;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.editor.api.CheckerboardBackground;
import org.imagine.editor.api.ImageEditorBackground;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.EnhRectangle2D;
import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.Triangle2D;
import com.mastfrog.geometry.path.FlyweightPathElement;
import com.mastfrog.geometry.path.PathElement;
import com.mastfrog.geometry.path.PathElementKind;
import com.mastfrog.geometry.path.PointKind;
import com.mastfrog.geometry.util.PooledTransform;
import org.imagine.nbutil.filechooser.FileChooserBuilder;
import org.imagine.nbutil.filechooser.FileKinds;
import org.netbeans.paint.tools.minidesigner.MiniToolCanvas.Painter;
import org.netbeans.paint.tools.responder.PathUIProperties;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
public class GenericDesignCustomizer extends JPanel implements PointsProvider, Painter {

    private final MiniToolCanvas mini = new MiniToolCanvas();
    private PathSegmentModel model;
    private PointEditTool tool;
    private PathUIProperties props = new PathUIProperties(() -> {
        return mini.getLookup().lookup(ToolUIContext.class);
    });
    private final Circle circ = new Circle();
    private final EqLine line = new EqLine();
    private Consumer<PathSegmentModel> onEdit;
    private final TinyButton editButton;
    private Consumer<PathSegmentModel> onHide;

    public GenericDesignCustomizer() {
        this(null);
    }

    public GenericDesignCustomizer(PathSegmentModel model) {
        this.model = model == null ? new PathSegmentModel() : model;
        setLayout(new BorderLayout());
        add(new JScrollPane(mini), BorderLayout.CENTER);
        JToolBar bar = new JToolBar();
        JButton in = new TinyButton("+");
        bar.add(in);
        in.addActionListener(ae -> {
            mini.setScale(mini.getScale() * 1.25);
        });
        JButton out = new TinyButton("-");
        out.addActionListener(ae -> {
            mini.setScale(mini.getScale() * 0.75);
        });
        JButton clr = new TinyButton("x");
        clr.addActionListener(ae -> {
            this.model.clear();
        });
        bar.add(out);
        bar.add(clr);
        JButton mirror = new TinyButton("m");
        mirror.addActionListener(ae -> {
//            model.clear();
            this.model.mirror();
        });
        JButton normX = new TinyButton("nx");
        normX.addActionListener(ae -> {
            this.model.renormalize();
        });

        JButton scaleY = new TinyButton("sy");
        scaleY.addActionListener(ae -> {
            this.model.scaleY(1.5);
        });

        editButton = new TinyButton("e");
        editButton.addActionListener(ae -> {
            if (onEdit != null) {
                onEdit.accept(getModel());
            }
        });

        JButton loadButton = new TinyButton("Load");
        loadButton.addActionListener(ae -> {
            loadModel();
        });
        JButton saveButton = new TinyButton("Save");
        saveButton.addActionListener(ae -> {
            saveModel();
        });

        editButton.setVisible(false);
        bar.add(new JSeparator());
        bar.add(mirror);
        bar.add(scaleY);
        bar.add(normX);
        bar.add(new JSeparator());
        bar.add(editButton);
        bar.add(loadButton);
        bar.add(saveButton);
        add(bar, BorderLayout.NORTH);
        bar.add(new JSeparator());

        mini.addToLookup(this.model);
        mini.addToLookup(this);
        mini.addPainter(this);
    }

    @Messages({
        "pathSegmentFiles=Path Segment Files",
        "load=Load"
    })
    private void loadModel() {
        Path file = new FileChooserBuilder(GenericDesignCustomizer.class)
                .withFileExtension(Bundle.pathSegmentFiles(), ".pathsegment")
                .setFileHiding(true)
                .setFileKinds(FileKinds.FILES_ONLY)
                .setApproveText(Bundle.load())
                .showOpenDialogNIO();
        if (file != null) {
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                ByteBuffer buf = ByteBuffer.allocate((int) channel.size());
                channel.read(buf);
                buf.flip();
                PathSegmentModel mdl = PathSegmentModel.read(buf);
                this.model.replaceFrom(mdl);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void saveModel() {
        Path file = new FileChooserBuilder(GenericDesignCustomizer.class)
                .withFileExtension(Bundle.pathSegmentFiles(), ".pathsegment")
                .setFileHiding(true)
                .setFileKinds(FileKinds.FILES_ONLY)
                .setApproveText(Bundle.load())
                .showSaveDialogNio();
        if (file != null) {
            if (!file.getFileName().endsWith(".pathsegment")) {
                file = file.getParent().resolve(file.getFileName() + ".pathsegment");
            }
            ByteBuffer buf = model.write();
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                buf.flip();
                channel.write(buf);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    public void onEdit(Consumer<PathSegmentModel> c) {
        if (c != null) {
            editButton.setVisible(true);
            this.onEdit = c;
        } else {
            editButton.setVisible(false);
        }
    }

    private static final class TinyButton extends JButton {

        TinyButton(String caption) {
            super(caption);
            setBorder(
                    BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 0, 1, 5),
                            BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow"), 1)));
        }

        @Override
        public Dimension getPreferredSize() {
            String txt = getText();
            if (txt == null || txt.isEmpty()) {
                txt = "O";
            }
            FontMetrics fm = getFontMetrics(getFont());
            if (fm == null) {
                return new Dimension(20, 20);
            }
            int w = fm.stringWidth(txt);
            int h = fm.getHeight() + fm.getDescent();
            Insets ins = getInsets();
            if (txt.length() == 1) {
                int sz = Math.max(w, h);
                return new Dimension(ins.left + ins.right + sz, ins.top + ins.bottom + sz);
            }
            return new Dimension(w + ins.left + ins.right, h + ins.top + ins.bottom);
        }

    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            ImageEditorBackground.getDefault().setStyle(CheckerboardBackground.DARK);
            JFrame jf = new JFrame();
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            GenericDesignCustomizer gdc = new GenericDesignCustomizer();
            JPanel pnl = new JPanel(new BorderLayout());
            gdc.getModel().addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 25, -10, 27, 10);
            gdc.getModel().addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 35, 10, 35, 0);
            gdc.getModel().addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 40, -5, 35, 0);
            gdc.getModel().addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 45, 5, 45, 0);
            gdc.getModel().addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 50, 0, 50, 5);
            gdc.getModel().addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 55, -5, 55, 5);
            gdc.getModel().addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 75, 10, 100, 0);
            pnl.add(gdc, BorderLayout.CENTER);
            pnl.add(gdc.satelliteView(), BorderLayout.SOUTH);
            jf.setContentPane(pnl);
//            jf.pack();
            jf.setSize(new Dimension(1200, 800));
            jf.setVisible(true);
        });
    }

    JComponent satelliteView() {
        return new SatelliteView();
    }

    class SatelliteView extends JComponent implements ChangeListener {

        SatelliteView() {
            setMinimumSize(new Dimension(200, 200));
            GenericDesignCustomizer.this.addPropertyChangeListener("model", pce -> {
                PathSegmentModel oldModel = (PathSegmentModel) pce.getOldValue();
                PathSegmentModel newModel = (PathSegmentModel) pce.getNewValue();
                if (oldModel != null) {
                    oldModel.removeChangeListener(this);
                }
                if (newModel != null) {
                    newModel.addChangeListener(this);
                }
                invalidate();
                revalidate();
                repaint();
            });
            if (model != null) {
                model.addChangeListener(this);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Container par = getParent();
            Dimension sz = par.getSize();
            if (sz.width > 0 && sz.height > 0) {
                int amt = Math.min(sz.width, sz.height);
                return new Dimension(amt / 2, amt / 2);
            }
            return new Dimension(300, 300);
        }

        Path2D.Double check;

        private Path2D.Double createShape() {
            if (model == null) {
                return new Path2D.Double();
            }
            int wh = Math.min(getWidth(), getHeight());
            Rectangle rect = new Rectangle(wh / 4, wh / 4, wh / 2, wh / 2);
            Path2D.Double result = new Path2D.Double();
            double max = model.maxDestinationScale();
            double factor = 1D / max;
            check = new Path2D.Double();

            Point2D last = model.apply(result, rect.getLocation(),
                    new EqPointDouble(rect.x + rect.width, rect.y), true, factor);
            check.moveTo(last.getX(), last.getY());
            last = model.apply(result, last,
                    new EqPointDouble(rect.x + rect.width, rect.y + rect.height), false, factor);
            check.lineTo(last.getX(), last.getY());
            last = model.apply(result, last,
                    new EqPointDouble(rect.x, rect.y + rect.height), false, factor);
            check.lineTo(last.getX(), last.getY());
            last = model.apply(result, last,
                    new EqPointDouble(rect.x, rect.y), false, factor);
            check.lineTo(last.getX(), last.getY());
            check.closePath();

            result.closePath();
            return result;
        }

        @Override
        protected void paintComponent(Graphics gg) {
            gg.setColor(Color.DARK_GRAY);
            gg.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g = (Graphics2D) gg;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Shape path = createShape();
            Rectangle r = new Area(path).getBounds();
            Obj<Shape> shape = Obj.of(path);
            if (r.width != getWidth() || r.height != getHeight()) {

                double workingWidth = getWidth() - (getWidth() / 4D);
                double workingHeight = getHeight() - (getHeight() / 4D);

                double scaleX = workingWidth / (double) r.width;
                double scaleY = workingHeight / (double) r.height;
                PooledTransform.withTranslateInstance(-r.x, -r.y, xf1 -> {
                    PooledTransform.withScaleInstance(scaleX, scaleY, xf2 -> {
                        shape.set(xf2.createTransformedShape(xf1.createTransformedShape(shape.get())));
                    });
                });
            }
            r = shape.get().getBounds();
            int diffX = (r.width - getWidth()) / 2;
            int diffY = (r.height - getHeight()) / 2;
            g.translate(-diffX, -diffY);

            g.setColor(new Color(80, 80, 255, 90));
            g.fill(shape.get());
            g.setColor(props.lineDraw());
            g.draw(shape.get());
            g.translate(diffX, diffY);
        }
    }

    public void withStartAndEnd(BiConsumer<EqPointDouble, EqPointDouble> c) {
        double sc = 1D / mini.getScale();
        EnhRectangle2D r = new EnhRectangle2D(0, 0, mini.getWidth() * sc, mini.getHeight() * sc);
        r.x = 11 * sc;
        r.y = 11 * sc;
        r.width -= 22 * sc;
        r.height -= 22 * sc;
        c.accept(new EqPointDouble(r.x, r.getCenterY()), new EqPointDouble(r.x + r.width, r.getCenterY()));
    }

    public void setModel(PathSegmentModel mdl) {
        if (!Objects.equals(model, mdl)) {
            PathSegmentModel old = model;
            model = mdl;
            mini.replaceInLookup(old, mdl);
            firePropertyChange("model", old, mdl);
        }
    }

    public PathSegmentModel getModel() {
        return model;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        PointEditTool tool = new PointEditTool(mini.getLookup().lookup(Surface.class
        ));
        mini.attach(tool);
    }

    @Override
    public void removeNotify() {
        if (tool != null) {
            tool.detach();
        }
        super.removeNotify();
        if (onHide != null) {
            onHide.accept(this.model);
        }
    }

    public GenericDesignCustomizer onHide(Consumer<PathSegmentModel> mdl) {
        this.onHide = onHide;
        return this;
    }

    private EqLine line() {
        EqLine result = new EqLine();
        withStartAndEnd(result::setLine);
        return result;
    }

    private static String p2d2string(Shape p) {
        StringBuilder sb = new StringBuilder(p.getClass().getSimpleName()).append("{");
        double[] d = new double[6];
        PathIterator it = p.getPathIterator(null);
        while (!it.isDone()) {
            int type = it.currentSegment(d);
            FlyweightPathElement el = PathElement.createFlyweight(type, d);
            sb.append("\n    ").append(el.kind()).append(' ').append(el);
            it.next();
        }
        return sb.append("\n}").toString();
    }

    public void paintRule(Graphics2D g, ToolUIContext ctx) {
        Rectangle2D.Float bds = new Rectangle2D.Float();
        bds.setFrame(mini.getBounds());
        bds.x = 0;
        bds.y = 0;

        Font f = getFont().deriveFont(ctx.zoom().getInverseTransform());
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();

        float lineWidth = ctx.zoom().inverseScale(5);
        float textOffset = ctx.zoom().inverseScale(7);

        EqLine line = new EqLine(0, bds.getCenterY(), lineWidth, bds.getCenterY());

        float textHeight = fm.getHeight();

        g.setColor(ImageEditorBackground.getDefault().style().contrasting());

        int value = 0;
        for (;;) {
            g.draw(line);
            float txtY = (float) (line.y1 - (textHeight / 2));
            g.drawString(Integer.toString(value), textOffset, txtY);
            value -= textHeight;
            line.y1 -= textHeight;
            line.y2 -= textHeight;
            if (line.y1 - textHeight < 0) {
                break;
            }
        }
        value = (int) textHeight;
        int h = getHeight();
//        for(;;) {
//            line.setLine(0, bds.getCenterY() + textHeight, lineWidth, bds.getCenterY() + textHeight);
//            float txtY = (float) (line.y1 - (textHeight / 2));
//            g.drawString(Integer.toString(value), textOffset, txtY);
//            value += textHeight;
//            line.y1 += textHeight;
//            line.y2 += textHeight;
//            if (line.y1 + textHeight > h) {
//                break;
//            }
//
//        }
    }

    public Dimension getPreferredSize() {
        Dimension result = super.getPreferredSize();
//        if (lastShape != null) {
//            Dimension shapeSize = lastShape.addToBounds().getSize();
//            result.width = Math.max(result.width, shapeSize.width);
//            result.height = Math.max(result.height, shapeSize.height);
//        }
        return result;
    }

    private Shape lastShape;

    @Override
    public void paint(Graphics2D g, ToolUIContext ctx) {
        paintRule(g, ctx);
        withStartAndEnd((start, end) -> {
            props = new PathUIProperties(() -> ctx);
            Path2D.Double path = new Path2D.Double();
            lastShape = path;
            model.apply(path, start, end, true, 1);
            g.setColor(props.lineDraw());
            g.setStroke(props.lineStroke());
            g.draw(path);
            g.setColor(props.connectorLineDraw());
            g.setStroke(props.connectorStroke());
            line.applyTransform(ctx.zoom().getInverseTransform());
            line.setLine(start, end);
            g.draw(line);
            circ.setRadius(props.pointRadius());
            paintStartPoint(start, PointKind.MOVE_DESTINATION_POINT, g);
//            paintOnePoint(end, PointKind.LINE_DESTINATION_POINT, g);
            model.visitPoints(this::line, (seg, index, kind, point) -> {
                if (!kind.isDestination()) {
                    g.setStroke(props.connectorStroke());
                    g.setColor(props.lineDraw());
                    line.setLine(point, seg.destination(line()));
                    g.draw(line);
                }
                paintOnePoint(point, kind, g);
            });
        });
    }

    private void paintOnePoint(Point2D point, PointKind kind, Graphics2D g) {
        circ.setCenter(point);
        circ.setRadius(props.pointRadius());
        if (kind.isDestination()) {
            g.setColor(new Color(128, 128, 255, 192));
        } else {
            if (kind.elementKind() == PathElementKind.QUADRATIC) {
                g.setColor(new Color(192, 255, 0, 192));
            } else {
                g.setColor(new Color(255, 192, 0, 192));
            }
        }
        g.fill(circ);
        g.setStroke(props.lineStroke());
        g.setColor(props.lineDraw());
        g.draw(circ);
    }

    private final Triangle2D scratchTriangle = new Triangle2D();

    private void paintStartPoint(Point2D point, PointKind kind, Graphics2D g) {
        circ.setCenter(point);
        circ.setRadius(props.pointRadius());
        circ.positionOf(90, (x1, y1) -> {
            circ.positionOf(180, (x2, y2) -> {
                circ.positionOf(0, (x3, y3) -> {
                    scratchTriangle.setPoints(x1, y1, x2, y2, x3, y3);
                    g.setColor(new Color(128, 128, 255, 192));
                    g.fill(scratchTriangle);
                    g.setStroke(props.lineStroke());
                    g.setColor(props.lineDraw());
                    g.draw(scratchTriangle);

                });
            });
        });
    }
}
