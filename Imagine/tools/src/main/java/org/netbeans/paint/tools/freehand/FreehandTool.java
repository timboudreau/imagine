/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.freehand;

import com.pump.geom.BasicMouseSmoothing;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.editor.api.PaintingStyle;
import org.netbeans.paint.tools.responder.PaintingResponder;
import org.netbeans.paint.tools.responder.Responder;
import org.netbeans.paint.tools.responder.ResponderTool;
import org.netbeans.paint.tools.spi.Fill;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Freehand", iconPath = "org/netbeans/paint/tools/resources/freehand.svg",
        category = "vector")
@Tool(value=Surface.class, toolbarPosition=2100)
public class FreehandTool extends ResponderTool {

    private BasicMouseSmoothing smoothing;
    private int pointCount;
    private static final Customizer<Float> ERROR_TOLERANCE
            = Customizers.getCustomizer(Float.class, NbBundle.getMessage(FreehandTool.class,
                    "ERROR_TOLERANCE"), 0F, 1F, 1F / 3F);
    private static final Customizer<Float> STRENGTH
            = Customizers.getCustomizer(Float.class, NbBundle.getMessage(FreehandTool.class, 
                    "STRENGTH"), 0F, 1F, 0.2F);
    private static final Customizer<Boolean> CLOSED
            = Customizers.getCustomizer(Boolean.class, NbBundle.getMessage(FreehandTool.class,
                    "CLOSED"), true);

    public FreehandTool(Surface obj) {
        super(obj);
    }

    @Override
    public Customizer<?> getCustomizer() {
        return new AggregateCustomizer<Void>(getClass().getSimpleName(),
                STRENGTH, ERROR_TOLERANCE, CLOSED,
                fillC, paintC, outlineC, strokeC);
    }

    @Override
    protected void onAttachRepainter(PaintParticipant.Repainter repainter) {
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    @Override
    protected void reset() {
        setCursor(Cursor.getDefaultCursor());
        smoothing = null;
        pointCount = 0;
    }

    @Override
    protected void onDetach() {
        reset();
    }

    @Override
    protected Responder firstResponder() {
        reset();
        return new MouseSmoothingResponder(smoothing = new BasicMouseSmoothing(
            ERROR_TOLERANCE.get(), STRENGTH.get()));
    }

    @Override
    protected Rectangle paintCommit(Graphics2D g) {
        if (smoothing != null && pointCount > 0 && !smoothing.isEmpty()) {
            GeneralPath shape = smoothing.getShape();
            if (CLOSED.get()) {
                shape.closePath();
            }
            if (shape != null) {
                PaintingStyle style = ResponderTool.fillC.get();
                Fill fill = ResponderTool.paintC.get();
                Color outline = ResponderTool.outlineC.get();
                if (style.isFill()) {
                    g.setPaint(fill.getPaint());
                    g.fill(shape);
                }
                if (style.isOutline()) {
                    g.setStroke(ResponderTool.strokeC.get());
                    g.setPaint(outline);
                    g.draw(shape);
                }
                return shape.getBounds();
            }
        }
        return new Rectangle();
    }

    @Override
    protected Rectangle paintLive(Graphics2D g, Rectangle layerBounds) {
        return paintCommit(g);
    }

    class MouseSmoothingResponder extends Responder implements PaintingResponder {

        private final BasicMouseSmoothing smoothing;

        MouseSmoothingResponder(BasicMouseSmoothing smoothing) {
            this.smoothing = smoothing;
        }

        private void addPoints(double x, double y, MouseEvent e) {
            smoothing.add((float) x, (float) y, e.getWhen());
            pointCount++;
            if (pointCount > 2) {
                FreehandTool.this.repaint(smoothing.getShape(), strokeC.get());
            }
        }

        private boolean hasPoints() {
            return pointCount >= 2;
        }

        @Override
        protected Responder onDrag(double x, double y, MouseEvent e) {
            addPoints(x, y, e);
            return this;
        }

        @Override
        protected Responder onKeyRelease(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                return firstResponder();
            }
            return this;
        }

        @Override
        protected Responder onRelease(double x, double y, MouseEvent e) {
            if (hasPoints()) {
                FreehandTool.this.commit();
            }
            return firstResponder();
        }

        @Override
        protected Responder onPress(double x, double y, MouseEvent e) {
            addPoints(x, y, e);
            return this;
        }

        @Override
        protected void resign(Rectangle addTo) {
            if (hasPoints()) {
                Shape s = smoothing.getShape();
                if (s != null) {
                    addTo.add(s.getBounds());
                }
            }
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            if (hasPoints()) {
                Shape shape = smoothing.getShape();
                if (shape != null) {
                    PaintingStyle style = ResponderTool.fillC.get();
                    Fill fill = ResponderTool.paintC.get();
                    Color outline = ResponderTool.outlineC.get();
                    if (style.isFill()) {
                        g.setPaint(fill.getPaint());
                        g.fill(shape);
                    }
                    if (style.isOutline()) {
                        g.setStroke(ResponderTool.strokeC.get());
                        g.setPaint(outline);
                        g.draw(shape);
                    }
                    return shape.getBounds();
                }
            }
            return new Rectangle();
        }
    }
}
