/*
 * BrushTool.java
 *
 * Created on October 15, 2005, 6:41 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.spi;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.java.dev.imagine.api.image.Layer;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.java.dev.imagine.api.image.Surface;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 * Convenience class for implementing tools which listen on the canvas and draw
 * on it or select it.  Implements the methods of PaintParticipant and 
 * CustomizerProvider w/o actually implementing the interfaces - just add them
 * to your subclass to have a basic implementation.
 * 
 * @author Timothy Boudreau
 */
public abstract class MouseDrivenTool extends MouseAdapter implements /*Tool,*/ MouseMotionListener, Attachable {

    private JComponent customizer = null;
    private final ChangeSupport changes = new ChangeSupport(this);
    protected Surface surface;
    protected Lookup.Provider layer;
    
    protected MouseDrivenTool(Surface surface) {
        this.surface = surface;
    }

    public final void attach(Lookup.Provider layer) {
        activated(layer.getLookup().lookup(Layer.class));
        this.layer = layer;
    }

    protected void activated(Layer layer) {
        if (surface == null) {
            surface = layer.getSurface();
        }
        //do nothing
    }
    
    protected void deactivated() {
        this.layer = null;
    }

    public final void detach() {
        painting = false;
    }

    protected final boolean isActive() {
        return true;
    }

    public final JComponent getCustomizer(boolean create) {
        if (customizer == null && create) {
            customizer = createCustomizer();
        }
        return customizer;
    }

    public boolean canAttach(Layer layer) {
        return layer.getLookup().lookup(Surface.class) != null;
    }

    /** Override to provide a customizer for the tool.  By default, 
     * simply creates a disabled JLabel saying "No customizer" */
    protected JComponent createCustomizer() {
        JLabel result = new JLabel();
        result.setEnabled(false);
        result.setHorizontalAlignment(SwingConstants.CENTER);
        result.setHorizontalTextPosition(SwingConstants.CENTER);
        result.setText(NbBundle.getMessage(MouseDrivenTool.class,
                "LBL_NoCustomizer")); //NOI18N
        return result;
    }
    boolean painting = false;

    @Override
    public final void mousePressed(MouseEvent e) {
        if (isActive()) {
            surface.beginUndoableOperation(null); //XXX
            painting = true;
            beginOperation(e.getPoint(), e.getModifiersEx());
        }
    }

    @Override
    public final void mouseReleased(MouseEvent e) {
        if (isActive() && painting) {
            dragged(e.getPoint(), e.getModifiersEx());
            endOperation(e.getPoint(), e.getModifiersEx());
            surface.endUndoableOperation();
        }
    }

    @Override
    public final void mouseDragged(MouseEvent e) {
        if (painting) {
            dragged(e.getPoint(), e.getModifiersEx());
        }
    }

    /** The user has pressed the mouse, starting an operation */
    protected void beginOperation(Point p, int modifiers) {
    }

    /** The user has released the mouse, ending an operation */
    protected void endOperation(Point p, int modifiers) {
    }

    /** The mouse was dragged */
    protected abstract void dragged(Point p, int modifiers);

    @Override
    public final void mouseMoved(MouseEvent e) {
        //do nothing
    }

    public Lookup getLookup() {
        return Lookups.singleton(this);
    }
    protected Repainter repainter;

    public void attachRepainter(Repainter repainter) {
        this.repainter = repainter;
    }

    public void paint(Graphics2D g2d) {
        //do nothing
    }

    public Customizer getCustomizer() {
        return this instanceof Customizer ? (Customizer) this
                : null;
    }

    public void addChangeListener(ChangeListener l) {
        changes.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        changes.removeChangeListener(l);
    }

    protected void fireChangeEvent() {
        changes.fireChange();
    }
}
