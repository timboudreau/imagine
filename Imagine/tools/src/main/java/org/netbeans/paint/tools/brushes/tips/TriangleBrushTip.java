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
package org.netbeans.paint.tools.brushes.tips;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import javax.swing.JComponent;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.Triangle;
import org.netbeans.paint.api.components.explorer.Customizable;
import org.netbeans.paint.tools.spi.BrushTip;

/**
 *
 * @author Timothy Boudreau
 */
public class TriangleBrushTip implements BrushTip, Customizable {

    public Rectangle draw(Graphics2D g, Point p, int size) {
        Rectangle r = new Rectangle();
        int[] xpoints = new int[]{p.x, p.x + (size / 2), p.x + size};
        int[] ypoints = new int[]{p.y, p.y + size, p.y};
        Polygon pol = new Polygon(xpoints, ypoints, 3);
        g.draw(pol);
        r.setLocation(p);
        r.width = size;
        r.height = size;
        return r;
    }

    @Override
    public boolean canEmit() {
        return true;
    }

    @Override
    public void emit(Point p, int size, ShapeEmitter em) {
        int half = size / 2;
        Triangle tri = new Triangle(p.x, p.y, p.x + half, p.y + size, p.x + size, p.y);
        em.emit(tri, PaintingStyle.OUTLINE);
    }

    public JComponent getCustomizer() {
        return null;
    }
}
