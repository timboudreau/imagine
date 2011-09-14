package org.netbeans.paint.api.splines;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
public class ShapeUtils {
    
    public static boolean shapesEqual(Shape a, Shape b, boolean normalize) {
        if (a instanceof Rectangle && b instanceof Rectangle) {
            return a.equals(b);
        }
        Rectangle2D ax = a.getBounds2D();
        Rectangle2D bx = b.getBounds2D();
        if (ax.getX() != bx.getX() && normalize) {
            double offX = bx.getX() - ax.getX();
            double offY = bx.getY() - ax.getY();
            b = AffineTransform.getTranslateInstance(-offX, -offY).createTransformedShape(b);
        }
        bx = b.getBounds2D();
        if (!bx.equals(ax)) {
            return false;
        }
        return toEntries(a).equals(toEntries(b));
    }

    private static List<Entry> toEntries(Shape shape) {
        double[] d = new double[6];
        List<Entry> entries = new LinkedList<Entry>();
        PathIterator iter = shape.getPathIterator(AffineTransform.getTranslateInstance(0, 0));
        while (!iter.isDone()) {
            int op = iter.currentSegment(d);
            switch (op) {
                case PathIterator.SEG_MOVETO:
                    entries.add(new MoveTo(d[0], d[1]));
                    break;
                case PathIterator.SEG_CUBICTO:
                    entries.add(new CurveTo(d[0], d[1], d[2], d[3], d[4], d[5]));
                    break;
                case PathIterator.SEG_LINETO:
                    entries.add(new LineTo(d[0], d[1]));
                    break;
                case PathIterator.SEG_QUADTO:
                    entries.add(new QuadTo(d[0], d[1], d[2], d[3]));
                    break;
                case PathIterator.SEG_CLOSE:
//                    if (!entries.isEmpty() && entries.get(0) instanceof MoveTo) {
//                        MoveTo mt = (MoveTo) entries.get(0);
//                        entries.add (new LineTo (mt.x, mt.y));
//                    }
                    entries.add(new Close());
                    break;
                default:
                    throw new AssertionError("Not a PathIterator segment type: " + op);
            }
            iter.next();
        }
        return entries;
    }
}
