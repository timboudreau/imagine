package org.imagine.vector.editor.ui.palette;

import org.netbeans.paint.api.components.TilingLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import org.netbeans.paint.api.components.TilingLayout.TilingPolicy;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
class PaletteItemsPanel<T> extends JPanel {

    private final AbstractTileFactory<T, ?> tileFactory;
    private static final Logger LOG = Logger.getLogger(PaletteItemsPanel.class.getName());

    @SuppressWarnings("LeakingThisInConstructor")
    PaletteItemsPanel(AbstractTileFactory<T, ?> tileFactory) {
        super(new TilingLayout(() -> {
            return tileFactory.getPreferredTileSize().width;
        }, TilingPolicy.FIXED_SIZE));
        this.tileFactory = tileFactory;
        this.tileFactory.attachPaletteComponent(this);
    }

    @Override
    public void reshape(int x, int y, int w, int h) {
        // Yes, it's deprecated; it's also the ONLY sure way
        // to intercept *every* call that changes the component
        // bounds.  Been there, got the t-shirt.
        super.reshape(x, y, w, h);
        invalidate();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        PaletteBackend<? extends T> storage = tileFactory.storage();
        if (storage != null) {
            storage.allNames((thrown, names) -> {
                if (thrown != null) {
                    LOG.log(Level.SEVERE, "Exception getting names for " + tileFactory, thrown);
                    Exceptions.printStackTrace(thrown);
                } else if (names != null) {
                    if (isDisplayable()) {
                        for (String nm : names) {
                            if (findTile(nm) == null) {
                                Tile<? super T> tile = tileFactory.createTile(nm, null);
                                addTile(tile);
                            }
                        }
                    } else {
                        LOG.log(Level.WARNING, "PaletteItemsPanel for {0} hidden before it could "
                                + "process {1}", new Object[]{tileFactory, names});
                    }
                } else {
                    LOG.log(Level.INFO, "PaletteItemsPanel for {0} got null names array",
                            tileFactory);
                }
            }).listen(l);
        }
    }

    private void addTile(Tile<? super T> tile) {
        add(tile);
        tile.setTransferHandler(tileFactory.getTransferHandler());
        invalidate();
        revalidate();
        repaint();
    }

    @Override
    public void removeNotify() {
        PaletteBackend<? extends T> storage = tileFactory.storage();
        if (storage != null) {
            storage.unlisten(l);
        }
        removeAll();
        super.removeNotify();
    }

    private Tile<? super T> findTile(String name) {
        for (Component c : getComponents()) {
            if (c instanceof Tile<?> && name.equals(c.getName())) {
                return (Tile<? super T>) c;
            }
        }
        return null;
    }

    private final L l = new L();

    class L implements Listener<T> {

        @Override
        public void onItemDeleted(String name) {
            Tile<? super T> tile = findTile(name);
            if (tile != null) {
                remove(tile);
                invalidate();
                revalidate();
                repaint();
            }
        }

        @Override
        public void onItemAdded(String name, T item) {
            LOG.log(Level.FINE, "Panel onItemAdded {0} with {1}", new Object[]{name, item});;
            Tile<? super T> tile = tileFactory.createTile(name, item);
            if (findTile(name) == null) {
                addTile(tile);
            }
        }

        @Override
        public void onItemChanged(String name, T item) {
            Tile<? super T> tile = findTile(name);
            if (tile != null) {
                if (item != null) {
                    tile.setItem(item);
                } else {
                    if (tile.isDisplayable()) {
                        tile.reload();
                    }
                }
            } else {
                onItemAdded(name, item);
            }
        }
    }

    static int compareComponents(Component ac, Component bc) {
        String a = ac.getName();
        String b = bc.getName();
        if (a == null || b == null) {
            return Integer.compare(ac.hashCode(), bc.hashCode());
        }
        return a.compareToIgnoreCase(b);
    }

    public static void main(String[] args) {
        JPanel pnl = new JP(new TilingLayout(() -> 96));
        for (int i = 0; i < 64; i++) {
            pnl.add(new TestTile());
        }
        JFrame jf = new JFrame("Tileit");
        jf.setContentPane(new JScrollPane(pnl));
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.pack();
        jf.setVisible(true);
    }

    static class JP extends JPanel {

        JP(LayoutManager m) {
            super(m);
        }

        public void reshape(int x, int y, int w, int h) {
            // Yes, it's deprecated; it's also the ONLY sure way
            // to intercept *every* call that changes the component
            // bounds.  Been there, got the t-shirt.
            super.reshape(x, y, w, h);
            invalidate();
        }
    }

    static class TestTile extends Tile<String> {

        @SuppressWarnings("OverridableMethodCallInConstructor")
        public TestTile() {
            super(Long.toString(System.nanoTime()));
            setToolTipText(getName());
            setBackground(randomColor());
            setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        }

        static Color randomColor() {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            return new Color(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255));
        }

        @Override
        protected PaletteStorage<String> storage() {
            return null;
        }
    }
}
