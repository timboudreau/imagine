package org.netbeans.paint.tools.spi;

import java.awt.Shape;
import java.util.List;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.geometry.EqPointDouble;

/**
 * Pluggable shape creator that is passed a set of points and can interpolate
 * points between them to create a shape. If it implements CustomizerProvider
 * the customizer will be shown.
 *
 * @author Tim Boudreau
 */
public interface PathCreator {
    public static final String REGISTRATION_PATH = "pathcreators";

    Shape create(boolean close, boolean commit, List<EqPointDouble> points, EqPointDouble nextProposal);

    /**
     * Clear any old state from this path creator.
     */
    default void reset() {

    }

    /**
     * Path creators may be recreated at any time - to keep data associated with a particular
     * image being edited, it may be useful to weakly cache models mapped to surface
     * instances.
     *
     * @param surface
     */
    default void init(Surface surface) {
        
    }

    default EqPointDouble acceptPoint(EqPointDouble proposal, List<EqPointDouble> into) {
        return proposal;
    }

    default void finish(List<EqPointDouble> points) {
        if (!points.isEmpty() && points.size() > 1 && !points.get(0).equals(points.get(points.size() - 1))) {
            points.add(points.get(0));
        }
    }

    default Shape proposedAddition(List<EqPointDouble> points, EqPointDouble nextProposal) {
        List<EqPointDouble> sub;
        switch (points.size()) {
            case 0:
                return null;
            case 1:
                sub = points;
                break;
            default:
                sub = points.subList(points.size() - 1, points.size());
                break;
        }
        return create(false, false, sub, nextProposal);
    }
}
