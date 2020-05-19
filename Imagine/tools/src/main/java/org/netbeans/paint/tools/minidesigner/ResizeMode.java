package org.netbeans.paint.tools.minidesigner;

import java.awt.Cursor;
import java.awt.geom.Point2D;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.geometry.EqPointDouble;
import org.netbeans.paint.api.cursor.Cursors;
import org.netbeans.paint.tools.MutableRectangle2D;

/**
 *
 * @author Tim Boudreau
 */
public enum ResizeMode {
    TOP_EDGE, LEFT_EDGE, RIGHT_EDGE, BOTTOM_EDGE, NORTHWEST, NORTHEAST, SOUTHEAST, SOUTHWEST;

    public Cursor cursor() {
        Cursors cur = ImageEditorBackground.getDefault().style().isBright()
                ? Cursors.forBrightBackgrounds() : Cursors.forDarkBackgrounds();
        switch (this) {
            case NORTHEAST:
                return cur.southWestNorthEast();
            case SOUTHWEST:
                System.out.println("A " + this);
                return cur.southEastNorthWest();
            case SOUTHEAST:
                return cur.southWestNorthEast();
            case NORTHWEST:
                System.out.println("B " + this);
                return cur.southEastNorthWest();
            case BOTTOM_EDGE:
            case TOP_EDGE:
                return cur.vertical();
            default:
                return cur.horizontal();
        }
    }

    public static ResizeMode forCornerConst(int c) {
        switch (c) {
            case MutableRectangle2D.NW:
                return NORTHWEST;
            case MutableRectangle2D.SE:
                return SOUTHEAST;
            case MutableRectangle2D.SW:
                return SOUTHWEST;
            default:
                return NORTHEAST;
        }
    }

    public int cornerConst() {
        switch (this) {
            case NORTHEAST:
                return MutableRectangle2D.NE;
            case NORTHWEST:
                return MutableRectangle2D.NW;
            case SOUTHEAST:
                return MutableRectangle2D.SE;
            case SOUTHWEST:
                return MutableRectangle2D.SW;
            default:
                return MutableRectangle2D.ANY;
        }
    }

    public ResizeMode apply(double x, double y, MutableRectangle2D rect) {
        switch (this) {
            case NORTHWEST:
            case SOUTHEAST:
            case SOUTHWEST:
            case NORTHEAST:
                int cc = cornerConst();
                EqPointDouble pt = new EqPointDouble(x, y);
                rect.setPoint(pt, cc);
                return forCornerConst(rect.nearestCorner(pt));
            case LEFT_EDGE:
                double off = x - rect.x;
                rect.x = x;
                rect.width -= off;
                if (rect.width < 0) {
                    rect.x += rect.width;
                    rect.width = -rect.width;
                    return RIGHT_EDGE;
                }
                break;
            case RIGHT_EDGE:
                double off1 = (rect.x + rect.width) - x;
                rect.width -= off1;
                if (rect.width < 0) {
                    rect.x += rect.width;
                    rect.width = -rect.width;
                    return LEFT_EDGE;
                }
                break;
            case TOP_EDGE:
                double off2 = y - rect.y;
                rect.y = y;
                rect.height -= off2;
                if (rect.height < 0) {
                    rect.y += rect.height;
                    rect.height = -rect.height;
                    return BOTTOM_EDGE;
                }
                break;
            case BOTTOM_EDGE:
                double off3 = (rect.y + rect.height) - y;
                rect.height -= off3;
                if (rect.height < 0) {
                    rect.y += rect.height;
                    rect.height = -rect.height;
                    return TOP_EDGE;
                }
                break;
            default:
                System.out.println("Huh? " + this);
        }
        return this;
    }

    public static ResizeMode forRect(double x, double y, double hitRadius, MutableRectangle2D rect) {
        int corn = rect.nearestCorner(x, y);
        hitRadius *= 2;
        switch (corn) {
            case MutableRectangle2D.ANY:
            case MutableRectangle2D.NONE:
                break;
            default:
                Point2D pt = rect.getPoint(corn);
                double dist = pt.distance(x, y);
                if (dist <= hitRadius) {
                    switch (corn) {
                        case MutableRectangle2D.NE:
                            System.out.println("nearest to " + x + ", " + y + " is NE in " + rect);
                            return NORTHEAST;
                        case MutableRectangle2D.NW:
                            System.out.println("nearest to " + x + ", " + y + " is NW in " + rect);
                            return NORTHWEST;
                        case MutableRectangle2D.SE:
                            System.out.println("nearest to " + x + ", " + y + " is SE in " + rect);
                            return SOUTHEAST;
                        case MutableRectangle2D.SW:
                            System.out.println("nearest to " + x + ", " + y + " is SW in " + rect);
                            return SOUTHWEST;
                        default:
                            throw new AssertionError("Corner " + corn);
                    }
                }
        }
        if (Math.abs(x - rect.x) < hitRadius) {
            return LEFT_EDGE;
        } else if (Math.abs(x - (rect.x + rect.width)) < hitRadius) {
            return RIGHT_EDGE;
        } else if (Math.abs(y - rect.y) < hitRadius) {
            return TOP_EDGE;
        } else if (Math.abs(y - (rect.y + rect.height)) < hitRadius) {
            return BOTTOM_EDGE;
        }
        return null;
    }

}
