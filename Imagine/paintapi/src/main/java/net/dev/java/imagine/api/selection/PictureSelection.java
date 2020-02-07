package net.dev.java.imagine.api.selection;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public final class PictureSelection implements Lookup.Provider {

    private final Map<Class<?>, Shape> shapeByType = new HashMap<>();
    private Selection<?> storedSelection;
    private Selection<?> currSelection;
    private final MutableProxyLookup lookup = new MutableProxyLookup();
    private final ChangeSupport supp = new ChangeSupport(this);

    public PictureSelection() {
    }

    public PictureSelection(LayerImplementation initial) {
        activeLayerChanged(null, initial);
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public void activeLayerChanged(LayerImplementation old, LayerImplementation nue) {
        if (nue == null) {
            storedSelection = currSelection == null ? old.getLookup().lookup(Selection.class)
                    : currSelection;
            if (storedSelection != null) {
                storedSelection = (Selection) storedSelection.clone();
            }
        }
        Selection ns = nue == null ? null : nue.getLookup().lookup(Selection.class);
        setCurrentSelection(ns);
        currSelection = ns;
    }

    public boolean isEmpty() {
        return currSelection == null ? true : currSelection.isEmpty();
    }

    public Shape toShape() {
        Selection s = currSelection == null ? storedSelection : currSelection;
        if (s == null) {
            return null;
        }
        return s.asShape();
    }

    public void paint(Graphics2D g, Rectangle sceneBounds) {
        Selection<?> s = currSelection == null ? storedSelection : currSelection;
        if (s != null) {
            s.paint(g, sceneBounds);
        }
    }

    public Class<?> getType() {
        return currSelection == null ? null : currSelection.type();
    }

    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        Selection s = currSelection == null ? storedSelection : currSelection;
        if (s == null) {
            return false;
        }
        if (o instanceof Point) {
            Point p = (Point) o;
            return s.asShape().contains(new Point2D.Double(p.x, p.y));
        } else if (o instanceof Point2D) {
            return s.asShape().contains((Point2D) o);
        } else if (s.type().isInstance(o)) {
            return s.contains(s.type().cast(o));
        } else if (o instanceof Shape) {
            return s.asShape().getBounds2D().intersects(((Shape) o).getBounds2D());
        } else if (s instanceof ObjectSelection<?>) {
            return ((ObjectSelection<?>) s).containsObject(o);
        }
        return false;
    }

    /**
     * Convenience method so implementations can be consistent in the colors and
     * outlining they use to paint the selection.
     *
     * @param g The graphics context
     * @param content A shape
     */
    public static void paintSelectionAsShape(Rectangle bounds, Graphics2D g, Shape shape) {
        Color fill = new Color(255, 255, 255, 120);
        Graphics2D g1 = (Graphics2D) g.create();
        g1.setColor(fill);
        g1.fill(shape);
        g1.setXORMode(Color.BLUE);
        g1.draw(shape);
        g1.dispose();
    }

    private void setCurrentSelection(Selection<?> sel) {
        if (currSelection == sel) {
            return;
        }
        attachTo(sel);
        change();
    }

    private void attachTo(Selection<?> selection) {
        if (currSelection == null) {
            currSelection = storedSelection;
        }
        if (currSelection != null) {
            if (selection != null && selection.type() != currSelection.type()) {
                shapeByType.put(currSelection.type(), currSelection.asShape());
            }
            currSelection.removeChangeListener(cl);
        }
        if (currSelection != null && selection != null) {
            selection.translateFrom(currSelection);
            currSelection.clearNoUndo();
        }
        if (selection != null) {
            selection.addChangeListener(cl);
        }
        Lookup lkp = selection instanceof Lookup.Provider
                ? ((Lookup.Provider) selection).getLookup() : null;
        if (lkp == null) {
            lookup.changeLookups(new Lookup[]{});
        } else {
            lookup.changeLookups(new Lookup[]{lkp});
        }
    }
    private final ChangeListener cl = new CL();

    private final class CL implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            Selection<?> s = (Selection<?>) e.getSource();
            shapeByType.put(s.type(), s.asShape());
            System.out.println("Forwarding change from " + e.getSource() + " with " + s.asShape());
            supp.fireChange();
        }
    }

    public void removeChangeListener(ChangeListener listener) {
        supp.removeChangeListener(listener);
    }

    public void addChangeListener(ChangeListener listener) {
        supp.addChangeListener(listener);
    }

    private void change() {
        supp.fireChange();
    }

    @Override
    public String toString() {
        return super.toString() + '{' + currSelection + '}';
    }
}
