package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import net.java.dev.imagine.api.customizers.Explicit;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.State;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class RowWidget<T> extends Widget {

    private final Renderer<T> renderer;
    private final Explicit<T> items;
    private final Widget inner;
    private final Widget leftButton;
    private final Widget rightButton;
    private final SelectionHandler<T> sel;

    RowWidget(Scene scene, Renderer<T> renderer, Explicit<T> items, SelectionHandler<T> sel, int scrollBy) {
        super(scene);
        this.renderer = renderer;
        this.items = items;
        this.sel = sel;
        inner = new Widget(scene);
        leftButton = new LabelWidget(scene, "<");
        rightButton = new LabelWidget(scene, ">");
        inner.setCheckClipping(true);
        inner.setLayout(new L());
        leftButton.getActions().addAction(new ScrollAction(scrollBy));
        rightButton.getActions().addAction(new ScrollAction(-scrollBy));
        addChild(leftButton);
        addChild(inner);
        addChild(rightButton);
        setLayout(new BL()); //XXX
        sync();
    }
    
    class ViewWidget extends Widget {
        int scrollPosition = 0;
        ViewWidget() {
            super(RowWidget.this.getScene());
            setLayout(new L());
        }
        
        void scrollLeft() {
            scrollPosition --;
        }
        
        void scrollRight() {
            scrollPosition++;
        }
    }
    
    class BL implements Layout {

        @Override
        public void layout(Widget widget) {
            Rectangle o = new Rectangle(0, 0, 100, 30); //widget.getPreferredBounds();
            o.width = Math.min(o.width, 100);
            
            Rectangle r = leftButton.getPreferredBounds();
            r.height = o.height;
            leftButton.resolveBounds(new Point(0,0), r);
            int w = r.width;
            
            r = rightButton.getPreferredBounds();
            r.height = o.height;
            rightButton.resolveBounds(new Point(o.width - r.width, 0), r);
            
            Rectangle x = inner.getPreferredBounds();
            x.width = Math.min (x.width, o.width - (w + r.width));
            Point p = inner.getPreferredLocation();
            if (p == null) {
                p = new Point(w, 0);
            }
            inner.resolveBounds(p, x);
        }

        @Override
        public boolean requiresJustification(Widget widget) {
            return false;
        }

        @Override
        public void justify(Widget widget) {
            //do nothing
        }
        
    }

    class ScrollAction extends WidgetAction.Adapter implements ActionListener {

        private final int direction;
        private final Timer timer = new Timer(50, this);

        public ScrollAction(int direction) {
            this.direction = direction;
            timer.setRepeats(true);
        }

        boolean scroll() {
            Point p = inner.getPreferredLocation();
            if (p == null) {
                p = new Point(0,0);
            }
            p.x += direction;
            inner.setPreferredLocation(p);
//            RowWidget.this.revalidate();
            getScene().validate();
            return true;
        }

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent event) {
            timer.start();
            return State.createLocked(widget, this);
        }

        @Override
        public State mouseReleased(Widget widget, WidgetMouseEvent event) {
            timer.stop();
            return State.CONSUMED;
        }

        @Override
        public State mouseClicked(Widget widget, WidgetMouseEvent event) {
            scroll();
            return State.CONSUMED;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            scroll();
        }
    }

    private void sync() {
        inner.removeChildren();
        for (T obj : items.getValues()) {
            inner.addChild(new CellWidget(getScene(), obj, sel, renderer));
        }
    }
    
    class L implements Layout {

        @Override
        public void layout(Widget widget) {
            int x = 0;
            for (Widget w : widget.getChildren()) {
                CellWidget<T> c = (CellWidget<T>) w;
                Dimension sz = renderer.getSize(c.item);
                c.resolveBounds(new Point(x, 0), null);
                x += sz.width;
            }
        }

        @Override
        public boolean requiresJustification(Widget widget) {
            return false;
        }

        @Override
        public void justify(Widget widget) {
            //do nothing
        }
        
    }
}
