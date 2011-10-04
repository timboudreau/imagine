package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.Popup;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.properties.Bounded;
import net.java.dev.imagine.api.customizers.ToolProperty;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.State;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tim Boudreau
 */
public final class IntegerWidget extends Widget {

    private final LabelWidget label;
    private final LabelWidget value;

    @SuppressWarnings("LeakingThisInConstructor")
    <T extends Number> IntegerWidget(final ToolProperty<T, ?> prop, ColumnDataScene scene) {
        this(scene, ToolProperty.scale(prop));
    }
    
    @SuppressWarnings("LeakingThisInConstructor")
    IntegerWidget(ColumnDataScene scene, final ToolProperty<Integer, ?> prop) {
        super(scene);
        label = new LabelWidget(scene);
        value = new LabelWidget(scene);
        label.setFont(new Font("Monospaced", Font.BOLD, 20)); //XXX
        label.setLabel(prop.name().toString());
        addChild(label);
        addChild(value);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setLayout(scene.getColumns().createLayout());
        getActions().addAction(new WidgetAction.Adapter() {

            private Popup popup;
            
            @Override
            public State mousePressed(Widget widget, WidgetMouseEvent event) {
                if (event.isPopupTrigger()) {
                    return State.REJECTED;
                }
                JComponent c = getScene().getView();
                Point p = event.getPoint();
                p = widget.convertLocalToScene(p);
                p = getScene().convertSceneToView(p);
                SwingUtilities.convertPointToScreen(p, c);
                int min = 0;
                int max = 100;
                if (prop instanceof Bounded) {
                    Bounded<?> b = (Bounded<?>) prop;
                    min = b.getMinimum().intValue();
                    max = b.getMaximum().intValue();
                }
                final DefaultBoundedRangeModel mdl = new DefaultBoundedRangeModel(prop.get(), 1, min, max);
                mdl.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        prop.set(mdl.getValue());
                    }
                });
                widget.getScene().getView().addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        Component c = (Component) e.getSource();
                        c.removeMouseListener(this);
                        if (popup != null) {
                            popup.hide();
                            popup = null;
                        }
                    }
                });

                popup = PopupSliderUI.showPopup(c, mdl, p, null);
                return State.createLocked(widget, this);
            }

            @Override
            public State mouseReleased(Widget widget, WidgetMouseEvent event) {
                System.out.println("MR");
                if (popup != null) {
                    popup.hide();
                    popup = null;
                    return State.CONSUMED;
                }
                return super.mouseReleased(widget, event);
            }
        });
        prop.addChangeListener(WeakListeners.change(cl, prop));
        cl.stateChanged(new ChangeEvent(prop));
    }
    private final CL cl = new CL();

    class CL implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            ToolProperty<Integer, ?> p = (ToolProperty<Integer, ?>) e.getSource();
            value.setLabel("" + p.get());
        }
    }
}
