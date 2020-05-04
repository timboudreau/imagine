/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.paint.tools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.java.dev.imagine.api.image.Surface;
import net.dev.java.imagine.api.tool.aspects.NonPaintingTool;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import org.netbeans.paint.tools.spi.MouseDrivenTool;

/**
 *
 * @author Timothy Boudreau
 */
@ToolDef(name="Move", iconPath="org/netbeans/paint/tools/resources/move-layer.svg")
@Tool(value=Surface.class, toolbarPosition=2000)
public class MoveTool extends MouseDrivenTool implements KeyListener, NonPaintingTool, Attachable, PaintParticipant {

    //XXX make sensitive to Movable instead
    public MoveTool(Surface surf) {
        super(surf);
    }

    private Point startPoint = null;
    protected void beginOperation(Point p, int modifiers) {
        startPoint = p;
    }

    protected void dragged(Point p, int modifiers) {
        int xdiff = startPoint.x - p.x;
        int ydiff = startPoint.y - p.y;
        if (xdiff != 0 || ydiff != 0) {
            Point loc = surface.getLocation();
            loc.x -= xdiff;
            loc.y -= ydiff;
            surface.setLocation(loc);
            startPoint = p;
        }
    }

    protected void endOperation(Point p, int modifiers) {

    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (startPoint == null) {
            startPoint = new Point(0,0);
        }
        int amount = 1;
        if (e.isShiftDown()) {
            amount *= 2;
        }
        if (e.isControlDown()) {
            amount *= 4;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT :
                dragged(new Point(startPoint.x + amount, startPoint.y), 0);
                break;
            case KeyEvent.VK_LEFT :
                dragged(new Point(startPoint.x - amount, startPoint.y), 0);
                break;
            case KeyEvent.VK_UP :
                dragged(new Point(startPoint.x, startPoint.y - amount), 0);
                break;
            case KeyEvent.VK_DOWN :
                dragged(new Point(startPoint.x, startPoint.y + amount), 0);
                break;
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void attachRepainter(Repainter repainter) {
        super.attachRepainter(repainter);
        surface.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
        //do nothing
    }
}
