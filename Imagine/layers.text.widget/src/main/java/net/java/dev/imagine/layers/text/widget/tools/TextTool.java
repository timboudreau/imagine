package net.java.dev.imagine.layers.text.widget.tools;

import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.layers.text.widget.api.Text;
import net.java.dev.imagine.layers.text.widget.api.TextItems;

/**
 * A tool which is sensitive to TextItems in the layer lookup.
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Text", iconPath = "net/java/dev/imagine/layers/text/widget/tools/text.png", category = "text")
@Tool(TextItems.class)
public class TextTool extends MouseAdapter {

    private final TextItems items;

    public TextTool(TextItems items) {
        this.items = items;
    }

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
}
