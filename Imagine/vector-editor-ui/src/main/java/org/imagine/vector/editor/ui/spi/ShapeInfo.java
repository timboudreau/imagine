/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.spi;

import com.mastfrog.util.collections.IntMap;
import java.awt.Shape;
import org.imagine.geometry.CornerAngle;
import org.imagine.geometry.LineVector;
import org.imagine.geometry.Polygon2D;
import org.imagine.geometry.RotationDirection;
import org.imagine.geometry.analysis.VectorVisitor;

/**
 *
 * @author Tim Boudreau
 */
public final class ShapeInfo {

    private final IntMap<PointInfo> map;

    private ShapeInfo(IntMap<PointInfo> map) {
        this.map = map;
    }

    public int size() {
        return map.size();
    }

    public PointInfo forIndex(int ix) {
        return map.valueAt(ix);
    }

    public PointInfo forControlPoint(int pt) {
        return map.get(pt);
    }

    public static ShapeInfo create(Shape shape) {
        IntMap<PointInfo> map = IntMap.create(20);
        VectorVisitor.analyze(shape, (int pointIndex, LineVector vect,
                int subpathIndex, RotationDirection subpathRotationDirection,
                Polygon2D approximate, int prevPointIndex, int nextPointIndex) -> {
            CornerAngle ang = vect.corner();
            map.put(pointIndex, new PointInfo(pointIndex, vect, ang, subpathIndex, subpathRotationDirection, prevPointIndex, nextPointIndex));
        });
        map.trim();
        return null;
    }

    public static final class PointInfo {

        public final int controlPointIndex;
        public final int previousControlPointIndex;
        public final int nextControlPointIndex;
        public final LineVector vector;
        public final CornerAngle angle;
        public final int subpath;
        public final RotationDirection direction;

        public PointInfo(int pointIndex, LineVector vector, CornerAngle angle, int subpath, RotationDirection direction,
                int prevPointIndex, int nextPointIndex) {
            this.controlPointIndex = pointIndex;
            this.previousControlPointIndex = prevPointIndex;
            this.nextControlPointIndex = nextPointIndex;
            this.vector = vector;
            this.angle = angle;
            this.subpath = subpath;
            this.direction = direction;
        }

    }
}
