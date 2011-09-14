package net.java.dev.imagine.layers.text.widget;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.selection.ObjectSelection;
import net.dev.java.imagine.api.selection.ShapeConverter;
import net.dev.java.imagine.api.selection.Universe;
import net.dev.java.imagine.spi.tools.Customizer;
import net.dev.java.imagine.spi.tools.CustomizerProvider;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.layers.text.widget.api.Text;
import net.java.dev.imagine.layers.text.widget.api.TextItems;
import net.java.dev.imagine.layers.text.widget.api.TextItems.TextItemsSupport;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.components.FontCellRenderer;
import org.netbeans.paint.api.components.FontComboBoxModel;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.netbeans.paint.api.util.Fonts;
import org.netbeans.paintui.widgetlayers.WidgetLayer;
import org.netbeans.paintui.widgetlayers.WidgetLayer.WidgetFactory;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;
import org.openide.util.WeakListeners;
import org.openide.util.WeakSet;
import org.openide.util.lookup.Lookups;

/**
 * A widget based layer which allows text items to be added and edited.
 *
 * @author Tim Boudreau
 */
class TextLayer extends LayerImplementation<TextLayerFactory> {

    private Dimension size;
    private State state = new State();
    TextItemsSupport.Editor editor = new Ed();
    private final TextItems items = new TextItemsSupport(editor);
    private final Set<WF> factories = new WeakSet<WF>();
    private final ObjectSelection<Text> selection = new ObjectSelection<Text>(Text.class, new S(), new S());

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
    
    public ObjectSelection<Text> getSelection() {
        return selection;
    }
    
    public boolean isSelected(Text text) {
        return selection.contains(Collections.singleton(text));
    }
    
    private class S implements ShapeConverter<Text>, Universe<Collection<Text>> {

        @Override
        public Class<Text> type() {
            return Text.class;
        }

        @Override
        public Shape toShape(Text obj) {
            for (WF wf : factories) {
                if (wf != null) {
                    TextWidget w = wf.find(obj);
                    if (w != null && w.getBounds() != null) {
                        return w.convertLocalToScene(w.getBounds());
                    }
                }
            }
            return null;
        }

        @Override
        public Collection<Text> getAll() {
            Set<Text> result = new HashSet<Text>();
            for (Text t : items) {
                result.add(t);
            }
            return result;
        }
        
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
                        w.revalidate();
                        w.getScene().validate();
                        w.edit();
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected Lookup createLookup() {
        return Lookups.fixed(this, wl, items, new CP(), selection);
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
                        tw = new TextWidget(TextLayer.this, container, t);
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
    
    TextItems getItems() {
        return items;
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

//    private class CP implements CustomizerProvider {
//        @Override
//        public Customizer getCustomizer() {
//            return Customizers.getCustomizer(Font.class, "default");
//        }
//    }
    
    private class CP implements CustomizerProvider, Customizer<Font> {

        private Font font = Fonts.getDefault().get();

        @Override
        public Customizer getCustomizer() {
            Iterator<Text> i = items.iterator();
            if (i.hasNext()) {
                font = i.next().getFont();
            }
            return this;
        }

        @Override
        public JComponent getComponent() {
            JPanel pnl = new JPanel(new FlowLayout()); //XXX just for now
            final JComboBox box = new JComboBox(new FontComboBoxModel());
            box.setRenderer(new FontCellRenderer());
            box.setSelectedItem(font.deriveFont(Font.PLAIN, 12));
            pnl.add(box);
            final JSlider slider = new JSlider(4, 200);
            slider.setValue(font.getSize());
            slider.setUI(new PopupSliderUI());
            pnl.add(slider);
            final JCheckBox bold = new JCheckBox(NbBundle.getMessage(CP.class, "BOLD")); //NOI18N
            final JCheckBox italic = new JCheckBox(NbBundle.getMessage(CP.class, "ITALIC")); //NOI18N
            pnl.add(bold);
            pnl.add(italic);

            class L implements ActionListener, ChangeListener {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Font f = (Font) box.getSelectedItem();
                    int size = slider.getValue();
                    int style = Font.PLAIN;
                    if (bold.isSelected()) {
                        style |= Font.BOLD;
                    }
                    if (italic.isSelected()) {
                        style |= Font.ITALIC;
                    }
                    f = f.deriveFont(style, size);
                    CP.this.font = f;
                    for (Text t : items) {
                        t.setFont(font);
                    }
                    Fonts.getDefault().set(f);
                }

                @Override
                public void stateChanged(ChangeEvent e) {
                    actionPerformed(null);
                }
            }
            L a = new L();
            box.addActionListener(a);
            bold.addActionListener(a);
            italic.addActionListener(a);
            slider.addChangeListener(a);
            return pnl;
        }

        @Override
        public String getName() {
            return NbBundle.getMessage(CP.class, "TextCustomizer"); //NOI18N
        }

        @Override
        public Font get() {
            return font;
        }
    }
}
