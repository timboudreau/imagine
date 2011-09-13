package net.java.dev.imagine.layers.text.widget;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.layers.text.widget.api.Text;
import net.java.dev.imagine.layers.text.widget.api.TextItems;
import net.java.dev.imagine.layers.text.widget.api.TextItems.TextItemsSupport;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.TextFieldInplaceEditor;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.WidgetLayer;
import org.netbeans.paintui.widgetlayers.WidgetLayer.WidgetFactory;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Parameters;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.WeakSet;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
class TextLayer extends LayerImplementation<TextLayerFactory> {

    private Dimension size;
    private State state = new State();
    TextItemsSupport.Editor editor = new Ed();
    private final TextItems items = new TextItemsSupport(editor);
    private final Set<WF> factories = new WeakSet<WF>();

    public TextLayer(TextLayerFactory factory, String name, RepaintHandle handle, Dimension size) {
        super(factory);
        Parameters.notNull("name", size);
        Parameters.notNull("handle", handle);
        this.size = size; //XXX
        addRepaintHandle(handle);
        System.out.println("created a text layer");
    }

    public TextLayer(TextLayerFactory factory, RepaintHandle handle, State state) {
        super(factory);
        this.state = state.clone();
        addRepaintHandle(handle);
        System.out.println("created a text layer");
    }
    
    private class Ed implements TextItemsSupport.Editor {

