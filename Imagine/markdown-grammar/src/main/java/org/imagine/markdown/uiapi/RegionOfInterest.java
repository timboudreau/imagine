package org.imagine.markdown.uiapi;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Tim Boudreau
 */
interface RegionOfInterest extends Comparable<RegionOfInterest> {

    String content();

    Shape region();

    Kind kind();

    default boolean contains(double x, double y) {
        return region().contains(x, y);
    }

    default Rectangle2D bounds() {
        Shape region = region();
        if (region instanceof Rectangle2D) {
            return (Rectangle2D) region;
        }
        return region.getBounds2D();
    }

    public enum Kind {
        LINK,
        IMAGE_ALTERNATIVE_TEXT
    }

    @Override
    default int compareTo(RegionOfInterest o) {
        if (o == null) {
            return -1;
        } else if (o == this) {
            return 0;
        }
        Rectangle2D myBounds = bounds();
        Rectangle2D otherBounds = o.bounds();
        double mmx = myBounds.getMinX();
        double mmy = myBounds.getMinY();
        double omx = otherBounds.getMinX();
        double omy = otherBounds.getMinY();
        int result = Double.compare(mmy, omy);
        if (result == 0) {
            result = Double.compare(mmx, omx);
            if (result == 0) {
                mmx = myBounds.getMaxX();
                mmy = myBounds.getMaxY();
                omx = otherBounds.getMaxX();
                omy = otherBounds.getMaxY();
                result = Double.compare(mmy, omy);
                if (result == 0) {
                    result = Double.compare(mmx, omx);
                }
            }
        }
        return result;
    }
}
