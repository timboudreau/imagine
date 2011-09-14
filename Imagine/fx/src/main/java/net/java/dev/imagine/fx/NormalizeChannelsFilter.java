package net.java.dev.imagine.fx;

import com.jhlabs.image.WholeImageFilter;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 *
 * @author tim
 */
public class NormalizeChannelsFilter extends WholeImageFilter {

    @Override
    protected int[] filterPixels(int x, int y, int[] ints, Rectangle rctngl) {
        int[] mins = new int[4];
        Arrays.fill(mins, 255);
        int[] maxes = new int[4];
        for (int i = 0; i < ints.length; i++) {
            int rgb = ints[i];
            int a = rgb & 0xff000000;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            mins(r, g, b, a, mins);
            maxes(r, g, b, a, maxes);
        }
        for (int i = 0; i < ints.length; i++) {
            int rgb = ints[i];
            int a = rgb & 0xff000000;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            r = scale(r, mins, maxes, 0);
            g = scale(g, mins, maxes, 1);
            b = scale(b, mins, maxes, 2);
            ints[i] = a | (r << 16) | (g << 8) | b;
        }
        return ints;
    }
    
    public String toString() {
        return "Normalize Channels";
    }

    private void maxes(int r, int g, int b, int a, int[] maxes) {
        max(r, maxes, 0);
        max(g, maxes, 1);
        max(b, maxes, 2);
        max(a, maxes, 3);
    }

    private void mins(int r, int g, int b, int a, int[] mins) {
        min(r, mins, 0);
        min(g, mins, 1);
        min(b, mins, 2);
        min(a, mins, 3);
    }

    private void min(int val, int[] mins, int offset) {
        mins[offset] = Math.min(val, mins[offset]);
    }

    private void max(int val, int[] maxes, int offset) {
        maxes[offset] = Math.max(val, maxes[offset]);
    }

    private int scale(double val, int[] mins, int[] maxes, int offset) {
        double range = maxes[offset] - mins[offset];
        if (range == 0) {
            return (int) val;
        }
        double factor = 255D / range;
        return (int) (val * factor);
    }
}
