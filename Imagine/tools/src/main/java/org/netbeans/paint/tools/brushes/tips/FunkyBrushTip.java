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
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.paint.api.components.explorer.Customizable;
import org.netbeans.paint.tools.spi.BrushTip;

/**
 *
 * @author Timothy Boudreau
 */
public class FunkyBrushTip implements BrushTip, Customizable {
    
    public FunkyBrushTip() {
    }

    public Rectangle draw (Graphics2D g, Point p, int size) {
        int half = size / 2;
        g.drawOval(p.x - half, p.y - half, p.x + size, p.y + size);
        return new Rectangle (p.x - half, p.y - half, p.x + size, p.y + size);
    }

    public JComponent getCustomizer() {
        //XXX customize offsets
	return null;
    }
}
