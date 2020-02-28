/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.function.BiFunction;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "UNION=Union",
    "INTERSECTION=Intersection",
    "SUBTRACT=Subtract",
    "XOR=Non-Overlap"
})
public enum CSGOperation {

    UNION,
    INTERSECTION,
    SUBTRACT,
    XOR;

    @Override
    public String toString() {
        return NbBundle.getMessage(CSGOperation.class, name());
    }

    public int maxMembers() {
        switch (this) {
            case SUBTRACT:
            case XOR:
                return 2;
            default:
                return Integer.MAX_VALUE;
        }
    }

    private Shape ensureSane(Shape initial, Shape[] others, BiFunction<Shape, Shape[], Shape> c) {
        switch (this) {
            case SUBTRACT:
                // Avoid ever removing all points
                Rectangle2D initialBounds = initial.getBounds2D();
                int useAsInitial = -1;
                for (int i = 0; i < others.length; i++) {
                    Shape shape = others[i];
                    Rectangle2D shapeBounds = shape.getBounds();
                    if (shapeBounds.contains(initialBounds)) {
                        initialBounds = shapeBounds;
                        useAsInitial = i;
                    }
                }
                if (useAsInitial != -1) {
                    Shape use = others[useAsInitial];
                    others[useAsInitial] = initial;
                    initial = use;
                }
                return c.apply(initial, others);
            case INTERSECTION:
                boolean noIntersection = true;
                Rectangle2D initialBounds2 = initial.getBounds2D();
                for (Shape s : others) {
                    if (s.intersects(initialBounds2)) {
                        noIntersection = false;
                        break;
                    }
                }
                if (noIntersection) {
                    return initial;
                } else {
                    return c.apply(initial, others);
                }
            default:
                return c.apply(initial, others);
        }
    }

    public Shape apply(Shape origInitial, Shape... origOthers) {
        return ensureSane(origInitial, origOthers, (initial, others) -> {
            if (others.length == 0) {
                return initial;
            }
            Area a = initial instanceof Area ? (Area) initial : new Area(initial);
            if (others.length + 1 > maxMembers()) {
//            throw new IllegalArgumentException("Can only apply " + this + " to "
//                    + maxMembers() + " shapes at a time, but got "
//                    + (others.length + 1));
            }
            for (Shape o : others) {
                applyToAreas(a, o);
            }
            return a;
        });
    }

    private void applyToAreas(Area a, Shape b) {
        switch (this) {
            case INTERSECTION:
                a.intersect(new Area(b));
                break;
            case SUBTRACT:
                a.subtract(new Area(b));
                break;
            case UNION:
                a.add(new Area(b));
                break;
            case XOR:
                a.exclusiveOr(new Area(b));
                break;
            default:
                throw new AssertionError(this);
        }
    }
}
