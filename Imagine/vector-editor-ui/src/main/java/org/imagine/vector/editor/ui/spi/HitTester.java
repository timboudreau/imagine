/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.spi;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
public interface HitTester {

    public int hits(Point2D point, Consumer<? super ShapeElement> c);

}
