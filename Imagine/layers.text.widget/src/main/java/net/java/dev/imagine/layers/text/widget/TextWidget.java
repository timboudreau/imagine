/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.layers.text.widget;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import net.java.dev.imagine.layers.text.widget.api.Text;
import net.java.dev.imagine.layers.text.widget.api.TextItems;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.TextFieldInplaceEditor;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.components.TextWrapLabelUI;
import org.imagine.utils.java2d.GraphicsUtils;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 * A variant on a label widget for use in a text layer.  Wraps text at a 
 * predefined point.
 *
 * @author Tim Boudreau
 */
final class TextWidget extends LabelWidget implements PropertyChangeListener, TextFieldInplaceEditor, MoveProvider, LookupListener {

    final Text text;
    private final Lookup.Result<TextItems> activeLayerResult = Utilities.actionsGlobalContext().lookupResult(TextItems.class);
    private final WidgetAction editAction = TextAreaInplaceEditorProvider.create(this);
    private final WidgetAction moveAction = ActionFactory.createMoveAction(ActionFactory.createFreeMoveStrategy(), this);
    private final TextLayer layer;

    //TODO: Modify TextWrapLabelUI to take a wrap char point parameter
    @SuppressWarnings(value = "LeakingThisInConstructor")
    TextWidget(TextLayer layer, Widget parent, Text text) {
        super(parent.getScene());
        this.layer = layer;
        this.text = text;
        text.addPropertyChangeListener(WeakListeners.propertyChange(this, text));
        setPreferredLocation(text.getLocation());
        setFont(text.getFont());
        activeLayerResult.addLookupListener(this);
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
        System.out.println("Got property change from " + text + " - " + evt.getPropertyName());
        setFont(text.getFont());
        setLabel(text.getText());
        setPreferredLocation(text.getLocation());
        revalidate();
        if (Text.PROP_FONT.equals(evt.getPropertyName())) {
            getScene().validate();
        }
    }

    @Override
    protected Rectangle calculateClientArea() {
        Font f = getFont();
        FontMetrics fm = getGraphics().getFontMetrics(f);
        Dimension result = TextWrapLabelUI.doPaint(null, new Insets(0, 0, 0, 0), text.getText(), fm, text.getPaint(), f, 1.0);
        //XXX get AffineTransform from Text
        return new Rectangle(new Point(), result);
    }

    @Override
    protected void paintWidget() {
        Font f = getFont();
        Graphics2D g = getGraphics();
        FontMetrics fm = g.getFontMetrics(f);
        Composite old = g.getComposite();
        if (layer.getOpacity() != 1.0F) {
            Composite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.getOpacity());
            if (layer.getComposite() == null) {
                g.setComposite(alpha);
            } else {
                g.setComposite(GraphicsUtils.combine(g.getComposite(), alpha));
            }
        } else if (layer.getComposite() != null) {
            g.setComposite(layer.getComposite());
        }
        try {
            TextWrapLabelUI.doPaint(getGraphics(), new Insets(0, 0, 0, 0), text.getText(), fm, text.getPaint(), f, 1.0);
        } finally {
            g.setComposite(old);
        }
        //XXX this will cause problems when saving as image
        if (layer.isSelected(text)) {
            g.setPaint(new Color(255, 220, 130, 128));
            g.drawRect(0, 0, getBounds().width, getBounds().height);
        }
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
        Lookup.Result<TextItems> r = le == null ? activeLayerResult : (Lookup.Result<TextItems>) le.getSource();
        boolean enableActions = r.allInstances().contains(layer.getItems());
        if (enableActions) {
            attachActions();
        } else {
            detachActions();
        }
    }

    void edit() {
        MouseEvent me = new MouseEvent(getScene().getView(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 2, false, MouseEvent.BUTTON1);
        WidgetMouseEvent e = new WidgetMouseEvent(0, me);
        editAction.mouseClicked(this, e);
    }
}
