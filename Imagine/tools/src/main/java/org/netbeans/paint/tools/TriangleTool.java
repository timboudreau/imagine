/*
 * OvalTool.java
 *
 * Created on September 29, 2006, 4:05 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import static org.netbeans.paint.tools.RectangleTool.strokeC;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.Triangle2D;
import org.imagine.geometry.util.PooledTransform;
import org.imagine.help.api.HelpItem;
import org.imagine.help.api.annotations.Help;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Triangle", iconPath = "org/netbeans/paint/tools/resources/triangle.svg")
@Tool(value = Surface.class, toolbarPosition = 300)
@Help(id = "TriangleTool", related = {"OvalTool", "RectangleTool", "CircleTool", "RhombusTool", "RoundRectangleTool"}, content = {
    @Help.HelpText(language = "en", country = "US",
            value = "# Triangle Tool\n\n"
            + "The rectangle tool allows you to draw triangles;  its customizer lets you "
            + "configure the fill and outline painting style, and the line stroke the outline"
            + "is drawn with (if any).\n\n"
            + "Holding down Shift while dragging rotates the created triangle.")})
public class TriangleTool extends RectangleTool {

    private double rotation;

    public TriangleTool(Surface surf) {
        super(surf);
    }

    @Override
    public HelpItem getHelpItem() {
        return HelpItems.TriangleTool;
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(TriangleTool.class, "Triangle");
    }

    @Override
    protected void draw(EnhRectangle2D toPaint, Graphics2D g2d, PaintingStyle style) {
        Triangle2D tri = new Triangle2D(toPaint.getCenterX(), toPaint.getY(),
                toPaint.getX(), toPaint.getY() + toPaint.getHeight(),
                toPaint.getX() + toPaint.getWidth(), toPaint.getY() + toPaint.getHeight());
        if (rotation != 0D) {
            PooledTransform.withRotateInstance(Math.toRadians(rotation), tri::applyTransform);
        }
        if (style.isFill()) {
            g2d.setPaint(paintC.get().getPaint());
            g2d.fill(tri);
        }
        if (style.isOutline()) {
            if (toPaint == TEMPLATE_RECTANGLE) {
                g2d.setStroke(scaledStroke());
            } else {
                g2d.setStroke(strokeC.get());
            }
            g2d.setColor(outlineC.get());
            g2d.draw(tri);
        }
    }

    private Point2D startPoint;

    @Override
    public void mousePressed(double x, double y, MouseEvent e) {
        startPoint = new EqPointDouble(x, y);
        super.mousePressed(x, y, e);
    }

    @Override
    public void mouseDragged(double x, double y, MouseEvent e) {
        if (e.isShiftDown() && startPoint != null) {
            double dist = startPoint.distance(e.getPoint());
            rotation = dist;
            e.consume();
        }
        super.mouseDragged(x, y, e);
    }

    @Override
    public void mouseReleased(double x, double y, MouseEvent e) {
        super.mouseReleased(x, y, e);
        startPoint = null;
    }
}
