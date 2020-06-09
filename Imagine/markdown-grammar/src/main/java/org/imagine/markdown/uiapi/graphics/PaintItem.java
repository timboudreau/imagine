/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.markdown.uiapi.graphics;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Tim Boudreau
 */
public interface PaintItem {

    default void fetchBounds(Rectangle2D into) {
        // do nothing
    }

    void paint(Graphics2D g);

}
