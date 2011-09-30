/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.tools.selectiontools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import net.dev.java.imagine.api.selection.Selection;
import net.dev.java.imagine.api.selection.Selection.Op;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.image.Layer;
import org.netbeans.paint.api.splines.Close;
import org.netbeans.paint.api.splines.CurveTo;
import org.netbeans.paint.api.splines.DefaultPathModel;
import org.netbeans.paint.api.splines.Entry;
import org.netbeans.paint.api.splines.Hit;
import org.netbeans.paint.api.splines.LineTo;
import org.netbeans.paint.api.splines.MoveTo;
import org.netbeans.paint.api.splines.Node;
import org.netbeans.paint.api.splines.PathModel;
import org.netbeans.paint.api.splines.QuadTo;
import org.openide.util.Lookup;

/**
 * A tool for adjusting control points on a selection
 *
 * @author Tim Boudreau
 */
@ToolDef(name="Lasso", iconPath="org/netbeans/paint/tools/resources/lasso.png", category="selection")
@Tool(Selection.class)
public class LassoTool extends MouseAdapter implements /* Tool,*/ PaintParticipant, KeyListener, MouseMotionListener, Attachable {
    private Repainter repainter;
    private Layer layer;
    private PathModel<Entry> mdl;
    private static final int hitZone = 10;
    private final Selection sel;
    
    public LassoTool(Selection sel) {
        this.sel = sel;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (mdl != null) {
            if (mdl.hit(e.getPoint(), hitZone) != null) {
                repainter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                repainter.setCursor(Cursor.getDefaultCursor());
            }
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (hit != null) {
//            hit.getNode().setLocation(e.getPoint());
            mdl.setPoint(hit.getNode(), e.getPoint());
            repainter.requestRepaint();
        } else {
            mdl.add(new LineTo(e.getPoint()));
        }
    }

    private Hit hit;
    @Override
    public void mousePressed(MouseEvent e) {
        if (mdl == null) {
            mdl = DefaultPathModel.newInstance();
        } else {
            Hit theHit = mdl.hit(e.getPoint(), hitZone);
            if (theHit != null) {
                hit = theHit;
                //Test for AIOOBE
                hit.getNode();
                repainter.requestRepaint(mdl.getBounds());
            } else {
                if (hit != null) {
                    repainter.requestRepaint(mdl.getBounds());
                }
                hit = null;
            }
        }
        repainter.requestRepaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (hit == null) {
            if (e.getClickCount() >= 2) {
                mdl.add(new Close());
                commit (e.getModifiersEx());
            } else {
                boolean shiftDown = (e.getModifiers() & KeyEvent.SHIFT_MASK) != 0;
                boolean altDown = (e.getModifiers() & KeyEvent.ALT_MASK) != 0;
                boolean ctrlDown = (e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
                if (mdl.isEmpty()) {
                    mdl.add (new MoveTo(e.getPoint()));
                } else {
                    if (ctrlDown) {
                        mdl.add (new MoveTo(e.getPoint()));
                    } else if (altDown) {
                        Entry entry = mdl.get(mdl.size() - 1);
                        Point intermed = new Point(e.getPoint());
                        intermed.x -= 20;
                        intermed.y -= 20; //XXX
                        mdl.add (new QuadTo (intermed.x, intermed.y, e.getPoint().x, e.getPoint().y));
                    } else if (shiftDown) {
                        Point ctrl1 = new Point(e.getPoint());
                        Point ctrl2 = new Point(e.getPoint());
                        ctrl1.x -= 15;
                        ctrl2.y -= 15; //XXX check bounds
                        mdl.add (new CurveTo(ctrl1, ctrl2, e.getPoint()));
                    } else {
                        mdl.add(new LineTo(e.getPoint()));
                    }
                }
                repainter.requestRepaint(mdl.getBounds());
            }
        } else if (e.getClickCount() >= 2) {
            mdl.add (new Close());
            commit (e.getModifiersEx());
        }
        repainter.requestRepaint();
    }

    public void attach(Lookup.Provider layer) {
        Selection s = sel;
        if (s != null) {
            Shape shape = s.asShape();
            if (shape != null) {
                mdl = DefaultPathModel.create(shape);
            }
        }
    }

    public void detach() {
       repainter.setCursor(Cursor.getDefaultCursor());
             this.layer = null;
        repainter = null;
        commit(0);
        mdl = null;
    }

    public void attachRepainter(Repainter repainter) {
        this.repainter = repainter;
        repainter.requestRepaint();
    }
    
    public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
        if (mdl != null && !commit) {
            Selection.paintSelectionAsShape(g2d, mdl, layerBounds);
            Node hitNode = hit == null ? null : hit.getNode();
            if (hitNode != null) {
                hitNode.paint(g2d, true);
            }
            for (Entry entry : mdl) {
                Node[] nodes = entry.getPoints();
                for (Node n : nodes) {
                    if (n != hitNode && n != null) {
                        n.paint(g2d, false);
                    }
                }
            }
        }
    }

    public void keyTyped(KeyEvent e) {
        
    }

    public void keyPressed(KeyEvent e) {
        if (hit != null) {
            int m = e.getModifiersEx();
            boolean shiftDown = (m & KeyEvent.SHIFT_DOWN_MASK) != 0;
            boolean altDown = (m & KeyEvent.ALT_DOWN_MASK) != 0;
            int amt = shiftDown ? altDown ? 10 : 5 : 1;
            Node n = hit.getNode();
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP :
                    n.setLocation(new Point2D.Double(n.getX(), n.y() - amt));
                    break;
                case KeyEvent.VK_DOWN :
                    n.setLocation(new Point2D.Double(n.getX(), n.y() + amt));
                    break;
                case KeyEvent.VK_RIGHT :
                    n.setLocation(new Point2D.Double(n.getX() + amt, n.y()));
                    break;
                case KeyEvent.VK_LEFT :
                    n.setLocation(new Point2D.Double(n.getX() - amt, n.y()));
                    break;
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!mdl.isEmpty()) {
                if (!(mdl.get(mdl.size() - 1) instanceof Close)) {
                    mdl.add(new Close());
                }
                commit(e.getModifiersEx());
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        
    }
    
    private void commit(int mods) {
        if (layer != null) {
            Selection<Shape> s = layer.getLookup().lookup(Selection.class);
            if (s == null) {
                Toolkit.getDefaultToolkit().beep();
            } else {
                boolean shiftDown = (mods & KeyEvent.SHIFT_DOWN_MASK) != 0;
                Op op = shiftDown ? Op.ADD : Op.REPLACE;
                //XXX add ways to merge, etc
                s.add(mdl, op);
            }
        }
    }
}
