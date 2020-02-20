/*
 * MoveShapeTool.java
 *
 * Created on November 3, 2006, 5:34 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.vectorlayers.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Proxy;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.aggregate.PaintedPrimitive;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ControlPointController;
import net.java.dev.imagine.api.vector.design.ControlPointFactory;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.util.Pt;
import net.java.dev.imagine.api.vector.util.Size;
import org.netbeans.paint.vectorlayers.ShapeStack;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "MoveShape", iconPath = "net/java/dev/imagine/api/tool/unknown.png",
        displayNameBundle = "net.dev.java.imagine.vectorlayers.tools.Bundle")
@Tool(name = "MoveShape", value = ShapeStack.class)
public class MoveShapeTool extends MouseMotionAdapter implements /* Tool, Icon, */ PaintParticipant, MouseListener, ControlPointController, Attachable {

    public MoveShapeTool(ShapeStack stack) {
        this.stack = stack;
    }

    ShapeStack stack;
    Layer layer;

    public void attach(Lookup.Provider p) {
        this.layer = p.getLookup().lookup(Layer.class);
        ShapeStack stack = (ShapeStack) layer.getLookup().lookup(ShapeStack.class);
        assert stack != null;
        this.stack = stack;
        if (repainter != null) {
            repainter.requestRepaint(layer.getBounds());
        }
    }

    public void detach() {
        this.layer = null;
        stack = null;
        shape = null;
        mousePressPoint = null;
        cpoint = null;
        repainter = null;
    }

