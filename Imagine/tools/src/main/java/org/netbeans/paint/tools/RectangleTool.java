/*
 * RectangleTool.java
 *
 * Created on September 28, 2006, 6:18 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

import java.awt.BasicStroke;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import static org.netbeans.paint.tools.MutableRectangle.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.ScalingMouseListener;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import org.imagine.editor.api.snap.SnapPoints;
import org.imagine.editor.api.snap.SnapPointsConsumer;
import org.imagine.editor.api.snap.SnapPointsSupplier;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.netbeans.paint.tools.fills.FillCustomizer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

import static net.java.dev.imagine.api.toolcustomizers.Constants.*;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.grid.SnapSettings;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqPointDouble;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Rectangle", iconPath = "org/netbeans/paint/tools/resources/rect.svg")
@Tool(value=Surface.class, toolbarPosition=100)
public class RectangleTool implements PaintParticipant, ScalingMouseListener, KeyListener, CustomizerProvider, Attachable, SnapPointsConsumer, ChangeListener {

    private EnhRectangle2D lastPaintedRectangle = new EnhRectangle2D();
    private static final int NO_ANCHOR_SIZE = 18;
    private double anchorSize = NO_ANCHOR_SIZE;
    protected EnhRectangle2D TEMPLATE_RECTANGLE = new EnhRectangle2D(0, 0, NO_ANCHOR_SIZE, NO_ANCHOR_SIZE);

    protected MutableRectangle2D rect;
    private int draggingCorner;
    protected final Surface surface;
    private Repainter repainter;
    private SnapPointsSupplier snapPoints;

    public RectangleTool(Surface surface) {
        this.surface = surface;
//        new Exception("Create "  + System.identityHashCode(this) ).printStackTrace(); //XXX
    }

    public EnhRectangle2D getRectangle() {
        return rect == null ? null : new EnhRectangle2D(rect);
    }

    private void setDraggingCorner(int draggingCorner) {
        this.draggingCorner = draggingCorner;
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getModifiersEx() == 0) {
            clear();
        }
    }

    private void clear() {
        rect = null;
        draggingCorner = ANY;
        armed = false;
        if (repainter != null) {
            repainter.requestRepaint(null);
        }
    }

    public void paint(Graphics2D g2d) {
        EnhRectangle2D toPaint = rect == null ? TEMPLATE_RECTANGLE : rect;
        lastPaintedRectangle.setFrame(toPaint);
        if (armed || committing) {
            PaintingStyle xfill = fillC.get();
            draw(toPaint, g2d, xfill);
        }
    }

    protected void draw(EnhRectangle2D toPaint, Graphics2D g2d, PaintingStyle style) {
        if (style.isFill()) {
            g2d.setPaint(paintC.get().getPaint());
            g2d.fill(toPaint);
        }
        if (style.isOutline()) {
            if (toPaint == TEMPLATE_RECTANGLE) {
                // At high zoom, the stroke can be bigger than the scaled template
                // object
                g2d.setStroke(scaledStroke());
            } else {
                g2d.setStroke(strokeC.get());
            }
            g2d.setColor(outlineC.get());
            g2d.draw(toPaint);
        }
    }

    protected BasicStroke scaledStroke() {
        BasicStroke stroke = strokeC.get();
        if (zoom != null) {
            stroke = zoom.inverseScaleStroke(stroke);
        }
        return stroke;
    }

    @Override
    public void mouseDragged(double x, double y, MouseEvent e) {
        mouseMoved(x, y, e);
    }

    @Override
    public void mouseMoved(double x, double y, MouseEvent e) {
        armed = true;
//        Point2D p = snapPoint(new EqPointDouble(x, y), e);
        Point2D p = snapPoint(new EqPointDouble(x, y), e);
        TEMPLATE_RECTANGLE.setLocation(p);
        if (rect != null) {
            dragged(p, e.getModifiersEx());
            repaintWithRect();
        } else {
            repaintNoRect(p);
        }
    }

    private void dragged(Point2D p, int modifiers) {
        int currCorner = draggingCorner;
        int corner = rect.setPoint(p, currCorner);

        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            rect.makeSquare(currCorner);
        }
        if (corner == MutableRectangle2D.NONE || (corner != currCorner && corner != -1)) {
            if (corner != -2) {
                setDraggingCorner(corner);
            }
        }
    }

    private void repaintNoRect(Point2D p) {
        if (repainter != null) {
            repainter.requestRepaint(null);
        }
    }

    private void repaintWithRect() {
        if (repainter != null) {
            repainter.requestRepaint(null);
        }
    }

    @Override
    public void mouseClicked(double x, double y, MouseEvent e) {
        mouseDragged(x, y, e);
        if (e.getClickCount() >= 2) {
            commit();
        }
    }

    private Point2D snapPoint(Point2D orig, MouseEvent e) {
        if (!SnapSettings.getGlobal().isEnabled()) {
            return orig;
        }
        if (snapPoints == null || e.isControlDown()) {
            return orig;
        }
        SnapPoints pts = snapPoints.get();
        if (pts == null) {
            return orig;
        }
        Point2D result = pts.snap(orig);
        return result;
    }

    @Override
    public void mousePressed(double x, double y, MouseEvent e) {
        EqPointDouble p = new EqPointDouble(x, y);
        TEMPLATE_RECTANGLE.setLocation(p);
        if (rect == null) {
            rect = new MutableRectangle2D(x, y, x + 1, y + 1);
            draggingCorner = rect.nearestCorner(p);
        }
    }

    @Override
    public void mouseReleased(double x, double y, MouseEvent e) {
        Point2D p = new EqPointDouble(x, y);
//        boolean inBounds = c.contains(p);
//        if (rect != null && inBounds) {
        if (rect != null) {
            int nearestCorner = rect.nearestCorner(p);

            if (p.distance(rect.getLocation()) < rect.diagonalLength() / 4) {
                p = snapPoint(p, e);
                setDraggingCorner(nearestCorner);
                rect.setLocation(p);
            } else {
                setDraggingCorner(nearestCorner);
                if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0) {
                    rect.makeSquare(nearestCorner);
                }
                p = snapPoint(p, e);
                rect.setPoint(p, nearestCorner);
                armed = false;
                commit();
                clear();
            }
        }
    }

    boolean committing = false;

    private void commit() {
        repainter.requestCommit();
    }

    public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
        committing = commit;
        try {
            paint(g2d);
        } finally {
            committing = false;
            g2d.dispose();
        }
    }

    boolean armed;

    @Override
    public void mouseExited(double x, double y, MouseEvent e) {
        armed = false;
        repaintNoRect(e.getPoint());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (rect == null) {
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                rect.y++;
                break;
            case KeyEvent.VK_UP:
                rect.y--;
                break;
            case KeyEvent.VK_LEFT:
                rect.x--;
                break;
            case KeyEvent.VK_RIGHT:
                rect.x++;
                break;
            case KeyEvent.VK_ENTER:
                commit();
                break;
            case KeyEvent.VK_ESCAPE:
                rect = null;
                repaintNoRect(null);
                armed = false;
                committing = false;
                break;
        }
    }

    public String getInstructions() {
        return NbBundle.getMessage(getClass(), "Click_and_move_mouse_or_click-and-drag_to_draw");
    }

    public void detach() {
        rect = null;
        TEMPLATE_RECTANGLE.setFrame(0, 0, anchorSize, anchorSize);
        committing = false;
        armed = false;
        snapPoints = null;
        if (zoom != null) {
            zoom.removeChangeListener(this);
            zoom = null;
        }
    }

    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    public Lookup getLookup() {
        return Lookups.singleton(this);
    }

    public void attachRepainter(PaintParticipant.Repainter repainter) {
        this.repainter = repainter;
    }

    //XXX these should not be static, but the old code assumes we keep a tool
    //instance for the life of the application
    protected static final FillCustomizer paintC = FillCustomizer.getDefault();
    protected static final Customizer<Color> outlineC = Customizers.getCustomizer(Color.class, FOREGROUND);
    protected static final Customizer<PaintingStyle> fillC = Customizers.getCustomizer(PaintingStyle.class, FILL);
    protected static final Customizer<BasicStroke> strokeC = Customizers.getCustomizer(BasicStroke.class, "Stroke", null);

    public Customizer getCustomizer() {
        return new AggregateCustomizer("foo", fillC, outlineC, strokeC, paintC);
    }

    @Override
    public void accept(SnapPointsSupplier t) {
        this.snapPoints = t;
    }

    private Zoom zoom;

    @Override
    public void attach(Lookup.Provider on, ToolUIContext ctx) {
        zoom = ctx.zoom();
        zoom.addChangeListener(this);
        stateChanged(null);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (zoom != null) {
            anchorSize = zoom.inverseScale(NO_ANCHOR_SIZE);
            Rectangle toRepaint = TEMPLATE_RECTANGLE.getBounds();
            TEMPLATE_RECTANGLE.width = anchorSize;
            TEMPLATE_RECTANGLE.height = anchorSize;
            if (repainter != null) {
                // Ensure we cover the entire bounds that needs repainting
                toRepaint.add(TEMPLATE_RECTANGLE);
                repainter.requestRepaint(toRepaint);
            }
        }
    }
}
