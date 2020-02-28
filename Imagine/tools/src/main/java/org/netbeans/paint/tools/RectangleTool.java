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
import java.lang.Float;
import net.dev.java.imagine.spi.tool.ToolDef;
import static org.netbeans.paint.tools.MutableRectangle.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.function.Supplier;

import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoints;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPointsConsumer;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.netbeans.paint.tools.fills.FillCustomizer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

import static net.java.dev.imagine.api.toolcustomizers.Constants.*;
import org.imagine.editor.api.PaintingStyle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Rectangle", iconPath = "org/netbeans/paint/tools/resources/rect.png")
@Tool(Surface.class)
public class RectangleTool implements PaintParticipant, MouseMotionListener, MouseListener, KeyListener, CustomizerProvider, Attachable, SnapPointsConsumer {

    private Rectangle lastPaintedRectangle = new Rectangle();
    int NO_ANCHOR_SIZE = 18;
    private Rectangle TEMPLATE_RECTANGLE = new Rectangle(0, 0, NO_ANCHOR_SIZE, NO_ANCHOR_SIZE);

    MutableRectangle rect;
    private int draggingCorner;
    protected final Surface surface;
    private Repainter repainter;
    private Supplier<SnapPoints> snapPoints;

    public RectangleTool(Surface surface) {
        this.surface = surface;
//        new Exception("Create "  + System.identityHashCode(this) ).printStackTrace(); //XXX
    }

    public Rectangle getRectangle() {
        return rect == null ? null : new Rectangle(rect);
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
        Rectangle toPaint = rect == null ? TEMPLATE_RECTANGLE : rect;
        lastPaintedRectangle.setBounds(toPaint);
        if (armed || committing) {
            PaintingStyle xfill = fillC.get();
            draw(toPaint, g2d, xfill);
        }
    }

    protected void draw(Rectangle toPaint, Graphics2D g2d, PaintingStyle style) {
        if (style.isFill()) {
            g2d.setPaint (paintC.get().getPaint());
            g2d.fillRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
        }
        if (style.isOutline()) {
            g2d.setStroke(new BasicStroke(strokeC.get()));
            g2d.setColor (outlineC.get());
            g2d.drawRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
        }
//        if (style.isFill()) {
//            Paint paint = paintC.get().getPaint();
//            g2d.setPaint(paint);
//            g2d.fill(toPaint);
//        }
//        if (style.isOutline()) {
//            g2d.setStroke(new BasicStroke(strokeC.get()));
//            g2d.setPaint(this.outlineC.get());
//            g2d.draw(toPaint);
//        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        armed = true;
        Point p = snapPoint(e.getPoint(), e);
        TEMPLATE_RECTANGLE.setLocation(p);
        if (rect != null) {
            dragged(e.getPoint(), e.getModifiersEx());
            repaintWithRect();
        } else {
            repaintNoRect(p);
        }
    }

    private void dragged(Point p, int modifiers) {
        int currCorner = draggingCorner;
        int corner = rect.setPoint(p, currCorner);

        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            rect.makeSquare(currCorner);
        }
        if (corner == -2 || (corner != currCorner && corner != -1)) {
            if (corner != -2) {
                setDraggingCorner(corner);
            }
        }
    }

    private void repaintNoRect(Point p) {
//        Rectangle old = new Rectangle (TEMPLATE_RECTANGLE);
//        TEMPLATE_RECTANGLE.setLocation(p);
//        Rectangle repaintRect = old.union(TEMPLATE_RECTANGLE);
//        c.repaint (repaintRect.x, repaintRect.y,  repaintRect.width, repaintRect.height);
        if (repainter != null) {
            repainter.requestRepaint(null);
        }
    }

    private void repaintWithRect() {
//        Rectangle repaintRect = lastPaintedRectangle.union(rect);
//        c.repaint (repaintRect.x, repaintRect.y,  repaintRect.width, repaintRect.height);
        if (repainter != null) {
            repainter.requestRepaint(null);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    private Point snapPoint(Point orig, MouseEvent e) {
        if (snapPoints == null || e.isControlDown()) {
            return orig;
        }
        SnapPoints pts = snapPoints.get();
        if (pts == null) {
            return orig;
        }
        Point2D result = pts.snap(orig);
        if (result != orig) {
//            System.out.println("SNAP " + orig.x + ", " + orig.y
//                    + " to " + result.getX() + "," + result.getY());
        }
        return result instanceof Point ? (Point) result
                : new Point((int) result.getX(), (int) result.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        TEMPLATE_RECTANGLE.setLocation(p);
        if (rect == null) {
            p.x++;
            p.y++;
            rect = new MutableRectangle(snapPoint(e.getPoint(), e), p);
            draggingCorner = rect.nearestCorner(p);
        }
    }

    private static final int CLICK_DIST = 7;

    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
//        boolean inBounds = c.contains(p);
//        if (rect != null && inBounds) {
        if (rect != null) {
            int nearestCorner = rect.nearestCorner(p);
            if (p.distance(rect.getLocation()) < CLICK_DIST) {
                p = snapPoint(p, e);
                setDraggingCorner(nearestCorner);
                rect.setLocation(p);
            } else {
                setDraggingCorner(nearestCorner);
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

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    boolean armed;

    @Override
    public void mouseExited(MouseEvent e) {
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
        Point p = rect.getLocation();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                p.y++;
                break;
            case KeyEvent.VK_UP:
                p.y--;
                break;
            case KeyEvent.VK_LEFT:
                p.x--;
                break;
            case KeyEvent.VK_RIGHT:
                p.x++;
                break;
            case KeyEvent.VK_ENTER:
                commit();
                break;
        }
    }

    public String getInstructions() {
        return NbBundle.getMessage(getClass(), "Click_and_move_mouse_or_click-and-drag_to_draw");
    }

    public void attach(Lookup.Provider layer) {
//        new Exception("Detach "  + System.identityHashCode(this) ).printStackTrace(); //XXX
    }

    public void detach() {
        rect = null;
        TEMPLATE_RECTANGLE.setBounds(0, 0, NO_ANCHOR_SIZE, NO_ANCHOR_SIZE);
        committing = false;
        armed = false;
        snapPoints = null;
//        new Exception("Detach "  + System.identityHashCode(this) ).printStackTrace(); //XXX
    }

    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    public Lookup getLookup() {
        return Lookups.singleton(this);
    }

    public void attachRepainter(PaintParticipant.Repainter repainter) {
        this.repainter = repainter;
//        new Exception("Attach repainter to "  + System.identityHashCode(this) + " " + repainter).printStackTrace(); //XXX
    }

    //XXX these should not be static, but the old code assumes we keep a tool
    //instance for the life of the application
    protected static final FillCustomizer paintC = FillCustomizer.getDefault();
    protected static final Customizer<Color> outlineC = Customizers.getCustomizer(Color.class, FOREGROUND);
    protected static final Customizer<PaintingStyle> fillC = Customizers.getCustomizer(PaintingStyle.class, FILL);
    protected static final Customizer<Float> strokeC = Customizers.getCustomizer(Float.class, STROKE, 0.05F, 10F);

    public Customizer getCustomizer() {
        return new AggregateCustomizer("foo", fillC, outlineC, strokeC, paintC);
    }

    @Override
    public void accept(Supplier<SnapPoints> t) {
        this.snapPoints = t;
    }
}
