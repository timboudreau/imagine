package org.netbeans.paint.tools.path;

import java.awt.geom.Rectangle2D;
import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.path.PointKind;

/**
 *
 * @author Tim Boudreau
 */
class Pt extends EnhPoint2D {

    final int index;
    final PathModel.Entry owner;

    public Pt(int index, PathModel.Entry owner) {
        this.index = index;
        this.owner = owner;
        if (index < 0 || index >= owner.kind().pointCount()) {
            throw new IllegalArgumentException("Bad point index " + index
                    + " of " + owner.kind().pointCount() + " for "
                    + owner.kind());
        }
    }

    public <T extends Rectangle2D> T addSiblingsToBounds(T rect) {
        for (int i = 0; i < owner.points.length; i += 2) {
            rect.add(owner.points[i], owner.points[i + 1]);
        }
        return rect;
    }

    Pt next() {
        if (isDestination()) {
            int elIx = owner.index();
            if (elIx < owner.model().size()) {
                return owner.model().entries.get(elIx + 1).pointsIterator().next();
            } else {
                return owner.model().firstPoint();
            }
        } else {
            return new Pt(index + 1, owner);
        }
    }

    Pt prev() {
        if (index == 0) {
            int elIx = owner.index();
            if (elIx == 0) {
                return owner.model().firstPoint();
            }
            return owner.model().entries.get(elIx - 1).destination();
        }
        return new Pt(index - 1, owner);
    }

    public boolean isFirstPointInModel() {
        return index == 0 && owner().index() == 0;
    }

    @Override
    public boolean isDestination() {
        return kind().isDestination();
    }

    public Pt configureLineToSiblings(EqLine line, Runnable painter) {
        switch (owner.kind()) {
            case CLOSE:
            case MOVE:
            case LINE:
                break;
            default:
                if (isDestination()) {
                    line.x1 = getX();
                    line.y1 = getY();
                    for (int i = 0; i < owner.points.length - 2; i += 2) {
                        line.x2 = owner.points[i];
                        line.y2 = owner.points[i + 1];
                        painter.run();
                    }
                }
        }
        return this;
    }

    @Override
    public PathModel.Entry owner() {
        return owner;
    }

    @Override
    public PointKind kind() {
        return owner.kind.pointKindFor(index);
    }

    @Override
    public double getX() {
        return owner.points[index * 2];
    }

    @Override
    public double getY() {
        return owner.points[(index * 2) + 1];
    }

    @Override
    public void setLocation(double x, double y) {
        double oldX = owner.points[index * 2];
        double oldY = owner.points[(index * 2) + 1];
        if (oldX != x || oldY != y) {
            owner.points[index * 2] = x;
            owner.points[(index * 2) + 1] = y;
            owner.model().onChange(oldX, oldY);
            owner.model().onChange(x, y);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        }
        if (o instanceof Pt) {
            Pt other = (Pt) o;
            return other.index == index && other.owner() == owner();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 73 * index + (47 * owner.hashCode());
    }
}
