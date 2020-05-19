package org.netbeans.paint.tools.waffle;

import org.netbeans.paint.tools.spi.PathCreator;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JComponent;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.netbeans.paint.api.cursor.Cursors;
import org.netbeans.paint.api.components.explorer.FolderPanel;
import static org.netbeans.paint.api.components.explorer.FolderPanel.PROP_SELECTION;
import org.netbeans.paint.tools.minidesigner.GenericPathCreator;
import org.netbeans.paint.tools.responder.PathUIProperties;
import org.netbeans.paint.tools.responder.PaintingResponder;
import org.netbeans.paint.tools.responder.Responder;
import org.netbeans.paint.tools.responder.ResponderTool;
import static org.netbeans.paint.tools.spi.PathCreator.REGISTRATION_PATH;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Waffle", iconPath = "org/netbeans/paint/tools/resources/waffle2.svg",
        category = "vector")
@Tool(value = Surface.class, toolbarPosition = 2200)
@Messages("waffle=Waffle")
public class WaffleTool extends ResponderTool implements Supplier<PathUIProperties> {

    private List<EqPointDouble> points = new ArrayList<>(20);
    private PathUIProperties colors;
    private Shape lastShape;
    private int hashAtLastShape;
    private static final Customizer<Boolean> close = Customizers.getCustomizer(
            Boolean.class, "Close", true);
    private FolderPanel<PathCreator> folderPanel;

    private PathCreator creator;

    public WaffleTool(Surface obj) {
        super(obj);
    }

    static List<PathCreator> creators() {
        List<PathCreator> all = new ArrayList<>();
        all.add(new WaffleCreator());
        all.add(new GenericPathCreator());
        all.addAll(Lookup.getDefault().lookupAll(PathCreator.class));
        return all;
    }

    private FolderPanel<PathCreator> createFolderPanel() {
        FolderPanel<PathCreator> result = FolderPanel.create(REGISTRATION_PATH, PathCreator.class);
        creator = result.getSelection();
        if (creator == null) {
            setCreator(new WaffleCreator());
        }
        result.addPropertyChangeListener(PROP_SELECTION, listener);
        return result;
    }

    private void setCreator(PathCreator creator) {
        if (creator != this.creator) {
            this.creator = creator;
            creator.init(super.obj);
        }
    }

    private final PropertyChangeListener listener = evt -> {
        setCreator((PathCreator) evt.getNewValue());
    };

    private FolderPanel<PathCreator> folderPanel() {
        return folderPanel == null ? folderPanel = createFolderPanel()
                : folderPanel;
    }

    private PathCreator creator() {
        if (creator != null) {
            return creator;
        }
        PathCreator result = creator = folderPanel().getSelection();
        if (result == null) {
            result = creator = new WaffleCreator();
        }
        return result;
    }

    @Override
    public Customizer<?> getCustomizer() {
        FolderPanel<PathCreator> panel = folderPanel(); // initializes creator
        CU cu = new CU(panel);
        return new AggregateCustomizer(Bundle.waffle(),
                cu, close, fillC, paintC, outlineC, strokeC);
    }

    class CU implements Customizer<PathCreator> {

        private final FolderPanel<PathCreator> fp;

        public CU(FolderPanel<PathCreator> fp) {
            this.fp = fp;
        }

        @Override
        public JComponent getComponent() {
            return fp;
        }

        @Messages("designer=Path Segments")
        @Override
        public String getName() {
            return Bundle.designer();
        }

        @Override
        public PathCreator get() {
            return fp.getSelection();
        }

    }

    public PathUIProperties get() {
        if (colors == null) {
            return new PathUIProperties(this::ctx);
        }
        return colors;
    }

    @Override
    protected void onAttach() {
        colors = new PathUIProperties(this::ctx);
    }

    @Override
    protected void onDetach() {
        colors = null;
    }

    @Override
    protected void reset() {
        setCursor(Cursor.getDefaultCursor());
        points.clear();
        lastShape = null;
        hashAtLastShape = -1;
    }

