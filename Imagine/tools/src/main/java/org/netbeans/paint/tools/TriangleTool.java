/*
 * OvalTool.java
 *
 * Created on September 29, 2006, 4:05 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.geometry.Triangle;
import static org.netbeans.paint.tools.RectangleTool.strokeC;
import org.imagine.editor.api.PaintingStyle;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Triangle", iconPath = "org/netbeans/paint/tools/resources/triangle.png")
@Tool(Surface.class)
public class TriangleTool extends RectangleTool {

    private double rotation;

    public TriangleTool(Surface surf) {
        super(surf);
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(TriangleTool.class, "Triangle");
    }

    @Override
    protected void draw(Rectangle toPaint, Graphics2D g2d, PaintingStyle style) {
        Triangle tri = new Triangle(toPaint.getCenterX(), toPaint.getY(),
                toPaint.getX(), toPaint.getY() + toPaint.getHeight(),
                toPaint.getX() + toPaint.getWidth(), toPaint.getY() + toPaint.getHeight());
        if (rotation != 0D) {
            tri.applyTransform(AffineTransform.getRotateInstance(Math.toRadians(rotation),
                    toPaint.getCenterX(), toPaint.getCenterY()));
        }
        if (style.isFill()) {
            g2d.setPaint(paintC.get().getPaint());
            g2d.fill(tri);
        }
        if (style.isOutline()) {
            g2d.setStroke(new BasicStroke(strokeC.get()));
            g2d.setColor(outlineC.get());
            g2d.draw(tri);
        }
    }

    private Point2D startPoint;

    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = e.getPoint();
        super.mousePressed(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (e.isShiftDown() && startPoint != null) {
            double dist = startPoint.distance(e.getPoint());
            rotation = dist;
            e.consume();
        }
        super.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        startPoint = null;
    }
}
