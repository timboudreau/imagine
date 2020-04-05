/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import com.mastfrog.function.DoubleQuadConsumer;
import org.imagine.geometry.util.GeometryUtils;
import com.mastfrog.function.state.Int;

/**
 * Interface with default implementations for shapes which can count how many
 * times they are intersected by other shapes; used for computing interiority
 * with some shapes.
 *
 * @author Tim Boudreau
 */
public interface Intersectable {

    void visitLines(DoubleQuadConsumer consumer, boolean includeClose);

    default boolean intersectsSegment(Intersectable inter, boolean includeClose) {
        return intersectionCount(inter, true) > 0;
    }

    default boolean intersectsSegment(Intersectable inter) {
        return intersectionCount(inter, true) > 0;
    }

    default int intersectionCount(Intersectable other, boolean includeClose) {
        if (other == this) {
            return -1;
        }
        Int result = Int.create();
        visitLines((ax, ay, bx, by) -> {
            other.visitLines((cx, cy, dx, dy) -> {
                if (GeometryUtils.linesIntersect(ax, ay, bx, by, cx, cy, dx, dy, false)) {
                    result.increment();
                }
            }, includeClose);
        }, includeClose);
        return result.get();
    }
}
