/*
 * RectangleTool.java
 *
 * Created on September 28, 2006, 6:18 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.netbeans.paint.tools;

import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.spi.tool.Tool;
import java.lang.Float;
import net.dev.java.imagine.spi.tool.ToolDef;
import static org.netbeans.paint.tools.MutableRectangle.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import net.java.dev.imagine.api.image.Layer;

import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.netbeans.paint.tools.fills.FillCustomizer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

import static net.java.dev.imagine.api.toolcustomizers.Constants.*;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name="Rectangle", iconPath="org/netbeans/paint/tools/resources/rect.png")
@Tool(Surface.class)
public class RectangleTool implements PaintParticipant, MouseMotionListener, MouseListener, KeyListener, CustomizerProvider, Attachable {
    MutableRectangle rect;
    private int draggingCorner;
    protected final Surface surface;

    public RectangleTool (Surface surface) {
        this.surface = surface;
//        new Exception("Create "  + System.identityHashCode(this) ).printStackTrace(); //XXX
    }

    public Rectangle getRectangle() {
        return rect == null ? null : new Rectangle (rect);
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
        lastPaintedRectangle.setBounds (toPaint);
        if (armed || committing) {
            g2d.setStroke(new BasicStroke (this.strokeC.get()));
            Boolean xfill = fillC.get();
            boolean fill = xfill == null ? false : xfill.booleanValue();
            draw (toPaint, g2d, fill);
        }
    }

    protected void draw (Rectangle toPaint, Graphics2D g2d, boolean fill) {
        g2d.setStroke(new BasicStroke(strokeC.get()));
        if (fill) {
            Paint paint = paintC.get().getPaint();
            g2d.setPaint (paint);
            g2d.fillRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
            g2d.setColor(this.outlineC.get());
            g2d.drawRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
        } else {
            g2d.setColor(this.outlineC.get());
            g2d.drawRect(toPaint.x, toPaint.y, toPaint.width, toPaint.height);
        }
    }

    public void mouseDragged(MouseEvent e) {
       mouseMoved (e);
    }

    public void mouseMoved(MouseEvent e) {
        armed = true;
        Point p = e.getPoint();
        TEMPLATE_RECTANGLE.setLocation(p);
        if (rect != null) {
            dragged (e.getPoint(), e.getModifiersEx());
            repaintWithRect();
        } else {
            repaintNoRect(p);
        }
    }

    private void dragged (Point p, int modifiers) {
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

    private Rectangle lastPaintedRectangle = new Rectangle();

    int NO_ANCHOR_SIZE = 18;
    private Rectangle TEMPLATE_RECTANGLE = new Rectangle (0, 0, NO_ANCHOR_SIZE, NO_ANCHOR_SIZE);
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

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        TEMPLATE_RECTANGLE.setLocation(p);
        if (rect == null) {
            p.x ++;
            p.y ++;
            rect = new MutableRectangle (e.getPoint(), p);
            draggingCorner = rect.nearestCorner(p);
        }
    }

    private static final int CLICK_DIST = 7;
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
//        boolean inBounds = c.contains(p);
//        if (rect != null && inBounds) {
        if (rect != null) {
            int nearestCorner = rect.nearestCorner(p);
            if (p.distance(rect.getLocation()) < CLICK_DIST) {
                setDraggingCorner (nearestCorner);
                rect.setLocation(p);
            } else {
                setDraggingCorner(nearestCorner);
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
        surface.beginUndoableOperation(null);
        try {
            paint (g2d);
        } finally {
            surface.endUndoableOperation();
            committing = false;
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    boolean armed;
    public void mouseExited(MouseEvent e) {
        armed = false;
        repaintNoRect(e.getPoint());
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (rect == null) {
            return;
        }
        Point p = rect.getLocation();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN :
                p.y ++;
                break;
            case KeyEvent.VK_UP :
                p.y--;
                break;
            case KeyEvent.VK_LEFT :
                p.x --;
                break;
            case KeyEvent.VK_RIGHT :
                p.x ++;
                break;
            case KeyEvent.VK_ENTER :
                commit();
                break;
        }
    }

    public String getInstructions() {
        return NbBundle.getMessage (getClass(), "Click_and_move_mouse_or_click-and-drag_to_draw");
    }

    public void attach(Lookup.Provider layer) {
//        new Exception("Detach "  + System.identityHashCode(this) ).printStackTrace(); //XXX
    }

    public void detach() {
        rect = null;
        TEMPLATE_RECTANGLE.setBounds (0, 0, NO_ANCHOR_SIZE, NO_ANCHOR_SIZE);
        committing = false;
        armed = false;
//        new Exception("Detach "  + System.identityHashCode(this) ).printStackTrace(); //XXX
    }

    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    public Lookup getLookup() {
        return Lookups.singleton (this);
    }

    private Repainter repainter;
    public void attachRepainter(PaintParticipant.Repainter repainter) {
        this.repainter = repainter;
//        new Exception("Attach repainter to "  + System.identityHashCode(this) + " " + repainter).printStackTrace(); //XXX
    }
    
    //XXX these should not be static, but the old code assumes we keep a tool
    //instance for the life of the application
    protected static final FillCustomizer paintC = FillCustomizer.getDefault();
    protected static final Customizer<Color> outlineC = Customizers.getCustomizer(Color.class, FOREGROUND);
    protected static final Customizer<Boolean> fillC = Customizers.getCustomizer(Boolean.class, FILL);
    protected static final Customizer<Float> strokeC = Customizers.getCustomizer(Float.class, STROKE);

    public Customizer getCustomizer() {
        return new AggregateCustomizer ("foo", fillC, outlineC, strokeC, paintC);
    }
}
