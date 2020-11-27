/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.spi;

import com.mastfrog.util.collections.DoubleMap;
import com.mastfrog.util.collections.IntMap;
import java.awt.Shape;
import java.util.function.Consumer;
import com.mastfrog.geometry.CornerAngle;
import com.mastfrog.geometry.LineVector;
import com.mastfrog.geometry.Polygon2D;
import com.mastfrog.geometry.RotationDirection;
import com.mastfrog.geometry.analysis.VectorVisitor;

/**
 * Caches info derived from a shape about its interior angles and other
 * expensive-to-compute information.
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

    public void forEachPoint(Consumer<PointInfo> c) {
        map.forEachValue(c);
    }

    public boolean hasInfo(int pointIndex) {
        return map.containsKey(pointIndex);
    }

    private DoubleMap<PointInfo> infoForCornerAngle;
    private DoubleMap<PointInfo> infoForTrailingLineAngle;

    public PointInfo forTrailingLineAngle(double ang) {
        if (infoForTrailingLineAngle == null) {
            infoForTrailingLineAngle = DoubleMap.create(map.size());
            map.forEachValue(pi -> {
                infoForTrailingLineAngle.put(pi.angle.trailingAngle(), pi);
            });
        }
        DoubleMap.Entry<? extends PointInfo> e
                = infoForTrailingLineAngle.nearestValueTo(
                        ang, 0.00000000001D);
        PointInfo result = e == null ? null : e.value();
//        if (result == null) {
//            System.out.println("No angle "
//                    + GeometryStrings.toDegreesString(ang)
//                    + " in " + infoForTrailingLineAngle.keySet());
//        }
        return result;
    }

    public PointInfo forCornerAngle(CornerAngle ang) {
        return ShapeInfo.this.forCornerAngle(ang.encodeSigned());
    }

    public PointInfo forCornerAngle(double angle) {
        if (infoForCornerAngle == null) {
            infoForCornerAngle
                    = DoubleMap.create(map.size());
            map.forEachValue(pi -> {
                infoForCornerAngle.put(pi.angle.encodeSigned(), pi);
            });
        }
        DoubleMap.Entry<? extends PointInfo> e
                = infoForCornerAngle.nearestValueTo(angle,
                        0.00000000001D);
        PointInfo result = e == null ? null : e.value();
//        if (result == null) {
//            System.out.println("No angle "
//                    + GeometryStrings.toDegreesString(angle)
//                    + " in " + infoForCornerAngle.keySet());
//        }
        return result;
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
            map.put(pointIndex, new PointInfo(pointIndex,
                    vect, ang, subpathIndex,
                    subpathRotationDirection, prevPointIndex,
                    nextPointIndex));
        });
        map.trim();
        return new ShapeInfo(map);
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