    private Point mousePressPoint;

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
        if (stack == null && layer != null) {
            stack = layer.getLookup().lookup(ShapeStack.class);
        }
        if (stack == null) {
            return;
        }
        List<Primitive> l = stack.getPrimitives();
        Rectangle2D.Double scratch = new Rectangle2D.Double(0, 0, 0, 0);
        Point point = mousePressPoint = e.getPoint();
        if (shape != null) {
            ControlPoint[] p = new ControlPointFactory().getControlPoints((Adjustable) shape, this);
            for (int i = 0; i < p.length; i++) {
                ControlPoint pt = p[i];
                if (pt.hit(point.x, point.y)) {
                    setSelectedControlPoint(pt);
                    return;
                }
            }
        }
        boolean found = false;
        for (Primitive p : l) {
            if (p instanceof Vector) {
                Vector vector = (Vector) p;
                Shape shape = vector.toShape();
                if (shape.contains(point.x, point.y)) {
                    setSelectedShape(p);
                    found = true;
                }
            } else if (p instanceof Volume) {
                Volume volume = (Volume) p;
                volume.getBounds(scratch);
                System.err.println(p);
                if (scratch.contains(point.x, point.y)) {
                    setSelectedShape(p);
                    found = true;
                }
            }
        }
        if (!found) {
            setSelectedShape(null);
        }
    }

    Primitive shape;

    private void setSelectedShape(Primitive p) {
        if (this.shape != p) {
            this.shape = p;
            repainter.requestRepaint(layer.getBounds());
        }
    }

    Repainter repainter;

    public void attachRepainter(Repainter repainter) {
        this.repainter = repainter;
        if (layer != null) {
            repainter.requestRepaint(layer.getBounds());
        }
    }

    public void paint(Graphics2D g, Rectangle layerBounds, boolean commit) {
        if (shape == null || stack == null) {
            return;
        }
        int full = 6;
        int half = 3;
        Rectangle2D.Double scratch = new Rectangle2D.Double(0, 0, full, full);
        g.setStroke(new BasicStroke(1F));
        int max = ((Adjustable) shape).getControlPointCount();
        double[] d = new double[max * 2];
        ((Adjustable) shape).getControlPoints(d);
        Point p = layer.getSurface().getLocation();
        for (int i = 0; i < d.length; i += 2) {
            d[i] -= p.x;
            d[i + 1] -= p.y;
            scratch.x = d[i] - half;
            scratch.y = d[i + 1] - half;
            g.setColor(Color.WHITE);
            g.fill(scratch);
            g.setColor(Color.BLACK);
            g.draw(scratch);
            if (cpoint != null && cpoint.getX() == d[i] && cpoint.getY() == d[i + 1]) {
                scratch.x -= 3;
                scratch.y -= 3;
                scratch.width += 6;
                scratch.height += 6;
                g.setColor(Color.YELLOW);
                g.draw(scratch);
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
        cpoint = null;
        repainter.requestRepaint();
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    private void setSelectedControlPoint(ControlPoint cpoint) {
        this.cpoint = cpoint;
        repainter.requestRepaint();
    }

    private final Rectangle2D.Double scratch = new Rectangle2D.Double();
    private final Rectangle2D.Double scratch2 = new Rectangle2D.Double();

    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        Primitive shape = this.shape;
        if (cpoint != null) {
            cpoint.set(p.x, p.y);
        } else if (shape instanceof Volume && shape instanceof Vector) {
            int xoff = p.x - mousePressPoint.x;
            int yoff = p.y - mousePressPoint.y;
            ((Volume) shape).getBounds(scratch);
            Vector v = (Vector) shape;
            Pt loc = v.getLocation();
            v.setLocation(loc.x + xoff, loc.y + yoff);
            ((Volume) shape).getBounds(scratch2);
            Rectangle toRepaint = scratch2
                    .createIntersection(scratch).getBounds();
            toRepaint.x -= 5;
            toRepaint.y -= 5;
            toRepaint.width += 10;
            toRepaint.height += 10;
            repainter.requestRepaint(toRepaint);
        } else if (shape instanceof Vector) {
            int xoff = p.x - mousePressPoint.x;
            int yoff = p.y - mousePressPoint.y;
            Vector v = (Vector) shape;
            Pt loc = v.getLocation();
            v.setLocation(loc.x + xoff, loc.y + yoff);
            repainter.requestRepaint();
        }
        mousePressPoint = p;
    }

    public void changed(ControlPoint pt) {
        repainter.requestRepaint();
    }

    private final Size SIZE = new Size(12, 12);

    public Size getControlPointSize() {
        return SIZE;
    }
    ControlPoint cpoint;

    private void showPopup(MouseEvent e) {
        Point point = e.getPoint();
        List<Primitive> l = stack.getPrimitives();
        Rectangle2D.Double scratch = new Rectangle2D.Double(0, 0, 0, 0);
        List<Primitive> shapes = new ArrayList<Primitive>();
        List<Vector> vectors = new ArrayList<Vector>();
        Primitive topMost = null;
        for (Primitive p : l) {
            if (p instanceof Vector) {
                Vector vector = (Vector) p;
                Shape shape = vector.toShape();
                System.err.println(shape);
                if (shape.contains(point.x, point.y)) {
                    topMost = vector;
                    shapes.add(vector);
                    vectors.add(vector);
                }
            } else if (p instanceof Volume) {
                Volume volume = (Volume) p;
                volume.getBounds(scratch);
                System.err.println(p);
                if (scratch.contains(point.x, point.y)) {
                    topMost = volume;
                    shapes.add(volume);
                }
            }
        }
        if (!shapes.isEmpty()) {
            assert topMost != null;
            JPopupMenu menu = new JPopupMenu();
            menu.add(new FrontBackAction(true, topMost, stack));
            menu.add(new FrontBackAction(false, topMost, stack));
            menu.add(new CSGAction(UNION, vectors, stack));
            menu.add(new CSGAction(INTERSECTION, vectors, stack));
            menu.add(new CSGAction(SUBTRACTION, vectors, stack));
            menu.add(new CSGAction(XOR, vectors, stack));
            menu.show(repainter.getDialogParent(), point.x, point.y);
        }
    }

    private class FrontBackAction extends AbstractAction {

        private final ShapeStack stack;
        private final Primitive primitive;
        private final boolean front;

        public FrontBackAction(boolean front, Primitive p, ShapeStack stack) {
            putValue(Action.NAME, front ? "To Front" : "To Back");
            this.front = front;
            this.primitive = p;
            this.stack = stack;
        }

        public void actionPerformed(ActionEvent ae) {
            if (front) {
                stack.toFront(primitive);
            } else {
                stack.toBack(primitive);
            }
            if (repainter != null) {
                repainter.requestRepaint();;
            }
        }
    }
    private static final int UNION = 0;
    public static final int INTERSECTION = 1;
    public static final int SUBTRACTION = 2;
    private static final int XOR = 3;

    private class CSGAction extends AbstractAction {

        private final int kind;
        private final List<Vector> shapes;
        private final ShapeStack stack;

        public CSGAction(int kind, List<Vector> shapes, ShapeStack stack) {
            String name;
            this.kind = kind;
            this.shapes = shapes;
            this.stack = stack;
            assert !shapes.isEmpty();
            switch (kind) {
                case UNION:
                    name = "Union";
                    break;
                case INTERSECTION:
                    name = "Intersection";
                    break;
                case SUBTRACTION:
                    name = "Subtract";
                    break;
                case XOR:
                    name = "XOR";
                    break;
                default:
                    throw new IllegalArgumentException("" + kind);
            }
            putValue(Action.NAME, name);
        }

        public boolean isEnabled() {
            return shapes.size() > 1;
        }

        public void actionPerformed(ActionEvent ae) {
            Area area = null;
            List<Attribute<?>> attrs = new ArrayList<>();
            boolean fill = false;
            for (Vector v : shapes) {
                Primitive vv = v;
                while (vv instanceof Proxy && !(vv instanceof PaintedPrimitive)) {
                    vv = ((Proxy) vv).getProxiedPrimitive();
                }
                if (vv instanceof PaintedPrimitive) {
                    PaintedPrimitive pp = (PaintedPrimitive) vv;
                    List<Attribute<?>> atts = pp.allAttributes();
                    System.err.println("  include attributes " + atts);
                    attrs.addAll(pp.allAttributes());
                }
                fill |= v instanceof Fillable && ((Fillable) v).isFill();
                if (area == null) {
                    area = new Area(v.toShape());
                } else {
                    Area other = new Area(v.toShape());
                    switch (kind) {
                        case UNION:
                            area.add(other);
                            break;
                        case INTERSECTION:
                            area.intersect(other);
                            break;
                        case SUBTRACTION:
                            area.subtract(other);
                            break;
                        case XOR:
                            area.exclusiveOr(other);
                            break;
                        default:
                            throw new AssertionError();
                    }
                }
            }
            if (area != null) {
                PathIteratorWrapper wrap = new PathIteratorWrapper(area.getPathIterator(
                        AffineTransform.getTranslateInstance(0, 0)), fill);
                //XXX probably don't want all attributes, they may be 
                //redundant
                PaintedPrimitive pp = PaintedPrimitive.create(wrap, attrs);

                stack.replace(shapes, pp);
                if (repainter != null) {
                    repainter.requestRepaint();;
                }
            }
        }
    }
}
