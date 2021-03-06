package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.geom.Point2D;

/**
 *
 * @author Tim Boudreau
 */
public class SnapPointsBuilder<T> {

    private final DoubleMap<T> xs = DoubleMap.create(128);
    private final DoubleMap<T> ys = DoubleMap.create(128);

    private final DoubleMap<T> xDists = DoubleMap.create(32);
    private final DoubleMap<T> yDists = DoubleMap.create(32);

    private final DoubleMap<T> angles = DoubleMap.create(16);
    private final DoubleMap<T> corners = DoubleMap.create(16);
    private final DoubleMap<T> extents = DoubleMap.create(16);
    private final DoubleMap<T> lengths = DoubleMap.create(16);

    private final double rad;
    private OnSnap notify;

    SnapPointsBuilder(double rad) {
        if (rad < 0) {
            throw new IllegalArgumentException("" + rad);
        }
        this.rad = rad;
    }

    public SnapPointsBuilder addExtent(double extent, T obj) {
        extents.put(extent, obj);
        return this;
    }

    public SnapPointsBuilder addLength(double length, T obj) {
        lengths.put(length, obj);
        return this;
    }

    public SnapPointsBuilder addAngle(double angle, T obj) {
        angles.put(angle, obj);
        return this;
    }

    public SnapPointsBuilder addCorner(double corner, T obj) {
        corners.put(corner, obj);
        return this;
    }

    public SnapPointsBuilder addDistance(SnapAxis axis, double distance, T obj) {
        DoubleMap set = axis == SnapAxis.Y ? yDists : xDists;
        set.put(distance, obj);
        return this;
    }

    public SnapPointsBuilder add(Point2D p2d, T obj) {
        add(SnapAxis.X, p2d.getX(), obj);
        add(SnapAxis.Y, p2d.getY(), obj);
        return this;
    }

    public SnapPointsBuilder notifying(OnSnap notify) {
        this.notify = notify;
        return this;
    }

    public void add(SnapAxis axis, double coord, T obj) {
        switch (axis) {
            case X:
                xs.put(coord, obj);
                break;
            case Y:
                ys.put(coord, obj);
                break;
            default:
                throw new AssertionError("Axis " + axis);
        }
    }

    public SnapPoints build() {
        return new SnapPoints(rad, notify, xs, ys, xDists,
                yDists, angles, corners, extents, lengths);
    }
}
