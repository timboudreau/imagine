package org.netbeans.paint.tools.responder;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Interface a Responder can implement in order to be called for painting on
 * input events.
 */
public interface PaintingResponder {

    Rectangle paint(Graphics2D g, Rectangle bounds);
}
