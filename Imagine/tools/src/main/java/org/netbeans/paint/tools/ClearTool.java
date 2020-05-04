/*
 * ClearTool.java
 *
 * Created on October 1, 2006, 1:57 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.netbeans.paint.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
@Tool(name="Clear", value=Surface.class, toolbarPosition = 1200)
@ToolDef(iconPath="org/netbeans/paint/tools/resources/clear.png")
public class ClearTool extends MouseAdapter implements /* Tool, */ KeyListener {
    private final Surface surface;

    public ClearTool(Surface surface) {
        this.surface = surface;
    }

    public JComponent getCustomizer(boolean create) {
        return null;
    }

    /*
    public String getInstructions() {
        return NbBundle.getMessage (getClass(), "Click_or_press_Enter_to_clear_the_canvas");
    }
    */

    public void mouseReleased (MouseEvent e) {
        go();
    }

    public void keyTyped(KeyEvent e) {
        //do nothing
    }

    public void keyPressed(KeyEvent e) {
        //do nothing
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            go();
        }
    }

    public String toString() {
        return NbBundle.getMessage (getClass(), "Clear");
    }

    private void go() {
        Graphics g = surface.getGraphics();
        g.setColor(new Color (255, 255, 255, 0));
        Rectangle r = new Rectangle (surface.getLocation(), surface.getSize());
        g.fillRect(r.x, r.y, r.width, r.height);
        g.dispose();
    }

    public Lookup getLookup() {
        return Lookups.singleton(this);
    }
}
