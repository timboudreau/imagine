package net.dev.java.imagine.spi.tool.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import org.openide.util.NbBundle.Messages;

/**
 * A quick demo, for now
 *
 * @author Tim Boudreau
 */
@ToolDef(category="demos", name="demo", position=Integer.MIN_VALUE)
@Tool(Graphics2D.class)
@Messages(value={"demo=Demo", "demos=Demos"})
public class DemoTool extends MouseAdapter {
    private final Graphics2D graphics;

    public DemoTool(Graphics2D graphics) {
        this.graphics = graphics;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        graphics.setColor(Color.BLUE);
        graphics.fillRect(e.getX(), e.getY(), 2, 2);
    }
}
