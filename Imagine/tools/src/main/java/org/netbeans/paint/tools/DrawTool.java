/*
 * DrawTool.java
 *
 * Created on September 27, 2006, 8:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.Constants;
import net.java.dev.imagine.api.toolcustomizers.Customizers;

/**
 *
 * @author Tim Boudreau
 */
@Tool(name = "Draw", value = Surface.class)
@ToolDef(iconPath = "org/netbeans/paint/tools/resources/draw.png", position = 1000)
public class DrawTool extends MouseAdapter implements CustomizerProvider {

    private final Customizer<Color> cust = Customizers.getCustomizer(Color.class, Constants.FOREGROUND);
    private final Surface surface;

    public DrawTool(Surface surface) {
        this.surface = surface;
    }
    Graphics2D g = null;
    private Point lastPoint = null;
    private float size = 5;

    public void setSize(float i) {
        this.size = i / 2;
        LINE_STROKE = new BasicStroke(i, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    public float getSize() {
        return size;
    }

    public void mousePressed(MouseEvent e) {
        g = surface.getGraphics();
        surface.beginUndoableOperation(toString());
        lastPoint = e.getPoint();
        assert g != null;
    }

    public void mouseReleased(MouseEvent e) {
        surface.endUndoableOperation();
        lastPoint = null;
        g = null;
    }
    private BasicStroke LINE_STROKE = new BasicStroke(8);

    public void mouseDragged(MouseEvent e) {
        if (g == null) {
            return;
        }
        Point p = e.getPoint();
        Color c = cust.get();
        if (c == null) c = Color.BLACK;
        g.setColor(c);
//        if (lastPoint != null && (Math.abs(lastPoint.x - p.x) > size * 2 ||  Math.abs (lastPoint.y - p.y) > size * 2)) {
        if (lastPoint != null) {
            g.setStroke(LINE_STROKE);
            g.drawLine(lastPoint.x, lastPoint.y, p.x, p.y);
        }
        lastPoint = p;
        int half = Math.max(1, (int) (getSize() / 2));
        int sz = (int) size;
        g.fillOval(p.x - half, p.y - half, sz, sz);
//        pc.repaint (p.x - half, p.y - half, sz, sz);
    }

    public Customizer getCustomizer() {
        return cust;
    }
    /*
    public String getInstructions() {
    return NbBundle.getMessage (getClass(), "Click_and_drag_to_draw");
    }
     */
}
