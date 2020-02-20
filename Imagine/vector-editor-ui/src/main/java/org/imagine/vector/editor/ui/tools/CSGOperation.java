/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.Shape;
import java.awt.geom.Area;
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

    public Shape apply(Shape initial, Shape... others) {
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
