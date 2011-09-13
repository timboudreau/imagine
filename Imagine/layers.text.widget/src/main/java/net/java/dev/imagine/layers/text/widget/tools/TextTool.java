package net.java.dev.imagine.layers.text.widget.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import net.dev.java.imagine.spi.tools.Tool;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.layers.text.widget.api.Text;
import net.java.dev.imagine.layers.text.widget.api.TextItems;
import org.openide.util.Lookup;

/**
 * A tool which is sensitive to TextItems in the layer lookup.
 *
 * @author Tim Boudreau
 */
public class TextTool extends MouseAdapter implements Tool {

    private TextItems items;

    @Override
    public void mouseClicked(MouseEvent e) {
        //do nothing
        if (items != null) {
            Text t = items.hit(e.getPoint());
            if (t == null) {
                final Text txt = new Text.TextImpl();
                txt.setLocation(e.getPoint());
                items.add(txt);
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (items != null) {
                            items.edit(txt);
                        }
                    }
                    
                });
                e.consume();
            } else {
                items.edit(t);
                e.consume();
            }
        }
    }

    @Override
    public Icon getIcon() {
        return new Icon() { //XXX

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(Color.MAGENTA);
                g.fillRect(0, 0, 16, 16);
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    @Override
    public String getName() {
        return "Text";
    }

    @Override
    public void activate(Layer layer) {
        System.out.println("Activate on " + layer);
        setItems(layer.getLookup().lookup(TextItems.class));
    }

    @Override
    public void deactivate() {
        setItems(null);
    }

    @Override
    public Lookup getLookup() {
        return Lookup.EMPTY;
    }

    @Override
    public boolean canAttach(Layer layer) {
        System.out.println("Can attach " + layer);
        return layer.getLookup().lookup(TextItems.class) != null;
    }

    private void setItems(TextItems items) {
        this.items = items;
    }
}
