package org.netbeans.paint.tools.geom;

import java.awt.geom.Point2D;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqPointDouble;

/**
 *
 * A rectangle which keeps an always-positive width and height, and which can be
 * adjusted by changing the location of any corner (and if the transformation by
 * adjusting a corner changes the logical corner being dragged, it will say so).
 */
public final class MutableRectangle2D extends EnhRectangle2D {

    public static final int NONE = -2;
    public static final int ANY = 0;
    public static final int NE = 1;
    public static final int SE = 2;
    public static final int NW = 3;
    public static final int SW = 4;

    public MutableRectangle2D(double x1, double y1, double x2, double y2) {
        super(genRectangle(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2)));
    }

    public MutableRectangle2D(Point2D nw, Point2D se) {
        super(genRectangle(nw, se));
    }

    public static String cornerString(int corner) {
        switch (corner) {
            case NONE:
                return "None";
            case ANY:
                return "Any";
            case NE:
                return "Northeast";
            case SE:
                return "Southeast";
            case NW:
                return "Northwest";
            case SW:
                return "Southwest";
            default:
                return "Unknown(" + corner + ")";

        }
    }

    /**
     * Given two points, generate a rectangle with positive width and height
     */
    private static EnhRectangle2D genRectangle(Point2D nw, Point2D se) {
        EqPointDouble a = new EqPointDouble();
        EqPointDouble b = new EqPointDouble();

        if (nw.getX() == se.getX() || nw.getY() == se.getY()) {
//            throw new IllegalArgumentException("" + nw + se);
        }
        if (nw.getX() < se.getX()) {
            a.x = nw.getX();
            b.x = se.getX();
        } else {
            a.x = se.getX();
            b.x = nw.getX();
        }
        if (nw.getY() < se.getY()) {
            a.y = nw.getY();
            b.y = se.getY();
        } else {
            a.y = se.getY();
            b.y = nw.getY();
        }
        double w = b.getX() - a.getX();
        double h = b.getY() - a.getY();

        return new EnhRectangle2D(a.x, a.y, w, h);
    }

    public void makeSquare(int corner) {
        makeSquare(corner, false);
    }

    public void makeSquare(int corner, boolean max) {
        double n = max ? Math.max(width, height) : Math.min(width, height);
        switch (corner) {
            case NW:

                x = (x + width) - n;

                y = (y + height) - n;

                width = n;

                height = n;

                break;
            case NE:

                width = n;

                y = (y + height) - n;

                height = n;

                break;
            case SW:

                width = n;

                height = n;

                break;
            case SE:

                x = (x + width) - n;

                width = n;

                height = n;

                break;
        }
    }

    /**
     * Get a point representing the requested corner
     */
    public Point2D getPoint(int which) {
        EqPointDouble result = new EqPointDouble(x, y);

        switch (which) {
            case NW:

                break;
            case NE:

                result.x += width;

                break;
            case SE:

                result.y += height;

                break;
            case SW:

                result.x += width;

                result.y += height;

                break;
            default:

                throw new IllegalArgumentException();
        }
        return result;
    }

    public int nearestCorner(double x, double y) {
        return nearestCorner(new EqPointDouble(x, y));
    }

    /**
     * Get the nearest corner to the passed point
     */
    public int nearestCorner(Point2D p) {
        int best = 0;
        double bestDistance = Integer.MAX_VALUE;

        for (int i = NE; i <= SW; i++) {
            Point2D check = getPoint(i);
            double dist = check.distance(p);

            if (dist < bestDistance) {
                bestDistance = dist;
                best = i;
            }
        }
        return best;
    }

    /**
     * Set one of the corners to a new location.
     *
     * @param p The location
     * @param which ID of the corner to set
     * @return -1 if nothing has changed (the corner to set to the new point
     * already had those coordinates); -2 if the corner was moved, but the
     * transform did not cause the corner to become logically a different
     * corner; a corner ID, != which, that is now the corner being moved. i.e.
     * if you drag the southeast corner above the northeast corner, you are now
     * dragging the northeast corner.
     */
    public int setPoint(Point2D p, int which) {
        double newX;
        double newY;
        double newW;
        double newH;
        int result = -1;

        switch (which) {
            case NW:

                newW = width + (x - p.getX());

                newH = height + (y - p.getY());

                if (changeBounds(p.getX(), p.getY(), newW, newH)) {
                    if (newW <= 0 && newH > 0) {
                        result = NE;
                    } else if (newW <= 0 && newH <= 0) {
                        result = SE;
                    } else if (newW > 0 && newH <= 0) {
                        result = SE;
                    } else {
                        result = -2;
                    }
                }

                break;
            case NE:

                newW = p.getX() - x;

                newY = p.getY();

                newH = height + (y - p.getY());

                if (changeBounds(x, newY, newW, newH)) {
                    if (newW <= 0 && newH > 0) {
                        result = NW;
                    } else if (newW <= 0 && newH <= 0) {
                        result = SE;
                    } else if (newW > 0 && newH <= 0) {
                        result = SW;
                    } else {
                        result = -2;
                    }
                }

                break;
            case SW:

                newW = p.getX() - x;

                newH = p.getY() - y;

                if (changeBounds(x, y, newW, newH)) {
                    if (newW <= 0 && newH > 0) {
                        result = SE;
                    } else if (newW <= 0 && newH <= 0) {
                        result = NW;
                    } else if (newW > 0 && newH <= 0) {
                        result = NE;
                    } else {
                        result = -2;
                    }
                }

                break;
            case SE:

                newX = p.getX();

                newW = width + (x - p.getX());

                newH = p.getY() - y;

                if (changeBounds(newX, y, newW, newH)) {
                    if (newW <= 0 && newH > 0) {
                        result = SW;
                    } else if (newW <= 0 && newH <= 0) {
                        result = NE;
                    } else if (newW > 0 && newH <= 0) {
                        result = NW;
                    } else {
                        result = -2;
                    }
                }

                break;
            default:

                assert false : "Bad corner: " + which;
        }
        return result;
    }

    /**
     * Set the bounds, returning true if an actual change occurred
     */
    private boolean changeBounds(double x, double y, double w, double h) {
        boolean change = x != this.x || y != this.y || w != this.width
                || h != this.height;

        if (change) {
            setFrame(x, y, w, h);
        }
        return change;
    }

    /**
     * Overridden to convert negative width/height into relocation with positive
     * width/height
     */
    @Override
    public void setFrame(double x, double y, double w, double h) {
        if (w < 0) {
            double newW = -w;

            x += w;
            w = newW;
        }
        if (h < 0) {
            double newH = -h;

            y += h;
            h = newH;
        }
        super.setFrame(x, y, w, h);
    }
}
