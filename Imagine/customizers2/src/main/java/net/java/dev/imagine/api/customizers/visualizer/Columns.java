package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.visual.layout.Layout;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class Columns {

    private final Set<ColumnPositionProvider> all = new HashSet<ColumnPositionProvider>();
    private final Map<Widget, Set<ColumnPositionProvider>> forOwner = new HashMap<Widget, Set<ColumnPositionProvider>>();
    private final int gap;

    Columns(int gap) {
        this.gap = gap;
    }

    void add(ColumnPositionProvider p, Widget owner) {
        all.add(p);
        p.setGap(gap);
        Set<ColumnPositionProvider> s = forOwner.get(owner);
        if (s == null) {
            s = new HashSet<ColumnPositionProvider>();
        }
        s.add(p);
        forOwner.put(owner, s);
    }

    void add(Widget w, Widget owner) {
        ColumnPositionProvider p = new ColumnPositionProvider(w);
        add(p, owner);
    }

    public int getColumnPosition(int column) {
        int result = 0;
        for (int c = 0; c < column; c++) {
            int w = 0;
            for (ColumnPositionProvider col : all) {
                if (col.isExpandableWidget()) {
                    continue;
                }
                w = Math.max(w, col.getWidth(c));
            }
            if (w > 0) {
                result += w + gap;
            }
        }
        return result;
    }

    interface Provider {

        public Columns getColumns();
    }

    Layout createLayout() {
        return new L();
    }

    void clear(Widget owner) {
        Set<ColumnPositionProvider> s = forOwner.remove(owner);
        if (s != null) {
            all.removeAll(s);
        }
        for (Iterator<ColumnPositionProvider> it = all.iterator(); it.hasNext();) {
            ColumnPositionProvider p = it.next();
            if (p.isProviderFor(owner)) {
                it.remove();
            }
        }
    }

    class L implements Layout {
//
//        Color[] colors = new Color[]{
//            Color.YELLOW, Color.CYAN, Color.GREEN, Color.LIGHT_GRAY, Color.ORANGE
//        };

        @Override
        public void layout(Widget widget) {
            Widget[] w = widget.getChildren().toArray(new Widget[0]);

            int maxHeight = 0;
            for (int i = 0; i < w.length; i++) {
                if (!w[i].isVisible()) {
                    continue;
                }
//                w[i].setOpaque(true);
//                w[i].setBackground(colors[i % colors.length]);
                maxHeight = Math.max(maxHeight, w[i].getPreferredBounds().height);
            }

//            System.out.println("Height for " + widget + " " + maxHeight);


            for (int i = 0; i < w.length; i++) {
                if (!w[i].isVisible()) {
                    w[i].resolveBounds(new Point(0, 0), new Rectangle());
                    continue;
                }
                int pos = getColumnPosition(i);
                Rectangle preferredBounds = w[i].getPreferredBounds();
                int x = preferredBounds.x;
                int y = preferredBounds.y;
                int width = preferredBounds.width;
                int height = preferredBounds.height;
                int lx = pos - x;
                int ly = -y;
                if (height < maxHeight) {
                    ly = (maxHeight - height / 2);
                }
                w[i].resolveBounds(new Point(lx, ly), new Rectangle(x, y, width, height));
            }
        }

        @Override
        public boolean requiresJustification(Widget widget) {
            return false;
        }

        @Override
        public void justify(Widget widget) {
            //already done
        }
    }
}