        @Override
        public Text hit(Point p) {
            for (WF f : factories) {
                if (f != null) { //WeakSet can have nulls
                    Text result = f.find(p);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        }

        @Override
        public void edit(Text text) {
            for (WF f : factories) {
                if (f != null) {
                    TextWidget w = f.find(text);
                    if (w != null) {
                        w.edit();
                        return;
                    }
                }
            }
        }
        
    }

    @Override
    protected Lookup createLookup() {
        return Lookups.fixed(this, wl, items);
    }
    private final WL wl = new WL();

    final class WL extends WidgetLayer {

        @Override
        public WidgetFactory createWidgetController(Widget container, WidgetController controller) {
            System.out.println("create widget controller");
            WF result = new WF(container);
            factories.add(result);
            return result;
        }
    }

    final class WF implements WidgetFactory, ChangeListener {

        private final Widget container;
        private boolean attached;
        private Layout oldLayout;

        WF(Widget container) {
            this.container = container;
            items.addChangeListener(WeakListeners.change(this, items));
        }

        @Override
        public void attach() {
            System.out.println("Widget factory attach to " + container);
            attached = true;
            oldLayout = container.getLayout();
            container.setLayout(LayoutFactory.createAbsoluteLayout());
            sync();
        }

        @Override
        public void detach() {
            System.out.println("widget factory detach");
            attached = false;
            container.removeChildren();
            container.setLayout(oldLayout);
        }

        private TextWidget widgetFor(Text text) {
            for (Widget w : container.getChildren()) {
                if (w instanceof TextWidget && (((TextWidget) w).text.equals(text))) {
                    return (TextWidget) w;
                }
            }
            return null;
        }

        private void sync() {
            if (attached) {
                List<TextWidget> toAdd = new LinkedList<TextWidget>();
                List<TextWidget> toRemove = new LinkedList<TextWidget>();
                for (Text t : items) {
                    TextWidget tw = widgetFor(t);
                    if (tw == null) {
                        tw = new TextWidget(items, container, t);
                        toAdd.add(tw);
                    }
                }
                for (Widget w : container.getChildren()) {
                    if (w instanceof TextWidget && !items.contains(((TextWidget) w).text)) {
                        toRemove.add((TextWidget) w);
                    }
                }
                for (TextWidget t : toRemove) {
                    container.removeChild(t);
                }
                for (TextWidget t : toAdd) {
                    container.addChild(t);
                }
                container.revalidate();
                container.repaint();
                System.out.println("sync adds " + toAdd.size() + " and removes " + toRemove.size());
            }
        }

        @Override
        public SetterResult setLocation(Point location) {
            return SetterResult.NOT_HANDLED;
        }

        @Override
        public SetterResult setOpacity(float opacity) {
            return SetterResult.NOT_HANDLED;
        }

        @Override
        public void setName(String name) {
            //do nothing
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            sync();
        }

        private Text find(Point p) {
            for (Widget w : container.getChildren()) {
                if (w instanceof TextWidget) {
                    Point widgetLocal = w.convertSceneToLocal(p);
                    if (w.isHitAt(widgetLocal)) {
                        return ((TextWidget) w).text;
                    }
                }
            }
            return null;
        }

        private TextWidget find(Text text) {
            for (Widget w : container.getChildren()) {
                if (w instanceof TextWidget) {
                    if (text.equals(((TextWidget) w).text)) {
                        return (TextWidget) w;
                    }
                }
            }
            return null;
        }
    }

    static final class TextWidget extends LabelWidget implements PropertyChangeListener, TextFieldInplaceEditor, MoveProvider, LookupListener {

        private final Text text;
        private final Lookup.Result<TextItems> activeLayerResult =
                Utilities.actionsGlobalContext().lookupResult(TextItems.class);
        private final WidgetAction editAction = ActionFactory.createInplaceEditorAction(this);
        private final WidgetAction moveAction = ActionFactory.createMoveAction(ActionFactory.createFreeMoveStrategy(), this);
        private final TextItems items;

        @SuppressWarnings("LeakingThisInConstructor")
        TextWidget(TextItems items, Widget parent, Text text) {
            super(parent.getScene(), text.getText());
            this.items = items;
            this.text = text;
            text.addPropertyChangeListener(WeakListeners.propertyChange(this, text));
            setPreferredLocation(text.getLocation());
            setFont(text.getFont());
            System.out.println("Created a text widget for " + text);
            resultChanged(null);
        }

        void attachActions() {
            getActions().addAction(editAction);
            getActions().addAction(moveAction);
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }

        void detachActions() {
            getActions().removeAction(editAction);
            getActions().removeAction(moveAction);
            setCursor(null);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            assert evt.getSource() == text;
            setFont(text.getFont());
            setLabel(text.getText());
            setPreferredLocation(text.getLocation());
        }

        @Override
        public boolean isEnabled(Widget widget) {
            return true;
        }

        @Override
        public String getText(Widget widget) {
            return getLabel();
        }

        @Override
        public void setText(Widget widget, String string) {
            text.setText(string);
            setLabel(string);
        }

        @Override
        public void movementStarted(Widget widget) {
            //do nothing
        }

        @Override
        public void movementFinished(Widget widget) {
            text.setLocation(widget.getPreferredLocation());
        }

        @Override
        public Point getOriginalLocation(Widget widget) {
            return widget.getPreferredLocation();
        }

        @Override
        public void setNewLocation(Widget widget, Point point) {
            widget.setPreferredLocation(point);
        }

        @Override
        public void resultChanged(LookupEvent le) {
            boolean enableActions = activeLayerResult.allInstances().contains(items);
            if (enableActions) {
                attachActions();
            } else {
                detachActions();
            }
        }

        private void edit() {
            MouseEvent me = new MouseEvent(getScene().getView(), MouseEvent.MOUSE_CLICKED, 
                    System.currentTimeMillis(), 0, 0, 0, 2, false, MouseEvent.BUTTON1);
            WidgetMouseEvent e = new WidgetMouseEvent(0, me);
            editAction.mouseClicked(this, e);
        }
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(state.location, new Dimension(size));
    }

    @Override
    public String getName() {
        return state.name;
    }

    @Override
    public void resize(int width, int height) {
        //do nothing for now
    }

    @Override
    public void setName(String name) {
        Parameters.notNull("name", name);
        if (!state.name.equals(name)) {
            String old = state.name;
            state.name = name;
            supp.firePropertyChange(Layer.PROP_NAME, old, name);
        }
    }
    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        supp.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        supp.removePropertyChangeListener(l);
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible == isVisible()) {
            state.visible = visible;
            supp.firePropertyChange(PROP_VISIBLE, !visible, visible);
        }
    }

    @Override
    public boolean isVisible() {
        return state.visible;
    }

    @Override
    public float getOpacity() {
        return state.opacity;
    }

    @Override
    public void setOpacity(float f) {
        float old = getOpacity();
        if (f != old) {
            state.opacity = f;
            supp.firePropertyChange(PROP_OPACITY, old, state);
        }
    }

    @Override
    public void commitLastPropertyChangeToUndoHistory() {
        System.out.println("commit not implemented");
        //do nothing
    }

    @Override
    public boolean paint(Graphics2D g, Rectangle bounds, boolean showSelection) {
        //do nothing
        return false;
    }

    @Override
    public LayerImplementation clone(boolean isUserCopy, boolean deepCopy) {
        return new TextLayer(getFactory(), getName(), super.getMasterRepaintHandle(), getBounds().getSize());
    }

    private static final class State {

        boolean visible = true;
        String name = "Text";
        float opacity = 1.0F;
        final Point location = new Point();

        State(boolean visible, String name, float opacity, Point p) {
            location.setLocation(p);
            this.name = name;
            this.opacity = opacity;
            this.visible = visible;
        }

        State() {
        }

        State(State other) {
            this(other.visible, other.name, other.opacity, other.location);
        }

        public State clone() {
            return new State(this);
        }
    }
}
