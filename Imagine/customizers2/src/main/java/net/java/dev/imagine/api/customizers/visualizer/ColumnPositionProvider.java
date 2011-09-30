package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Rectangle;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class ColumnPositionProvider {

    private final Widget widget;
    private int gap;

    public ColumnPositionProvider(Widget widget) {
        this.widget = widget;
    }
    
    boolean isExpandableWidget() {
        return widget instanceof ExpandableWidget;
    }

    void setGap(int gap) {
        this.gap = gap;
    }

    public int getWidth(int col) {
        List<Widget> kids = widget.getChildren();
        if (col < kids.size()) {
            Widget w = kids.get(col);
            Rectangle r = null;
            if (w instanceof ExpandableWidget) {
                return 0;
            }
            try {
                r = w.getPreferredBounds();
            } catch (NullPointerException e) {
//                e.printStackTrace();
                Logger.getAnonymousLogger().log(Level.SEVERE, "NPE on " + widget + " visible? " + widget.isVisible() + " really? " + reallyVisible(widget), e);
            }
            return r == null ? 0 : r.width;
        }
        return 0;
    }
    
    boolean reallyVisible(Widget w) {
        boolean result = w.isVisible();
        while (w != null) {
            w = w.getParentWidget();
            if (w instanceof Scene) {
                break;
            }
            if (w == null) {
                result = false;
            } else {
                result = w.isVisible();
            }
        }
        return result;
    }

    public boolean isProviderFor(Widget w) {
        return w == widget;
    }
}
