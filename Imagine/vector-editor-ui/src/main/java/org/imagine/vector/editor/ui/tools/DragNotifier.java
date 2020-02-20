/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 * Notified by DMA to allow the shape widget to replace its contents with a
 * temporary copy during control point drag operations, so the shapes model is
 * not actually modified until the operation completes.
 *
 * @author Tim Boudreau
 */
interface DragNotifier {

    ShapeElement onStartControlPointDrag();

    void onEndControlPointDrag();

    void onControlPointDragUpdate(int controlPointIndex, double x, double y);
}
