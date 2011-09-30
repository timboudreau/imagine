package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.State;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public final class CellWidget<T> extends Widget {

    final T item;
    private final Renderer<T> renderer;

    public CellWidget(Scene scene, final T item, final SelectionHandler<T> sel, final Renderer<T> renderer) {
        super(scene);
        assert sel != null;
        this.item = item;
        this.renderer = renderer;
        setCheckClipping(true);
        getActions().addAction(new WidgetAction.Adapter() {

            @Override
            public State mouseClicked(Widget widget, WidgetMouseEvent event) {
                assert sel != null;
                sel.itemSelected(item);
                return State.CONSUMED;
            }
        });
    }

    public T getItem() {
        return item;
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();
        renderer.paint(g, item, 0, 0);
        Rectangle r = getBounds();
        g.setColor(Color.BLACK);
//        g.drawRect(0, 0, r.width - 1, r.height - 1);
    }

    @Override
    protected Rectangle calculateClientArea() {
        Rectangle result = new Rectangle(0, 0);
        result.setSize(renderer.getSize(item));
        return result;
    }
}
