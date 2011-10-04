package net.java.dev.imagine.api.customizers.visualizer.popup;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import net.java.dev.imagine.api.properties.Explicit;
import net.java.dev.imagine.api.customizers.visualizer.CellWidget;
import net.java.dev.imagine.api.customizers.visualizer.Renderer;
import net.java.dev.imagine.api.customizers.visualizer.SelectionHandler;
import net.java.dev.imagine.api.customizers.visualizer.popup.ScenePopupSupport.PopupCallback;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class PopupScene<T> extends Scene {

    private final Renderer<T> renderer;
    private final SelectionHandler<T> sel;
    private final Explicit<T> data;
    private final LabelWidget info = new LabelWidget(this, "Description");

    public PopupScene(Renderer<T> renderer, final SelectionHandler<T> sel, Explicit<T> data) {
        this(renderer, sel, data, Integer.MAX_VALUE);
    }

    public PopupScene(Renderer<T> renderer, final SelectionHandler<T> sel, Explicit<T> data, int maxColumns) {
        this.renderer = renderer;
        this.sel = sel;
        setOpaque(true);
        this.data = data;
        setLayout(new Grid());
        info.setVerticalAlignment(LabelWidget.VerticalAlignment.CENTER);
        info.setAlignment(LabelWidget.Alignment.RIGHT);
        addChild(info);
    }

    void update() {
        removeChildren();
        for (T t : data.getValues()) {
            final CellWidget<T> widget = new CellWidget<T>(this, t, sel, renderer);
            addChild(widget);
            widget.setToolTipText(t.toString());
        }
        addChild(info);
    }

    public void setDescription(String desc) {
        info.setLabel(desc);
        validate();
    }
    private final ScenePopupSupport supp = new ScenePopupSupport(this, new PopupCallback() {

        @Override
        public void widgetSelected(Widget widget) {
            System.out.println("widget selected " + widget);
            if (widget instanceof CellWidget) {
                sel.itemSelected(((CellWidget<T>) widget).getItem());
            } else if (widget == null) {
                sel.itemSelected(null);
            } else {
                System.out.println("odd selection " + widget);
            }
        }

        @Override
        public void onShowPopup() {
            update();
        }
    });

    public WidgetAction createAction() {
        return supp.createAction();
    }
    int maxColumns = Integer.MAX_VALUE;

    private class Grid implements Layout {

        int gap = 5;

        @Override
        public void layout(Widget widget) {
            int sz = data.getValues().size();
            if (sz == 0) {
                return;
            }
            int w = sz;
            int h = 1;
            if (sz > 5) {
                w = (int) Math.ceil(Math.sqrt(sz));
                h = sz / w;
            }
            if (maxColumns != Integer.MAX_VALUE) {
                w = maxColumns;
            }
            int x = 0;
            int y = -1;
            List<Widget> kids = widget.getChildren();
            int max = kids.size();
            int ht = 0;
            boolean partialRow = false;
            int maxX = 0;
            for (int i = 0; i < max; i++) {
                Widget ww = kids.get(i);
                if (ww == info) {
                    continue;
                }
                CellWidget<T> child = (CellWidget<T>) ww;
                if (i == 0 || (i % w) == 0) {
                    partialRow = false;
                    y += ht + gap;
                    x = 0;
                    ht = 0;
                    for (int j = i; j < i + w; j++) {
                        if (j >= sz) {
                            break;
                        }
                        Widget check = kids.get(j);
                        CellWidget<T> cw = (CellWidget<T>) check;
                        ht = Math.max(ht, renderer.getSize(cw.getItem()).height);
                    }
                    System.out.println("row at " + i + " ht " + ht);
                } else {
                    partialRow = true;
                }
                child.resolveBounds(new Point(x, y), null);
                x += renderer.getSize(child.getItem()).width;
                maxX = Math.max(maxX, x);
            }
            x = 0;
            y += ht;
            Rectangle r = info.getPreferredBounds();
            r.x = 0;
            r.y = 0;
            r.width = Math.min(maxX, r.width);
            info.resolveBounds(new Point(x, y), r);
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
