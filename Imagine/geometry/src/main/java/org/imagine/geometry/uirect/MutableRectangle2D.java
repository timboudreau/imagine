package org.imagine.geometry.uirect;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;

/**
 * A rectangle which keeps an always-positive width and height, and which can be
 * adjusted by changing the location of any corner (and if the transformation by
 * adjusting a corner changes the logical corner being dragged, it will say so).
 * Note that the implementation of setFrame() behaves differently than ordinary
 * rectangles and should be used with care.
 */
public final class MutableRectangle2D extends EnhRectangle2D {

    public static final int NONE = -2;
    public static final int ANY = 0;
    public static final int NE = 1;
    public static final int SE = 2;
    public static final int NW = 3;
    public static final int SW = 4;

    public static final int NORTH = 5;
    public static final int EAST = 6;
    public static final int SOUTH = 7;
    public static final int WEST = 8;

    public MutableRectangle2D() {
        super();
    }

    /**
     * This is NOT a width + height constructor!
     *
     * @param x1 The first diagonal x coordinate
     * @param y1 The first diagonal y coordinate
     * @param x2 The second diagonal x coordinate
     * @param y2 The seconddiagonal y coordinate
     */
    public MutableRectangle2D(double x1, double y1, double x2, double y2) {
        super(genRectangle(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2)));
    }

    public MutableRectangle2D(Point2D nw, Point2D se) {
        super(genRectangle(nw, se));
    }

    public MutableRectangle2D(RectangularShape shape) {
        this(shape.getMinX(), shape.getMinY(), shape.getMaxX(), shape.getMaxY());
    }

    private MutableRectangle2D(boolean reg, double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    /**
     * Create a MutableRectangle2D using traditional x/y/width/height.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param w The width
     * @param h The height
     * @return A rectangle
     */
    public static MutableRectangle2D of(double x, double y, double w, double h) {
        return new MutableRectangle2D(true, x, y, w, h);
    }

    public static MutableRectangle2D ofDiagonal(double x1, double y1, double x2, double y2) {
        return new MutableRectangle2D(x1, y1, x2, y2);
    }

    public static MutableRectangle2D ofDiagonal(Point2D a, Point2D b) {
        return new MutableRectangle2D(a, b);
    }

    /**
     * Grow the bounds of this rectangle outward from the center by the
     * specified amount, making each edge the passed value / 2 for that axis
     * more distant from the center point than it was.
     *
     * @param xOff
     * @param yOff
     * @return
     */
    public MutableRectangle2D growBy(double xOff, double yOff) {
        double halfX = xOff / 2;
        double halfY = yOff / 2;
        x -= halfX;
        width += xOff;
        y -= halfY;
        height += yOff;
        if (xOff < 0 || yOff < 0) {
            renormalize();
        }
        return this;
    }

    private void renormalize() {
        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }
    }

    public boolean isEmpty() {
        renormalize();
        return super.isEmpty();
    }

    /**
     * Get the edge nearest to the passed point.
     *
     * @param pt A point
     * @return The constant signifying the nearest edge
     */
    public int nearestEdge(Point2D pt) {
        EqLine ln = new EqLine();
        int best = NONE;
        double bestDist = java.lang.Double.MAX_VALUE;
        for (int edge : new int[]{NORTH, SOUTH, EAST, WEST}) {
            getEdge(edge, ln);
            double coord, testCoord;
            switch (edge) {
                case NORTH:
                    coord = y;
                    testCoord = pt.getY();
                    break;
                case SOUTH:
                    coord = y + height;
                    testCoord = pt.getY();
                    break;
                case EAST:
                    coord = x + width;
                    testCoord = pt.getX();
                    break;
                case WEST:
                    coord = x;
                    testCoord = pt.getX();
                    break;
                default:
                    throw new AssertionError(edge);
            }
            double dist = Math.abs(coord - testCoord);
            if (dist < bestDist) {
                best = edge;
                bestDist = dist;
            }
        }
        return best;
    }

    public int nearestEdgeMidpoint(Point2D pt) {
        EqLine ln = new EqLine();
        int best = NONE;
        double bestDist = java.lang.Double.MAX_VALUE;
        for (int i : new int[]{NORTH, SOUTH, EAST, WEST}) {
            getEdge(i, ln);
            double dist = ln.midPoint().distance(pt);
            if (dist < bestDist) {
                best = i;
                bestDist = dist;
            }
        }
        return best;
    }

    /**
     * Get the edge or corner nearest to the passed point, whichever is nearer.
     *
     * @param pt A point
     * @return A constant signifying an edge or a corner
     */
    public int nearestFeature(Point2D pt) {
        EqLine ln = new EqLine();
        int best = NONE;
        double bestDist = java.lang.Double.MAX_VALUE;
        for (int i : new int[]{NORTH, SOUTH, EAST, WEST}) {
            getEdge(i, ln);
            double dist = ln.midPoint().distance(pt);
            if (dist < bestDist) {
                best = i;
                bestDist = dist;
            }
        }
        int corner = nearestCorner(pt);
        Point2D cornerPoint = getPoint(corner);
        if (pt.distance(cornerPoint) < bestDist) {
            best = corner;
        }
        return best;
    }

    /**
     * Fetch the coordinates of one edge into the passed Line2D.
     *
     * @param which The edge constant, one of NORTH, SOUTH, EAST or WEST
     * @param into The line which should have its coordinates replaced
     */
    public void getEdge(int which, Line2D into) {
        switch (which) {
            case NORTH:
                into.setLine(x, y, x + width, y);
                break;
            case SOUTH:
                into.setLine(x, y + height, x + width, y + height);
                break;
            case EAST:
                into.setLine(x + width, y, x + width, y + height);
                break;
            case WEST:
                into.setLine(x, y, x, y + height);
                break;
            default:
                throw new IllegalArgumentException("Not an edge constant: " + which);
        }
    }

    /**
     * For logging purposes, get a string describing a corner or edge constant.
     *
     * @param cornerOrEdge
     * @return a String (not localized)
     */
    public static String cornerOrEdgeString(int cornerOrEdge) {
        switch (cornerOrEdge) {
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
            case NORTH:
                return "North";
            case SOUTH:
                return "South";
            case EAST:
                return "East";
            case WEST:
                return "West";
            default:
                return "Unknown(" + cornerOrEdge + ")";

        }
    }

    /**
     * Given two points, generate a rectangle with positive width and height
     */
    private static EnhRectangle2D genRectangle(Point2D nw, Point2D se) {
        EqPointDouble a = new EqPointDouble();
        EqPointDouble b = new EqPointDouble();
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

    /**
     * Equivalent of <code>makeSquare(corner, false)</code>.
     *
     * @param corner A corner constant
     */
    public void makeSquare(int corner) {
        makeSquare(corner, false);
    }

    /**
     * Make this rectangle into a square which retains the passed corner's
     * location as one of its corners.
     *
     * @param corner A corner constant
     * @param max Use the maximum, rather than minimum, dimension size for the
     * resulting square
     */
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
            default:
                throw new IllegalArgumentException("Not a corner constant: " + corner);
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
            case SW:

                result.y += height;

                break;
            case SE:

                result.x += width;

                result.y += height;

                break;
            default:

                throw new IllegalArgumentException("Not a corner constant: " + which);
        }
        return result;
    }

    /**
     * Get the corner nearest the passed coordinates.
     *
     * @param x An x coordinate
     * @param y A y coordinate
     * @return A corner constant
     */
    public int nearestCorner(double x, double y) {
        return nearestCorner(new EqPointDouble(x, y));
    }

    /**
     * Get the nearest corner to the passed point.
     *
     * @param p A point
     * @return A corner constant
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
            case SE:

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
            case SW:

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
     * Set the bounds, returning true if an actual change occurred.
     *
     * @param x the new x
     * @param y the new y
     * @param w the new width
     * @param h the new height
     * @return true if the bounds were changed
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
     * width/height.
     *
     * @param x the new x
     * @param y the new y
     * @param w the new width
     * @param h the new height
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