    @Override
    protected Responder firstResponder() {
        return new FirstResponder();
    }

    @Override
    protected void onAttachRepainter(PaintParticipant.Repainter repainter) {
        boolean dark = ImageEditorBackground.getDefault().style().isDark();
        Cursors cursors = dark ? Cursors.forDarkBackgrounds() : Cursors.forBrightBackgrounds();
        repainter.setCursor(cursors.star());
    }

    @Override
    protected Rectangle paintCommit(Graphics2D g) {
        boolean finish = close.get();
        if (finish) {
            creator().finish(points);
        }
        Shape shape = currentShape(finish, true);
        if (shape == null) {
            return null;
        }
        PaintingStyle ps = fillC.get();
        if (ps.isFill()) {
            g.setPaint(paintC.get().getPaint());
            g.fill(shape);
        }
        BasicStroke stroke = ResponderTool.strokeC.get();
        if (ps.isOutline()) {
            g.setStroke(stroke);
            g.setPaint(ResponderTool.outlineC.get().getPaint());
            g.draw(shape);
        }

        EnhRectangle2D rect = new EnhRectangle2D();
        if (ps.isOutline()) {
            rect.add(shape, stroke);
        } else {
            rect.add(shape);
        }
        points.clear();
        return rect.getBounds();
    }

    @Override
    protected Rectangle paintLive(Graphics2D g, Rectangle layerBounds) {
        Shape shape = currentShape(false, false);
        if (shape == null) {
            return new Rectangle();
        }
        BasicStroke stroke = ResponderTool.strokeC.get();
        if (stroke != null) {
            g.setStroke(stroke);
        }
        Paint p = ResponderTool.paintC.get().getPaint();
        if (p == null) {
            p = Color.BLACK;
        }
        g.setPaint(p);
        g.draw(shape);
        EnhRectangle2D rect = new EnhRectangle2D();
        rect.add(shape, stroke);
        BasicStroke lineStroke = get().lineStroke();
        for (int i = 0; i < points.size(); i++) {
            configureCircle(points.get(i));
            if (layerBounds == null || layerBounds.contains(circ.getBounds())) {
                g.setStroke(lineStroke);
                g.setColor(i == 0 ? get().initialPointFill() : get().destinationPointFill());
                g.fill(circ);
                g.setColor(i == 0 ? get().destinationPointDraw() : get().destinationPointDraw());
                g.draw(circ);
                rect.add(circ, lineStroke);
            }
        }
        return rect.getBounds();
    }

    private final Circle circ = new Circle();

    private boolean addPoint(EqPointDouble pt) {
        EqPointDouble realAdd = creator().acceptPoint(pt, points);
        if (realAdd != null) {
            points.add(pt);
            return true;
        }
        return false;
    }

    private class FirstResponder extends Responder {

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            addPoint(new EqPointDouble(x, y));
            return new AddPointsResponder();
        }

        @Override
        protected void activate(Rectangle addTo) {
            boolean dark = ImageEditorBackground.getDefault().style().isDark();
            Cursors cursors = dark ? Cursors.forDarkBackgrounds() : Cursors.forBrightBackgrounds();
            setCursor(cursors.star());
        }

        @Override
        protected void resign(Rectangle addTo) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private EnhRectangle2D shapeBounds() {
        return shapeBounds(false);
    }

    private EnhRectangle2D shapeBounds(boolean includePointRadii) {
        Shape shape = currentShape(false, false);
        EnhRectangle2D result;
        if (shape == null) {
            result = new EnhRectangle2D();
            if (points.size() > 0) {
                for (EqPointDouble pt : points) {
                    repaintPoint(pt);
                }
            }
            return result;
        } else {
            result = EnhRectangle2D.of(shape.getBounds2D());
        }
        if (includePointRadii) {
            BasicStroke stroke = get().lineStroke();
            for (EqPointDouble p : points) {
                configureCircle(p);
                result.add(circ, stroke);
            }
        }
        return result;
    }

