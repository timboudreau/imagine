/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api;

import com.mastfrog.util.collections.DoubleMap;
import com.mastfrog.util.collections.DoubleSet;
import com.mastfrog.util.search.Bias;
import java.awt.BasicStroke;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleConsumer;

/**
 *
 * @author Tim Boudreau
 */
final class ZoomPrivate {

    static final DoubleSet FIXED_ZOOMS = DoubleSet.ofDoubles(0.025, 0.05, 0.1,
            0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1, 1.125, 1.25, 1.5,
            1.75, 2, 2.5, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30);

    static void fixedZoomLevels(DoubleConsumer dc) {
        FIXED_ZOOMS.forEachDouble(dc);
    }

    static boolean hasPreviousZoom(double val) {
        int ix = FIXED_ZOOMS.nearestIndexTo(val, Bias.BACKWARD);
        return ix > 0;
    }

    static boolean hasNextZoom(double val) {
        return FIXED_ZOOMS.nearestIndexTo(val, Bias.FORWARD) < FIXED_ZOOMS.size() - 1;
    }

    static double nearestZoom(double val) {
        return FIXED_ZOOMS.getAsDouble(FIXED_ZOOMS.nearestIndexTo(val, Bias.NEAREST));
    }

    private static final NumberFormat PERCENTAGE = DecimalFormat.getPercentInstance();

    static String zoomToString(double val) {
        return PERCENTAGE.format(val);
    }

    static final DoubleMap<Map<BasicStroke, BasicStroke>> SCALED_STROKES = DoubleMap.create(5);

    static BasicStroke inverseScaledStroke(Zoom zoom, BasicStroke stroke) {
        double z = zoom.getZoom();
        Map<BasicStroke, BasicStroke> map = SCALED_STROKES.get(z);
        if (map == null) {
            map = new HashMap<>();
            SCALED_STROKES.put(z, map);
        }
        BasicStroke result = map.get(stroke);
        if (result == null) {
            double inv = 1D / z;
            float w = (float) (inv * stroke.getLineWidth());
            float m = (float) Math.max(1, inv * stroke.getMiterLimit());
            float p = stroke.getDashPhase();
            float[] dashes = stroke.getDashArray();
            if (dashes != null) {
                for (int i = 0; i < dashes.length; i++) {
                    dashes[i] = (float) (inv * dashes[i]);
                }
            }
            result = new BasicStroke(w, stroke.getEndCap(), stroke.getLineJoin(),
                    m, dashes, p);
            map.put(stroke, result);
        }
        return result;
    }

    static double nextZoom(double val) {
        if (val <= 0) {
            return FIXED_ZOOMS.getAsDouble(0);
        }
        int ix = FIXED_ZOOMS.nearestIndexTo(val, Bias.FORWARD);
        if (ix < 0) {
            return val * 2;
        }
        double actual = FIXED_ZOOMS.getAsDouble(ix);
        if (actual == val || Math.abs(val - actual) < 0.01) {
            int next = ix + 1;
            if (next < FIXED_ZOOMS.size()) {
                return FIXED_ZOOMS.getAsDouble(next);
            } else {
                return val * 2;
            }
        } else {
            return actual;
        }
    }

    static double prevZoom(double val) {
        if (val <= 0) {
            return 1;
        }
        int ix = FIXED_ZOOMS.nearestIndexTo(val, Bias.BACKWARD);
        if (ix < 0) {
            return val / 2;
        }
        double actual = FIXED_ZOOMS.getAsDouble(ix);
        if (actual == val || Math.abs(actual - val) < 0.01) {
            if (ix == 0) {
                return val / 2;
            } else {
                int prev = ix - 1;
                return FIXED_ZOOMS.getAsDouble(prev);
            }
        } else {
            return actual;
        }
    }
}
