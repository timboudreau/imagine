package org.netbeans.paint.tools;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.image.ToolCommitPreference;
import org.imagine.editor.api.PaintingStyle;
import org.netbeans.paint.api.components.explorer.SelectAndCustomizePanel;
import org.netbeans.paint.tools.fills.AddFillPanel;
import org.netbeans.paint.tools.spi.Brush;
import org.netbeans.paint.tools.spi.MouseDrivenTool;
import org.openide.util.NbBundle;

@ToolDef(name = "Brush", iconPath = "org/netbeans/paint/tools/resources/brush.png")
@Tool(Surface.class)
public final class BrushTool extends MouseDrivenTool implements CustomizerProvider, Customizer, PaintParticipant {

    private boolean emit;

    public BrushTool(Surface surface) {
        super(surface);
        emit = surface.toolCommitPreference() == ToolCommitPreference.COLLECT_GEOMETRY;
    }

    @Override
    public JComponent createCustomizer() {
        return getComponent();
    }

    private Brush getBrush() {
        return (Brush) sel.getSelection();
    }

    @Override
    public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
        if (commit && inCommit) {
            try {
                doPaint(g2d, lastPoint, lastModifiers);
            } finally {
                inCommit = false;
            }
        } else if (emit && coll != null) {
            coll.interimPaint(g2d);
        }
    }

    boolean inCommit;
    private Point lastPoint;
    private int lastModifiers;

    protected void dragged(java.awt.Point p, int modifiers) {
        if (!isActive()) {
            return;
        }
        if (emit && coll != null) {
            Brush b = getBrush();
            b.emit(p, (Shape shape, Paint paint, PaintingStyle style) -> {
                coll.add(paint, shape, style.isFill());
            });
            return;
        }
        if (!inCommit) {
            inCommit = true;
            lastPoint = p;
            lastModifiers = modifiers;
            repainter.requestCommit();
//            return;
        }
        doPaint(surface.getGraphics(), p, modifiers);
    }

    private static final class GeometryCollection {

        private Paint paint;
        private final List<Shape> shapes = new ArrayList<>(1024);
        private boolean fill;
        private final BasicStroke stroke = new BasicStroke(1);

        void add(Paint p, Shape s, boolean fill) {
            if (!fill) {
                s = stroke.createStrokedShape(s);
            }
            paint = p;
            this.fill = fill;
            shapes.add(s);
        }

        private Shape coalesce() {
            return new AggregateShape(shapes);
//            Area area = new Area();
//            for (Shape s : shapes) {
//                if (!fill) {
//                    s = stroke.createStrokedShape(s);
//                }
//                area.add(new Area(s));
//            }
//            return area;
        }

        void interimPaint(Graphics2D g) {
            g.setPaint(paint);
            g.setStroke(stroke);
            for (Shape s : shapes) {
                g.fill(s);
//                if (fill) {
//                    g.fill(s);
//                } else {
//                    g.draw(s);
//                }
            }
        }

        void commit(Graphics2D g) {
            Shape s = coalesce();
//            if (fill) {
            g.setPaint(paint);
            g.fill(s);
//            } else {
//                g.setColor(new Color(0,0,0,0));
//                g.fill(s);
//                g.setPaint(paint);
//                g.draw(s);
//            }
        }
    }

    GeometryCollection coll;

    @Override
    protected void beginOperation(Point p, int modifiers) {
        if (emit) {
            coll = new GeometryCollection();
        }
    }

    @Override
    protected void endOperation(Point p, int modifiers) {
        if (emit && coll != null) {
            coll.commit(surface.getGraphics());
        }
        coll = null;
    }

    private void doPaint(Graphics2D g, Point p, int modifiers) {
        if (emit && coll != null) {
            coll.commit(g);
            return;
        }
        Brush brush = getBrush();

        if (brush != null) {
            Rectangle painted = brush.paint(surface.getGraphics(), p, modifiers);
            repainter.requestRepaint(painted);
        }
    }

    @Override
    public Customizer getCustomizer() {
        return this;
    }

    SelectAndCustomizePanel sel = new SelectAndCustomizePanel("brushes", true); //NOI18N

    @Override
    public JComponent getComponent() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl.add(sel, gbc);
        gbc.gridy++;
        pnl.add(new AddFillPanel(), gbc);
        return pnl;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(BrushTool.class, "Brush");
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Object get() {
        return null;
    }
}