    private double circleRadius() {
        return get().pointRadius();
    }

    private void configureCircle(EqPointDouble pt) {
        configureCircle(pt.x, pt.y);
    }

    private void configureCircle(double x, double y) {
        circ.setCenterAndRadius(x, y, circleRadius());
    }

    private void repaintPoint(EqPointDouble pt) {
        repaintPoint(pt.x, pt.y);
    }

    private void addAllPoints(EnhRectangle2D to) {
        BasicStroke stroke = get().lineStroke();
        for (EqPointDouble pt : points) {
            configureCircle(pt);
            to.add(circ, stroke);
        }
    }

    private EqPointDouble targetPointWithinRadius(double x, double y) {
        double rad = get().pointRadius();
        EnhRectangle2D rect = new EnhRectangle2D(0, 0, rad, rad);
        rect.setCenter(x, y);
        for (EqPointDouble pt : points) {
            if (rect.contains(pt)) {
                return pt;
            }
        }
        return null;
    }

    abstract class AbstractWaffleToolResponder extends Responder {

        @Override
        protected Responder onKeyPress(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                EnhRectangle2D rect = new EnhRectangle2D();
                addAllPoints(rect);
                rect.add(shapeBounds());
                EqPointDouble hoverPoint = hoverPoint();
                if (hoverPoint != null) {
                    repaintPoint(hoverPoint);
                }
                repaint(rect);
                reset();
                return firstResponder();
            }
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (points.size() > 1) {
                    commit();
                    return new FirstResponder();
                }
            }
            return this;
        }

        protected abstract EqPointDouble hoverPoint();
    }

    private class AddPointsResponder extends AbstractWaffleToolResponder implements PaintingResponder {

        private final EqPointDouble hoverPoint = new EqPointDouble();
        private boolean hoverSet;

        @Override
        protected EqPointDouble hoverPoint() {
            return hoverSet ? hoverPoint : null;
        }

        private class MoveExistingPointResponder extends AbstractWaffleToolResponder {

            private final EqPointDouble targetPoint;
            private final double offX;
            private final double offY;
            private boolean hasDrag;

            MoveExistingPointResponder(EqPointDouble targetPoint, double initX, double initY) {
                this.targetPoint = targetPoint;
                offX = initX - targetPoint.x;
                offY = initY - targetPoint.y;
            }

            @Override
            protected EqPointDouble hoverPoint() {
                return new EqPointDouble(targetPoint);
            }

            @Override
            protected Responder onDrag(double x, double y, MouseEvent e) {
                EnhRectangle2D old = shapeBounds();
                addAllPoints(old);
                BasicStroke lineStroke = get().lineStroke();
                if (!hasDrag) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                hasDrag = true;
                targetPoint.x = x + offX;
                targetPoint.y = y + offY;
                old.add(shapeBounds());
                configureCircle(targetPoint.x, targetPoint.y);
                old.add(circ, lineStroke);
                repaint(old);
                return this;
            }

            @Override
            protected Responder onRelease(double x, double y, MouseEvent e) {
                if (hasDrag) {
                    onDrag(x, y, e);
                }
                return AddPointsResponder.this;
            }

            @Override
            protected void resign(Rectangle addTo) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
        }

        @Override
        protected Responder onPress(double x, double y, MouseEvent e) {
            EqPointDouble target = targetPointWithinRadius(x, y);
            if (target != null) {
                if (hoverSet) {
                    repaintPoint(hoverPoint);
                    Shape proposal = proposedAddition(hoverPoint);
                    if (proposal != null) {
                        repaint(proposal, get().proposedLineStroke());
                    }
                }
                return new MoveExistingPointResponder(target, x, y);
            }
            return this;
        }

        @Override
        protected void activate(Rectangle addTo) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        @Override
        protected void resign(Rectangle addTo) {
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            hoverPoint.setLocation(x, y);
            hoverSet = true;
            repaintPoint(hoverPoint);
            return this;
        }

        @Override
        protected Responder onEnter(double x, double y, MouseEvent e) {
            hoverSet = true;
            hoverPoint.setLocation(x, y);
            repaintPoint(hoverPoint);
            return this;
        }

        @Override
        protected Responder onExit(double x, double y, MouseEvent e) {
            if (hoverSet) {
                repaintPoint(hoverPoint.x, hoverPoint.y);
            }
            hoverSet = false;
            return this;
        }

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            if (hoverSet) {
                repaintPoint(hoverPoint);
            }
            EqPointDouble pt = new EqPointDouble(x, y);
            EqPointDouble match = targetPointWithinRadius(x, y);
            if (match == points.get(0)) {
                commit();
                return new FirstResponder();
            }
            if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
                commit();
                return new FirstResponder();
            }
            hoverSet = true;
            addPoint(pt);
            doRepaint(pt);
            return this;
        }

        private void doRepaint(EqPointDouble pt) {
            doRepaint(pt.x, pt.y);
        }

        private void doRepaint(double currX, double currY) {
            if (hoverSet) {
                repaintPoint(hoverPoint);
                Shape proposal = proposedAddition(hoverPoint);
                if (proposal != null) {
                    repaint(proposal, get().lineStroke());
                }
                repaint(shapeBounds());
                repaintPoint(currX, currY);
            }
            hoverPoint.setLocation(currX, currY);
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            switch (points.size()) {
                case 0:
                    return new Rectangle();
                case 1:
                    EqPointDouble single = points.iterator().next();
                    repaintPoint(single);
                    if (hoverSet) {
                        repaintPoint(hoverPoint);
                    }
                    return circ.getBounds();
                default:
                    if (hoverSet) {
                        EqPointDouble first = points.get(0);

                        EqPointDouble last = points.get(points.size() - 1);
                        Shape p2d = proposedAddition(hoverPoint);
                        BasicStroke stroke = get().proposedLineStroke();
                        g.setStroke(stroke);
                        g.setColor(get().proposedLineDraw());
                        g.draw(p2d);
                        EqLine ln = new EqLine(last, hoverPoint);
                        ln.shiftPerpendicular(ctx().zoom().inverseScale(stroke.getLineWidth()));
                        g.translate(last.x - ln.x1, last.y - ln.y1);
                        g.setColor(get().proposedLineShadow());
                        g.draw(p2d);
                        g.translate(-(last.x - ln.x1), -(last.y - ln.y1));
                        EnhRectangle2D paintedBounds = new EnhRectangle2D();
                        paintedBounds.add(p2d, stroke);
                        configureCircle(hoverPoint);
                        EqPointDouble target = targetPointWithinRadius(hoverPoint.x, hoverPoint.y);
                        if (target == first) {
                            g.setColor(get().hoveredDotFill());
                        } else {
                            g.setColor(get().destinationPointFill());
                        }
                        g.fill(circ);
                        if (target == first) {
                            g.setColor(get().hoveredDotDraw());
                        } else {
                            g.setColor(get().destinationPointDraw());
                        }
                        g.draw(circ);
                        paintedBounds.add(circ, stroke);
                        return paintedBounds.getBounds();
                    }

            }
            return new Rectangle();
        }
    }

    private Shape proposedAddition(EqPointDouble hoverPoint) {
        return creator().proposedAddition(points, hoverPoint);
    }

    private Shape currentShape(boolean close, boolean commit) {

        if (points.size() < 2) {
            return null;
        }
        int hash = points.hashCode();
        if (close) {
            hash *= 7;
        }
        if (lastShape != null) {
            if (hash == hashAtLastShape) {
                return lastShape;
            }
        }
        Shape result = creator().create(close, true, points, null);
        hashAtLastShape = result == null ? -1 : hash;

        return lastShape = result;
    }
}
