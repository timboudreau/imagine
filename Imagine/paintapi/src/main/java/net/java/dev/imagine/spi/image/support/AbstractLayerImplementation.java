package net.java.dev.imagine.spi.image.support;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.UndoableEditEvent;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.editor.api.Zoom;
import org.netbeans.paint.api.editing.LayerFactory;
import org.netbeans.paint.api.editing.UndoManager;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AbstractLayerImplementation extends LayerImplementation {

    private final SwingPropertyChangeSupport propertyChanges
            = new SwingPropertyChangeSupport(this, true);
    private LayerInternalState istate;
    private LayerInternalState prev;

    protected AbstractLayerImplementation(LayerFactory factory, String initialName) {
        super(factory);
        istate = new LayerInternalState(initialName);
    }

    protected AbstractLayerImplementation(LayerFactory factory, boolean resolutionIndependent, String initialName) {
        super(factory, resolutionIndependent);
        istate = new LayerInternalState(initialName);
    }

    @Override
    public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds, boolean showSelection, boolean paintWhenVisibleFalse, Zoom zoom) {
        if (!istate.visible && !paintWhenVisibleFalse) {
            return false;
        }
        return paint(goal, g, bounds, showSelection, zoom);
    }

    protected abstract boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds, boolean showSelection, Zoom zoom);

    @Override
    public final void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChanges.addPropertyChangeListener(l);
    }

    @Override
    public final void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChanges.removePropertyChangeListener(l);
    }

    protected final void firePropertyChange(PropertyChangeEvent evt) {
        propertyChanges.firePropertyChange(evt);
    }

    protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChanges.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected final void firePropertyChange(String propertyName, int oldValue, int newValue) {
        propertyChanges.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected final void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        propertyChanges.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public final String getName() {
        return istate.name;
    }

    @Override
    public final void setName(String name) {
        updateState(istate.setName(name));
    }

    @Override
    public final void setVisible(boolean visible) {
        updateState(istate.setVisible(visible));
    }

    @Override
    public final boolean isVisible() {
        return istate.visible;
    }

    @Override
    public final float getOpacity() {
        return istate.opacity;
    }

    @Override
    public final void setOpacity(float f) {
        updateState(istate.setOpacity(f));
    }

    protected void repaint() {
        Rectangle bounds = getBounds();
        if (bounds != null) {
            getMasterRepaintHandle().repaintArea(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private void onChange(String prop, Object oldValue, Object newValue,
            LayerInternalState old, LayerInternalState nue) {
        firePropertyChange(prop, oldValue, newValue);
        UndoManager mgr = Utilities.actionsGlobalContext().lookup(UndoManager.class);
        if (mgr != null) {
            String key = null;
            switch (prop) {
                case PROP_NAME:
                    key = "NAME";
                    break;
                case PROP_OPACITY:
                    key = "OPACITY";
                    break;
                case PROP_VISIBLE:
                    key = "VISIBILITY";
                    break;
                case PROP_BOUNDS:
                    key = "BOUNDS";
                    break;
            }
            String propName;
            if (key != null) {
                propName = NbBundle.getMessage(AbstractLayerImplementation.class, key);
            } else {
                propName = prop;
            }
            String changeName = NbBundle.getMessage(AbstractLayerImplementation.class, "CHANGE_PROPERTY_EDIT", propName);
            GenericUndoableEdit ed = new GenericUndoableEdit(changeName, () -> {
                this.istate = old;
                firePropertyChange(prop, newValue, oldValue);
            }, () -> {
                this.istate = nue;
                firePropertyChange(prop, oldValue, newValue);
            });
            UndoableEditEvent evt = new UndoableEditEvent(this, ed);
            mgr.undoableEditHappened(evt);
        }
    }

    private void updateState(LayerInternalState nue) {
        if (!nue.equals(this.istate)) {
            prev = this.istate;
            this.istate = nue;
            boolean needRepaint = false;
            if (!prev.name().equals(nue.name())) {
                onChange(PROP_NAME, prev.name(), nue.name(), prev, nue);
            }
            if (prev.opacity() != nue.opacity()) {
                onChange(PROP_OPACITY, prev.opacity(),
                        nue.opacity(), prev, nue);
                needRepaint = true;
            }
            if (prev.visible() != nue.visible()) {
                onChange(PROP_VISIBLE, prev.visible(),
                        nue.visible(), prev, nue);
                needRepaint = true;
            }
            if (needRepaint) {
                repaint();
            }
        }
    }

    static class LayerInternalState {

        private final float opacity;
        private final boolean visible;
        private final String name;

        LayerInternalState(String name) {
            this(1, true, name);
        }

        LayerInternalState(float opacity, boolean visible, String name) {
            if (opacity < 0 || opacity > 1) {
                throw new IllegalArgumentException("Bad opacity " + opacity);
            }
            if (name == null) {
                throw new IllegalArgumentException("Null name");
            }
            this.opacity = opacity;
            this.visible = visible;
            this.name = name;
        }

        public String name() {
            return name;
        }

        public float opacity() {
            return opacity;
        }

        public boolean visible() {
            return visible;
        }

        public LayerInternalState setOpacity(float val) {
            return new LayerInternalState(val, this.visible, this.name);
        }

        public LayerInternalState setVisible(boolean viz) {
            return new LayerInternalState(opacity, viz, this.name);
        }

        public LayerInternalState setName(String name) {
            return new LayerInternalState(opacity, visible, name);
        }

        @Override
        public String toString() {
            return name + " " + visible + " " + opacity;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + Float.floatToIntBits(this.opacity);
            hash = 59 * hash + (this.visible ? 1 : 0);
            hash = 59 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LayerInternalState other = (LayerInternalState) obj;
            if (Float.floatToIntBits(this.opacity) != Float.floatToIntBits(other.opacity)) {
                return false;
            }
            if (this.visible != other.visible) {
                return false;
            }
            return Objects.equals(this.name, other.name);
        }
    }
}
